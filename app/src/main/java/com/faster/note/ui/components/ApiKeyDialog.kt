package com.faster.note.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.faster.note.data.repository.AiConfigRepository

@Composable
fun ApiKeyDialog(
    initialKey: String = "",
    onDismiss: () -> Unit
) {
    var editingKey by remember(initialKey) { mutableStateOf(initialKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 API Key") },
        text = {
            Column {
                Text(
                    "请输入 DeepSeek API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editingKey,
                    onValueChange = { editingKey = it },
                    label = { Text("DeepSeek API Key") },
                    placeholder = { Text("输入你的 API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = {
                        if (editingKey.isNotEmpty()) {
                            IconButton(onClick = { editingKey = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AiConfigRepository.saveApiKey(editingKey)
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
