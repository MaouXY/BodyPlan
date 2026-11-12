package com.maou.bodyplanapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.ui.*
import com.maou.bodyplanapp.ui.components.*
import java.util.Locale

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile = viewModel.profile
    var historyTab by remember { mutableIntStateOf(0) }
    var showAll by remember { mutableStateOf(false) }
    var logout by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(viewModel.displayName.ifBlank { "训练伙伴" }, style = MaterialTheme.typography.titleLarge)
                Text("${goalLabel(profile?.primaryGoal)} · ${levelLabel(viewModel.fitness?.level)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIconButton(Icons.Outlined.Edit, "修改档案", !viewModel.busy && viewModel.loadError == null, viewModel::editProfile)
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Metric(numberText(profile?.weightKg), "体重 / 公斤", Modifier.weight(1f))
                Metric(numberText(profile?.heightCm), "身高 / 厘米", Modifier.weight(1f))
                Metric("${profile?.score ?: "—"}", "身体评分", Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("身体指数", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(numberText(profile?.bmi), style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = viewModel::editProfile, enabled = !viewModel.busy && viewModel.loadError == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
            Text("更新身体数据与体能", Modifier.padding(start = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = historyTab == 0, onClick = { historyTab = 0; showAll = false }, label = { Text("身体记录") })
            FilterChip(selected = historyTab == 1, onClick = { historyTab = 1; showAll = false }, label = { Text("历史计划") })
        }
        if (historyTab == 0) {
            val history = viewModel.bodyHistory
            if (history.isEmpty()) EmptyContent(Icons.Outlined.Edit, "变化，从第一次记录开始", "更新身体数据后，记录会出现在这里")
            if (history.size >= 2) {
                val latest = history[0].weightKg
                val previous = history[1].weightKg
                if (latest != null && previous != null) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("较上次体重", style = MaterialTheme.typography.bodyMedium)
                    StatusLabel("${String.format(Locale.CHINA, "%+.1f", latest - previous)} 公斤")
                }
            }
            history.take(if (showAll) history.size else 5).forEach { row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(shortDate(row.recordedAt), style = MaterialTheme.typography.titleSmall)
                        Text("身体指数 ${numberText(row.bmi)} · 评分 ${row.score ?: "—"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${numberText(row.weightKg)} 公斤", style = MaterialTheme.typography.titleSmall)
                }
                HorizontalDivider()
            }
        } else {
            val history = viewModel.planHistory
            if (history.isEmpty()) EmptyContent(Icons.Outlined.Edit, "还没有历史计划", "每一阶段的训练都会留在这里")
            history.take(if (showAll) history.size else 5).forEach { plan ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(goalLabel(plan.goal), style = MaterialTheme.typography.titleSmall)
                        Text("${shortDate(plan.startDate)}至${shortDate(plan.endDate)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val currentDate = viewModel.today?.trainingDate ?: todayDate()
                    StatusLabel(planStatusLabel(plan, currentDate), warm = planExpired(plan, currentDate))
                }
                HorizontalDivider()
            }
        }
        if ((if (historyTab == 0) viewModel.bodyHistory.size else viewModel.planHistory.size) > 5) {
            TextButton(onClick = { showAll = !showAll }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (showAll) "收起记录" else "查看全部记录")
            }
        }
        TextButton(onClick = { logout = true }, enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth()) { Text("退出登录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(4.dp))
    }
    if (logout) AlertDialog(onDismissRequest = { logout = false },
        title = { Text("退出当前账号？") }, text = { Text("训练记录会保留，下次登录后可继续查看。") },
        confirmButton = { TextButton(onClick = { logout = false; viewModel.logout() }) { Text("退出登录") } },
        dismissButton = { TextButton(onClick = { logout = false }) { Text("取消") } })
}
