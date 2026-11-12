package com.maou.bodyplanapp

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maou.bodyplanapp.ui.AppViewModel
import com.maou.bodyplanapp.ui.screens.LoginScreen
import com.maou.bodyplanapp.ui.screens.OnboardingScreen
import com.maou.bodyplanapp.ui.theme.BodyPlanAppTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 仅浏览本地表单，不登录、注册、保存档案或提交训练记录。 */
@RunWith(AndroidJUnit4::class)
class AppearanceSmokeTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()

    private fun viewModel(): AppViewModel {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        return ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[AppViewModel::class.java]
    }

    @After fun clearModel() { compose.runOnUiThread { store.clear() } }

    @Test fun loginHasNoEmbeddedDemoCredentialsAndRegistrationRemainsReachable() {
        lateinit var model: AppViewModel
        compose.runOnUiThread { model = viewModel() }
        compose.setContent { BodyPlanAppTheme { Surface(Modifier.fillMaxSize()) { LoginScreen(model) } } }
        compose.waitUntil(15_000) { !model.busy }
        compose.onNodeWithText("律动健身").assertIsDisplayed()
        compose.onNodeWithText("demo", substring = true).assertDoesNotExist()
        compose.onNodeWithText("123456", substring = true).assertDoesNotExist()
        compose.onNodeWithText("后端地址", substring = true).assertDoesNotExist()
        compose.onNodeWithText("登录，开始训练").assertIsNotEnabled()
        saveScreen("login")
        compose.onNodeWithText("注册账号").performClick()
        compose.onNodeWithText("昵称（选填）").assertIsDisplayed()
        compose.onNodeWithText("创建账号").performScrollTo().assertIsDisplayed()
    }

    @Test fun onboardingMovesBetweenStepsWithoutSaving() {
        lateinit var model: AppViewModel
        compose.runOnUiThread { model = viewModel() }
        compose.setContent { BodyPlanAppTheme { Surface(Modifier.fillMaxSize()) { OnboardingScreen(model) } } }
        compose.onNodeWithText("认识你的身体").assertIsDisplayed()
        compose.waitUntil(15_000) { !model.busy }
        saveScreen("onboarding")
        compose.onNodeWithText("继续").performClick()
        compose.onNodeWithText("你想收获什么").assertIsDisplayed()
        compose.onNodeWithText("上一步").performClick()
        compose.onNodeWithText("认识你的身体").assertIsDisplayed()
    }

    private fun saveScreen(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val target = File(context.getExternalFilesDir(null), "ui-$name.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
