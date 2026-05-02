package com.faster.note.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.data.db.entity.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNote: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { /* handled by nav */ }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text("搜索笔记...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(results) { note ->
                SearchResultItem(note = note, onClick = { onOpenNote(note.id) })
            }
            if (query.isNotBlank() && results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp)) {
                        Text("未找到相关笔记", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(note: NoteEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(note.title.ifBlank { "无标题" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(note.content.replace(Regex("<[^>]*>"), "").take(100), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
