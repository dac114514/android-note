package com.faster.note.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    schedule: ScheduleEntity?,
    categories: List<CategoryEntity>,
    defaultDateMillis: Long = System.currentTimeMillis(),
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
    var scheduleDate by remember(schedule) { mutableStateOf(schedule?.date ?: defaultDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (showDetails && newValue == SheetValue.Hidden) false
            else true
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEdit) "编辑日程" else "添加日程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
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
                    Spacer(Modifier.width(8.dp))
                }
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
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存")
                }
            }
            Spacer(Modifier.height(20.dp))

            // === Required: Title ===
            Text(
                text = "标题 *",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("日程标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // === Required: Date ===
            Text(
                text = "日期",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = {
                    val cal = Calendar.getInstance().apply { timeInMillis = scheduleDate }
                    SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(cal.time)
                }(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
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

            // === All-day toggle ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "全天事件",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }
            Spacer(Modifier.height(16.dp))

            // === Category ===
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            CategoryPicker(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = { selectedCategoryId = it }
            )
            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            // === Collapsible: More details ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "更多详情",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showDetails) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    if (!isAllDay) {
                        Text(
                            text = "开始时间",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = "%02d:%02d".format(startHour, startMinute),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("开始时间") },
                            trailingIcon = {
                                IconButton(onClick = { showStartTimePicker = true }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "选择开始时间")
                                }
                            }
                        )
                        if (showStartTimePicker) {
                            val startTimePickerState = rememberTimePickerState(
                                initialHour = startHour,
                                initialMinute = startMinute,
                                is24Hour = true
                            )
                            AlertDialog(
                                onDismissRequest = { showStartTimePicker = false },
                                title = { Text("选择开始时间") },
                                text = { TimePicker(state = startTimePickerState) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        startHour = startTimePickerState.hour
                                        startMinute = startTimePickerState.minute
                                        showStartTimePicker = false
                                    }) { Text("确认") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showStartTimePicker = false }) { Text("取消") }
                                }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "结束时间",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = "%02d:%02d".format(endHour, endMinute),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("结束时间") },
                            trailingIcon = {
                                IconButton(onClick = { showEndTimePicker = true }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "选择结束时间")
                                }
                            }
                        )
                        if (showEndTimePicker) {
                            val endTimePickerState = rememberTimePickerState(
                                initialHour = endHour,
                                initialMinute = endMinute,
                                is24Hour = true
                            )
                            AlertDialog(
                                onDismissRequest = { showEndTimePicker = false },
                                title = { Text("选择结束时间") },
                                text = { TimePicker(state = endTimePickerState) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        endHour = endTimePickerState.hour
                                        endMinute = endTimePickerState.minute
                                        showEndTimePicker = false
                                    }) { Text("确认") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEndTimePicker = false }) { Text("取消") }
                                }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

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
                }
            }

        }
    }
}
