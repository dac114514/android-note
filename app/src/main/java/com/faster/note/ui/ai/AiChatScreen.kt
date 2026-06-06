package com.faster.note.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import com.faster.note.data.ai.ActionParser
import com.faster.note.data.ai.CardData
import com.faster.note.data.ai.ResponseBlock
import com.faster.note.data.repository.MessageRole
import com.faster.note.ui.components.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
    onNavigateToDay: (year: Int, month: Int, day: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisible == null || lastVisible.index >= uiState.messages.size - 2) {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, actionLabel = "关闭")
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI日程助手", style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            InputCard(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                enabled = !uiState.isLoading
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "描述你的日程安排",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "例如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "\"明天下午3点创建一个团队会议\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            "\"帮我看看这周有什么安排\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        when (msg.role) {
                            MessageRole.USER -> UserBubble(msg.content)
                            MessageRole.AI -> AiBubble(
                                content = msg.content,
                                cards = msg.scheduleCards,
                                onCardClick = { card ->
                                    val cal = Calendar.getInstance().apply { timeInMillis = card.date }
                                    onNavigateToDay(
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH) + 1,
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            AiLoadingBubble()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AiBubble(
    content: String,
    cards: List<CardData>,
    onCardClick: (CardData) -> Unit
) {
    val blocks = remember(content, cards) { buildDisplayBlocks(content, cards) }
    if (blocks.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                for (block in blocks) {
                    when (block) {
                        is ResponseBlock.Text -> {
                            if (block.content.isNotBlank()) {
                                MarkdownText(text = block.content)
                            }
                        }
                        is ResponseBlock.Card -> {
                            Spacer(Modifier.height(6.dp))
                            ScheduleCardCompact(
                                card = block.data,
                                onClick = { onCardClick(block.data) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Build display blocks from stored content and cards.
 * When the original full response is available (content has tags embedded), parseResponseBlocks
 * recovers the original text+card order. Otherwise, append cards after text.
 */
private fun buildDisplayBlocks(content: String, cards: List<CardData>): List<ResponseBlock> {
    val fromParser = ActionParser.parseResponseBlocks(content)
    val hasCards = fromParser.any { it is ResponseBlock.Card }
    if (hasCards) return fromParser

    // No cards found in content text — append stored cards after text blocks
    val blocks = mutableListOf<ResponseBlock>()
    if (content.isNotBlank()) {
        blocks.add(ResponseBlock.Text(content))
    }
    cards.forEach { blocks.add(ResponseBlock.Card(it)) }
    return blocks
}

@Composable
private fun AiLoadingBubble() {
    val statusMessages = remember {
        listOf("思考中", "分析中", "操作中", "请稍候", "读取中", "请等待")
    }
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentIndex = (currentIndex + 1) % statusMessages.size
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusMessages[currentIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InputCard(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("描述你想安排的日程...") },
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    enabled = enabled
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onSend,
                    enabled = enabled && text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "AI可能会犯错，请注意甄别",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ScheduleCardCompact(
    card: CardData,
    onClick: () -> Unit
) {
    val categoryColor = Color(card.categoryColor)
    val sdf = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    val cal = remember { Calendar.getInstance() }
    cal.timeInMillis = card.date
    val dateStr = sdf.format(cal.time)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(categoryColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = card.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!card.isAllDay && card.startTime != null) {
                    val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val timeStr = if (card.endTime != null)
                        "${tf.format(Date(card.startTime))} - ${tf.format(Date(card.endTime))}"
                    else tf.format(Date(card.startTime))
                    Text(
                        text = timeStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (card.categoryName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = card.categoryName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = categoryColor
                        )
                    }
                }
            }
        }
    }
}
