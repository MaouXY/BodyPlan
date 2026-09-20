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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.data.PlanItem
import com.maou.bodyplanapp.ui.AppViewModel
import com.maou.bodyplanapp.ui.components.InfoRow
import com.maou.bodyplanapp.ui.components.SectionCard
import com.maou.bodyplanapp.ui.goalLabel
import com.maou.bodyplanapp.ui.levelLabel
import com.maou.bodyplanapp.ui.planVolumeText

@Composable
fun PlanScreen(viewModel: AppViewModel) {
    val plan = viewModel.plan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (plan == null) {
            SectionCard(
                title = "还没有训练计划",
                subtitle = "完成建档后系统会自动生成；也可以在这里手动生成或按最新档案重新生成",
            ) {
                Button(
                    onClick = { viewModel.regeneratePlan() },
                    enabled = !viewModel.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("生成训练计划")
                }
            }
            return@Column
        }

        val total = plan.totalItems ?: 0
        val done = plan.doneItems ?: 0
        val progress = if (total > 0) done.toFloat() / total else 0f

        SectionCard(
            title = plan.name ?: "训练计划",
            subtitle = "${goalLabel(plan.goal)} · ${levelLabel(plan.level)} · ${plan.daysPerWeek ?: 0} 天/周 · ${plan.minutesPerSession ?: 0} 分钟/次",
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "已完成 $done / $total 个动作（${(progress * 100).toInt()}%）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            InfoRow(label = "开始日期", value = plan.startDate ?: "-")
            InfoRow(label = "结束日期", value = plan.endDate ?: "-")
        }

        SectionCard(
            title = "推荐依据",
            subtitle = "由「目标 + 体能等级 + 重点部位 + 伤病禁忌 + 场地器械」五要素规则生成",
        ) {
            Text(
                text = plan.reason ?: "暂无推荐依据",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        plan.days?.forEach { day ->
            SectionCard(
                title = "第 ${day.dayIndex ?: "-"} 天 · ${day.weekdayLabel ?: ""}",
                subtitle = day.trainingDate ?: "",
            ) {
                val items = day.items ?: emptyList()
                if (items.isEmpty()) {
                    Text("本日无训练安排", style = MaterialTheme.typography.bodySmall)
                } else {
                    items.forEachIndexed { index, item ->
                        PlanItemRow(index + 1, item)
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.regeneratePlan() },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("按最新档案重新生成计划")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlanItemRow(index: Int, item: PlanItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            Text(
                text = item.exerciseName ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = planVolumeText(item.sets, item.reps, item.durationSec, item.restSec),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
