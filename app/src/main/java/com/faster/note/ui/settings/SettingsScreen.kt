package com.faster.note.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenAbout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("外观", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            item {
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("深色模式", modifier = Modifier.weight(1f))
                        Switch(checked = uiState.isDarkMode, onCheckedChange = {
                            viewModel.toggleDarkMode(it)
                            onToggleDarkMode(it)
                        })
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("分类管理", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card {
                    Column {
                        uiState.categories.forEach { category ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(12.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(category.color)
                                ) {}
                                Spacer(Modifier.width(12.dp))
                                Text(category.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = { editingCategory = category }) { Text("编辑") }
                                if (!category.isPreset) {
                                    IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (category != uiState.categories.last()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        TextButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加分类")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("AI 设置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("API 配置", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("后续版本将支持接入 AI API 进行智能分析", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("关于", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card {
                    TextButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("关于日程")
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showAddDialog || editingCategory != null) {
        val category = editingCategory
        var name by remember { mutableStateOf(category?.name ?: "") }
        var selectedColor by remember { mutableIntStateOf(category?.color ?: 0xFF1565C0.toInt()) }
        val presetColors = listOf(
            0xFF1565C0.toInt(), 0xFFE53935.toInt(), 0xFF43A047.toInt(),
            0xFFFB8C00.toInt(), 0xFF8E24AA.toInt(), 0xFF00ACC1.toInt(),
            0xFF6D4C41.toInt(), 0xFF546E7A.toInt()
        )

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingCategory = null
            },
            title = { Text(if (category == null) "添加分类" else "编辑分类") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("分类名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("颜色", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetColors.forEach { c ->
                            Surface(
                                onClick = { selectedColor = c },
                                modifier = Modifier.size(36.dp),
                                shape = MaterialTheme.shapes.small,
                                color = Color(c),
                                border = if (c == selectedColor) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
                            ) {}
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCategory(CategoryEntity(
                        id = category?.id ?: 0,
                        name = name,
                        color = selectedColor,
                        isPreset = category?.isPreset ?: false
                    ))
                    showAddDialog = false
                    editingCategory = null
                }, enabled = name.isNotBlank()) { Text("保存") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddDialog = false
                    editingCategory = null
                }) { Text("取消") }
            }
        )
    }
}
