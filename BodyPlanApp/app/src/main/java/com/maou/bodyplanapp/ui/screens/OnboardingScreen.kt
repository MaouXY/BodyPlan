package com.maou.bodyplanapp.ui.screens

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
    var step by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }

    var gender by remember { mutableStateOf("MALE") }
    var age by remember { mutableStateOf("28") }
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("78") }
    var bodyType by remember { mutableStateOf("APPLE") }

    var primaryGoal by remember { mutableStateOf("LOSE_FAT") }
    val subGoals = remember { mutableStateListOf<String>() }

    val focusParts = remember { mutableStateListOf("ABDOMEN", "LEG") }
    val sportTypes = remember { mutableStateListOf("CARDIO", "STRENGTH") }
    val weeklyDays = remember { mutableStateListOf(1, 3, 5) }
    var dailyMinutes by remember { mutableStateOf("30") }
    var remindEnabled by remember { mutableStateOf(true) }
    var remindTime by remember { mutableStateOf("20:30") }

    val injuries = remember { mutableStateListOf("KNEE") }
    val trainLimits = remember { mutableStateListOf("DORM", "NO_JUMP", "NO_EQUIPMENT") }

    var pushUp by remember { mutableStateOf("15") }
    var squat by remember { mutableStateOf("25") }
    var crunch by remember { mutableStateOf("20") }
    var stairStatus by remember { mutableStateOf("SLIGHT_HARD") }

    fun validateCurrentStep(): String? = when (step) {
        0 -> when {
            age.toIntOrNull() == null -> "请填写年龄"
            height.toDoubleOrNull() == null -> "请填写身高"
            weight.toDoubleOrNull() == null -> "请填写体重"
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
        else -> null
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
            text = "建档问卷（第 ${step + 1} / $TOTAL_STEPS 步）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when (step) {
                0 -> "填写基础身体数据，系统据此计算 BMI"
                1 -> "系统会根据身体数据推荐目标，也可以自己选"
                2 -> "选择重点部位、运动偏好与每周可训练时间"
                3 -> "填写伤病与训练限制，生成计划时会避开这些动作"
                4 -> "完成简单体能测试，用于确定计划难度等级"
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
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
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
                Text(if (step < TOTAL_STEPS - 1) "下一步" else "提交并生成计划")
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
    SectionCard(title = "基础身体数据", subtitle = "用于计算 BMI 与推荐目标") {
        ChipFlow(GENDER_OPTIONS, setOf(gender), onGenderChange)
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "年龄", value = age, onValueChange = onAgeChange, suffix = "岁")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "身高", value = height, onValueChange = onHeightChange, suffix = "cm")
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(label = "体重", value = weight, onValueChange = onWeightChange, suffix = "kg")
    }
    SectionCard(title = "身体类型") {
        ChipFlow(BODY_TYPE_OPTIONS, setOf(bodyType), onBodyTypeChange)
    }
}

@Composable
private fun StepGoal(
    viewModel: AppViewModel,
    primaryGoal: String,
    onPrimaryGoalChange: (String) -> Unit,
    subGoals: List<String>,
    onToggleSubGoal: (String) -> Unit,
) {
    SectionCard(
        title = "训练目标",
        subtitle = "可以先看系统建议，再结合自己的想法选择",
    ) {
        Button(
            onClick = { viewModel.loadGoalSuggestion() },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("获取系统目标建议")
        }
        viewModel.goalSuggestion?.let { suggestion ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "建议主目标：${goalLabel(suggestion.recommendedGoal)}（BMI ${suggestion.bmi ?: "-"}，${suggestion.bmiLevel ?: "-"}）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = suggestion.reason ?: "",
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
    SectionCard(title = "主要目标（单选）") {
        ChipFlow(GOAL_OPTIONS, setOf(primaryGoal), onPrimaryGoalChange)
    }
    SectionCard(title = "次要目标（可多选）") {
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
    SectionCard(title = "重点部位（可多选）") {
        ChipFlow(PART_OPTIONS, focusParts.toSet()) { focusParts.toggleValue(it) }
    }
    SectionCard(title = "喜欢的运动类型（可多选）") {
        ChipFlow(SPORT_TYPE_OPTIONS, sportTypes.toSet()) { sportTypes.toggleValue(it) }
    }
    SectionCard(title = "每周训练日（可多选）") {
        ChipFlow(
            WEEK_DAY_OPTIONS.map { it.first.toString() to it.second },
            weeklyDays.map { it.toString() }.toSet(),
        ) { code -> code.toIntOrNull()?.let { weeklyDays.toggleValue(it) } }
    }
    SectionCard(title = "每日运动时长") {
        ChipFlow(DURATION_OPTIONS, setOf(dailyMinutes), onDailyMinutesChange)
    }
    SectionCard(title = "训练提醒") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("开启训练提醒")
            Switch(checked = remindEnabled, onCheckedChange = onRemindEnabledChange)
        }
        if (remindEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = remindTime,
                onValueChange = onRemindTimeChange,
                label = { Text("提醒时间（HH:mm）") },
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
        title = "伤病情况（可多选）",
        subtitle = "生成计划时会排除与伤病冲突的动作",
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
        title = "训练限制（可多选）",
        subtitle = "宿舍、零噪音、不能跳、无器械等限制会直接影响动作筛选",
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
        subtitle = "三项成绩可留空表示暂时做不了，系统会按固定规则折算 L1/L2/L3 等级",
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
