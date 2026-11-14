package com.maou.bodyplanapp

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.maou.bodyplanapp.tracking.LocalRun
import com.maou.bodyplanapp.tracking.RunRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class RunRepositoryTest {
    private lateinit var context: Context
    private val user = Long.MAX_VALUE - 901
    @Before fun isolatedStorage() {
        val original = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(original.cacheDir, "wellness-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(original) { override fun getFilesDir(): File = directory }
    }
    private fun draft() = LocalRun(user, UUID.randomUUID().toString(), "RUN", "AEROBIC", 3000, 360, true,
        System.currentTimeMillis(), durationMs = 12000, distanceM = 40.0)

    @Test fun interruptedRunRestoresPausedWithoutInventingTime() = runBlocking {
        RunRepository.bind(context, user + 1)
        RunRepository.bind(context, user)
        val original = draft()
        RunRepository.save(context, original)
        RunRepository.bind(context, user + 1)
        RunRepository.bind(context, user)
        val restored = RunRepository.sessions.value.single()
        assertEquals("PAUSED", restored.status)
        assertEquals(12000L, restored.durationMs)
        assertEquals(40.0, restored.distanceM, 0.01)
        assertEquals(1, restored.segment)
        assertEquals(3000, restored.targetDistanceM)
        RunRepository.discard(context, restored.clientRecordId, user)
        assertTrue(RunRepository.sessions.value.isEmpty())
    }
    @Test fun finishedPendingUploadSurvivesReloadAndAccountsAreIsolated() = runBlocking {
        RunRepository.bind(context, user + 1)
        RunRepository.bind(context, user)
        val finished = draft().copy(status = "FINISHED", endedAt = System.currentTimeMillis())
        RunRepository.save(context, finished)
        RunRepository.bind(context, user + 1)
        assertTrue(RunRepository.sessions.value.isEmpty())
        try {
            RunRepository.save(context, finished)
            fail("不能跨账号写入运动记录")
        } catch (expected: IllegalStateException) {
            android.util.Log.i("BodyPlan", "scenario=test-sport-owner userId=$user expected-rejection", expected)
        }
        RunRepository.bind(context, user)
        val restored = RunRepository.sessions.value.single()
        assertEquals("FINISHED", restored.status)
        assertEquals(finished.request(), restored.request())
        assertNull(restored.serverId)
    }
    @Test fun corruptDraftIsReportedAndOriginalFileIsPreserved() = runBlocking {
        RunRepository.bind(context, user + 1)
        val directory = File(context.filesDir, "sport-runs/$user").apply { mkdirs() }
        val file = File(directory, "damaged-record.json")
        file.writeText("{损坏的草稿", Charsets.UTF_8)
        RunRepository.problem.value = null
        RunRepository.bind(context, user)
        assertTrue(RunRepository.sessions.value.isEmpty())
        assertTrue(requireNotNull(RunRepository.problem.value).contains("原文件已保留"))
        assertEquals("{损坏的草稿", file.readText(Charsets.UTF_8))
        RunRepository.problem.value = null
    }
}
