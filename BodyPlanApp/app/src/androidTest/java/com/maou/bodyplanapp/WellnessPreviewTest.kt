package com.maou.bodyplanapp

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.maou.bodyplanapp.data.*
import com.maou.bodyplanapp.ui.required
import com.maou.bodyplanapp.tracking.RunRepository
import com.maou.bodyplanapp.tracking.TrackingService
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

/** 仅使用 wellness_demo 专用账号；模拟路线会显式标记，不代表真实户外实跑。 */
class WellnessPreviewTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null
    @Before fun launch() = runBlocking {
        val response = ApiClient.service.login(LoginRequest("wellness_demo","123456"))
        assertEquals(response.message,0,response.code)
        val login = requireNotNull(response.data)
        val store = TokenStore(context)
        store.token = login.token
        store.userId = login.user?.id
        store.username = login.user?.nickname
        ApiClient.token = login.token
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName,it) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15000) { compose.onAllNodesWithText("今日安排").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun close() {
        RunRepository.sessions.value.firstOrNull { it.userId == 940002L && it.active }?.let {
            TrackingService.command(context,TrackingService.DISCARD)
        }
        scenario?.close()
    }
    @Test fun browseNewScreensAndSavePreviewImages() {
        save("today")
        compose.onNode(hasText("饮食",substring=false) and hasClickAction()).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("记录饮食").fetchSemanticsNodes().isNotEmpty() }
        save("nutrition")
        compose.onNodeWithText("记录饮食").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("米饭（熟）").fetchSemanticsNodes().isNotEmpty() }
        save("food-picker")
        compose.onNodeWithContentDescription("关闭食物选择").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNode(hasText("运动",substring=false) and hasClickAction()).performClick()
        compose.onNodeWithContentDescription("运动目标").performClick()
        compose.onNodeWithText("目标距离（留空为自由运动）").performTextReplacement("3")
        compose.onNodeWithContentDescription("设置目标配速").performClick()
        save("sport-goal")
        compose.onNodeWithText("确定").performClick()
        save("sport-ready")
    }
    @Suppress("DEPRECATION")
    @Test fun gpsGoalPauseAutoFinishAndServerRoundTrip() = runBlocking {
        val locations = context.getSystemService(LocationManager::class.java)
        shell("appops set ${context.packageName} android:mock_location allow")
        locations.addTestProvider(LocationManager.GPS_PROVIDER,false,false,false,false,true,true,true,1,1)
        locations.setTestProviderEnabled(LocationManager.GPS_PROVIDER,true)
        try {
            compose.onNode(hasText("运动",substring=false) and hasClickAction()).performClick()
            compose.onNodeWithContentDescription("运动目标").performClick()
            compose.onNodeWithText("目标距离（留空为自由运动）").performTextReplacement("0.1")
            compose.onNodeWithContentDescription("距离达标后自动结束").performClick()
            compose.onNodeWithText("确定").performClick()
            compose.onNodeWithText("开始跑步").performClick()
            compose.waitUntil(10000) { RunRepository.sessions.value.any { it.userId == 940002L && it.status=="RUNNING" } }
            val id = RunRepository.sessions.value.first { it.userId==940002L && it.status=="RUNNING" }.clientRecordId
            fun run() = RunRepository.sessions.value.first { it.clientRecordId==id }
            fun fix(step: Int) {
                val location=Location(LocationManager.GPS_PROVIDER).apply {
                    latitude=39.9900+step*(if (step <= 8) 0.000015 else 0.00005)
                    longitude=116.3200
                    accuracy=5f
                    time=System.currentTimeMillis()
                    elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos()
                }
                locations.setTestProviderLocation(LocationManager.GPS_PROVIDER,location)
            }
            for(step in 0..7) { fix(step); Thread.sleep(2100) }
            assertTrue("慢走的小步移动需要累积距离", run().distanceM > 5)
            compose.onNodeWithText("暂停",substring=false).performClick()
            compose.waitUntil(5000) { run().status=="PAUSED" }
            val pausedTime=run().durationMs
            val pausedDistance=run().distanceM
            fix(8)
            Thread.sleep(2500)
            assertEquals(pausedTime,run().durationMs)
            assertEquals(pausedDistance,run().distanceM,0.01)
            compose.onNodeWithText("继续运动",substring=false).performClick()
            compose.waitUntil(5000) { run().status=="RUNNING" }
            for(step in 9..40) {
                if(!run().active) break
                fix(step)
                Thread.sleep(2100)
                if(step==18) save("sport-live")
            }
            compose.waitUntil(15000) { run().status=="UPLOADED" }
            assertTrue(run().distanceM>=100)
            assertTrue(run().points.map { it.segment }.distinct().size>=2)
            assertTrue(run().simulated)
            val server=ApiClient.service.sportDetail(requireNotNull(run().serverId))
            assertEquals(0,server.code)
            assertTrue(requireNotNull(server.data).summary.targetReached)
            assertEquals(run().points.size,server.data?.points?.size)
            assertEquals("模拟定位验证记录",server.data?.summary?.remark)
            save("sport-finished")
            compose.onNodeWithText("完成",substring=false).performClick()
            save("sport-history")
        } finally {
            locations.removeTestProvider(LocationManager.GPS_PROVIDER)
            shell("appops set ${context.packageName} android:mock_location default")
        }
    }
    @Test fun indoorTrainingCanFinishManuallyWithoutGps() = runBlocking {
        compose.onNode(hasText("运动",substring=false) and hasClickAction()).performClick()
        compose.onNodeWithText("力量训练",substring=false).performClick()
        compose.onNodeWithText("开始力量训练",substring=false).performClick()
        compose.waitUntil(10000) { RunRepository.sessions.value.any { it.userId==940002L && it.active && it.sportType=="STRENGTH" } }
        val id=RunRepository.sessions.value.first { it.userId==940002L && it.active }.clientRecordId
        fun run()=RunRepository.sessions.value.first { it.clientRecordId==id }
        compose.waitUntil(5000) { run().durationSec>=2 }
        compose.onNodeWithText("结束",substring=false).performClick()
        compose.onNodeWithText("结束并保存",substring=false).performClick()
        compose.waitUntil(15000) { run().status=="UPLOADED" }
        val detail=requireNotNull(ApiClient.service.sportDetail(requireNotNull(run().serverId)).data)
        assertEquals("STRENGTH",detail.summary.sportType)
        assertEquals(0.0,detail.summary.distanceM,0.01)
        assertTrue(detail.points.isEmpty())
        save("indoor-finished")
    }
    @Test fun coachReminderCanBeReadFromMobileInbox() = runBlocking {
        val coach=requireNotNull(ApiClient.service.login(LoginRequest("coach_demo","123456")).data)
        val content="新功能验收：记得完成今天的训练"
        val existing=ApiClient.service.messages().required().items.firstOrNull { it.content==content && it.isRead==0 }
        if(existing==null) {
            val request=Request.Builder().url(ApiClient.BASE_URL+"api/v1/coach/students/940002/reminders")
                .header("Authorization","Bearer ${coach.token}")
                .post("""{"content":"$content"}""".toRequestBody("application/json; charset=utf-8".toMediaType())).build()
            OkHttpClient().newCall(request).execute().use {
                assertTrue(it.isSuccessful)
                val body=com.google.gson.JsonParser.parseString(requireNotNull(it.body).string()).asJsonObject
                assertEquals(body.toString(),0,body.get("code").asInt)
            }
        }
        compose.onNode(hasContentDescription("消息，",substring=true)).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText(content).fetchSemanticsNodes().isNotEmpty() }
        save("inbox")
        compose.onNodeWithText("标记已读",substring=false).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("标记已读",substring=false).fetchSemanticsNodes().isEmpty() }
        assertTrue(ApiClient.service.messages().required().items.first { it.content==content }.isRead==1)
        compose.onNodeWithText("关闭",substring=false).performClick()
        Unit
    }
    private fun shell(command:String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
    private fun save(name:String) {
        compose.waitForIdle()
        scenario?.onActivity { activity ->
            if (android.os.Build.VERSION.SDK_INT >= 30) activity.window.insetsController?.hide(android.view.WindowInsets.Type.ime())
        }
        Thread.sleep(700)
        compose.waitForIdle()
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(context.getExternalFilesDir(null),"wellness-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
}
