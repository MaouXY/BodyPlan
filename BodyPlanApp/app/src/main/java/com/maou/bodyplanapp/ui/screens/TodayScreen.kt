package com.maou.bodyplanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.ui.AppViewModel
import com.maou.bodyplanapp.ui.components.Metric
import com.maou.bodyplanapp.ui.components.EmptyContent
import com.maou.bodyplanapp.ui.components.AppIconButton
import com.maou.bodyplanapp.ui.components.NumberField
import com.maou.bodyplanapp.ui.components.SectionCard
import com.maou.bodyplanapp.ui.planVolumeText
import com.maou.bodyplanapp.ui.planExpired
import com.maou.bodyplanapp.ui.shortDate
import com.maou.bodyplanapp.ui.todayDate
import com.maou.bodyplanapp.ui.theme.Pine
import com.maou.bodyplanapp.ui.theme.Mint
import java.util.Calendar

@Composable
fun TodayScreen(viewModel: AppViewModel, onOpenPlan: () -> Unit = {}) {
    var showFeedback by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Long?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Long?>(null) }
    val today = viewModel.today
    val available = !viewModel.busy && viewModel.loadError == null && today != null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        val total = today?.totalCount ?: 0
        val done = today?.doneCount ?: 0
        val expired = planExpired(viewModel.plan, today?.trainingDate ?: todayDate())
        Row(Modifier.fillMaxWidth().background(Pine).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (today?.hasTaskToday == true) "今天，也要为自己动一动" else "按自己的节奏，继续前进",
                    color = Mint, style = MaterialTheme.typography.bodySmall)
                Text(when {
                    today == null -> "准备训练中"
                    expired -> "开启新阶段"
                    total > 0 && done >= total -> "今日训练已完成"
                    total > 0 -> "今日训练"
                    else -> "今天是恢复日"
                }, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text(when {
                    today == null -> "正在读取你的训练安排"
                    expired -> "上一阶段已结束，准备再次出发"
                    total > 0 -> "已完成 $done / $total 个动作"
                    else -> "放松一下，也可以记录自由训练"
                }, color = Mint, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxSize(), color = Mint, trackColor = Color(0xFF396157), strokeWidth = 6.dp)
                Icon(if (total > 0 && done >= total) Icons.Outlined.CheckCircle else Icons.Outlined.Favorite,
                    null, Modifier.size(30.dp), tint = Mint)
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
            Metric("${viewModel.streak?.currentStreak ?: 0}", "连续训练 / 天", Modifier.weight(1f))
            Metric("${viewModel.streak?.totalDays ?: 0}", "累计训练 / 天", Modifier.weight(1f))
            Metric("${viewModel.streak?.maxStreak ?: 0}", "最佳连续 / 天", Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("今日安排", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenPlan) { Text("查看计划") }
        }
        if (today == null) {
            Text(if (viewModel.busy) "正在加载今日任务…" else "今日任务未能加载")
        } else if (today.hasTaskToday != true) {
            EmptyContent(Icons.Outlined.Favorite,
                if (expired) "该开启新计划了" else "给身体一点恢复时间",
                if (expired) "继续保持节奏，从新一轮训练开始"
                else today.nextTrainingDate?.let { "下一次训练：${shortDate(it)}" } ?: "散步、拉伸，也可以成为今天的运动")
            if (expired) Button(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("制定下一阶段计划")
            }
        } else {
            today.items.orEmpty().forEachIndexed { index, item ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            if (item.checkedIn == true) Icon(Icons.Outlined.CheckCircle, "已完成", tint = MaterialTheme.colorScheme.primary)
                            else Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.exerciseName ?: "训练动作", style = MaterialTheme.typography.titleMedium)
                            Text(planVolumeText(item.sets, item.reps, item.durationSec, item.restSec),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (item.checkedIn == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.durationMin ?: 0} 分钟 · ${feelingLabel(item.feeling)}", Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { pendingCancel = item.checkinId },
                                enabled = available && item.checkinId != null) { Text("撤销") }
                        }
                    } else {
                        Button(onClick = {
                            selectedItem = item.itemId
                            selectedName = item.exerciseName ?: "训练动作"
                            showFeedback = true
                        }, enabled = available && item.itemId != null, modifier = Modifier.fillMaxWidth()) { Text("完成并打卡") }
                    }
                }
                }
            }
        }
        OutlinedButton(onClick = {
            selectedItem = null
            selectedName = "自由训练"
            showFeedback = true
        }, enabled = available, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Text("记录自由训练", Modifier.padding(start = 8.dp))
        }

        CalendarSection(viewModel)

        SectionCard(title = "训练足迹") {
            if (viewModel.checkinHistory.isEmpty()) Text("第一次打卡，从今天开始。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            viewModel.checkinHistory.take(if (showHistory) 100 else 3).forEach { record ->
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                Text(record.exerciseName?.takeIf { it.isNotBlank() }
                    ?: if (record.planItemId == null) "自由训练" else "计划训练",
                    style = MaterialTheme.typography.titleSmall)
                Text("${shortDate(record.trainingDate)} · ${record.durationMin ?: 0} 分钟 · ${feelingLabel(record.feeling)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!record.remark.isNullOrBlank()) Text(record.remark, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { pendingCancel = record.id },
                    enabled = available && record.id != null) { Text("撤销") }
                }
            }
            if (viewModel.checkinHistory.size > 3) TextButton(onClick = { showHistory = !showHistory },
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (showHistory) "收起记录" else "查看近期记录")
            }
        }
        Spacer(Modifier.height(8.dp))
        }
    }

    pendingCancel?.let { id ->
        AlertDialog(onDismissRequest = { pendingCancel = null }, title = { Text("撤销这次打卡？") },
            text = { Text("撤销后，训练进度和连续打卡天数会重新计算。") },
            confirmButton = { TextButton(onClick = { pendingCancel = null; viewModel.cancelCheckin(id) },
                enabled = !viewModel.busy) { Text("撤销打卡") } },
            dismissButton = { TextButton(onClick = { pendingCancel = null }) { Text("保留记录") } })
    }
    if (showFeedback) FeedbackDialog(
        title = selectedName,
        busy = viewModel.busy,
        error = viewModel.error,
        isFree = selectedItem == null,
        loadExercises = viewModel::searchExercises,
        userId = viewModel.profile?.userId,
        onDismiss = { if (!viewModel.busy) showFeedback = false },
        onSubmit = { minutes, feeling, remark, exerciseId ->
            viewModel.checkin(selectedItem, feeling, minutes, remark, exerciseId) { showFeedback = false }
        },
    )
}

@Composable
internal fun FeedbackDialog(title: String, busy: Boolean, error: String?, isFree: Boolean,
    loadExercises: suspend (Long, String) -> com.maou.bodyplanapp.data.PageResult<com.maou.bodyplanapp.data.ExerciseOption>,
    userId: Long? = null, onDismiss: () -> Unit, onSubmit: (Int, String, String, Long?) -> Unit) {
    var minutes by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf("NORMAL") }
    var remark by remember { mutableStateOf("") }
    var selectedExercise by remember { mutableStateOf<com.maou.bodyplanapp.data.ExerciseOption?>(null) }
    var choosingExercise by remember { mutableStateOf(isFree) }
    val valid = minutes.toIntOrNull()?.let { it in 1..300 } == true && remark.length <= 255 &&
        (!isFree || selectedExercise != null)
    if (choosingExercise) {
        ExercisePickerDialog(loadExercises = loadExercises, userId = userId,
            onDismiss = { choosingExercise = false },
            onSelect = { selectedExercise = it; choosingExercise = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (isFree) {
                    OutlinedButton(onClick = { choosingExercise = true }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(selectedExercise?.name?.let { "$it · 更换" } ?: "选择运动")
                    }
                    selectedExercise?.cautions?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                NumberField("训练了多久", minutes, { minutes = it }, suffix = "分钟")
                Text("记录 1 至 300 分钟", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("EASY", "NORMAL", "HARD").forEach { value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = feeling == value, onClick = { feeling = value }, enabled = !busy)
                        Text(feelingLabel(value))
                    }
                }
                OutlinedTextField(value = remark, onValueChange = { remark = it }, enabled = !busy,
                    label = { Text("备注（可选）") }, supportingText = { Text("${remark.length}/255") },
                    isError = remark.length > 255, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(minutes.toInt(), feeling, remark, selectedExercise?.id) },
                enabled = !busy && valid) { Text("确认打卡") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}

@Composable
private fun CalendarSection(viewModel: AppViewModel) {
    val calendar = viewModel.calendar
    SectionCard(title = "打卡日历") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一月", !viewModel.busy) { viewModel.changeMonth(-1) }
            Text("${viewModel.calendarMonth.take(4)}年${viewModel.calendarMonth.takeLast(2).toIntOrNull()}月",
                style = MaterialTheme.typography.titleSmall)
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一月", !viewModel.busy) { viewModel.changeMonth(1) }
        }
        if (calendar != null) {
            Text("本月已训练 ${calendar.activeDays} 天", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val firstDay = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.YEAR, calendar.month.substring(0, 4).toInt())
                set(Calendar.MONTH, calendar.month.substring(5, 7).toInt() - 1)
            }
            val offset = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val cells = List<com.maou.bodyplanapp.data.CheckinCalendarDay?>(offset) { null } + calendar.days
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (0..6).forEach { index ->
                        val day = week.getOrNull(index)
                        Column(Modifier.weight(1f).height(52.dp).padding(3.dp)
                            .background(if (day?.checkedIn == true) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent, RoundedCornerShape(8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Text(day?.date?.takeLast(2)?.toIntOrNull()?.toString().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (day?.date == (viewModel.today?.trainingDate ?: todayDate())) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (day?.checkedIn == true) Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

private fun feelingLabel(code: String?) = when (code) {
    "EASY" -> "轻松"
    "HARD" -> "吃力"
    "NORMAL" -> "适中"
    else -> "未填写"
}
