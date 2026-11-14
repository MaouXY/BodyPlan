package com.maou.bodyplanapp.data

data class RoutePoint(val latitude: Double, val longitude: Double, val timestampMs: Long, val segment: Int, val accuracy: Double)
data class SportSave(
    val clientRecordId: String, val sportType: String, val trainingMode: String,
    val targetDistanceM: Int?, val targetPaceSec: Int?, val startedAt: Long, val endedAt: Long,
    val durationSec: Int, val points: List<RoutePoint>, val remark: String? = null,
)
data class SportSummary(
    val id: Long, val clientRecordId: String, val sportType: String, val trainingMode: String,
    val targetDistanceM: Int?, val targetPaceSec: Int?, val startedAt: Long, val endedAt: Long,
    val durationSec: Int, val distanceM: Double, val paceSecPerKm: Int?, val averageSpeedKmh: Double?,
    val pointCount: Int, val targetReached: Boolean, val remark: String?,
)
data class SportDetail(val summary: SportSummary, val points: List<RoutePoint>)
data class NutritionFood(
    val id: Long, val name: String, val category: String, val kcal: Double, val protein: Double,
    val fat: Double, val carbohydrate: Double, val fiber: Double?, val source: String, val enabled: Boolean,
)
data class DietEntry(
    val id: Long, val entryDate: String, val meal: String, val foodId: Long, val foodName: String,
    val grams: Double, val kcal: Double, val protein: Double, val fat: Double, val carbohydrate: Double,
    val fiber: Double?, val source: String,
)
data class DietSave(val entryDate: String, val meal: String, val foodId: Long, val grams: Double)
data class NutritionTarget(
    val targetDate: String, val goal: String, val kcal: Int, val protein: Int,
    val fat: Int, val carbohydrate: Int, val fiber: Int,
)
data class NutritionTotals(
    val kcal: Double, val protein: Double, val fat: Double, val carbohydrate: Double,
    val fiber: Double, val fiberComplete: Boolean,
)
data class NutritionDay(val date: String, val entries: List<DietEntry>, val totals: NutritionTotals, val target: NutritionTarget?)
data class CoachMessage(
    val id: Long, val title: String, val content: String, val taskDate: String, val isRead: Int,
    val createdAt: String?, val senderName: String?,
)
data class MessageInbox(val items: List<CoachMessage>, val unreadCount: Int)
