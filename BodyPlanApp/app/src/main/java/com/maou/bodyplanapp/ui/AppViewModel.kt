package com.maou.bodyplanapp.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maou.bodyplanapp.data.ApiClient
import com.maou.bodyplanapp.data.CheckinRequest
import com.maou.bodyplanapp.data.CheckinStreak
import com.maou.bodyplanapp.data.FitnessTestRequest
import com.maou.bodyplanapp.data.GoalSuggestion
import com.maou.bodyplanapp.data.LoginRequest
import com.maou.bodyplanapp.data.PlanGenerateRequest
import com.maou.bodyplanapp.data.ProfileResponse
import com.maou.bodyplanapp.data.ProfileSaveRequest
import com.maou.bodyplanapp.data.RegisterRequest
import com.maou.bodyplanapp.data.TodayTask
import com.maou.bodyplanapp.data.TokenStore
import com.maou.bodyplanapp.data.TrainingPlan
import kotlinx.coroutines.launch

/** App 的四个阶段：启动检查 → 登录注册 → 建档问卷 → 主界面（我的计划 / 今日打卡） */
enum class Stage { Booting, Login, Onboarding, Ready }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TokenStore(application)

    var stage by mutableStateOf(Stage.Booting)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var displayName by mutableStateOf("")
        private set
    var profile by mutableStateOf<ProfileResponse?>(null)
        private set
    var goalSuggestion by mutableStateOf<GoalSuggestion?>(null)
        private set
    var plan by mutableStateOf<TrainingPlan?>(null)
        private set
    var today by mutableStateOf<TodayTask?>(null)
        private set
    var streak by mutableStateOf<CheckinStreak?>(null)
        private set

    init {
        val saved = store.token
        if (saved.isNullOrBlank()) {
            stage = Stage.Login
        } else {
            ApiClient.token = saved
            displayName = store.username ?: "健身用户"
            viewModelScope.launch { loadProfileAndRoute() }
        }
    }

    /* ----------------------------- 登录 / 注册 ----------------------------- */

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            error = "请输入账号和密码"
            return
        }
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val response = ApiClient.service.login(LoginRequest(username.trim(), password))
                val data = response.data
                if (response.code == 0 && data?.token != null) {
                    ApiClient.token = data.token
                    store.token = data.token
                    displayName = data.user?.nickname ?: data.user?.username ?: username
                    store.username = displayName
                    message = "登录成功"
                    loadProfileAndRoute()
                } else {
                    error = response.message ?: "登录失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    fun register(username: String, password: String, nickname: String, phone: String) {
        if (username.isBlank() || password.length < 6) {
            error = "账号不能为空，密码至少 6 位"
            return
        }
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val response = ApiClient.service.register(
                    RegisterRequest(
                        username = username.trim(),
                        password = password,
                        nickname = nickname.ifBlank { null },
                        phone = phone.ifBlank { null },
                    ),
                )
                if (response.code == 0) {
                    message = "注册成功，正在登录"
                    val loginResponse = ApiClient.service.login(LoginRequest(username.trim(), password))
                    val data = loginResponse.data
                    if (loginResponse.code == 0 && data?.token != null) {
                        ApiClient.token = data.token
                        store.token = data.token
                        displayName = data.user?.nickname ?: data.user?.username ?: username
                        store.username = displayName
                        loadProfileAndRoute()
                    } else {
                        error = "注册成功，但自动登录失败，请手动登录"
                        stage = Stage.Login
                    }
                } else {
                    error = response.message ?: "注册失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        store.clear()
        ApiClient.token = null
        profile = null
        plan = null
        today = null
        streak = null
        goalSuggestion = null
        displayName = ""
        stage = Stage.Login
    }

    /* ------------------------------- 建档流程 ------------------------------- */

    private suspend fun loadProfileAndRoute() {
        try {
            val response = ApiClient.service.profile()
            val data = response.data
            if (response.code == 0) {
                profile = data
                if ((data?.profileCompleted ?: 0) == 1) {
                    stage = Stage.Ready
                    refreshPlanAndToday()
                } else {
                    stage = Stage.Onboarding
                }
            } else {
                error = response.message ?: "读取档案失败"
                stage = Stage.Login
            }
        } catch (exception: Exception) {
            error = networkMessage(exception)
            stage = Stage.Login
        }
    }

    fun loadGoalSuggestion() {
        viewModelScope.launch {
            error = null
            try {
                val response = ApiClient.service.goalSuggestion()
                if (response.code == 0) {
                    goalSuggestion = response.data
                } else {
                    error = response.message ?: "获取目标建议失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            }
        }
    }

    /** 建档问卷 + 体能测试一次性提交，随后自动生成训练计划 */
    fun submitOnboarding(request: ProfileSaveRequest, fitness: FitnessTestRequest) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val profileResponse = ApiClient.service.saveProfile(request)
                if (profileResponse.code != 0) {
                    error = profileResponse.message ?: "保存建档问卷失败"
                    return@launch
                }
                profile = profileResponse.data

                val fitnessResponse = ApiClient.service.saveFitnessTest(fitness)
                if (fitnessResponse.code != 0) {
                    error = fitnessResponse.message ?: "保存体能测试失败"
                    return@launch
                }

                val planResponse = ApiClient.service.generatePlan(PlanGenerateRequest())
                if (planResponse.code == 0) {
                    plan = planResponse.data
                    message = "训练计划已生成"
                } else {
                    error = planResponse.message ?: "计划生成失败，可稍后在计划页重试"
                }
                stage = Stage.Ready
                refreshPlanAndToday()
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    /* ------------------------------- 计划与打卡 ------------------------------- */

    fun refreshPlanAndToday() {
        viewModelScope.launch {
            busy = true
            try {
                val planResponse = ApiClient.service.currentPlan()
                plan = if (planResponse.code == 0) planResponse.data else null

                val todayResponse = ApiClient.service.todayTask()
                if (todayResponse.code == 0) {
                    today = todayResponse.data
                }

                val streakResponse = ApiClient.service.streak()
                if (streakResponse.code == 0) {
                    streak = streakResponse.data
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    fun regeneratePlan() {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val response = ApiClient.service.generatePlan(PlanGenerateRequest(force = true))
                if (response.code == 0) {
                    plan = response.data
                    message = "已按最新档案重新生成计划"
                    refreshPlanAndToday()
                } else {
                    error = response.message ?: "重新生成计划失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    fun checkin(itemId: Long, feeling: String, durationMin: Int?) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val response = ApiClient.service.checkin(
                    CheckinRequest(planItemId = itemId, feeling = feeling, durationMin = durationMin),
                )
                if (response.code == 0) {
                    message = "打卡成功"
                    refreshPlanAndToday()
                } else {
                    error = response.message ?: "打卡失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    fun cancelCheckin(checkinId: Long) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                val response = ApiClient.service.cancelCheckin(checkinId)
                if (response.code == 0) {
                    message = "已撤销打卡"
                    refreshPlanAndToday()
                } else {
                    error = response.message ?: "撤销打卡失败"
                }
            } catch (exception: Exception) {
                error = networkMessage(exception)
            } finally {
                busy = false
            }
        }
    }

    /* -------------------------------- 工具方法 -------------------------------- */

    fun consumeMessage() {
        message = null
    }

    fun consumeError() {
        error = null
    }

    private fun networkMessage(exception: Exception): String =
        "网络异常：${exception.message ?: "无法连接后端"}（请确认后端已在 8993 端口启动）"
}
