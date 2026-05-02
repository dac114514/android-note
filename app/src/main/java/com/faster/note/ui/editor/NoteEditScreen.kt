package com.faster.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faster.note.ui.editor.model.AlignmentType
import com.faster.note.ui.editor.model.ParagraphType
import com.faster.note.ui.theme.NoteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val htmlContent by viewModel.htmlContent.collectAsState()
    val noteColor by viewModel.noteColor.collectAsState()

    LaunchedEffect(noteId) {
        if (noteId != null) viewModel.loadNote(noteId)
    }

    var textFieldValue by remember(htmlContent) {
        mutableStateOf(TextFieldValue(htmlContent))
    }

    var editorState by remember { mutableStateOf(EditorState()) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        placeholder = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateColor(
                            if (noteColor == null) NoteColors.random().value.toInt() else null
                        )
                    }) { Icon(Icons.Default.Palette, "颜色") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FormatToolbar(
                state = editorState,
                onToggleBold = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isBold = !editorState.spanStyle.isBold)) },
                onToggleItalic = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isItalic = !editorState.spanStyle.isItalic)) },
                onToggleUnderline = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isUnderline = !editorState.spanStyle.isUnderline)) },
                onToggleStrikethrough = { editorState = editorState.copy(spanStyle = editorState.spanStyle.copy(isStrikethrough = !editorState.spanStyle.isStrikethrough)) },
                onHeadingClick = { editorState = editorState.copy(paragraphType = it) },
                onAlignmentClick = { editorState = editorState.copy(alignment = it) }
            )

            RichTextEditor(
                textFieldValue = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    viewModel.updateContent(it.text)
                },
                editorState = editorState,
                modifier = Modifier.weight(1f)
            )

            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(noteColor!!))
                )
            }
        }
    }
}
