package com.maou.bodyplanapp.ui

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maou.bodyplanapp.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WellnessViewModel : ViewModel() {
    private val api = ApiClient.service
    private var sessionJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var generation = 0L
    private var pending = 0
    private val operations = Mutex()
    var userId by mutableStateOf<Long?>(null)
        private set
    var sports by mutableStateOf<List<SportSummary>>(emptyList())
        private set
    var sportsLoaded by mutableStateOf(false)
        private set
    var todayNutrition by mutableStateOf<NutritionDay?>(null)
        private set
    var sportDetail by mutableStateOf<SportDetail?>(null)
        private set
    var nutrition by mutableStateOf<NutritionDay?>(null)
        private set
    var inbox by mutableStateOf(MessageInbox(emptyList(), 0))
        private set
    var date by mutableStateOf(todayDate())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun bind(id: Long?) {
        if (userId == id) return
        generation++
        sessionJob.cancel()
        sessionJob = SupervisorJob(viewModelScope.coroutineContext[Job])
        pending = 0
        busy = false
        userId = id
        sports = emptyList()
        sportsLoaded = false
        todayNutrition = null
        sportDetail = null
        nutrition = null
        inbox = MessageInbox(emptyList(), 0)
        date = todayDate()
        error = null
        notice = null
    }

    private fun task(scenario: String, context: String = "", block: suspend () -> Unit) {
        if (userId == null) return
        val owner = userId
        val epoch = generation
        pending++
        busy = true
        error = null
        CoroutineScope(viewModelScope.coroutineContext + sessionJob).launch {
            try {
                operations.withLock {
                    if (epoch == generation) block()
                }
            }
            catch (exception: CancellationException) {
                Log.d("BodyPlan", "scenario=$scenario userId=$owner $context cancelled", exception)
                throw exception
            } catch (exception: Exception) {
                Log.e("BodyPlan", "scenario=$scenario userId=$owner $context", exception)
                if (epoch == generation) error = if (exception is ApiFailure) exception.message else "暂时无法完成，请检查网络后重试"
            } finally {
                if (epoch == generation) {
                    pending--
                    busy = pending > 0
                }
            }
        }
    }

    fun loadOverview() = task("wellness-overview") {
        sports = api.sportHistory().required()
        sportsLoaded = true
        todayNutrition = api.nutritionDay(todayDate()).required()
    }
    fun loadSports() = task("sport-history") { sports = api.sportHistory().required(); sportsLoaded = true }
    fun openSport(id: Long) {
        sportDetail = null
        task("sport-detail", "sessionId=$id") { sportDetail = api.sportDetail(id).required() }
    }
    fun closeSport() { sportDetail = null }
    suspend fun uploadSport(save: SportSave): SportSummary = api.saveSport(save).required()
    fun loadNutrition(requestedDate: String = date) = task("nutrition-day", "date=$requestedDate") {
        val result = api.nutritionDay(requestedDate).required()
        nutrition = result
        if (requestedDate == todayDate()) todayNutrition = result
        date = requestedDate
    }
    suspend fun searchFoods(keyword: String, page: Int): PageResult<NutritionFood> = api.foods(keyword, page).required()
    fun saveDiet(id: Long?, request: DietSave, onSaved: () -> Unit) = task("diet-save", "entryId=$id foodId=${request.foodId}") {
        if (id == null) api.addDiet(request).required() else api.editDiet(id, request).required()
        onSaved()
        notice = "饮食已记录"
        nutrition = api.nutritionDay(date).required()
        if (date == todayDate()) todayNutrition = nutrition
    }
    fun removeDiet(id: Long) = task("diet-delete", "entryId=$id") {
        api.deleteDiet(id).check()
        nutrition = api.nutritionDay(date).required()
        if (date == todayDate()) todayNutrition = nutrition
    }
    fun saveTarget(target: NutritionTarget, onSaved: () -> Unit) = task("nutrition-target", "date=${target.targetDate}") {
        api.nutritionTarget(target).check()
        onSaved()
        nutrition = api.nutritionDay(date).required()
        if (date == todayDate()) todayNutrition = nutrition
    }
    suspend fun pollInbox() {
        val owner = userId ?: return
        try {
            val result = api.messages().required()
            if (owner == userId) inbox = result
        } catch (exception: CancellationException) {
            Log.d("BodyPlan", "scenario=inbox-poll userId=$owner cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=inbox-poll userId=$owner", exception)
        }
    }
    fun refreshInbox() = task("inbox-refresh") { inbox = api.messages().required() }
    fun read(id: Long, onRead: () -> Unit = {}) = task("inbox-read", "messageId=$id") {
        api.readMessage(id).check()
        inbox = api.messages().required()
        onRead()
    }
    fun consumeError() { error = null }
    fun consumeNotice() { notice = null }
}
