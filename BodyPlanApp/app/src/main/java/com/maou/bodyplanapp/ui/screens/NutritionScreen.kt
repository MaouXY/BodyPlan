package com.maou.bodyplanapp.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maou.bodyplanapp.data.*
import com.maou.bodyplanapp.ui.WellnessViewModel
import com.maou.bodyplanapp.ui.components.*
import com.maou.bodyplanapp.ui.todayDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val meals = listOf("BREAKFAST" to "早餐", "LUNCH" to "午餐", "DINNER" to "晚餐", "SNACK" to "加餐")

@Composable
fun NutritionScreen(vm: WellnessViewModel) {
    val context = LocalContext.current
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DietEntry?>(null) }
    var targetEditor by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<DietEntry?>(null) }
    val day = vm.nutrition?.takeIf { it.date == vm.date }
    LaunchedEffect(vm.userId) { vm.loadNutrition() }
    fun shift(offset: Int) {
        val calendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(vm.date)!!
            add(Calendar.DAY_OF_MONTH, offset)
        }
        vm.loadNutrition(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(calendar.time))
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(Icons.Outlined.KeyboardArrowLeft, "前一天", !vm.busy) { shift(-1) }
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { time = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(vm.date)!! }
                    DatePickerDialog(context, { _, year, month, date ->
                        vm.loadNutrition(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, date))
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                    }.show()
                }, modifier = Modifier.weight(1f), enabled = !vm.busy) { Text(if (vm.date == todayDate()) "今天 · ${vm.date}" else vm.date) }
                AppIconButton(Icons.Outlined.KeyboardArrowRight, "后一天", !vm.busy && vm.date < todayDate()) { shift(1) }
            }
        }
        if (day == null) item {
            EmptyContent(Icons.Outlined.List, if (vm.busy) "正在读取饮食记录" else "饮食记录未能加载", "")
            TextButton(onClick = { vm.loadNutrition() }, enabled = !vm.busy) { Text("重试") }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(String.format(Locale.ROOT, "%.0f", day.totals.kcal), style = MaterialTheme.typography.displaySmall)
                        Text("已摄入 · 千卡", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(day.target?.let { "目标 ${it.kcal} 千卡" } ?: "未设置目标", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { targetEditor = true }) { Text(if (day.target == null) "设置目标" else "调整目标") }
                    }
                }
                day.target?.let { target ->
                    LinearProgressIndicator(progress = { (day.totals.kcal / target.kcal).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    Text(if (day.totals.kcal > target.kcal) "超出 ${String.format(Locale.ROOT, "%.0f", day.totals.kcal - target.kcal)} 千卡"
                        else "距目标 ${String.format(Locale.ROOT, "%.0f", target.kcal - day.totals.kcal)} 千卡",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NutrientMetric("蛋白质", day.totals.protein, day.target?.protein, Modifier.weight(1f))
                    NutrientMetric("脂肪", day.totals.fat, day.target?.fat, Modifier.weight(1f))
                    NutrientMetric("碳水", day.totals.carbohydrate, day.target?.carbohydrate, Modifier.weight(1f))
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("膳食纤维", style = MaterialTheme.typography.bodyMedium)
                    Text("${if (!day.totals.fiberComplete) "已知 " else ""}${String.format(Locale.ROOT, "%.1f", day.totals.fiber)} 克${day.target?.let { " / ${it.fiber} 克" }.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (!day.totals.fiberComplete) Text("部分食物纤维数据暂缺", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            item {
                Button(onClick = { editing = null; editor = true }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("记录饮食")
                }
            }
            meals.forEach { (code, label) ->
                val entries = day.entries.filter { it.meal == code }
                item(key = "meal-$code") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("${String.format(Locale.ROOT, "%.0f", entries.sumOf { it.kcal })} 千卡", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (entries.isEmpty()) item(key = "empty-$code") { Text("尚未记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(entries.size, key = { entries[it].id }) { index ->
                    val entry = entries[index]
                    ListItem(
                        headlineContent = { Text(entry.foodName) },
                        supportingContent = { Text("${String.format(Locale.ROOT, "%.0f", entry.grams)} 克 · ${String.format(Locale.ROOT, "%.0f", entry.kcal)} 千卡") },
                        trailingContent = {
                            Row {
                                AppIconButton(Icons.Outlined.Edit, "修改${entry.foodName}", !vm.busy) { editing = entry; editor = true }
                                AppIconButton(Icons.Outlined.Delete, "删除${entry.foodName}", !vm.busy) { removing = entry }
                            }
                        }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
                    HorizontalDivider()
                }
            }
            item { Text("营养估算 · 以实际食材、包装与食用重量为准", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (editor) DietEditor(editing, vm.date, vm.userId, vm.busy, vm::searchFoods, { editor = false }) { request ->
        vm.saveDiet(editing?.id, request) { editor = false }
    }
    if (targetEditor) NutritionTargetEditor(day?.target, vm.date, vm.busy, { targetEditor = false }) { target ->
        vm.saveTarget(target) { targetEditor = false }
    }
    removing?.let { entry ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("删除这条饮食记录？") }, text = { Text(entry.foodName) },
            confirmButton = { TextButton(onClick = { removing = null; vm.removeDiet(entry.id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("取消") } })
    }
}

@Composable
private fun NutrientMetric(label: String, value: Double, target: Int?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${String.format(Locale.ROOT, "%.1f", value)} 克", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        target?.let { Text("目标 $it 克", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun DietEditor(entry: DietEntry?, date: String, userId: Long?, busy: Boolean,
    load: suspend (String, Int) -> PageResult<NutritionFood>, onDismiss: () -> Unit, onSave: (DietSave) -> Unit) {
    var foodId by remember { mutableStateOf(entry?.foodId) }
    var foodName by remember { mutableStateOf(entry?.foodName.orEmpty()) }
    var grams by remember { mutableStateOf(entry?.grams?.toInt()?.toString() ?: "100") }
    var meal by remember { mutableStateOf(entry?.meal ?: "BREAKFAST") }
    var picker by remember { mutableStateOf(entry == null) }
    var selected by remember { mutableStateOf<NutritionFood?>(null) }
    if (picker) {
        FoodPicker(userId, load, { picker = false }, {
            selected = it; foodId = it.id; foodName = it.name; picker = false
        })
        return
    }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (entry == null) "记录饮食" else "修改饮食") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)); Text(if (foodName.isBlank()) "选择食物" else foodName, Modifier.padding(start = 8.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                meals.forEach { (code, label) -> FilterChip(selected = meal == code, onClick = { meal = code }, label = { Text(label) }) }
            }
            NumberField("食用重量", grams, { grams = it }, suffix = "克")
            selected?.let { food ->
                val weight = grams.toDoubleOrNull() ?: 0.0
                Text("${String.format(Locale.ROOT, "%.0f", food.kcal * weight / 100)} 千卡 · 蛋白质 ${String.format(Locale.ROOT, "%.1f", food.protein * weight / 100)} 克",
                    style = MaterialTheme.typography.bodyMedium)
                Text(food.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }, confirmButton = { TextButton(onClick = { onSave(DietSave(date, meal, foodId!!, grams.toDouble())) },
        enabled = !busy && foodId != null && grams.toDoubleOrNull()?.let { it in 1.0..3000.0 } == true) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } })
}

@Composable
private fun FoodPicker(userId: Long?, load: suspend (String, Int) -> PageResult<NutritionFood>, onClose: () -> Unit, onChoose: (NutritionFood) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var retry by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<NutritionFood>>(emptyList()) }
    var total by remember { mutableLongStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(keyword, page, retry) {
        loading = true
        failed = false
        try {
            if (keyword.isNotBlank() && page == 1) delay(300)
            val result = load(keyword, page)
            rows = if (page == 1) result.items else (rows + result.items).distinctBy { it.id }
            total = result.total
        } catch (exception: CancellationException) {
            Log.d("BodyPlan", "scenario=food-picker userId=$userId page=$page cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=food-picker userId=$userId page=$page keyword=$keyword", exception)
            failed = true
        } finally { loading = false }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().widthIn(max = 560.dp).fillMaxHeight(0.9f).padding(16.dp), shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("选择食物", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    AppIconButton(Icons.Outlined.Close, "关闭食物选择", onClick = onClose)
                }
                OutlinedTextField(keyword, { keyword = it; page = 1; rows = emptyList(); total = 0; loading = true },
                    modifier = Modifier.fillMaxWidth(), label = { Text("搜索食物") }, singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    items(rows.size, key = { rows[it].id }) { index ->
                        val food = rows[index]
                        TextButton(onClick = { onChoose(food) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth()) {
                            ListItem(headlineContent = { Text(food.name) },
                                supportingContent = { Text("${String.format(Locale.ROOT, "%.0f", food.kcal)} 千卡 / 100克 · ${food.category}") })
                        }
                        HorizontalDivider()
                    }
                    item {
                        if (loading) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp)) }
                        else if (failed) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("食物加载失败"); TextButton(onClick = { retry++ }) { Text("重试") }
                        } else if (rows.isEmpty()) EmptyContent(Icons.Outlined.Search, "没有找到食物", "")
                        else if (rows.size < total) TextButton(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) { Text("加载更多") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionTargetEditor(target: NutritionTarget?, date: String, busy: Boolean, onClose: () -> Unit, onSave: (NutritionTarget) -> Unit) {
    var goal by remember { mutableStateOf(target?.goal ?: "MAINTAIN") }
    var kcal by remember { mutableStateOf(target?.kcal?.toString().orEmpty()) }
    var protein by remember { mutableStateOf(target?.protein?.toString().orEmpty()) }
    var fat by remember { mutableStateOf(target?.fat?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(target?.carbohydrate?.toString().orEmpty()) }
    var fiber by remember { mutableStateOf(target?.fiber?.toString().orEmpty()) }
    val valid = kcal.toIntOrNull() in 500..10000 && protein.toIntOrNull() in 0..1000
        && fat.toIntOrNull() in 0..1000 && carbs.toIntOrNull() in 0..2000 && fiber.toIntOrNull() in 0..200
    AlertDialog(onDismissRequest = onClose, title = { Text("每日营养目标") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LOSE_FAT" to "减脂", "BUILD_MUSCLE" to "增肌", "MAINTAIN" to "维持").forEach { (code, label) ->
                    FilterChip(selected = goal == code, onClick = { goal = code }, label = { Text(label) })
                }
            }
            NumberField("热量", kcal, { kcal = it }, suffix = "千卡")
            NumberField("蛋白质", protein, { protein = it }, suffix = "克")
            NumberField("脂肪", fat, { fat = it }, suffix = "克")
            NumberField("碳水化合物", carbs, { carbs = it }, suffix = "克")
            NumberField("膳食纤维", fiber, { fiber = it }, suffix = "克")
            Text("自定目标 · $date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(onClick = { onSave(NutritionTarget(date, goal, kcal.toInt(), protein.toInt(), fat.toInt(), carbs.toInt(), fiber.toInt())) }, enabled = valid && !busy) { Text("保存") } },
        dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("取消") } })
}
