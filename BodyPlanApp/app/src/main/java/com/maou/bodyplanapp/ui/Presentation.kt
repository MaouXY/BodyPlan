package com.maou.bodyplanapp.ui

import com.maou.bodyplanapp.data.TrainingPlan
import com.maou.bodyplanapp.data.PlanDay
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

fun shortDate(value: String?): String {
    val date = value?.take(10).orEmpty()
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) return "日期待定"
    return "${date.substring(5, 7).toInt()}月${date.substring(8, 10).toInt()}日"
}

fun planExpired(plan: TrainingPlan?, today: String = todayDate()): Boolean =
    !plan?.endDate.isNullOrBlank() && plan!!.endDate!! < today

fun planStatusLabel(plan: TrainingPlan, today: String = todayDate()): String = when {
    plan.status == "REPLACED" -> "已更新"
    plan.status == "FINISHED" || planExpired(plan, today) -> "已结束"
    !plan.startDate.isNullOrBlank() && plan.startDate > today -> "即将开始"
    plan.status == "ACTIVE" -> "进行中"
    else -> "已归档"
}

fun numberText(value: Double?): String = value?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "—"

// dayIndex 在接口中是星期几，跨周展示必须以实际日期为准。
fun displayPlanDays(plan: TrainingPlan?): List<PlanDay> {
    if (plan == null) return emptyList()
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    val start = plan.startDate?.let { format.parse(it, ParsePosition(0)) }
    val end = plan.endDate?.let { format.parse(it, ParsePosition(0)) }
    val original = plan.days.orEmpty().sortedBy { it.trainingDate }
    if (start == null || end == null || end < start) return original
    val count = ((end.time - start.time) / 86_400_000L + 1).coerceAtMost(366).toInt()
    val byDate = original.associateBy { it.trainingDate }
    val calendar = Calendar.getInstance(format.timeZone).apply { time = start }
    return List(count) { index ->
        val date = format.format(calendar.time)
        val weekday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val day = byDate[date] ?: PlanDay(index + 1, date, weekDayLabel(weekday), emptyList())
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        day
    }
}
