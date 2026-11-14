package com.maou.bodyplanapp.tracking

import com.maou.bodyplanapp.data.RoutePoint
import com.maou.bodyplanapp.data.SportSave
import net.sf.geographiclib.Geodesic
import java.util.Locale

data class LocalRun(
    val userId: Long, val clientRecordId: String, val sportType: String, val trainingMode: String,
    val targetDistanceM: Int?, val targetPaceSec: Int?, val autoFinish: Boolean,
    val startedAt: Long, val endedAt: Long? = null, val durationMs: Long = 0,
    val points: List<RoutePoint> = emptyList(), val distanceM: Double = 0.0,
    val segment: Int = 0, val status: String = "RUNNING", val signal: String = "等待定位",
    val simulated: Boolean = false, val serverId: Long? = null,
) {
    val outdoor: Boolean get() = sportType in listOf("RUN", "WALK", "RIDE")
    val active: Boolean get() = status == "RUNNING" || status == "PAUSED"
    val durationSec: Int get() = (durationMs / 1000).toInt()
    val pace: Int? get() = if (distanceM >= 10 && durationSec > 0) (durationSec * 1000 / distanceM).toInt() else null
    fun request() = SportSave(clientRecordId, sportType, trainingMode, targetDistanceM, targetPaceSec,
        startedAt, requireNotNull(endedAt), durationSec, points, if (simulated) "模拟定位验证记录" else null)
}

object TrackMath {
    fun distance(a: RoutePoint, b: RoutePoint, sport: String): Double {
        val seconds = (b.timestampMs - a.timestampMs) / 1000.0
        if (a.segment != b.segment || seconds <= 0 || seconds > 30) return 0.0
        val meters = Geodesic.WGS84.Inverse(a.latitude, a.longitude, b.latitude, b.longitude).s12
        val maxSpeed = if (sport == "RIDE") 25 else if (sport == "WALK") 5 else 12
        return if (meters >= 3 && meters / seconds <= maxSpeed) meters else 0.0
    }
    fun clock(seconds: Int): String = String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    fun pace(seconds: Int?): String = seconds?.let { String.format(Locale.ROOT, "%d′%02d″", it / 60, it % 60) } ?: "--"
    fun sportName(type: String) = when(type) { "RUN" -> "跑步"; "WALK" -> "步行"; "RIDE" -> "骑行"; "STRENGTH" -> "力量训练"; else -> "其他运动" }
}
