package com.faster.note.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.note.data.repository.TokenUsageRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val todayRecords = remember { TokenUsageRepository.getTodayRecords() }
    val weeklySummary = remember { TokenUsageRepository.getWeeklySummary() }
    val todayCalls = todayRecords.size
    val todayTotal = todayRecords.sumOf { it.totalTokens }
    val todayPrompt = todayRecords.sumOf { it.promptTokens }
    val todayCompletion = todayRecords.sumOf { it.completionTokens }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // === Today's usage card ===
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("今日用量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem("API 调用", "$todayCalls 次", Modifier.weight(1f))
                        StatItem("总 Token", "$todayTotal", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem("Prompt", "$todayPrompt", Modifier.weight(1f))
                        StatItem("Completion", "$todayCompletion", Modifier.weight(1f))
                    }
                }
            }

            // === Token bar chart (today per-call) ===
            if (todayRecords.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Token 用量（逐次调用）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TokenBarChart(
                            records = todayRecords,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                }
            }

            // === Weekly line chart ===
            if (weeklySummary.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("近 7 天用量趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TokenLineChart(
                            data = weeklySummary,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                }
            }

            // === Clear messages ===
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI 对话记录", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "当前共 ${viewModel.uiState.value.messageCount} 条对话",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        enabled = viewModel.uiState.value.messageCount > 0
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("清除")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清理 AI 对话") },
            text = { Text("将清除所有 AI 对话记录，此操作不可撤销。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearContext()
                    showClearConfirm = false
                }) { Text("清理") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TokenBarChart(
    records: List<com.faster.note.data.repository.TokenUsageRecord>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) return
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxVal = records.maxOf { it.totalTokens }.coerceAtLeast(1)
    val labelFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val chartLeft = 0f
        val chartBottom = size.height - 20f
        val chartTop = 10f
        val chartHeight = chartBottom - chartTop
        val barCount = records.size
        val totalBarArea = size.width
        val barWidth = (totalBarArea / barCount) * 0.6f
        val gap = (totalBarArea / barCount) * 0.4f

        records.forEachIndexed { index, r ->
            val x = index * (barWidth + gap) + gap / 2
            val promptH = (r.promptTokens.toFloat() / maxVal) * chartHeight
            val completionH = (r.completionTokens.toFloat() / maxVal) * chartHeight

            // Prompt segment (bottom)
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, chartBottom - promptH),
                size = androidx.compose.ui.geometry.Size(barWidth, promptH)
            )
            // Completion segment (top)
            drawRect(
                color = tertiaryColor,
                topLeft = Offset(x, chartBottom - promptH - completionH),
                size = androidx.compose.ui.geometry.Size(barWidth, completionH)
            )

            // Time label
            val label = labelFormatter.format(Date(r.timestamp))
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2,
                size.height,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Y-axis label: max value
        drawContext.canvas.nativeCanvas.drawText(
            "$maxVal",
            0f,
            chartTop + 10f,
            android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 20f
            }
        )
    }
}

@Composable
private fun TokenLineChart(
    data: List<com.faster.note.data.repository.DailyTokenSummary>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val maxVal = data.maxOf { it.totalTokens }.coerceAtLeast(1)

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val leftPadding = 40f
        val bottomPadding = 30f
        val topPadding = 20f
        val chartWidth = size.width - leftPadding
        val chartHeight = size.height - bottomPadding - topPadding
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        // Grid lines
        for (i in 0..4) {
            val y = topPadding + chartHeight * (1f - i / 4f)
            drawLine(gridColor, Offset(leftPadding, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "${maxVal * i / 4}",
                2f,
                y + 6f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 18f
                }
            )
        }

        // Data points & lines
        if (data.size >= 2) {
            val path = Path()
            data.forEachIndexed { index, d ->
                val x = leftPadding + index * stepX
                val y = topPadding + chartHeight * (1f - d.totalTokens.toFloat() / maxVal)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        data.forEachIndexed { index, d ->
            val x = leftPadding + index * stepX
            val y = topPadding + chartHeight * (1f - d.totalTokens.toFloat() / maxVal)
            drawCircle(dotColor, 4f, Offset(x, y))

            // Date label
            drawContext.canvas.nativeCanvas.drawText(
                d.date,
                x,
                size.height - 2f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 20f
                    textAlign = if (index == 0) android.graphics.Paint.Align.LEFT
                        else if (index == data.size - 1) android.graphics.Paint.Align.RIGHT
                        else android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
