package com.faster.note.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
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
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Appearance section
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("深色模式", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = uiState.isDarkMode, onCheckedChange = {
                        viewModel.toggleDarkMode(it)
                        onToggleDarkMode(it)
                    })
                }
            }

            Spacer(Modifier.height(12.dp))

            // Category management section
            var catExpanded by remember { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { catExpanded = !catExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("分类管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(
                            if (catExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (catExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    AnimatedVisibility(visible = catExpanded) {
                        Column {
                            HorizontalDivider()
                            uiState.categories.forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(12.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = Color(category.color)
                                    ) {}
                                    Spacer(Modifier.width(12.dp))
                                    Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                            HorizontalDivider()
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
            }

            Spacer(Modifier.height(12.dp))

            // AI Settings section
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("AI 设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("后续版本将支持接入 AI API 进行智能分析", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // About section
            var aboutExpanded by remember { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aboutExpanded = !aboutExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(
                            if (aboutExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (aboutExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    AnimatedVisibility(visible = aboutExpanded) {
                        Column {
                            HorizontalDivider()
                            FileEntryItem("关于日程", Icons.Filled.Star, onOpenAbout)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddDialog || editingCategory != null) {
        val category = editingCategory
        var name by remember { mutableStateOf(category?.name ?: "") }
        var selectedColor by remember { mutableIntStateOf(category?.color ?: 0xFF1565C0.toInt()) }
        var showCustomPicker by remember { mutableStateOf(false) }
        val presetColors = listOf(
            0xFF1565C0.toInt(), 0xFF1E88E5.toInt(), 0xFF42A5F5.toInt(), 0xFF90CAF9.toInt(),
            0xFFE53935.toInt(), 0xFFEF5350.toInt(), 0xFFEC407A.toInt(), 0xFFAB47BC.toInt(),
            0xFF43A047.toInt(), 0xFF66BB6A.toInt(), 0xFF26A69A.toInt(), 0xFF00ACC1.toInt(),
            0xFFFB8C00.toInt(), 0xFFFFA726.toInt(), 0xFF8D6E63.toInt(), 0xFF78909C.toInt()
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
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = MaterialTheme.shapes.small,
                            color = Color(selectedColor),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text("当前颜色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showCustomPicker = true }) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("自定义颜色")
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

        if (showCustomPicker) {
            var hue by remember { mutableFloatStateOf(selectedColor.let {
                val r = (it shr 16) and 0xFF; val g = (it shr 8) and 0xFF; val b = it and 0xFF
                val cmax = maxOf(r, g, b); val cmin = minOf(r, g, b); val delta = cmax - cmin
                if (delta == 0) 0f else when (cmax) {
                    r -> ((((g - b).toFloat() / delta) % 6) * 60).let { if (it < 0) it + 360 else it }
                    g -> (((b - r).toFloat() / delta) + 2) * 60
                    else -> (((r - g).toFloat() / delta) + 4) * 60
                }
            }) }
            var saturation by remember { mutableFloatStateOf(selectedColor.let {
                val r = (it shr 16) and 0xFF; val g = (it shr 8) and 0xFF; val b = it and 0xFF
                val cmax = maxOf(r, g, b); val cmin = minOf(r, g, b); val delta = cmax - cmin
                if (cmax == 0) 0f else delta.toFloat() / cmax
            }) }
            var value by remember { mutableFloatStateOf(selectedColor.let {
                maxOf((it shr 16) and 0xFF, (it shr 8) and 0xFF, it and 0xFF).toFloat() / 255f
            }) }

            AlertDialog(
                onDismissRequest = { showCustomPicker = false },
                title = { Text("自定义颜色") },
                text = {
                    Column {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.small,
                            color = Color.hsv(hue, saturation, value)
                        ) {}
                        Spacer(Modifier.height(16.dp))
                        Text("色相", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = hue,
                            onValueChange = { hue = it },
                            valueRange = 0f..360f
                        )
                        Text("饱和度", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..1f
                        )
                        Text("亮度", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            valueRange = 0f..1f
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        selectedColor = Color.hsv(hue, saturation, value).toArgb()
                        showCustomPicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomPicker = false }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun FileEntryItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ">",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
