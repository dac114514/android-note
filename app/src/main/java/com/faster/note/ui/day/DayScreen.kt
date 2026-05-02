package com.faster.note.ui.day

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.ui.components.ApiKeyDialog
import com.faster.note.ui.components.ScheduleBottomSheet
import com.faster.note.ui.components.ScheduleCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    viewModel: DayViewModel,
    onNavigateToMonth: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }

    val currentViewDateMillis = remember(uiState.year, uiState.month, uiState.day) {
        Calendar.getInstance().apply {
            set(uiState.year, uiState.month - 1, uiState.day)
        }.timeInMillis
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val cal = remember(uiState.year, uiState.month, uiState.day) {
                            Calendar.getInstance().apply {
                                set(uiState.year, uiState.month - 1, uiState.day)
                            }
                        }
                        Text(
                            text = dateFormat.format(cal.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.year}年${uiState.month}月",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::goToToday) {
                        Icon(Icons.Default.Today, contentDescription = "今天", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("今天")
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main content
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress bar
                if (uiState.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { uiState.completedCount.toFloat() / uiState.totalCount },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "已完成 ${uiState.completedCount}/${uiState.totalCount}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Schedule list
                if (uiState.schedules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val dateDisplayText = remember(uiState.year, uiState.month, uiState.day) {
                                val cal = Calendar.getInstance().apply {
                                    set(uiState.year, uiState.month - 1, uiState.day)
                                }
                                SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(cal.time)
                            }
                            Text(
                                text = "${dateDisplayText} 暂无日程",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "点击 + 添加日程",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.schedules, key = { it.id }) { schedule ->
                            val category = uiState.categories.find { it.id == schedule.categoryId }
                            ScheduleCard(
                                schedule = schedule,
                                category = category,
                                onToggleCompleted = { viewModel.toggleCompleted(schedule) },
                                onClick = {
                                    editingSchedule = schedule
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                }
            }

            // FABs column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // AI analyze button
                SmallFloatingActionButton(
                    onClick = {
                        if (uiState.aiApiKeyConfigured) {
                            showAiDialog = true
                            viewModel.requestDayAiAnalysis()
                        } else {
                            showApiKeyDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI 分析", modifier = Modifier.size(22.dp))
                }

                // Add schedule button
                FloatingActionButton(
                    onClick = {
                        editingSchedule = null
                        showBottomSheet = true
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加日程", modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (showBottomSheet) {
        ScheduleBottomSheet(
            schedule = editingSchedule,
            categories = uiState.categories,
            defaultDateMillis = currentViewDateMillis,
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                showBottomSheet = false
            },
            onDelete = if (editingSchedule != null) ({
                viewModel.deleteSchedule(editingSchedule!!.id)
                showBottomSheet = false
            }) else null,
            onDismiss = { showBottomSheet = false }
        )
    }

    // AI analysis dialog
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("今日 AI 分析")
                }
            },
            text = {
                Column {
                    if (uiState.aiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("AI 分析中...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (uiState.aiError != null) {
                        Text(
                            text = uiState.aiError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.requestDayAiAnalysis() }) {
                            Text("重试")
                        }
                    } else if (uiState.aiAnalysisText.isNotBlank()) {
                        Text(
                            text = uiState.aiAnalysisText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "暂无日程数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // API Key dialog (when not configured yet)
    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = "",
            onDismiss = { showApiKeyDialog = false }
        )
    }
}
