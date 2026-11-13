package com.maou.bodyplanapp

import com.google.gson.Gson
import com.maou.bodyplanapp.data.*
import com.maou.bodyplanapp.ui.*
import org.junit.Assert.*
import org.junit.Test

class DemoFlowTest {
    @Test fun completedProfileWithoutFitnessResumesOnboarding() {
        assertEquals(Stage.Onboarding, restoredStage(ProfileResponse(profileCompleted = 1), null))
    }

    @Test fun incompleteProfileAlwaysResumesOnboarding() {
        assertEquals(Stage.Onboarding, restoredStage(ProfileResponse(profileCompleted = 0), FitnessTestResponse(id = 1)))
    }

    @Test fun completedProfileAndFitnessEnterReadyEvenWithoutPlan() {
        assertEquals(Stage.Ready, restoredStage(ProfileResponse(profileCompleted = 1), FitnessTestResponse(id = 1)))
    }

    @Test fun failedPlanRequestIsNotConvertedIntoEmptyPlan() {
        val failure = assertThrows(ApiFailure::class.java) {
            ApiResponse<TrainingPlan>(500, "模拟服务异常", null).checkedData()
        }
        assertEquals(500, failure.code)
        assertNull(ApiResponse<TrainingPlan>(0, "success", null).checkedData())
    }

    @Test fun missingRequiredPayloadIsRejected() {
        assertThrows(ApiFailure::class.java) { ApiResponse<CheckinCalendar>(0, "success", null).required() }
    }

    @Test fun freeTrainingSendsActualFeedbackWithoutItemOrWholeSessionDuration() {
        val json = Gson().toJsonTree(CheckinRequest(durationMin = 8, feeling = "HARD", remark = "实际训练反馈", exerciseId = 7)).asJsonObject
        assertFalse(json.has("planItemId"))
        assertEquals(8, json["durationMin"].asInt)
        assertEquals("HARD", json["feeling"].asString)
        assertEquals("实际训练反馈", json["remark"].asString)
        assertEquals(7L, json["exerciseId"].asLong)
    }

    @Test fun historyDecodesExerciseSnapshotAndLegacyRowsRemainReadable() {
        val record = Gson().fromJson("""{"id":9,"exerciseId":7,"exerciseName":"慢跑"}""", CheckinResponse::class.java)
        assertEquals("慢跑", record.exerciseName)
        assertEquals(7L, record.exerciseId)
        val legacy = Gson().fromJson("""{"id":2,"remark":"旧记录"}""", CheckinResponse::class.java)
        assertNull(legacy.exerciseName)
        assertEquals("旧记录", legacy.remark)
    }
}
