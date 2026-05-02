package com.faster.note.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType

@Composable
fun RichTextEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        fontWeight = if (editorState.spanStyle.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (editorState.spanStyle.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            editorState.spanStyle.isUnderline && editorState.spanStyle.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            editorState.spanStyle.isUnderline -> TextDecoration.Underline
            editorState.spanStyle.isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        },
        textAlign = when (editorState.alignment) {
            AlignmentType.CENTER -> TextAlign.Center
            AlignmentType.RIGHT -> TextAlign.Right
            AlignmentType.LEFT -> TextAlign.Start
        }
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = "开始写点什么...",
                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
            }
            innerTextField()
        }
    )
}
