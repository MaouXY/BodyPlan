package com.maou.bodyplanapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maou.bodyplanapp.ui.components.AppIconButton
import com.maou.bodyplanapp.ui.components.EmptyContent
import com.maou.bodyplanapp.ui.screens.*

@Composable
fun BodyPlanRoot(viewModel: AppViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(viewModel.stage) {
        if (viewModel.stage == Stage.Login) tab = 0
    }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let { snackbar.showSnackbar(it); viewModel.consumeError() }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (viewModel.stage) {
                Stage.Booting -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Text("正在准备你的训练", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Stage.ConnectionError -> Column(Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyContent(Icons.Outlined.Info, "暂时无法连接", "请检查网络，稍后再试")
                    Button(onClick = viewModel::retryConnection, enabled = !viewModel.busy) { Text("重新连接") }
                    TextButton(onClick = viewModel::logout, enabled = !viewModel.busy) { Text("返回登录") }
                }
                Stage.Login -> LoginScreen(viewModel)
                Stage.Onboarding -> OnboardingScreen(viewModel)
                Stage.Ready -> MainContent(viewModel, tab) { tab = it }
            }
            if (viewModel.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun MainContent(viewModel: AppViewModel, tab: Int, onSelectTab: (Int) -> Unit) {
    val labels = listOf("今日", "计划", "我的")
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.DateRange, Icons.Outlined.Person)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(listOf("律动健身", "训练计划", "我的")[tab], style = MaterialTheme.typography.titleLarge)
                Text(if (tab == 0) "${shortDate(viewModel.today?.trainingDate ?: todayDate())} · 每一步，都算数"
                    else if (tab == 1) "找到自己的训练节奏" else "记录每一次改变",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIconButton(Icons.Outlined.Refresh, "刷新数据", !viewModel.busy, viewModel::refreshPlanAndToday)
        }
        viewModel.loadError?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("未能刷新，当前显示上次数据", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = viewModel::refreshPlanAndToday, enabled = !viewModel.busy) { Text("重试") }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TodayScreen(viewModel, onOpenPlan = { onSelectTab(1) })
                1 -> PlanScreen(viewModel, onOpenToday = { onSelectTab(0) })
                else -> ProfileScreen(viewModel)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)) {
            labels.forEachIndexed { index, label ->
                NavigationBarItem(selected = tab == index, onClick = { onSelectTab(index) },
                    icon = { Icon(icons[index], null) }, label = { Text(label) })
            }
        }
    }
}
