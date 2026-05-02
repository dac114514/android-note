package com.faster.note.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onToggleBold,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (state.spanStyle.isBold)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Default.FormatBold, "粗体") }

            FilledTonalIconButton(
                onClick = onToggleItalic,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (state.spanStyle.isItalic)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Default.FormatItalic, "斜体") }

            FilledTonalIconButton(
                onClick = onToggleUnderline,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (state.spanStyle.isUnderline)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Default.FormatUnderlined, "下划线") }

            FilledTonalIconButton(
                onClick = onToggleStrikethrough,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (state.spanStyle.isStrikethrough)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Default.FormatStrikethrough, "删除线") }

            VerticalDivider(Modifier.height(28.dp).padding(horizontal = 4.dp))

            listOf(
                ParagraphType.H1 to "H1",
                ParagraphType.H2 to "H2",
                ParagraphType.H3 to "H3"
            ).forEach { (type, label) ->
                TextButton(
                    onClick = { onHeadingClick(if (state.paragraphType == type) ParagraphType.NORMAL else type) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (state.paragraphType == type)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            }

            VerticalDivider(Modifier.height(28.dp).padding(horizontal = 4.dp))

            listOf(
                AlignmentType.LEFT to "L",
                AlignmentType.CENTER to "C",
                AlignmentType.RIGHT to "R"
            ).forEach { (type, label) ->
                FilledTonalIconButton(
                    onClick = { onAlignmentClick(type) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (state.alignment == type)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
