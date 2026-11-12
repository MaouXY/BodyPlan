package com.maou.bodyplanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.data.PlanItem
import com.maou.bodyplanapp.ui.*
import com.maou.bodyplanapp.ui.components.*
import com.maou.bodyplanapp.ui.theme.Pine
import com.maou.bodyplanapp.ui.theme.Mint

@Composable
fun PlanScreen(viewModel: AppViewModel, onOpenToday: () -> Unit = {}) {
    val plan = viewModel.plan
    var confirmUpdate by remember { mutableStateOf(false) }
    var explanation by rememberSaveable { mutableStateOf(false) }
    val today = viewModel.today?.trainingDate ?: todayDate()
    val days = remember(plan) { displayPlanDays(plan) }
    val initialIndex = days.indexOfFirst { it.trainingDate.orEmpty() >= today }.coerceAtLeast(0)
    var selectedIndex by rememberSaveable(plan?.id) { mutableIntStateOf(initialIndex) }
    val weeks = days.withIndex().groupBy { it.index / 7 }
    var selectedWeek by rememberSaveable(plan?.id) {
        mutableIntStateOf(initialIndex / 7)
    }
    val dateScroll = rememberScrollState()
    LaunchedEffect(selectedWeek) { dateScroll.scrollTo(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (plan == null) {
            EmptyContent(Icons.Outlined.DateRange, if (viewModel.busy) "正在加载计划" else "开启你的训练计划",
                if (viewModel.loadError != null) "稍后重试，计划会保留在这里" else "从适合自己的强度开始")
            Button(onClick = viewModel::regeneratePlan, enabled = !viewModel.busy && viewModel.loadError == null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Text("生成计划") }
            return@Column
        }
        val total = plan.totalItems ?: 0
        val done = plan.doneItems ?: 0
        val progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
        Column(Modifier.fillMaxWidth().background(Pine).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(goalLabel(plan.goal), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                StatusLabel(planStatusLabel(plan, today), warm = planExpired(plan, today))
            }
            Text("${levelLabel(plan.level)} · ${shortDate(plan.startDate)}至${shortDate(plan.endDate)}",
                style = MaterialTheme.typography.bodySmall, color = Mint)
            Row(Modifier.fillMaxWidth()) {
                Metric("${plan.daysPerWeek ?: 0} 天", "每周训练", Modifier.weight(1f), light = true)
                Metric("${plan.minutesPerSession ?: 0} 分钟", "单次时长", Modifier.weight(1f), light = true)
                Metric("${(progress * 100).toInt()}%", "完成进度", Modifier.weight(1f), light = true)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Mint, trackColor = Color(0xFF396157))
            Text("已完成 $done / $total 个动作", style = MaterialTheme.typography.labelSmall, color = Mint)
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (planExpired(plan, today)) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("上一阶段已结束", style = MaterialTheme.typography.titleSmall)
                            Text("准备好开启新一轮训练了吗？", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { confirmUpdate = true }, enabled = !viewModel.busy && viewModel.loadError == null) {
                            Text("更新计划")
                        }
                    }
                }
            }
            Text("训练日程", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.forEach { (week, entries) ->
                    FilterChip(selected = selectedWeek == week,
                        onClick = { selectedWeek = week; selectedIndex = entries.first().index },
                        label = { Text("第 ${week + 1} 周") })
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(dateScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks[selectedWeek].orEmpty().forEach { indexed ->
                    val day = indexed.value
                    val selected = selectedIndex == indexed.index
                    Surface(onClick = { selectedIndex = indexed.index }, shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                        Column(Modifier.width(64.dp).heightIn(min = 80.dp).padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(day.weekdayLabel ?: "训练日", style = MaterialTheme.typography.labelSmall)
                            Text(day.trainingDate?.takeLast(2)?.toIntOrNull()?.toString() ?: "—",
                                style = MaterialTheme.typography.titleLarge)
                            Text(if (day.trainingDate == today) "今天" else if (day.items.isNullOrEmpty()) "休息" else "训练",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            val selected = days.getOrNull(selectedIndex)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(shortDate(selected?.trainingDate), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("${selected?.items.orEmpty().size} 个动作", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected?.items.isNullOrEmpty()) {
                EmptyContent(Icons.Outlined.CheckCircle, "恢复也是训练的一部分", "今天留点时间，好好休息")
            } else {
                selected?.items.orEmpty().forEachIndexed { index, item -> ExerciseRow(index + 1, item) }
            }
            if (selected?.trainingDate == today) {
                Button(onClick = onOpenToday, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("去完成今日训练")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 8.dp).size(18.dp))
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            TextButton(onClick = { explanation = !explanation }, contentPadding = PaddingValues(0.dp)) {
                Text("关于这份计划", Modifier.weight(1f))
                Icon(if (explanation) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
            }
            if (explanation) {
                Text("围绕${goalLabel(plan.goal)}安排训练，采用${levelLabel(plan.level)}强度，每周训练 ${plan.daysPerWeek ?: 0} 天。调整身体状态或训练偏好后，可重新生成计划。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { confirmUpdate = true }, enabled = !viewModel.busy && viewModel.loadError == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重新制定计划") }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmUpdate) AlertDialog(onDismissRequest = { confirmUpdate = false },
        title = { Text("开启新一轮训练？") },
        text = { Text("将根据当前档案生成新计划，原计划和训练记录会保留在「我的」中。") },
        confirmButton = { TextButton(onClick = { confirmUpdate = false; viewModel.regeneratePlan() }) { Text("生成新计划") } },
        dismissButton = { TextButton(onClick = { confirmUpdate = false }) { Text("再想想") } })
}

@Composable
private fun ExerciseRow(index: Int, item: PlanItem) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Text(index.toString().padStart(2, '0'), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.exerciseName ?: "训练动作", style = MaterialTheme.typography.titleSmall)
                Text(planVolumeText(item.sets, item.reps, item.durationSec, item.restSec),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.status == "DONE") Icon(Icons.Outlined.CheckCircle, "已完成", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
