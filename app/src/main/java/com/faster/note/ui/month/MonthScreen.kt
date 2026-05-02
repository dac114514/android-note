package com.faster.note.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.note.ui.components.ApiKeyDialog
import com.faster.note.ui.components.CalendarGrid
import com.faster.note.ui.components.MarkdownText
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    viewModel: MonthViewModel,
    onDaySelected: (Int, Int, Int) -> Unit,
    onNavigateToDay: (Int, Int, Int) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "月视图",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { showMonthPicker = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.year}年 ${uiState.month}月",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("搜索日程...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Calendar grid
            CalendarGrid(
                year = uiState.year,
                month = uiState.month,
                markedDateCounts = uiState.markedDateCounts,
                selectedDate = uiState.selectedDay,
                onDateSelected = { day ->
                    viewModel.selectDay(day)
                    onDaySelected(uiState.year, uiState.month, day)
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Selected date schedule preview
            if (uiState.selectedDay != null) {
                val selectedDay = uiState.selectedDay!!
                val dayCal = remember(uiState.year, uiState.month, selectedDay) {
                    Calendar.getInstance().apply {
                        set(uiState.year, uiState.month - 1, selectedDay)
                    }
                }
                val dateStr = remember(dayCal) {
                    java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINESE).format(dayCal.time)
                }

                Surface(
                    onClick = { onNavigateToDay(uiState.year, uiState.month, selectedDay) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.selectedDaySchedules.isEmpty()) {
                                Text(
                                    text = "创建日程 >",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "查看完整日程 >",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        if (uiState.selectedDaySchedules.isEmpty()) {
                            Text(
                                text = "暂无日程",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            val previewSchedules = uiState.selectedDaySchedules.take(3)
                            previewSchedules.forEach { schedule ->
                                val category = uiState.categories.find { it.id == schedule.categoryId }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    if (category != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(androidx.compose.ui.graphics.Color(category.color))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = schedule.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (schedule.isCompleted) {
                                        Text(
                                            text = "✓",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (uiState.selectedDaySchedules.size > 3) {
                                Text(
                                    text = "还有 ${uiState.selectedDaySchedules.size - 3} 项...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))

            // Stats overview
            Text(
                text = "本月概览",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("总日程", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("${uiState.totalCount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("已完成", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${uiState.completedCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("完成率", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${if (uiState.totalCount > 0) (uiState.completedCount * 100 / uiState.totalCount) else 0}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // AI Summary card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("AI 分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))

                    if (!uiState.aiApiKeyConfigured) {
                        Surface(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "未配置 API Key，点击配置",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    ">",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else if (uiState.aiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("AI 分析中...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (uiState.aiAnalysisText.isNotBlank()) {
                        MarkdownText(
                            text = uiState.aiAnalysisText,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.requestAiAnalysis() }) {
                            Text("重新分析")
                        }
                    } else {
                        Text(
                            "点击下方按钮，将本月日程发送给 AI 生成分析报告",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.requestAiAnalysis() }) {
                            Text("开始分析")
                        }
                    }

                    if (uiState.aiError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.aiError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { viewModel.requestAiAnalysis() }) {
                            Text("重试")
                        }
                    }
                }
            }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Month picker dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            currentYear = uiState.year,
            currentMonth = uiState.month,
            onYearMonthSelected = { year, month ->
                viewModel.goToYearMonth(year, month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    // API Key dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = "",
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onYearMonthSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerYear by remember { mutableIntStateOf(currentYear) }
    val months = listOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { pickerYear-- }) {
                    Text("<", fontSize = 18.sp)
                }
                Text(
                    text = "${pickerYear}年",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { pickerYear++ }) {
                    Text(">", fontSize = 18.sp)
                }
            }
        },
        text = {
            Column {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..2) {
                            val monthIndex = row * 3 + col
                            val monthNum = monthIndex + 1
                            val isCurrent = pickerYear == currentYear && monthNum == currentMonth

                            TextButton(
                                onClick = { onYearMonthSelected(pickerYear, monthNum) },
                                modifier = Modifier.size(80.dp, 44.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = months[monthIndex],
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
