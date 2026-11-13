package com.maou.bodyplanapp.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 后端接口（阶段 1 闭环所需）。路径与 /doc.html 中的接口文档一致。
 */
interface ApiService {

    @GET("api/v1/exercises")
    suspend fun exercises(@Query("page") page: Long = 1, @Query("size") size: Int = 30,
        @Query("keyword") keyword: String? = null): ApiResponse<PageResult<ExerciseOption>>

    @POST("api/v1/checkins/free")
    suspend fun freeCheckin(@Body body: CheckinRequest): ApiResponse<CheckinResponse>

    @POST("api/v1/profile/complete")
    suspend fun completeProfile(@Body body: ProfileCompleteRequest): ApiResponse<TrainingPlan>

    @POST("api/v1/profile/goal-suggestion")
    suspend fun previewGoal(@Body body: ProfileSaveRequest): ApiResponse<GoalSuggestion>

    @GET("api/v1/profile/body-data")
    suspend fun bodyHistory(@Query("limit") limit: Int = 20): ApiResponse<List<BodyData>>

    @GET("api/v1/plans/history")
    suspend fun planHistory(): ApiResponse<List<TrainingPlan>>

    @GET("api/v1/checkins/calendar")
    suspend fun calendar(@Query("month") month: String): ApiResponse<CheckinCalendar>

    @GET("api/v1/checkins")
    suspend fun checkinHistory(@Query("size") size: Int = 100): ApiResponse<PageResult<CheckinResponse>>

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<UserInfo>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun me(): ApiResponse<UserInfo>

    @GET("api/v1/profile")
    suspend fun profile(): ApiResponse<ProfileResponse>

    @PUT("api/v1/profile")
    suspend fun saveProfile(@Body body: ProfileSaveRequest): ApiResponse<ProfileResponse>

    @GET("api/v1/profile/goal-suggestion")
    suspend fun goalSuggestion(): ApiResponse<GoalSuggestion>

    @POST("api/v1/fitness-tests")
    suspend fun saveFitnessTest(@Body body: FitnessTestRequest): ApiResponse<FitnessTestResponse>

    @GET("api/v1/fitness-tests/latest")
    suspend fun latestFitnessTest(): ApiResponse<FitnessTestResponse>

    @POST("api/v1/plans/generate")
    suspend fun generatePlan(@Body body: PlanGenerateRequest): ApiResponse<TrainingPlan>

    @GET("api/v1/plans/current")
    suspend fun currentPlan(): ApiResponse<TrainingPlan>

    @GET("api/v1/plans/current/reason")
    suspend fun currentReason(): ApiResponse<RecommendReason>

    @GET("api/v1/checkins/today")
    suspend fun todayTask(): ApiResponse<TodayTask>

    @POST("api/v1/checkins")
    suspend fun checkin(@Body body: CheckinRequest): ApiResponse<CheckinResponse>

    @DELETE("api/v1/checkins/{id}")
    suspend fun cancelCheckin(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/v1/checkins/streak")
    suspend fun streak(): ApiResponse<CheckinStreak>
}
