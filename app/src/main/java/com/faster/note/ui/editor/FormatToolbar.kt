package com.faster.note.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType
import com.faster.note.ui.editor.model.SpanStyle

data class EditorState(
    val spanStyle: SpanStyle = SpanStyle(),
    val paragraphType: ParagraphType = ParagraphType.NORMAL,
    val alignment: AlignmentType = AlignmentType.LEFT
)

@Composable
fun FormatToolbar(
    state: EditorState,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onHeadingClick: (ParagraphType) -> Unit,
    onAlignmentClick: (AlignmentType) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FilterChip(
                selected = state.spanStyle.isBold,
                onClick = onToggleBold,
                label = { Text("B", style = MaterialTheme.typography.titleSmall) }
            )
            FilterChip(
                selected = state.spanStyle.isItalic,
                onClick = onToggleItalic,
                label = { Text("I", style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic)) }
            )
            FilterChip(
                selected = state.spanStyle.isUnderline,
                onClick = onToggleUnderline,
                label = { Text("U", style = MaterialTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)) }
            )
            FilterChip(
                selected = state.spanStyle.isStrikethrough,
                onClick = onToggleStrikethrough,
                label = { Text("S", style = MaterialTheme.typography.titleSmall.copy(textDecoration = TextDecoration.LineThrough)) }
            )

            Divider(Modifier.height(24.dp).padding(horizontal = 4.dp))

            listOf(ParagraphType.H1 to "H1", ParagraphType.H2 to "H2", ParagraphType.H3 to "H3").forEach { (type, label) ->
                FilterChip(
                    selected = state.paragraphType == type,
                    onClick = { onHeadingClick(if (state.paragraphType == type) ParagraphType.NORMAL else type) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }

            Divider(Modifier.height(24.dp).padding(horizontal = 4.dp))

            listOf(
                AlignmentType.LEFT,
                AlignmentType.CENTER,
                AlignmentType.RIGHT
            ).forEach { type ->
                FilterChip(
                    selected = state.alignment == type,
                    onClick = { onAlignmentClick(type) },
                    label = { Text(type.name.first().toString(), style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}
