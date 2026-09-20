package com.maou.bodyplanapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.data.TodayTaskItem
import com.maou.bodyplanapp.ui.AppViewModel
import com.maou.bodyplanapp.ui.components.InfoRow
import com.maou.bodyplanapp.ui.components.SectionCard
import com.maou.bodyplanapp.ui.planVolumeText

@Composable
fun TodayScreen(viewModel: AppViewModel) {
    val today = viewModel.today
    val streak = viewModel.streak

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "今日概览", subtitle = "数据来自当前执行中的训练计划") {
            InfoRow(label = "训练日期", value = today?.trainingDate ?: today?.taskDate ?: "-")
            InfoRow(label = "当前计划", value = today?.planName ?: "-")
            InfoRow(
                // 非训练日展示的是「下一个训练日」的任务量，标签要跟着变，避免和「今天没有安排」自相矛盾
                label = if (today?.hasTaskToday == true) "今日完成" else "下次任务",
                value = "${today?.doneCount ?: 0} / ${today?.totalCount ?: 0} 个动作",
            )
            InfoRow(
                label = "连续打卡",
                value = "${streak?.currentStreak ?: 0} 天（最长 ${streak?.maxStreak ?: 0} 天）",
            )
            InfoRow(label = "累计打卡", value = "${streak?.totalDays ?: 0} 天")
        }

        if (today?.hasTaskToday != true) {
            SectionCard(
                title = "今天没有训练安排",
                subtitle = "下次训练日：${today?.nextTrainingDate ?: "尚未生成计划"}",
            ) {
                Text(
                    text = "训练日按建档时选择的每周训练日安排，非训练日可以休息或做拉伸。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            today?.items?.forEach { item ->
                TaskItemCard(
                    item = item,
                    onCheckin = {
                        viewModel.checkin(
                            itemId = item.itemId ?: 0L,
                            feeling = "NORMAL",
                            durationMin = viewModel.plan?.minutesPerSession,
                        )
                    },
                    onCancel = {
                        item.checkinId?.let { viewModel.cancelCheckin(it) }
                    },
                )
            }
        }

        Button(
            onClick = { viewModel.refreshPlanAndToday() },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("刷新今日任务")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TaskItemCard(
    item: TodayTaskItem,
    onCheckin: () -> Unit,
    onCancel: () -> Unit,
) {
    val checkedIn = item.checkedIn == true

    SectionCard(
        title = item.exerciseName ?: "-",
        subtitle = planVolumeText(item.sets, item.reps, item.durationSec, item.restSec),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (checkedIn) "已完成打卡" else "待完成",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (checkedIn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                item.durationMin?.let { minutes ->
                    Text(
                        text = "本次记录时长：$minutes 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (checkedIn) {
                TextButton(onClick = onCancel) { Text("撤销打卡") }
            } else {
                Button(onClick = onCheckin) { Text("打卡") }
            }
        }
    }
}
