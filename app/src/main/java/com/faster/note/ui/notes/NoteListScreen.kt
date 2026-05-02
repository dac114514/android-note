package com.faster.note.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.ui.notes.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NoteListViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("极简笔记") },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, "搜索") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, "新建笔记")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("点击 + 创建第一条笔记", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val leftNotes = notes.filterIndexed { i, _ -> i % 2 == 0 }
            val rightNotes = notes.filterIndexed { i, _ -> i % 2 == 1 }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(leftNotes.size) { index ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        NoteCard(
                            note = leftNotes[index],
                            onClick = { onOpenNote(leftNotes[index].id) },
                            modifier = Modifier.weight(1f).padding(4.dp)
                        )
                        if (index < rightNotes.size) {
                            NoteCard(
                                note = rightNotes[index],
                                onClick = { onOpenNote(rightNotes[index].id) },
                                modifier = Modifier.weight(1f).padding(4.dp)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
