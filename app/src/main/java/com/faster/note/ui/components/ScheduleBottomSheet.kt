package com.faster.note.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    schedule: ScheduleEntity?,
    categories: List<CategoryEntity>,
    onSave: (ScheduleEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val isEdit = schedule != null
    var title by remember { mutableStateOf(schedule?.title ?: "") }
    var location by remember { mutableStateOf(schedule?.location ?: "") }
    var notes by remember { mutableStateOf(schedule?.notes ?: "") }
    var isAllDay by remember { mutableStateOf(schedule?.isAllDay ?: false) }
    var selectedCategoryId by remember { mutableStateOf(schedule?.categoryId) }
    var startHour by remember { mutableIntStateOf(if (schedule?.startTime != null) {
        Calendar.getInstance().apply { timeInMillis = schedule.startTime }.get(Calendar.HOUR_OF_DAY)
    } else 9) }
    var startMinute by remember { mutableIntStateOf(if (schedule?.startTime != null) {
        Calendar.getInstance().apply { timeInMillis = schedule.startTime }.get(Calendar.MINUTE)
    } else 0) }
    var endHour by remember { mutableIntStateOf(if (schedule?.endTime != null) {
        Calendar.getInstance().apply { timeInMillis = schedule.endTime }.get(Calendar.HOUR_OF_DAY)
    } else 10) }
    var endMinute by remember { mutableIntStateOf(if (schedule?.endTime != null) {
        Calendar.getInstance().apply { timeInMillis = schedule.endTime }.get(Calendar.MINUTE)
    } else 0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isEdit) "编辑日程" else "添加日程",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("全天事件", modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }
            Spacer(Modifier.height(12.dp))

            if (!isAllDay) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = "%02d".format(startHour),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) startHour = h } },
                        label = { Text("开始") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = "%02d".format(startMinute),
                        onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) startMinute = m } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = "%02d".format(endHour),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) endHour = h } },
                        label = { Text("结束") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = "%02d".format(endMinute),
                        onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) endMinute = m } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Text("分类", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            CategoryPicker(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("地点") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEdit && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("删除") }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(ScheduleEntity(
                                id = schedule?.id ?: 0,
                                title = title.trim(),
                                startTime = null, // simplified for MVP
                                endTime = null,
                                isAllDay = isAllDay,
                                categoryId = selectedCategoryId,
                                location = location.ifBlank { null },
                                notes = notes.ifBlank { null },
                                date = schedule?.date ?: Calendar.getInstance().timeInMillis,
                                createdAt = schedule?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            ))
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("保存") }
            }
        }
    }
}
