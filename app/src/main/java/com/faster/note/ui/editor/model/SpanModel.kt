package com.faster.note.ui.editor.model

enum class SpanFormat {
    BOLD, ITALIC, UNDERLINE, STRIKETHROUGH
}

enum class HeadingLevel { H1, H2, H3 }

data class SpanStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false
)

data class SpanNode(
    val text: String,
    val style: SpanStyle = SpanStyle()
) {
    val isEmpty get() = text.isEmpty()
}

data class ParagraphData(
    val type: ParagraphType = ParagraphType.NORMAL,
    val alignment: AlignmentType = AlignmentType.LEFT,
    val spans: List<SpanNode> = emptyList()
)

enum class ParagraphType { NORMAL, H1, H2, H3, BULLET_LIST, ORDERED_LIST }

enum class AlignmentType { LEFT, CENTER, RIGHT }

data class RichTextDocument(
    val paragraphs: List<ParagraphData> = listOf(ParagraphData())
)
