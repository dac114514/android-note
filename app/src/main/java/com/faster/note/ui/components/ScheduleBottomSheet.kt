package com.faster.note.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity

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
    var scheduleDate by remember { mutableStateOf(schedule?.date ?: Calendar.getInstance().timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

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

            // Basic info section
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(12.dp))

            // Date picker field
            OutlinedTextField(
                value = {
                    val cal = Calendar.getInstance().apply { timeInMillis = scheduleDate }
                    SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(cal.time)
                }(),
                onValueChange = {},
                label = { Text("日期") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                    }
                }
            )
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = scheduleDate
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { scheduleDate = it }
                            showDatePicker = false
                        }) { Text("确认") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("全天事件", modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }
            Spacer(Modifier.height(12.dp))

            if (!isAllDay) {
                Text(
                    text = "开始时间",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = "%02d".format(startHour),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) startHour = h } },
                        label = { Text("时") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        ":",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = "%02d".format(startMinute),
                        onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) startMinute = m } },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "结束时间",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = "%02d".format(endHour),
                        onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) endHour = h } },
                        label = { Text("时") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        ":",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = "%02d".format(endMinute),
                        onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) endMinute = m } },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Category section
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            CategoryPicker(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )
            Spacer(Modifier.height(16.dp))

            // Details section
            Text(
                text = "详情",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

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
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                    Spacer(Modifier.weight(1f))
                }
                OutlinedButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val startMillis = if (isAllDay) null else {
                                Calendar.getInstance().apply {
                                    timeInMillis = scheduleDate
                                    set(Calendar.HOUR_OF_DAY, startHour)
                                    set(Calendar.MINUTE, startMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            val endMillis = if (isAllDay) null else {
                                Calendar.getInstance().apply {
                                    timeInMillis = scheduleDate
                                    set(Calendar.HOUR_OF_DAY, endHour)
                                    set(Calendar.MINUTE, endMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            onSave(ScheduleEntity(
                                id = schedule?.id ?: 0,
                                title = title.trim(),
                                startTime = startMillis,
                                endTime = endMillis,
                                isAllDay = isAllDay,
                                categoryId = selectedCategoryId,
                                location = location.ifBlank { null },
                                notes = notes.ifBlank { null },
                                date = scheduleDate,
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
