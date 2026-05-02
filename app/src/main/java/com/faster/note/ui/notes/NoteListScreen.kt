package com.faster.note.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
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
    onOpenSearch: () -> Unit,
    viewModel: NoteListViewModel = viewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val filter by viewModel._filter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("极简笔记") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, "新建笔记")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter is NoteFilter.All,
                    onClick = { viewModel.setFilter(NoteFilter.All) },
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = filter is NoteFilter.Favorite,
                    onClick = { viewModel.setFilter(NoteFilter.Favorite) },
                    label = { Text("收藏") },
                    leadingIcon = if (filter is NoteFilter.Favorite) {
                        { Icon(Icons.Default.Favorite, null, Modifier.size(16.dp)) }
                    } else null
                )
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "点击 + 创建第一条笔记",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 8.dp, end = 8.dp,
                        top = 4.dp, bottom = 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
