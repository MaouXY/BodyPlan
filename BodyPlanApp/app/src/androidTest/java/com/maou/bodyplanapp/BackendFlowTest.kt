package com.maou.bodyplanapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maou.bodyplanapp.data.ApiClient
import com.maou.bodyplanapp.data.CheckinRequest
import com.maou.bodyplanapp.data.FitnessTestRequest
import com.maou.bodyplanapp.data.LoginRequest
import com.maou.bodyplanapp.data.PlanGenerateRequest
import com.maou.bodyplanapp.data.ProfileSaveRequest
import com.maou.bodyplanapp.data.RegisterRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/**
 * 阶段 1 闭环验证（在模拟器里跑，直连电脑上的后端 http://10.0.2.2:8993/）。
 *
 * 覆盖：注册 → 登录 → 建档问卷 → 体能测试 → 生成计划（含禁忌动作排除、多周排期）→
 * 今日任务 → 打卡 → 重复打卡幂等 → 提前打卡拦截 → 连续打卡。
 *
 * 运行方式：先启动后端与模拟器，然后 `gradlew :app:connectedDebugAndroidTest`。
 */
@RunWith(AndroidJUnit4::class)
class BackendFlowTest {

    private val service = ApiClient.service

    private fun username(prefix: String): String =
        prefix + SimpleDateFormat("HHmmss", Locale.CHINA).format(Date())

    @Test
    fun fullClosedLoopWorksOnEmulator() = runBlocking {
        val account = username("apptest")
        val password = "123456"

        // 1. 注册
        val register = service.register(
            RegisterRequest(account, password, "App 闭环测试", "13500000000"),
        )
        assertEquals("注册失败：${register.message}", 0, register.code)
        assertNotNull(register.data?.id)

        // 2. 登录并带上 token
        val login = service.login(LoginRequest(account, password))
        assertEquals("登录失败：${login.message}", 0, login.code)
        val token = login.data?.token
        assertNotNull("登录未返回 token", token)
        ApiClient.token = token

        // 3. 建档问卷（刻意带膝伤 + 宿舍 + 不能跳 + 无器械）
        val profile = service.saveProfile(
            ProfileSaveRequest(
                gender = "MALE",
                age = 28,
                heightCm = 175.0,
                weightKg = 78.0,
                bodyType = "APPLE",
                primaryGoal = "LOSE_FAT",
                subGoals = listOf("DAILY_FITNESS"),
                focusParts = listOf("ABDOMEN", "LEG"),
                sportTypes = listOf("CARDIO", "STRENGTH", "STRETCH"),
                weeklyDays = listOf(1, 3, 5),
                dailyMinutes = 30,
                remindEnabled = 1,
                remindTime = "20:30",
                injuries = listOf("KNEE"),
                trainLimits = listOf("DORM", "NO_JUMP", "NO_EQUIPMENT"),
            ),
        )
        assertEquals("保存建档失败：${profile.message}", 0, profile.code)
        assertEquals(1, profile.data?.profileCompleted)

        // 4. 体能测试
        val fitness = service.saveFitnessTest(
            FitnessTestRequest(pushUp = 15, squat = 25, crunch = 20, stairStatus = "SLIGHT_HARD"),
        )
        assertEquals("保存体能测试失败：${fitness.message}", 0, fitness.code)
        assertEquals("L2", fitness.data?.level)

        // 5. 生成计划
        val plan = service.generatePlan(PlanGenerateRequest())
        assertEquals("生成计划失败：${plan.message}", 0, plan.code)
        val planData = plan.data
        assertNotNull(planData)
        assertTrue("计划没有每日安排", !planData!!.days.isNullOrEmpty())
        assertTrue("推荐依据为空", !planData.reason.isNullOrBlank())

        val exerciseNames = planData.days!!
            .flatMap { it.items ?: emptyList() }
            .mapNotNull { it.exerciseName }
        assertTrue("计划为空", exerciseNames.isNotEmpty())
        val banned = listOf("深蹲", "开合跳", "箭步蹲", "波比跳", "跳绳", "慢跑", "哑铃", "弹力带")
        val violations = exerciseNames.filter { name -> banned.any { name.contains(it) } }
        assertTrue("计划里出现了禁忌动作：$violations", violations.isEmpty())

        // 6. 当前计划：明细要覆盖多周，否则第二周起今日任务会全空
        val current = service.currentPlan()
        assertEquals(0, current.code)
        assertNotNull(current.data?.id)
        val days = current.data?.days ?: emptyList()
        assertTrue("计划明细未覆盖多周（只有 ${days.size} 个训练日）", days.size >= 4)

        // 7. 今日任务
        val today = service.todayTask()
        assertEquals("查询今日任务失败：${today.message}", 0, today.code)
        val todayData = today.data
        assertNotNull(todayData)

        // 8. 打卡：训练日按计划任务打卡；非训练日走自由训练
        val todayItem = todayData!!.items?.firstOrNull()?.takeIf { todayData.hasTaskToday == true }
        if (todayItem?.itemId != null) {
            val checkin = service.checkin(
                CheckinRequest(planItemId = todayItem.itemId, feeling = "NORMAL", durationMin = 30),
            )
            assertEquals("打卡失败：${checkin.message}", 0, checkin.code)

            val duplicated = service.checkin(
                CheckinRequest(planItemId = todayItem.itemId, feeling = "NORMAL"),
            )
            assertEquals("重复打卡未被拦截", 10502, duplicated.code)
        } else {
            assertNotNull("非训练日应返回下次训练日", todayData.nextTrainingDate)
            val free = service.checkin(CheckinRequest(feeling = "EASY", remark = "自由训练"))
            assertEquals("自由训练打卡失败：${free.message}", 0, free.code)
        }

        // 9. 提前打卡必须被拒绝（10504），避免打卡记录落到未来日期
        val today2 = LocalDate.now()
        val futureItem = days
            .filter { it.trainingDate != null && LocalDate.parse(it.trainingDate).isAfter(today2) }
            .flatMap { it.items ?: emptyList() }
            .firstOrNull()
        if (futureItem?.id != null) {
            val tooEarly = service.checkin(CheckinRequest(planItemId = futureItem.id, feeling = "NORMAL"))
            assertEquals("未来训练日的打卡未被拦截", 10504, tooEarly.code)
        }

        // 10. 打卡后连续打卡天数应 >= 1
        val streak = service.streak()
        assertEquals(0, streak.code)
        assertTrue("打卡后连续打卡天数应 >= 1", (streak.data?.currentStreak ?: 0) >= 1)
    }
}
