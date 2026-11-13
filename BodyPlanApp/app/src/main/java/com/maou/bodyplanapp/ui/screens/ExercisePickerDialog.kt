package com.maou.bodyplanapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maou.bodyplanapp.data.ExerciseOption
import com.maou.bodyplanapp.data.PageResult
import com.maou.bodyplanapp.ui.partLabel
import com.maou.bodyplanapp.ui.sportTypeLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun ExercisePickerDialog(
    loadExercises: suspend (Long, String) -> PageResult<ExerciseOption>,
    userId: Long?, onDismiss: () -> Unit, onSelect: (ExerciseOption) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var page by remember { mutableLongStateOf(1) }
    var retry by remember { mutableIntStateOf(0) }
    var exercises by remember { mutableStateOf<List<ExerciseOption>>(emptyList()) }
    var total by remember { mutableLongStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(keyword, page, retry) {
        loading = true
        failed = false
        try {
            if (keyword.isNotBlank() && page == 1L) delay(300)
            val result = loadExercises(page, keyword)
            exercises = (if (page == 1L) result.items else exercises + result.items).distinctBy { it.id }
            total = result.total
        } catch (exception: CancellationException) {
            Log.d("BodyPlan", "scenario=exercise-picker userId=$userId page=$page reason=cancelled", exception)
            throw exception
        } catch (exception: Exception) {
            Log.e("BodyPlan", "scenario=exercise-picker userId=$userId page=$page keyword=$keyword", exception)
            failed = true
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(0.9f).imePadding(),
            shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("选择运动", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "关闭运动选择") }
                }
                OutlinedTextField(value = keyword, onValueChange = {
                    keyword = it; page = 1; exercises = emptyList(); total = 0; loading = true
                }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("搜索运动") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) })
                LazyColumn(Modifier.weight(1f)) {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(headlineContent = { Text(exercise.name) }, supportingContent = {
                            Text("${sportTypeLabel(exercise.sportType)} · ${partLabel(exercise.part)}")
                        }, modifier = Modifier.fillMaxWidth().clickable { onSelect(exercise) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        when {
                            loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp))
                            }
                            failed -> Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("运动列表加载失败，请重试", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { retry++ }) { Text("重试") }
                            }
                            exercises.isEmpty() -> Text(if (keyword.isBlank()) "暂无可选运动" else "没有找到相关运动，换个关键词试试",
                                Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            exercises.size.toLong() < total -> TextButton(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) {
                                Text("加载更多")
                            }
                        }
                    }
                }
            }
        }
    }
}
