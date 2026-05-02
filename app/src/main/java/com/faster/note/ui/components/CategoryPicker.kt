package com.faster.note.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faster.note.data.db.entity.CategoryEntity

@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = { onSelect(null) },
            shape = RoundedCornerShape(16.dp),
            color = if (selectedId == null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = "无",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 13.sp
            )
        }
        categories.forEach { category ->
            val catColor = Color(category.color)
            Surface(
                onClick = { onSelect(category.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedId == category.id) catColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = category.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}
