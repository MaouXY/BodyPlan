package com.maou.bodyplanapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maou.bodyplanapp.data.FitnessTestRequest
import com.maou.bodyplanapp.data.ProfileSaveRequest
import com.maou.bodyplanapp.ui.AppViewModel
import com.maou.bodyplanapp.ui.BODY_TYPE_OPTIONS
import com.maou.bodyplanapp.ui.DURATION_OPTIONS
import com.maou.bodyplanapp.ui.GENDER_OPTIONS
import com.maou.bodyplanapp.ui.GOAL_OPTIONS
import com.maou.bodyplanapp.ui.INJURY_OPTIONS
import com.maou.bodyplanapp.ui.PART_OPTIONS
import com.maou.bodyplanapp.ui.SPORT_TYPE_OPTIONS
import com.maou.bodyplanapp.ui.STAIR_OPTIONS
import com.maou.bodyplanapp.ui.TRAIN_LIMIT_OPTIONS
import com.maou.bodyplanapp.ui.WEEK_DAY_OPTIONS
import com.maou.bodyplanapp.ui.components.NumberField
import com.maou.bodyplanapp.ui.components.SectionCard
import com.maou.bodyplanapp.ui.goalLabel
import com.maou.bodyplanapp.ui.toggleValue

private const val TOTAL_STEPS = 5

@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    val saved = viewModel.profile
    val tested = viewModel.fitness
    var step by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    val formScroll = rememberScrollState()
    LaunchedEffect(step) { formScroll.scrollTo(0); hint = null }
    BackHandler(enabled = step > 0 || viewModel.editingProfile) {
        if (!viewModel.busy) {
            if (step > 0) step -= 1 else viewModel.cancelEdit()
        }
    }

    var gender by remember { mutableStateOf(saved?.gender?.takeIf { it != "UNKNOWN" } ?: "MALE") }
    var age by remember { mutableStateOf(saved?.age?.toString() ?: "28") }
    var height by remember { mutableStateOf(saved?.heightCm?.toString() ?: "175") }
    var weight by remember { mutableStateOf(saved?.weightKg?.toString() ?: "78") }
    var bodyType by remember { mutableStateOf(saved?.bodyType ?: "APPLE") }

    var primaryGoal by remember { mutableStateOf(saved?.primaryGoal ?: "LOSE_FAT") }
    val subGoals = remember { mutableStateListOf<String>().apply { addAll(saved?.subGoals.orEmpty()) } }

    val focusParts = remember { mutableStateListOf<String>().apply { addAll(saved?.focusParts ?: listOf("ABDOMEN", "LEG")) } }
    val sportTypes = remember { mutableStateListOf<String>().apply { addAll(saved?.sportTypes ?: listOf("CARDIO", "STRENGTH")) } }
    val weeklyDays = remember { mutableStateListOf<Int>().apply { addAll(saved?.weeklyDays ?: listOf(1, 3, 5)) } }
    var dailyMinutes by remember { mutableStateOf(saved?.dailyMinutes?.toString() ?: "30") }
    var remindEnabled by remember { mutableStateOf(saved?.remindEnabled == 1) }
    var remindTime by remember { mutableStateOf(saved?.remindTime ?: "20:30") }

    val injuries = remember { mutableStateListOf<String>().apply { addAll(saved?.injuries ?: listOf("NONE")) } }
    val trainLimits = remember { mutableStateListOf<String>().apply { addAll(saved?.trainLimits.orEmpty()) } }

    var pushUp by remember { mutableStateOf(tested?.pushUp?.toString() ?: "") }
    var squat by remember { mutableStateOf(tested?.squat?.toString() ?: "") }
    var crunch by remember { mutableStateOf(tested?.crunch?.toString() ?: "") }
    var stairStatus by remember { mutableStateOf(tested?.stairStatus ?: "HARD") }

    fun validateCurrentStep(): String? = when (step) {
        0 -> when {
            age.toIntOrNull() !in 10..100 -> "年龄应为 10-100 岁"
            height.toDoubleOrNull()?.let { it.isFinite() && it in 100.0..250.0 } != true -> "身高应为 100 至 250 厘米"
            weight.toDoubleOrNull()?.let { it.isFinite() && it in 25.0..250.0 } != true -> "体重应为 25 至 250 公斤"
            else -> null
        }
        1 -> if (primaryGoal.isBlank()) "请选择主要训练目标" else null
        2 -> when {
            focusParts.isEmpty() -> "请至少选择一个重点部位"
            sportTypes.isEmpty() -> "请至少选择一种运动偏好"
            weeklyDays.isEmpty() -> "请至少选择一个训练日"
            dailyMinutes.toIntOrNull() == null -> "请选择每日运动时长"
            else -> null
        }
        3 -> null
        else -> if (listOf(pushUp, squat, crunch).any { it.isNotBlank() && (it.toIntOrNull()?.let { n -> n in 0..500 } != true) })
            "体能成绩请填写 0-500 的整数，或留空" else null
    }

    fun submit() {
        val request = ProfileSaveRequest(
            gender = gender,
            age = age.toIntOrNull(),
            heightCm = height.toDoubleOrNull(),
            weightKg = weight.toDoubleOrNull(),
            bodyType = bodyType,
            primaryGoal = primaryGoal,
            subGoals = subGoals.toList(),
            focusParts = focusParts.toList(),
            sportTypes = sportTypes.toList(),
            weeklyDays = weeklyDays.sorted(),
            dailyMinutes = dailyMinutes.toIntOrNull(),
            remindEnabled = if (remindEnabled) 1 else 0,
            remindTime = if (remindEnabled) remindTime else null,
            injuries = injuries.toList(),
            trainLimits = trainLimits.toList(),
        )
        val fitness = FitnessTestRequest(
            pushUp = pushUp.toIntOrNull(),
            squat = squat.toIntOrNull(),
            crunch = crunch.toIntOrNull(),
            stairStatus = stairStatus,
        )
        viewModel.submitOnboarding(request, fitness)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = listOf("认识你的身体", "你想收获什么", "找到训练节奏", "照顾好自己", "了解当前体能")[step],
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when (step) {
                0 -> "第 1 步，共 5 步 · 从现在的身体状态开始"
                1 -> "第 2 步，共 5 步 · 选择最想实现的改变"
                2 -> "第 3 步，共 5 步 · 把运动融入你的日常"
                3 -> "第 4 步，共 5 步 · 让训练更适合你"
                4 -> "最后一步 · 按实际能力填写，不必勉强"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / TOTAL_STEPS },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(formScroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                0 -> StepBasic(
                    gender = gender,
                    onGenderChange = { gender = it },
                    age = age,
                    onAgeChange = { age = it },
                    height = height,
                    onHeightChange = { height = it },
                    weight = weight,
                    onWeightChange = { weight = it },
                    bodyType = bodyType,
                    onBodyTypeChange = { bodyType = it },
                )
                1 -> StepGoal(
                    viewModel = viewModel,
                    onPreview = {
                        viewModel.loadGoalSuggestion(ProfileSaveRequest(gender = gender, age = age.toIntOrNull(),
                            heightCm = height.toDoubleOrNull(), weightKg = weight.toDoubleOrNull(), bodyType = bodyType))
                    },
                    primaryGoal = primaryGoal,
                    onPrimaryGoalChange = { primaryGoal = it },
                    subGoals = subGoals,
                    onToggleSubGoal = { subGoals.toggleValue(it) },
                )
                2 -> StepPreference(
                    focusParts = focusParts,
                    sportTypes = sportTypes,
                    weeklyDays = weeklyDays,
                    dailyMinutes = dailyMinutes,
                    onDailyMinutesChange = { dailyMinutes = it },
                    remindEnabled = remindEnabled,
                    onRemindEnabledChange = { remindEnabled = it },
                    remindTime = remindTime,
                    onRemindTimeChange = { remindTime = it },
                )
                3 -> StepInjury(
                    injuries = injuries,
                    trainLimits = trainLimits,
                )
                else -> StepFitness(
                    pushUp = pushUp,
                    onPushUpChange = { pushUp = it },
                    squat = squat,
                    onSquatChange = { squat = it },
                    crunch = crunch,
                    onCrunchChange = { crunch = it },
                    stairStatus = stairStatus,
                    onStairStatusChange = { stairStatus = it },
                )
            }
        }

        hint?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step == 0 && viewModel.editingProfile) {
                TextButton(onClick = { viewModel.cancelEdit() }, enabled = !viewModel.busy) { Text("取消") }
            }
            if (step > 0) {
                TextButton(onClick = { step -= 1 }, enabled = !viewModel.busy) {
                    Text("上一步")
                }
            }
            Button(
                onClick = {
                    val problem = validateCurrentStep()
                    if (problem != null) {
                        hint = problem
                        return@Button
                    }
                    hint = null
                    if (step < TOTAL_STEPS - 1) {
                        step += 1
                    } else {
                        submit()
                    }
                },
                enabled = !viewModel.busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (step < TOTAL_STEPS - 1) "继续" else if (viewModel.editingProfile) "保存并更新计划" else "开启我的训练")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (code, label) ->
            FilterChip(
                selected = selected.contains(code),
                onClick = { onToggle(code) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun StepBasic(
    gender: String,
    onGenderChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    bodyType: String,
    onBodyTypeChange: (String) -> Unit,
) {
    SectionCard(title = "基础信息") {
        ChipFlow(GENDER_OPTIONS, setOf(gender), onGenderChange)
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "年龄", value = age, onValueChange = onAgeChange, suffix = "岁")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "身高", value = height, onValueChange = onHeightChange, suffix = "厘米")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "体重", value = weight, onValueChange = onWeightChange, suffix = "公斤")
    }
    SectionCard(title = "身体类型") {
        ChipFlow(BODY_TYPE_OPTIONS, setOf(bodyType), onBodyTypeChange)
    }
}

@Composable
private fun StepGoal(
    viewModel: AppViewModel,
    onPreview: () -> Unit,
    primaryGoal: String,
    onPrimaryGoalChange: (String) -> Unit,
    subGoals: List<String>,
    onToggleSubGoal: (String) -> Unit,
) {
    SectionCard(
        title = "还没想好目标？",
    ) {
        Button(
            onClick = onPreview,
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("看看适合我的目标")
        }
        viewModel.goalSuggestion?.let { suggestion ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "推荐：${goalLabel(suggestion.recommendedGoal)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "身体指数 ${suggestion.bmi ?: "—"} · 你也可以选择其他目标",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            suggestion.alternatives?.takeIf { it.isNotEmpty() }?.let { alternatives ->
                Text(
                    text = "备选目标：${alternatives.joinToString("、") { goalLabel(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
    SectionCard(title = "最想实现的目标") {
        ChipFlow(GOAL_OPTIONS, setOf(primaryGoal), onPrimaryGoalChange)
    }
    SectionCard(title = "还希望改善哪些方面", subtitle = "可多选") {
        ChipFlow(GOAL_OPTIONS, subGoals.toSet(), onToggleSubGoal)
    }
}

@Composable
private fun StepPreference(
    focusParts: MutableList<String>,
    sportTypes: MutableList<String>,
    weeklyDays: MutableList<Int>,
    dailyMinutes: String,
    onDailyMinutesChange: (String) -> Unit,
    remindEnabled: Boolean,
    onRemindEnabledChange: (Boolean) -> Unit,
    remindTime: String,
    onRemindTimeChange: (String) -> Unit,
) {
    SectionCard(title = "想重点练哪里", subtitle = "可多选") {
        ChipFlow(PART_OPTIONS, focusParts.toSet()) { focusParts.toggleValue(it) }
    }
    SectionCard(title = "喜欢哪些运动", subtitle = "可多选") {
        ChipFlow(SPORT_TYPE_OPTIONS, sportTypes.toSet()) { sportTypes.toggleValue(it) }
    }
    SectionCard(title = "每周哪几天有空") {
        ChipFlow(
            WEEK_DAY_OPTIONS.map { it.first.toString() to it.second },
            weeklyDays.map { it.toString() }.toSet(),
        ) { code -> code.toIntOrNull()?.let { weeklyDays.toggleValue(it) } }
    }
    SectionCard(title = "每日运动时长") {
        ChipFlow(DURATION_OPTIONS, setOf(dailyMinutes), onDailyMinutesChange)
    }
    SectionCard(title = "习惯几点训练", subtitle = "仅记录偏好，不发送通知") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("记录时间偏好")
            Switch(checked = remindEnabled, onCheckedChange = onRemindEnabledChange)
        }
        if (remindEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = remindTime,
                onValueChange = onRemindTimeChange,
                label = { Text("训练时间，例如 20:30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepInjury(
    injuries: MutableList<String>,
    trainLimits: MutableList<String>,
) {
    SectionCard(
        title = "哪些部位需要留意",
        subtitle = "可多选",
    ) {
        ChipFlow(INJURY_OPTIONS, injuries.toSet()) { code ->
            if (code == "NONE") {
                injuries.clear()
                injuries.add("NONE")
            } else {
                injuries.remove("NONE")
                injuries.toggleValue(code)
            }
        }
    }
    SectionCard(
        title = "你的训练环境",
        subtitle = "可多选",
    ) {
        ChipFlow(TRAIN_LIMIT_OPTIONS, trainLimits.toSet()) { trainLimits.toggleValue(it) }
    }
}

@Composable
private fun StepFitness(
    pushUp: String,
    onPushUpChange: (String) -> Unit,
    squat: String,
    onSquatChange: (String) -> Unit,
    crunch: String,
    onCrunchChange: (String) -> Unit,
    stairStatus: String,
    onStairStatusChange: (String) -> Unit,
) {
    SectionCard(
        title = "基础体能测试",
        subtitle = "填写一次能完成的数量，暂时做不了可以留空",
    ) {
        NumberField(label = "一次俯卧撑", value = pushUp, onValueChange = onPushUpChange, suffix = "个")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "一次深蹲", value = squat, onValueChange = onSquatChange, suffix = "个")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "一次卷腹", value = crunch, onValueChange = onCrunchChange, suffix = "个")
    }
    SectionCard(title = "上五楼后的状态") {
        ChipFlow(STAIR_OPTIONS, setOf(stairStatus), onStairStatusChange)
    }
}
