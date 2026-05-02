package com.faster.note.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val exporting by viewModel.exporting.collectAsState()
    val backupFiles by viewModel.backupFiles.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshBackupFiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = { Text("设置") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text("备份与导出", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("导出全部笔记") },
                    supportingContent = { Text("将所有笔记导出为 ZIP 备份文件") },
                    trailingContent = {
                        if (exporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(onClick = { viewModel.exportAllNotes() }) {
                                Icon(Icons.Default.Backup, "导出")
                            }
                        }
                    }
                )
            }
            item { HorizontalDivider() }

            item {
                Text("历史备份", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            if (backupFiles.isEmpty()) {
                item {
                    Text("暂无备份文件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else {
                items(backupFiles) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        supportingContent = { Text("${file.length() / 1024} KB") }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("外观", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("深色模式") },
                    supportingContent = { Text("跟随系统设置") },
                    trailingContent = { Switch(checked = false, onCheckedChange = {}) }
                )
            }
        }
    }
}
