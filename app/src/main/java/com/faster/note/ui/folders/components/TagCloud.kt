package com.faster.note.ui.folders.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faster.note.data.db.entity.TagEntity

@Composable
fun TagCloud(
    tags: List<TagEntity>,
    onDeleteTag: (TagEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(tag.name) },
                trailingIcon = {
                    IconButton(onClick = { onDeleteTag(tag) }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, "删除标签", modifier = Modifier.size(14.dp))
                    }
                }
            )
        }
    }
}
