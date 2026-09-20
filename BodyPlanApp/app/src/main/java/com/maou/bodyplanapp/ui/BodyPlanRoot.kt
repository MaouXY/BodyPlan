package com.maou.bodyplanapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maou.bodyplanapp.ui.screens.LoginScreen
import com.maou.bodyplanapp.ui.screens.OnboardingScreen
import com.maou.bodyplanapp.ui.screens.PlanScreen
import com.maou.bodyplanapp.ui.screens.TodayScreen

@Composable
fun BodyPlanRoot(viewModel: AppViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (viewModel.stage) {
                Stage.Booting -> BootingContent()
                Stage.Login -> LoginScreen(viewModel)
                Stage.Onboarding -> OnboardingScreen(viewModel)
                Stage.Ready -> MainContent(viewModel)
            }
            if (viewModel.busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun BootingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "正在恢复登录状态…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun MainContent(viewModel: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = if (tab == 0) "我的训练计划" else "今日打卡",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = viewModel.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { viewModel.logout() }) { Text("退出登录") }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (tab == 0) {
                PlanScreen(viewModel)
            } else {
                TodayScreen(viewModel)
            }
        }

        NavigationBar {
            NavigationBarItem(
                selected = tab == 0,
                onClick = { tab = 0 },
                icon = { Text("📅") },
                label = { Text("我的计划") },
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { tab = 1 },
                icon = { Text("✅") },
                label = { Text("今日打卡") },
            )
        }
    }
}
