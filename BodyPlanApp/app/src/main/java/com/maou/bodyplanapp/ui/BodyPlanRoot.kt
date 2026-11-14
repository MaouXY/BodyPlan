package com.maou.bodyplanapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.maou.bodyplanapp.data.CoachMessage
import com.maou.bodyplanapp.tracking.RunRepository
import com.maou.bodyplanapp.ui.components.AppIconButton
import com.maou.bodyplanapp.ui.components.EmptyContent
import com.maou.bodyplanapp.ui.screens.*

@Composable
fun BodyPlanRoot(viewModel: AppViewModel = viewModel()) {
    val wellness: WellnessViewModel = viewModel()
    val lifecycle = LocalLifecycleOwner.current
    var offlineSport by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.sessionUserId) { wellness.bind(viewModel.sessionUserId); offlineSport = false }
    LaunchedEffect(viewModel.stage, viewModel.sessionUserId) {
        if (viewModel.stage == Stage.Ready) {
            wellness.loadOverview()
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) { wellness.pollInbox(); delay(30000) }
            }
        }
    }
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
    LaunchedEffect(wellness.error) { wellness.error?.let { snackbar.showSnackbar(it); wellness.consumeError() } }
    LaunchedEffect(wellness.notice) { wellness.notice?.let { snackbar.showSnackbar(it); wellness.consumeNotice() } }
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
                    viewModel.sessionUserId?.let {
                        TextButton(onClick = { offlineSport = true }) { Text("本地运动记录") }
                    }
                    TextButton(onClick = viewModel::logout, enabled = !viewModel.busy) { Text("返回登录") }
                }
                Stage.Login -> LoginScreen(viewModel)
                Stage.Onboarding -> OnboardingScreen(viewModel)
                Stage.Ready -> MainContent(viewModel, wellness, tab) { tab = it }
            }
            if (offlineSport && viewModel.sessionUserId != null) Surface(Modifier.fillMaxSize()) {
                Column {
                    TextButton(onClick = { offlineSport = false }) { Text("返回连接页面") }
                    Box(Modifier.weight(1f)) { SportScreen(wellness, viewModel.sessionUserId!!) }
                }
            }
            if (viewModel.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun MainContent(viewModel: AppViewModel, wellness: WellnessViewModel, tab: Int, onSelectTab: (Int) -> Unit) {
    val labels = listOf("今日", "运动", "饮食", "计划", "我的")
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.LocationOn, Icons.Outlined.List, Icons.Outlined.DateRange, Icons.Outlined.Person)
    var inboxOpen by remember { mutableStateOf(false) }
    var popup by remember { mutableStateOf<CoachMessage?>(null) }
    val shown = remember(viewModel.sessionUserId) { mutableStateListOf<Long>() }
    val runs by RunRepository.sessions.collectAsState()
    LaunchedEffect(wellness.inbox, tab, runs.any { it.active }, viewModel.busy) {
        if (!inboxOpen && !viewModel.busy && runs.none { it.active } && popup == null)
            popup = wellness.inbox.items.firstOrNull { it.isRead == 0 && it.id !in shown }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(listOf("律动健身", "运动", "饮食", "训练计划", "我的")[tab], style = MaterialTheme.typography.titleLarge)
                Text(if (tab == 0) "${shortDate(viewModel.today?.trainingDate ?: todayDate())} · 每一步，都算数"
                    else if (tab == 1) "每一步，都算数" else if (tab == 2) "认真对待每一餐" else if (tab == 3) "找到自己的训练节奏" else "记录每一次改变",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BadgedBox(badge = { if (wellness.inbox.unreadCount > 0) Badge { Text(wellness.inbox.unreadCount.coerceAtMost(99).toString()) } }) {
                AppIconButton(Icons.Outlined.Notifications, "消息，${wellness.inbox.unreadCount}条未读") { inboxOpen = true; wellness.refreshInbox() }
            }
            AppIconButton(Icons.Outlined.Refresh, "刷新数据", !viewModel.busy && !wellness.busy) {
                viewModel.refreshPlanAndToday()
                when(tab) { 1 -> wellness.loadSports(); 2 -> wellness.loadNutrition(); else -> wellness.loadOverview() }
            }
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
                0 -> TodayScreen(viewModel, onOpenPlan = { onSelectTab(3) }, wellness = wellness,
                    onOpenSport = { onSelectTab(1) }, onOpenNutrition = { onSelectTab(2) })
                1 -> viewModel.sessionUserId?.let { SportScreen(wellness, it) }
                2 -> NutritionScreen(wellness)
                3 -> PlanScreen(viewModel, onOpenToday = { onSelectTab(0) })
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
    if (inboxOpen) MessageInboxDialog(wellness, { inboxOpen = false })
    popup?.let { message ->
        AlertDialog(onDismissRequest = { shown.add(message.id); popup = null },
            title = { Text(message.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${message.senderName ?: "教练"} · ${message.taskDate}", style = MaterialTheme.typography.labelMedium)
                Text(message.content)
            } }, confirmButton = {
                TextButton(onClick = { shown.add(message.id); popup = null; wellness.read(message.id); onSelectTab(0) }, enabled = !wellness.busy) { Text("查看训练") }
            }, dismissButton = { TextButton(onClick = { shown.add(message.id); popup = null }) { Text("稍后") } })
    }
}

@Composable
private fun MessageInboxDialog(vm: WellnessViewModel, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("消息") }, text = {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (vm.inbox.items.isEmpty()) item { Text(if (vm.busy) "正在加载" else "暂无消息") }
            items(vm.inbox.items.size, key = { vm.inbox.items[it].id }) { index ->
                val message = vm.inbox.items[index]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${if (message.isRead == 0) "未读 · " else ""}${message.title}", style = MaterialTheme.typography.titleSmall)
                    Text(message.content, style = MaterialTheme.typography.bodyMedium)
                    Text("${message.senderName ?: "教练"} · ${message.taskDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (message.isRead == 0) TextButton(onClick = { vm.read(message.id) }, enabled = !vm.busy) { Text("标记已读") }
                    HorizontalDivider()
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
        dismissButton = { TextButton(onClick = vm::refreshInbox, enabled = !vm.busy) { Text("刷新") } })
}
