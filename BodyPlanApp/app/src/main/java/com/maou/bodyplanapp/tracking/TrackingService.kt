package com.maou.bodyplanapp.tracking

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.maou.bodyplanapp.MainActivity
import com.maou.bodyplanapp.R
import com.maou.bodyplanapp.data.RoutePoint
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackingService : Service(), LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val changes = Mutex()
    private var current: LocalRun? = null
    private var lastTick = 0L
    private var lastPersist = 0L
    private val locations by lazy { getSystemService(LocationManager::class.java) }
    private var timer: Job? = null
    private var listening = false
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        scope.launch {
            changes.withLock {
                try {
                    when (action) {
                        START -> {
                            check(current?.active != true) { "已有进行中的运动" }
                            val run = Gson().fromJson(intent.getStringExtra("run"), LocalRun::class.java)
                            check(run.userId > 0 && run.active)
                            RunRepository.bind(this@TrackingService, run.userId)
                            check(RunRepository.sessions.value.none { it.active && it.clientRecordId != run.clientRecordId }) { "请先结束上一场运动" }
                            current = run
                            liveClientId = run.clientRecordId
                            foreground(run)
                            RunRepository.save(this@TrackingService, run)
                            if (run.outdoor) listen()
                            lastTick = SystemClock.elapsedRealtime()
                            startTimer()
                        }
                        PAUSE -> current?.let { run ->
                            if (run.status == "RUNNING") {
                                val next = advance(run).copy(status = "PAUSED", segment = run.segment + 1, signal = "运动已暂停")
                                current = next
                                RunRepository.save(this@TrackingService, next)
                                stopLocation()
                                foreground(next)
                            }
                        }
                        RESUME -> current?.let { run ->
                            if (run.status == "PAUSED") {
                                val next = run.copy(status = "RUNNING", signal = if (run.outdoor) "等待定位" else "正在计时")
                                foreground(next)
                                if (next.outdoor) listen()
                                lastTick = SystemClock.elapsedRealtime()
                                current = next
                                RunRepository.save(this@TrackingService, next)
                            }
                        }
                        FINISH -> finish()
                        DISCARD -> current?.let {
                            RunRepository.discard(this@TrackingService, it.clientRecordId, it.userId)
                            stop()
                        }
                    }
                } catch (exception: Exception) {
                    Log.e("BodyPlan", "scenario=sport-service action=$action userId=${current?.userId} clientRecordId=${current?.clientRecordId}", exception)
                    RunRepository.problem.value = "运动记录已暂停，请检查定位权限或存储空间后重试"
                    val paused = current?.takeIf { it.active }?.copy(status = "PAUSED", segment = (current?.segment ?: 0) + 1)
                    if (paused != null) {
                        current = paused
                        try { RunRepository.save(this@TrackingService, paused) }
                        catch (persistError: Exception) {
                            Log.e("BodyPlan", "scenario=sport-emergency-save userId=${paused.userId} clientRecordId=${paused.clientRecordId}", persistError)
                        }
                    }
                    stop()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun advance(run: LocalRun): LocalRun {
        val now = SystemClock.elapsedRealtime()
        val delta = if (run.status == "RUNNING" && lastTick > 0) (now - lastTick).coerceAtLeast(0) else 0
        lastTick = now
        return run.copy(durationMs = run.durationMs + delta)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = scope.launch {
            while (isActive) {
                delay(1000)
                changes.withLock {
                    try {
                        val run = current ?: return@withLock
                        var next = advance(run)
                        val lastPointAt = next.points.lastOrNull()?.timestampMs ?: next.startedAt
                        if (next.status == "RUNNING" && next.outdoor && System.currentTimeMillis() - lastPointAt > 15000)
                            next = next.copy(signal = "GPS 信号弱，等待定位")
                        current = next
                        RunRepository.tick(next)
                        if (SystemClock.elapsedRealtime() - lastPersist >= 5000) {
                            RunRepository.save(this@TrackingService, next)
                            lastPersist = SystemClock.elapsedRealtime()
                            foreground(next)
                        }
                        if (next.durationMs >= 86400000L) finish()
                    } catch (exception: Exception) {
                        Log.e("BodyPlan", "scenario=sport-timer userId=${current?.userId} clientRecordId=${current?.clientRecordId}", exception)
                        RunRepository.problem.value = "运动保存异常，请尽快暂停并检查存储空间"
                        stopLocation()
                        current = current?.copy(status = "PAUSED", signal = "本地存储异常")
                        current?.let { RunRepository.tick(it) }
                        stop()
                    }
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        scope.launch {
            changes.withLock {
                try {
                    val run = current?.takeIf { it.status == "RUNNING" && it.outdoor } ?: return@withLock
                    if (!location.hasAccuracy() || location.accuracy > 50 || !location.latitude.isFinite() || !location.longitude.isFinite()) {
                        current = run.copy(signal = "GPS 精度不足，等待定位")
                        RunRepository.tick(current!!)
                        return@withLock
                    }
                    if ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000 > 15000) {
                        Log.w("BodyPlan", "scenario=sport-location userId=${run.userId} clientRecordId=${run.clientRecordId} reason=stale-location")
                        return@withLock
                    }
                    val now = System.currentTimeMillis()
                    val previous = run.points.lastOrNull()
                    if (previous != null && now - previous.timestampMs < 1500) return@withLock
                    if (run.points.size >= 30000) {
                        Log.w("BodyPlan", "scenario=sport-location userId=${run.userId} clientRecordId=${run.clientRecordId} reason=point-limit")
                        finish()
                        return@withLock
                    }
                    var segment = run.segment
                    var point = RoutePoint(location.latitude, location.longitude, now, segment, location.accuracy.toDouble())
                    val delta = previous?.let { TrackMath.distance(it, point, run.sportType) } ?: 0.0
                    if (previous != null && previous.segment == segment) {
                        val separation = location.distanceTo(Location("previous").apply { latitude = previous.latitude; longitude = previous.longitude })
                        // 小幅移动保留旧锚点，慢走可以累积到有效距离，而不是每次都从零开始。
                        if (now - previous.timestampMs <= 30000 && separation < 3) {
                            current = run.copy(signal = "GPS · 精度 ${location.accuracy.toInt()} 米")
                            RunRepository.tick(current!!)
                            return@withLock
                        }
                        if (now - previous.timestampMs > 30000 || (separation >= 3 && delta == 0.0)) {
                            segment++
                            point = point.copy(segment = segment)
                            Log.w("BodyPlan", "scenario=sport-location userId=${run.userId} clientRecordId=${run.clientRecordId} reason=gap-or-jump segment=$segment")
                        }
                    }
                    val simulated = run.simulated || (if (Build.VERSION.SDK_INT >= 31) location.isMock else @Suppress("DEPRECATION") location.isFromMockProvider)
                    val next = advance(run).copy(points = run.points + point, distanceM = run.distanceM + delta,
                        segment = segment, signal = "GPS · 精度 ${location.accuracy.toInt()} 米", simulated = simulated)
                    current = next
                    RunRepository.save(this@TrackingService, next)
                    if (next.autoFinish && next.targetDistanceM != null && next.distanceM >= next.targetDistanceM) finish()
                } catch (exception: Exception) {
                    Log.e("BodyPlan", "scenario=sport-location userId=${current?.userId} clientRecordId=${current?.clientRecordId}", exception)
                    RunRepository.problem.value = "定位记录异常，运动已暂停"
                    stopLocation()
                    current = current?.let { it.copy(status = "PAUSED", segment = it.segment + 1, signal = "定位记录异常") }
                    current?.let { RunRepository.tick(it) }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun listen() {
        check(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { "缺少精确定位权限" }
        stopLocation()
        check(locations.isProviderEnabled(LocationManager.GPS_PROVIDER)) { "请开启系统定位服务" }
        locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this)
        listening = true
    }

    private fun stopLocation() {
        if (listening) locations.removeUpdates(this)
        listening = false
    }

    override fun onProviderDisabled(provider: String) {
        Log.w("BodyPlan", "scenario=sport-location userId=${current?.userId} clientRecordId=${current?.clientRecordId} reason=provider-disabled provider=$provider")
        RunRepository.problem.value = "系统定位已关闭，路线暂时无法更新"
    }

    private suspend fun finish() {
        val run = current ?: return
        val next = advance(run)
        check(next.durationSec >= 1) { "运动不足1秒" }
        val finished = next.copy(status = "FINISHED", endedAt = System.currentTimeMillis(), signal = "运动已结束")
        RunRepository.save(this, finished)
        Log.i("BodyPlan", "scenario=sport-finish userId=${run.userId} clientRecordId=${run.clientRecordId} distanceM=${finished.distanceM} durationSec=${finished.durationSec}")
        stop()
    }

    private fun foreground(run: LocalRun) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("sport", "运动记录", NotificationManager.IMPORTANCE_LOW))
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, "sport")
            .setSmallIcon(R.drawable.ic_bodyplan).setContentTitle("${TrackMath.sportName(run.sportType)} · ${if (run.status == "PAUSED") "已暂停" else "记录中"}")
            .setContentText("${TrackMath.clock(run.durationSec)} · ${String.format(java.util.Locale.ROOT, "%.2f", run.distanceM / 1000)} 公里")
            .setContentIntent(pending).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(0, if (run.status == "PAUSED") "继续" else "暂停",
                PendingIntent.getService(this, 72, Intent(this, TrackingService::class.java).setAction(if (run.status == "PAUSED") RESUME else PAUSE),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(0, "结束并保存",
                PendingIntent.getService(this, 73, Intent(this, TrackingService::class.java).setAction(FINISH),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
        val type = when {
            Build.VERSION.SDK_INT >= 29 && run.outdoor -> android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            Build.VERSION.SDK_INT >= 34 -> android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else -> 0
        }
        ServiceCompat.startForeground(this, 71, notification, type)
    }

    private fun stop() {
        timer?.cancel()
        stopLocation()
        current = null
        liveClientId = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopLocation()
        scope.cancel()
        liveClientId = null
        super.onDestroy()
    }

    companion object {
        @Volatile var liveClientId: String? = null
            private set
        const val START = "bodyplan.sport.START"
        const val PAUSE = "bodyplan.sport.PAUSE"
        const val RESUME = "bodyplan.sport.RESUME"
        const val FINISH = "bodyplan.sport.FINISH"
        const val DISCARD = "bodyplan.sport.DISCARD"
        fun command(context: Context, action: String, run: LocalRun? = null) {
            val intent = Intent(context, TrackingService::class.java).setAction(action)
            if (run != null) intent.putExtra("run", Gson().toJson(run))
            try {
                if (action == START) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
            } catch (exception: Exception) {
                Log.e("BodyPlan", "scenario=sport-command userId=${run?.userId} clientRecordId=${run?.clientRecordId} action=$action", exception)
                RunRepository.problem.value = "无法启动运动服务，请检查定位权限后重试"
            }
        }
    }
}
