package com.maou.bodyplanapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.content.ContextCompat
import com.maou.bodyplanapp.data.*
import com.maou.bodyplanapp.tracking.*
import com.maou.bodyplanapp.ui.WellnessViewModel
import com.maou.bodyplanapp.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class SportGoalSettings(val distance: String, val paceEnabled: Boolean, val minutes: String,
                                     val seconds: String, val autoFinish: Boolean, val mode: String)

@Composable
fun SportScreen(vm: WellnessViewModel, userId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessions by RunRepository.sessions.collectAsState()
    val problem by RunRepository.problem.collectAsState()
    var selectedId by rememberSaveable(userId) { mutableStateOf<String?>(null) }
    var history by rememberSaveable(userId) { mutableStateOf(false) }
    var remoteId by rememberSaveable(userId) { mutableStateOf<Long?>(null) }
    var sport by rememberSaveable { mutableStateOf("RUN") }
    var mode by rememberSaveable { mutableStateOf("AEROBIC") }
    var settings by remember { mutableStateOf(false) }
    var targetKm by rememberSaveable { mutableStateOf("") }
    var paceEnabled by rememberSaveable { mutableStateOf(false) }
    var paceMin by rememberSaveable { mutableStateOf("6") }
    var paceSec by rememberSaveable { mutableStateOf("00") }
    var autoFinish by rememberSaveable { mutableStateOf(false) }
    var previousSettings by remember { mutableStateOf<SportGoalSettings?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var confirmEnd by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingResume by remember { mutableStateOf<LocalRun?>(null) }
    val ownerSessions = sessions.filter { it.userId == userId }
    val active = ownerSessions.firstOrNull { it.active }
    val local = ownerSessions.firstOrNull { it.clientRecordId == selectedId }
    val remote = vm.sportDetail?.takeIf { it.summary.id == remoteId }
    val outdoor = sport in listOf("RUN", "WALK", "RIDE")
    val target = targetKm.toDoubleOrNull()?.takeIf { it in 0.1..200.0 }?.let { (it * 1000).toInt() }
    val pace = if (paceEnabled && (paceMin.toIntOrNull() ?: -1) in 2..60 && (paceSec.toIntOrNull() ?: -1) in 0..59)
        paceMin.toInt() * 60 + paceSec.toInt() else null

    suspend fun upload(run: LocalRun) {
        if (uploading) return
        uploading = true
        uploadError = null
        try {
            val saved = vm.uploadSport(run.request())
            RunRepository.save(context, run.copy(status = "UPLOADED", serverId = saved.id, distanceM = saved.distanceM))
            vm.loadSports()
        } catch (exception: kotlinx.coroutines.CancellationException) {
            Log.d("BodyPlan", "scenario=sport-upload userId=$userId clientRecordId=${run.clientRecordId} cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=sport-upload userId=$userId clientRecordId=${run.clientRecordId}", exception)
            uploadError = "已保存在本机，暂未同步成功"
        } finally { uploading = false }
    }

    LaunchedEffect(userId) {
        try {
            RunRepository.bind(context, userId)
            selectedId = RunRepository.sessions.value.firstOrNull { it.userId == userId && it.active }?.clientRecordId
            vm.loadSports()
        } catch (exception: kotlinx.coroutines.CancellationException) {
            Log.d("BodyPlan", "scenario=sport-open userId=$userId cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=sport-open userId=$userId", exception)
            RunRepository.problem.value = "无法读取本地记录，请重试"
        }
    }
    LaunchedEffect(local?.clientRecordId, local?.status) {
        if (local?.status == "FINISHED") upload(local)
    }
    BackHandler(selectedId != null || history || remoteId != null) {
        if (remoteId != null) { remoteId = null; vm.closeSport() }
        else if (selectedId != null) selectedId = null else history = false
    }

    fun begin() {
        if (active != null) { selectedId = active.clientRecordId; return }
        if (outdoor && targetKm.isNotBlank() && target == null) { RunRepository.problem.value = "目标距离需为 0.1~200 公里"; return }
        if (outdoor && paceEnabled && (pace == null || pace > 3600)) { RunRepository.problem.value = "请设置有效目标配速"; return }
        val run = LocalRun(userId, UUID.randomUUID().toString(), sport, if (sport == "STRENGTH") "STRENGTH" else mode,
            if (outdoor) target else null, if (outdoor) pace else null, outdoor && autoFinish && target != null,
            System.currentTimeMillis(), simulated = Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"))
        selectedId = run.clientRecordId
        uploadError = null
        history = false
        TrackingService.command(context, TrackingService.START, run)
    }
    fun resume(run: LocalRun) {
        if (TrackingService.liveClientId == run.clientRecordId) TrackingService.command(context, TrackingService.RESUME)
        else TrackingService.command(context, TrackingService.START, run.copy(status = "RUNNING"))
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val paused = pendingResume
        pendingResume = null
        if (paused != null) {
            if (!paused.outdoor || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) resume(paused)
            else RunRepository.problem.value = "记录路线需要精确定位权限"
        } else if (!outdoor || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) begin()
        else RunRepository.problem.value = "记录路线需要精确定位权限"
    }
    fun startWithPermission() {
        if (active != null) { selectedId = active.clientRecordId; return }
        val needed = mutableListOf<String>()
        if (outdoor && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed.addAll(listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isEmpty()) begin() else permissions.launch(needed.toTypedArray())
    }
    suspend fun stopRecovered(run: LocalRun) {
        try {
            if (TrackingService.liveClientId == run.clientRecordId) TrackingService.command(context, TrackingService.FINISH)
            else RunRepository.save(context, run.copy(status = "FINISHED", endedAt = System.currentTimeMillis()))
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=sport-finish-recovered userId=$userId clientRecordId=${run.clientRecordId}", exception)
            RunRepository.problem.value = "结束保存失败，请重试"
        }
    }
    fun showSettings() {
        previousSettings = SportGoalSettings(targetKm, paceEnabled, paceMin, paceSec, autoFinish, mode)
        settings = true
    }
    fun cancelSettings() {
        previousSettings?.let {
            targetKm = it.distance
            paceEnabled = it.paceEnabled
            paceMin = it.minutes
            paceSec = it.seconds
            autoFinish = it.autoFinish
            mode = it.mode
        }
        settings = false
    }

    Column(Modifier.fillMaxSize()) {
        if (problem != null) Surface(color = MaterialTheme.colorScheme.errorContainer) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(problem!!, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { RunRepository.problem.value = null }) { Icon(Icons.Outlined.Close, "关闭提示") }
            }
        }
        if (remoteId != null) {
            SportSubHeader("运动详情", onBack = { remoteId = null; vm.closeSport() })
            if (remote != null) {
                if (remote.summary.sportType in listOf("RUN", "WALK", "RIDE"))
                    RouteMap(remote.points, "remote-${remote.summary.id}", true, Modifier.weight(1f).fillMaxWidth())
                else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
                }
                SportMetrics(remote.summary.distanceM, remote.summary.durationSec, remote.summary.paceSecPerKm, remote.summary.sportType)
                Text("${TrackMath.sportName(remote.summary.sportType)} · ${sportDate(remote.summary.startedAt)}",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                if (remote.summary.remark != null) Text(remote.summary.remark, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
                if (remote.points.isEmpty() && remote.summary.sportType in listOf("RUN", "WALK", "RIDE"))
                    Text("未获得有效路线", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            } else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (vm.busy) CircularProgressIndicator()
                else { Text("记录暂未加载"); TextButton(onClick = { remoteId?.let(vm::openSport) }) { Text("重试") } }
            }
        } else if (local != null) {
            SportSubHeader("${TrackMath.sportName(local.sportType)} · ${when(local.status) { "PAUSED" -> "已暂停"; "RUNNING" -> "进行中"; else -> "运动完成" }}",
                onBack = { selectedId = null })
            if (local.outdoor) RouteMap(local.points, "${local.clientRecordId}-${!local.active}", !local.active, Modifier.weight(1f).fillMaxWidth())
            else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (local.active) Text(local.signal, Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SportMetrics(local.distanceM, local.durationSec, local.pace, local.sportType)
            if (local.targetDistanceM != null) {
                val progress = (local.distanceM / local.targetDistanceM).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                Text("目标 ${String.format(Locale.ROOT, "%.1f", local.targetDistanceM / 1000.0)} 公里${local.targetPaceSec?.let { " · 目标配速 ${TrackMath.pace(it)}" }.orEmpty()}",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
            } else if (local.targetPaceSec != null) Text("目标配速 ${TrackMath.pace(local.targetPaceSec)}", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
            if (local.simulated) Text("模拟定位记录", Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            if (local.active) Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { confirmDiscard = true }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Outlined.Delete, "放弃本次运动") }
                Button(onClick = {
                    if (local.status == "RUNNING") TrackingService.command(context, TrackingService.PAUSE)
                    else if (local.outdoor && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        pendingResume = local
                        permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    } else resume(local)
                }, modifier = Modifier.weight(1f)) { Text(if (local.status == "RUNNING") "暂停" else "继续运动") }
                FilledTonalButton(onClick = { confirmEnd = true }, enabled = local.durationSec >= 1, modifier = Modifier.weight(1f)) { Text("结束") }
            } else Column(Modifier.fillMaxWidth().padding(16.dp)) {
                if (uploadError != null) Text(uploadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (local.points.isEmpty() && local.outdoor) Text("未获得有效路线，距离记为 0", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (local.status != "UPLOADED") Button(onClick = { scope.launch { upload(local) } }, enabled = !uploading, modifier = Modifier.weight(1f)) {
                        if (uploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Refresh, null)
                        Text(if (uploading) "同步中" else "同步记录")
                    }
                    FilledTonalButton(onClick = { selectedId = null; history = true }, modifier = Modifier.weight(1f)) { Text(if (local.status == "UPLOADED") "完成" else "稍后同步") }
                }
            }
        } else if (history) {
            SportSubHeader("运动记录", onBack = { history = false }, onRefresh = vm::loadSports)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val pending = ownerSessions.filter { it.status == "FINISHED" }
                items(pending.size, key = { "pending-${pending[it].clientRecordId}" }) { index ->
                    val run = pending[index]
                    OutlinedCard(onClick = { selectedId = run.clientRecordId }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("${TrackMath.sportName(run.sportType)} · 待同步", style = MaterialTheme.typography.titleSmall)
                            Text("${String.format(Locale.ROOT, "%.2f", run.distanceM / 1000)} 公里 · ${TrackMath.clock(run.durationSec)}", style = MaterialTheme.typography.bodyMedium)
                            Text(sportDate(run.startedAt), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(vm.sports.size, key = { "remote-${vm.sports[it].id}" }) { index ->
                    val run = vm.sports[index]
                    OutlinedCard(onClick = { remoteId = run.id; vm.openSport(run.id) }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(TrackMath.sportName(run.sportType), style = MaterialTheme.typography.titleMedium)
                                Text(sportDate(run.startedAt), style = MaterialTheme.typography.bodySmall)
                            }
                            if (run.sportType in listOf("STRENGTH", "OTHER"))
                                Text(TrackMath.clock(run.durationSec), style = MaterialTheme.typography.headlineSmall)
                            else {
                                Text("${String.format(Locale.ROOT, "%.2f", run.distanceM / 1000)} 公里", style = MaterialTheme.typography.headlineSmall)
                                Text(if (run.sportType == "RIDE") "${TrackMath.clock(run.durationSec)} · ${run.averageSpeedKmh} 公里/小时"
                                    else "${TrackMath.clock(run.durationSec)} · ${TrackMath.pace(run.paceSecPerKm)} / 公里", style = MaterialTheme.typography.bodySmall)
                            }
                            if (run.targetReached) StatusLabel("目标已达成")
                        }
                    }
                }
                if (vm.sports.isEmpty() && pending.isEmpty()) item { EmptyContent(Icons.Outlined.LocationOn, "还没有运动记录", "从今天的第一步开始") }
                item { Text("最近 100 次已同步运动", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RUN", "WALK", "RIDE", "STRENGTH", "OTHER").forEach { type ->
                    FilterChip(selected = sport == type, onClick = { sport = type; mode = if (type == "STRENGTH") "STRENGTH" else "AEROBIC" }, label = { Text(TrackMath.sportName(type)) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (outdoor) "户外${TrackMath.sportName(sport)}" else TrackMath.sportName(sport), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                AppIconButton(Icons.Outlined.DateRange, "运动记录") { history = true; vm.loadSports() }
                AppIconButton(Icons.Outlined.Settings, "运动目标", onClick = ::showSettings)
            }
            if (outdoor) RouteMap(emptyList(), "prepare", false, Modifier.weight(1f).fillMaxWidth())
            else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (outdoor && target != null) "${String.format(Locale.ROOT, "%.1f", target / 1000.0)} 公里" else "自由运动", style = MaterialTheme.typography.headlineSmall)
                        Text(if (outdoor && pace != null) "目标配速 ${TrackMath.pace(pace)} / 公里" else if (outdoor) "不限配速" else "运动计时",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = ::showSettings) { Text("设置目标") }
                }
                Button(onClick = ::startWithPermission, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (active != null) "返回当前运动" else "开始${TrackMath.sportName(sport)}")
                }
                if (ownerSessions.any { it.status == "FINISHED" }) TextButton(onClick = { history = true }, modifier = Modifier.fillMaxWidth()) { Text("有运动记录待同步") }
            }
        }
    }

    if (settings) AlertDialog(onDismissRequest = ::cancelSettings, title = { Text("运动目标") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (outdoor) {
                NumberField("目标距离（留空为自由运动）", targetKm, { targetKm = it }, suffix = "公里")
                Row(verticalAlignment = Alignment.CenterVertically) { Text("目标配速", Modifier.weight(1f)); Switch(checked = paceEnabled, onCheckedChange = { paceEnabled = it }, modifier = Modifier.semantics { contentDescription = "设置目标配速" }) }
                if (paceEnabled) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("分钟", paceMin, { paceMin = it }, Modifier.weight(1f))
                    NumberField("秒", paceSec, { paceSec = it }, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("距离达标后自动结束", Modifier.weight(1f)); Switch(checked = autoFinish, onCheckedChange = { autoFinish = it }, enabled = target != null, modifier = Modifier.semantics { contentDescription = "距离达标后自动结束" }) }
            }
            Text("训练类型", style = MaterialTheme.typography.labelLarge)
            val options = if (sport == "STRENGTH") listOf("STRENGTH" to "力量训练") else listOf("AEROBIC" to "有氧训练", "INTERVAL" to "间歇训练", "FREE" to "自由训练")
            options.forEach { (code, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == code, onClick = { mode = code })
                    Text(label)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = {
        if ((outdoor && targetKm.isNotBlank() && target == null) || (outdoor && paceEnabled && (pace == null || pace > 3600)))
            RunRepository.problem.value = "请检查目标距离和配速"
        else settings = false
    }) { Text("确定") } }, dismissButton = { TextButton(onClick = ::cancelSettings) { Text("取消") } })

    if (confirmEnd && local != null) AlertDialog(onDismissRequest = { confirmEnd = false }, title = { Text("结束本次运动？") },
        text = { Text("已运动 ${TrackMath.clock(local.durationSec)}，结束后保存记录。") },
        confirmButton = { TextButton(onClick = { confirmEnd = false; scope.launch { stopRecovered(local) } }) { Text("结束并保存") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("继续运动") } })
    if (confirmDiscard && local != null) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("放弃本次运动？") },
        text = { Text("本次路线和计时将被删除。") },
        confirmButton = { TextButton(onClick = {
            confirmDiscard = false
            if (TrackingService.liveClientId == local.clientRecordId) TrackingService.command(context, TrackingService.DISCARD)
            else scope.launch {
                try { RunRepository.discard(context, local.clientRecordId, userId) }
                catch (exception: Exception) { Log.e("BodyPlan", "scenario=sport-discard userId=$userId clientRecordId=${local.clientRecordId}", exception); RunRepository.problem.value = "删除失败，请重试" }
            }
            selectedId = null
        }) { Text("放弃运动") } }, dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("保留") } })
}

@Composable
private fun SportSubHeader(title: String, onBack: () -> Unit, onRefresh: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(Icons.Outlined.ArrowBack, "返回", onClick = onBack)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        onRefresh?.let { AppIconButton(Icons.Outlined.Refresh, "刷新记录", onClick = it) }
    }
}

@Composable
internal fun SportMetrics(distance: Double, seconds: Int, pace: Int?, type: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (type in listOf("RUN", "WALK", "RIDE")) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format(Locale.ROOT, "%.2f", distance / 1000), fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                Text(" 公里", Modifier.padding(bottom = 7.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric(TrackMath.clock(seconds), "运动时间", Modifier.weight(1f))
            if (type == "RIDE") Metric(if (seconds > 0) String.format(Locale.ROOT, "%.1f", distance * 3.6 / seconds) else "--", "平均速度 · 公里/时", Modifier.weight(1f))
            else if (type in listOf("RUN", "WALK")) Metric(TrackMath.pace(pace), "平均配速 · 分/公里", Modifier.weight(1f))
        }
    }
}

private fun sportDate(millis: Long): String = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(millis))
