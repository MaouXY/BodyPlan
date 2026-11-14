package com.maou.bodyplanapp.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maou.bodyplanapp.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.maou.bodyplanapp.tracking.RunRepository

enum class Stage { Booting, ConnectionError, Login, Onboarding, Ready }

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TokenStore(application)
    private val api = ApiClient.service
    var sessionUserId by mutableStateOf(store.userId)
        private set
    var stage by mutableStateOf(Stage.Booting)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var displayName by mutableStateOf("")
        private set
    var profile by mutableStateOf<ProfileResponse?>(null)
        private set
    var fitness by mutableStateOf<FitnessTestResponse?>(null)
        private set
    var editingProfile by mutableStateOf(false)
        private set
    var goalSuggestion by mutableStateOf<GoalSuggestion?>(null)
        private set
    var plan by mutableStateOf<TrainingPlan?>(null)
        private set
    var today by mutableStateOf<TodayTask?>(null)
        private set
    var streak by mutableStateOf<CheckinStreak?>(null)
        private set
    var calendar by mutableStateOf<CheckinCalendar?>(null)
        private set
    var bodyHistory by mutableStateOf<List<BodyData>>(emptyList())
        private set
    var planHistory by mutableStateOf<List<TrainingPlan>>(emptyList())
        private set
    var checkinHistory by mutableStateOf<List<CheckinResponse>>(emptyList())
        private set
    var calendarMonth by mutableStateOf(monthText(Calendar.getInstance()))
        private set

    init {
        ApiClient.token = store.token
        displayName = store.username.orEmpty()
        if (store.token.isNullOrBlank()) stage = Stage.Login else retryConnection()
    }

    private fun operation(scenario: String, context: String = "", block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                Log.d("BodyPlan", "scenario=$scenario userId=${profile?.userId} $context reason=cancelled", exception)
                throw exception
            } catch (exception: Exception) {
                Log.e("BodyPlan", "scenario=$scenario userId=${profile?.userId} planId=${plan?.id} $context", exception)
                val unauthorized = (exception is HttpException && exception.code() == 401) ||
                    (exception is ApiFailure && exception.code in listOf(401, 9005))
                if (unauthorized) {
                    if (RunRepository.sessions.value.any { it.userId == sessionUserId && it.active }) {
                        store.token = null
                        ApiClient.token = null
                        stage = Stage.ConnectionError
                        Log.w("BodyPlan", "scenario=session-expired userId=$sessionUserId reason=active-local-workout-retained")
                    } else clearSession()
                    error = "登录已失效，请重新登录"
                } else {
                    error = failureMessage(exception, scenario)
                    if (stage == Stage.Booting) stage = Stage.ConnectionError
                }
            } finally {
                busy = false
            }
        }
    }

    private fun failureMessage(exception: Exception, scenario: String): String {
        if (exception is ApiFailure) return exception.message ?: "服务返回异常"
        if (exception is HttpException) {
            if (scenario == "free-checkin-create" && exception.code() == 404) {
                return "服务尚未支持自选运动，请先升级后端后再打卡"
            }
            try {
                val payload = Gson().fromJson(exception.response()?.errorBody()?.string(), ApiResponse::class.java)
                if (!payload?.message.isNullOrBlank()) {
                    Log.w("BodyPlan", "scenario=$scenario userId=${profile?.userId} code=${payload?.code} reason=${payload?.message}", exception)
                    return payload!!.message!!
                }
            } catch (parseError: Exception) {
                Log.e("BodyPlan", "scenario=$scenario userId=${profile?.userId} httpStatus=${exception.code()} reason=error-body-invalid", parseError)
            }
            return "服务请求失败（${exception.code()}），请稍后重试"
        }
        return "请求失败，请检查网络后重试"
    }

    fun retryConnection() = operation("session-restore") {
        stage = Stage.Booting
        loadProfileAndRoute()
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入账号和密码"
            return
        }
        operation("login", "username=${username.trim()}") {
            acceptLogin(api.login(LoginRequest(username.trim(), password)).required())
            loadProfileAndRoute()
        }
    }

    fun register(username: String, password: String, nickname: String, phone: String) {
        if (username.isBlank() || password.length < 6) {
            error = "账号不能为空，密码至少 6 位"
            return
        }
        operation("register", "username=${username.trim()}") {
            api.register(RegisterRequest(username.trim(), password, nickname.ifBlank { null }, phone.ifBlank { null })).check()
            message = "注册成功"
            acceptLogin(api.login(LoginRequest(username.trim(), password)).required())
            loadProfileAndRoute()
        }
    }

    private fun acceptLogin(data: AuthResponse) {
        if (data.user?.role != "USER") {
            Log.w("BodyPlan", "scenario=app-login userId=${data.user?.id} role=${data.user?.role} reason=role-not-user")
            throw ApiFailure(403, "教练和管理员请登录 Web 工作台")
        }
        val token = requireNotNull(data.token) { "登录响应缺少 token" }
        ApiClient.token = token
        store.token = token
        displayName = data.user?.nickname ?: data.user?.username.orEmpty()
        store.username = displayName
        sessionUserId = data.user?.id
        store.userId = sessionUserId
        stage = Stage.Booting
    }

    fun logout() {
        if (RunRepository.sessions.value.any { it.userId == sessionUserId && it.active }) {
            error = "请先结束或放弃当前运动，再退出登录"
            return
        }
        if (!busy) clearSession()
    }

    private fun clearSession() {
        store.clear()
        ApiClient.token = null
        profile = null
        fitness = null
        plan = null
        today = null
        streak = null
        calendar = null
        bodyHistory = emptyList()
        planHistory = emptyList()
        checkinHistory = emptyList()
        goalSuggestion = null
        editingProfile = false
        loadError = null
        message = null
        displayName = ""
        sessionUserId = null
        stage = Stage.Login
    }

    private suspend fun loadProfileAndRoute() {
        profile = api.profile().required()
        sessionUserId = profile?.userId
        store.userId = sessionUserId
        val result = api.latestFitnessTest()
        fitness = if (result.code == 10201) {
            Log.w("BodyPlan", "scenario=onboarding-resume userId=${profile?.userId} code=${result.code} reason=${result.message}")
            null
        } else result.required()
        editingProfile = false
        stage = restoredStage(profile, fitness)
        if (stage == Stage.Ready) {
            refreshData()
        }
    }

    fun loadGoalSuggestion(draft: ProfileSaveRequest) = operation("goal-preview") {
        goalSuggestion = null
        goalSuggestion = api.previewGoal(draft).required()
    }

    fun editProfile() {
        if (busy) return
        goalSuggestion = null
        editingProfile = true
        stage = Stage.Onboarding
    }

    fun cancelEdit() {
        if (busy) return
        editingProfile = false
        stage = Stage.Ready
    }

    fun submitOnboarding(request: ProfileSaveRequest, fitness: FitnessTestRequest) =
        operation("profile-complete") {
            plan = api.completeProfile(ProfileCompleteRequest(request, fitness)).required()
            editingProfile = false
            stage = Stage.Ready
            message = "档案、体能测试与新计划已保存"
            refreshData()
        }

    fun refreshPlanAndToday() = operation("dashboard-refresh") { refreshData() }

    private suspend fun refreshData() {
        loadError = null
        try {
            // 全部读成功后再更新页面，失败保留原快照，并明确标记为未刷新。
            val newPlan = api.currentPlan().checkedData()
            val newToday = api.todayTask().required()
            val newStreak = api.streak().required()
            val newCalendar = api.calendar(calendarMonth).required()
            val newProfile = api.profile().required()
            val newFitness = api.latestFitnessTest().required()
            val newBodyHistory = api.bodyHistory().required()
            val newPlanHistory = api.planHistory().required()
            val newCheckins = api.checkinHistory().required().items
            plan = newPlan
            today = newToday
            streak = newStreak
            calendar = newCalendar
            profile = newProfile
            fitness = newFitness
            bodyHistory = newBodyHistory
            planHistory = newPlanHistory
            checkinHistory = newCheckins
        } catch (exception: CancellationException) {
            Log.d("BodyPlan", "scenario=dashboard-refresh userId=${profile?.userId} reason=cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=dashboard-refresh userId=${profile?.userId} planId=${plan?.id}", exception)
            loadError = "数据刷新失败，当前内容可能不是最新状态"
            throw exception
        }
    }

    fun changeMonth(offset: Int) = operation("checkin-calendar", "month=$calendarMonth offset=$offset") {
        val date = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, calendarMonth.substring(0, 4).toInt())
            set(Calendar.MONTH, calendarMonth.substring(5, 7).toInt() - 1)
            add(Calendar.MONTH, offset)
        }
        val requested = monthText(date)
        val result = api.calendar(requested).required()
        calendar = result
        calendarMonth = requested
    }

    fun regeneratePlan() = operation("plan-regenerate") {
        plan = api.generatePlan(PlanGenerateRequest(force = true)).required()
        message = "已按最新档案重新生成计划"
        refreshData()
    }

    suspend fun searchExercises(page: Long, keyword: String): PageResult<ExerciseOption> =
        api.exercises(page = page, keyword = keyword.trim().ifBlank { null }).required()

    fun checkin(itemId: Long?, feeling: String, durationMin: Int, remark: String,
        exerciseId: Long? = null, onSaved: () -> Unit) =
        operation(if (itemId == null) "free-checkin-create" else "checkin-create", "planItemId=$itemId exerciseId=$exerciseId") {
            val request = CheckinRequest(planItemId = itemId, feeling = feeling,
                durationMin = durationMin, remark = remark.ifBlank { null }, exerciseId = exerciseId)
            val result = if (itemId == null) {
                requireNotNull(exerciseId) { "请选择具体运动" }
                api.freeCheckin(request).required()
            } else api.checkin(request).required()
            check(result.id != null) { "打卡响应缺少记录编号" }
            message = "打卡成功"
            onSaved()
            refreshData()
        }

    fun cancelCheckin(checkinId: Long) = operation("checkin-cancel", "checkinId=$checkinId") {
        api.cancelCheckin(checkinId).check()
        message = "已撤销打卡"
        refreshData()
    }

    fun consumeMessage() { message = null }
    fun consumeError() { error = null }

    private fun monthText(date: Calendar) = SimpleDateFormat("yyyy-MM", Locale.ROOT).format(date.time)
}

internal fun restoredStage(profile: ProfileResponse?, fitness: FitnessTestResponse?): Stage =
    if (profile?.profileCompleted == 1 && fitness != null) Stage.Ready else Stage.Onboarding

internal class ApiFailure(val code: Int?, message: String) : RuntimeException(message)

internal fun <T> ApiResponse<T>.check() {
    if (code != 0) throw ApiFailure(code, message ?: "服务返回异常（$code）")
}
internal fun <T> ApiResponse<T>.checkedData(): T? { check(); return data }
internal fun <T> ApiResponse<T>.required(): T = checkedData() ?: throw ApiFailure(code, "服务返回的数据不完整")
