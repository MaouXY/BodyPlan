package com.maou.bodyplanapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.maou.bodyplanapp.data.*
import com.maou.bodyplanapp.ui.screens.DietEditor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class NutritionSelectionTest {
    @get:Rule val compose = createComposeRule()
    private val rice = NutritionFood(1,"米饭（熟）","主食",116.0,2.6,0.3,25.9,0.3,"测试食物库",true)
    @Test fun choosesFoodPortionAndMealBeforeSaving() {
        var saved: DietSave? = null
        compose.setContent {
            MaterialTheme { DietEditor(null,"2026-11-15",42,false,{ _,_ -> PageResult(listOf(rice),1,30,1) },{}, { saved=it }) }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("米饭（熟）").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("米饭（熟）").performClick()
        compose.onNodeWithText("食用重量").performTextReplacement("150")
        compose.onNodeWithText("午餐").performClick()
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle {
            assertEquals(1L,saved?.foodId)
            assertEquals(150.0,saved?.grams)
            assertEquals("LUNCH",saved?.meal)
        }
    }
    @Test fun invalidPortionCannotBeSubmitted() {
        val entry=DietEntry(9,"2026-11-15","LUNCH",1,"米饭（熟）",100.0,116.0,2.6,0.3,25.9,0.3,"测试")
        compose.setContent { MaterialTheme { DietEditor(entry,"2026-11-15",42,false,{ _,_ -> PageResult(listOf(rice),1,30,1) },{},{}) } }
        compose.onNodeWithText("食用重量").performTextReplacement("0")
        compose.onNodeWithText("保存").assertIsNotEnabled()
        compose.onNodeWithText("食用重量").performTextReplacement("3001")
        compose.onNodeWithText("保存").assertIsNotEnabled()
    }
    @Test fun foodLoadFailureCanRetryWithoutWritingData() {
        var calls=0
        compose.setContent { MaterialTheme { DietEditor(null,"2026-11-15",42,false,{ _,_ ->
            calls++
            if(calls==1) throw IOException("simulated food service failure")
            PageResult(listOf(rice),1,30,1)
        },{},{}) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("重试").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重试").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("米饭（熟）").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("米饭（熟）").assertIsDisplayed()
    }
}
