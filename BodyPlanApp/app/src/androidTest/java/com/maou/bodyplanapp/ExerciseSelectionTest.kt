package com.maou.bodyplanapp

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maou.bodyplanapp.data.ExerciseOption
import com.maou.bodyplanapp.data.PageResult
import com.maou.bodyplanapp.ui.screens.ExercisePickerDialog
import com.maou.bodyplanapp.ui.screens.FeedbackDialog
import com.maou.bodyplanapp.ui.theme.BodyPlanAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 使用内存动作列表与提交回调，不连接后端、不写业务数据。 */
@RunWith(AndroidJUnit4::class)
class ExerciseSelectionTest {
    @get:Rule val compose = createComposeRule()
    private val running = ExerciseOption(7, "慢跑", "FULL_BODY", "RUNNING")
    private val stretching = ExerciseOption(8, "肩颈拉伸", "SHOULDER_NECK", "STRETCH")

    @Test fun selectionIsRequiredAndChangingExerciseKeepsFeedback() {
        var savedId: Long? = null
        var savedMinutes = 0
        compose.setContent {
            BodyPlanAppTheme {
                FeedbackDialog("自由训练", false, null, true,
                    loadExercises = { page, _ -> PageResult(listOf(running, stretching), page, 30, 2) },
                    onDismiss = {}, onSubmit = { minutes, _, _, id -> savedMinutes = minutes; savedId = id })
            }
        }
        compose.onNodeWithContentDescription("关闭运动选择").performClick()
        compose.onNodeWithText("训练了多久").performTextInput("12")
        compose.onNodeWithText("确认打卡").assertIsNotEnabled()
        compose.onNodeWithText("选择运动").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("慢跑").fetchSemanticsNodes().isNotEmpty() }
        saveDialog("exercise-picker")
        compose.onNodeWithText("慢跑").performClick()
        compose.onNodeWithText("慢跑 · 更换").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("肩颈拉伸").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("肩颈拉伸").performClick()
        compose.onNodeWithText("训练了多久").assertTextContains("12")
        saveDialog("exercise-feedback")
        compose.onNodeWithText("确认打卡").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(8L, savedId); assertEquals(12, savedMinutes) }
    }

    @Test fun failedSearchCanRetryAndNoMatchesShowAnEmptyState() {
        var calls = 0
        compose.setContent {
            BodyPlanAppTheme {
                ExercisePickerDialog(loadExercises = { page, keyword ->
                    calls++
                    if (calls == 1) throw java.io.IOException("simulated catalogue failure")
                    PageResult(if (keyword.isBlank()) listOf(running) else emptyList(), page, 30, if (keyword.isBlank()) 1 else 0)
                }, userId = 42, onDismiss = {}, onSelect = {})
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("重试").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重试").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("慢跑").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("搜索运动").performTextInput("不存在的动作")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("没有找到相关运动，换个关键词试试").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("慢跑").assertDoesNotExist()
    }

    private fun saveDialog(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun laterPagesRemainSelectable() {
        var selected: Long? = null
        compose.setContent {
            BodyPlanAppTheme {
                ExercisePickerDialog(loadExercises = { page, _ ->
                    PageResult(if (page == 1L) listOf(running) else listOf(stretching), page, 1, 2)
                }, userId = 42, onDismiss = {}, onSelect = { selected = it.id })
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("加载更多").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("加载更多").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("肩颈拉伸").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("慢跑").assertIsDisplayed()
        compose.onNodeWithText("肩颈拉伸").performClick()
        compose.runOnIdle { assertEquals(8L, selected) }
    }
}
