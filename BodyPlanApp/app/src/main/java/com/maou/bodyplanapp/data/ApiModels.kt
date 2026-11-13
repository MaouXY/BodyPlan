package com.maou.bodyplanapp.data

/**
 * 后端统一返回体：code = 0 表示成功。
 * 所有字段可空，避免 Gson 反序列化时出现空值崩溃。
 */
data class ApiResponse<T>(
    val code: Int? = null,
    val message: String? = null,
    val data: T? = null,
)

/* ------------------------------- 认证 ------------------------------- */

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val phone: String? = null,
)

data class UserInfo(
    val id: Long? = null,
    val username: String? = null,
    val nickname: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val status: Int? = null,
    val createdAt: String? = null,
)

data class AuthResponse(
    val token: String? = null,
    val expiresIn: Long? = null,
    val user: UserInfo? = null,
)

/* ------------------------------- 建档 ------------------------------- */

data class ProfileResponse(
    val userId: Long? = null,
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bmi: Double? = null,
    val bmiLevel: String? = null,
    val score: Int? = null,
    val bodyType: String? = null,
    val primaryGoal: String? = null,
    val subGoals: List<String>? = null,
    val focusParts: List<String>? = null,
    val sportTypes: List<String>? = null,
    val weeklyDays: List<Int>? = null,
    val dailyMinutes: Int? = null,
    val remindEnabled: Int? = null,
    val remindTime: String? = null,
    val injuries: List<String>? = null,
    val trainLimits: List<String>? = null,
    val profileCompleted: Int? = null,
)

data class ProfileSaveRequest(
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyType: String? = null,
    val primaryGoal: String? = null,
    val subGoals: List<String>? = null,
    val focusParts: List<String>? = null,
    val sportTypes: List<String>? = null,
    val weeklyDays: List<Int>? = null,
    val dailyMinutes: Int? = null,
    val remindEnabled: Int? = null,
    val remindTime: String? = null,
    val injuries: List<String>? = null,
    val trainLimits: List<String>? = null,
)

data class GoalSuggestion(
    val recommendedGoal: String? = null,
    val reason: String? = null,
    val bmi: Double? = null,
    val bmiLevel: String? = null,
    val alternatives: List<String>? = null,
)

data class FitnessTestRequest(
    val pushUp: Int? = null,
    val squat: Int? = null,
    val crunch: Int? = null,
    val stairStatus: String? = null,
)

data class FitnessTestResponse(
    val id: Long? = null,
    val pushUp: Int? = null,
    val squat: Int? = null,
    val crunch: Int? = null,
    val stairStatus: String? = null,
    val level: String? = null,
    val testedAt: String? = null,
)

/* ------------------------------- 计划 ------------------------------- */

data class PlanItem(
    val id: Long? = null,
    val dayIndex: Int? = null,
    val trainingDate: String? = null,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restSec: Int? = null,
    val sortNo: Int? = null,
    val status: String? = null,
)

data class PlanDay(
    val dayIndex: Int? = null,
    val trainingDate: String? = null,
    val weekdayLabel: String? = null,
    val items: List<PlanItem>? = null,
)

data class TrainingPlan(
    val id: Long? = null,
    val name: String? = null,
    val goal: String? = null,
    val level: String? = null,
    val daysPerWeek: Int? = null,
    val minutesPerSession: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val reason: String? = null,
    val createdAt: String? = null,
    val totalItems: Int? = null,
    val doneItems: Int? = null,
    val days: List<PlanDay>? = null,
)

data class PlanGenerateRequest(
    val goal: String? = null,
    val force: Boolean? = null,
    val startDate: String? = null,
)

data class RecommendReason(
    val planId: Long? = null,
    val reason: String? = null,
    val matchedRule: String? = null,
    val excludedCount: Int? = null,
    val inputSnapshot: String? = null,
    val createdAt: String? = null,
)

/* ------------------------------- 打卡 ------------------------------- */

data class TodayTaskItem(
    val itemId: Long? = null,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val restSec: Int? = null,
    val itemStatus: String? = null,
    val checkedIn: Boolean? = null,
    val checkinId: Long? = null,
    val durationMin: Int? = null,
    val feeling: String? = null,
)

data class TodayTask(
    val planId: Long? = null,
    val planName: String? = null,
    val planLevel: String? = null,
    val trainingDate: String? = null,
    val hasTaskToday: Boolean? = null,
    val taskDate: String? = null,
    val nextTrainingDate: String? = null,
    val totalCount: Int? = null,
    val doneCount: Int? = null,
    val items: List<TodayTaskItem>? = null,
)

data class CheckinRequest(
    val planItemId: Long? = null,
    val planId: Long? = null,
    val trainingDate: String? = null,
    val durationMin: Int? = null,
    val feeling: String? = null,
    val remark: String? = null,
    val exerciseId: Long? = null,
)

data class CheckinResponse(
    val id: Long? = null,
    val planId: Long? = null,
    val planItemId: Long? = null,
    val trainingDate: String? = null,
    val finished: Boolean? = null,
    val durationMin: Int? = null,
    val feeling: String? = null,
    val remark: String? = null,
    val createdAt: String? = null,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
)

data class ExerciseOption(
    val id: Long,
    val name: String,
    val part: String? = null,
    val sportType: String? = null,
    val cautions: String? = null,
    val keyPoints: String? = null,
    val status: Int? = null,
)

data class CheckinStreak(
    val currentStreak: Int? = null,
    val maxStreak: Int? = null,
    val totalDays: Int? = null,
)

data class ProfileCompleteRequest(val profile: ProfileSaveRequest, val fitness: FitnessTestRequest)
data class CheckinCalendarDay(val date: String, val finishedCount: Int, val checkedIn: Boolean)
data class CheckinCalendar(val month: String, val days: List<CheckinCalendarDay>, val monthFinishedCount: Int, val activeDays: Int)
data class PageResult<T>(val items: List<T>, val page: Long, val size: Long, val total: Long)
data class BodyData(val id: Long, val weightKg: Double?, val bmi: Double?, val score: Int?, val recordedAt: String?)
