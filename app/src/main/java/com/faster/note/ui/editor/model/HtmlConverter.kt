package com.faster.note.ui.editor.model

object HtmlConverter {

    fun toHtml(doc: RichTextDocument): String {
        val sb = StringBuilder()
        doc.paragraphs.forEach { para ->
            when (para.type) {
                ParagraphType.H1 -> sb.append("<h1>")
                ParagraphType.H2 -> sb.append("<h2>")
                ParagraphType.H3 -> sb.append("<h3>")
                ParagraphType.BULLET_LIST -> sb.append("<li>")
                ParagraphType.ORDERED_LIST -> sb.append("<li>")
                ParagraphType.NORMAL -> {
                    when (para.alignment) {
                        AlignmentType.CENTER -> sb.append("<p style=\"text-align:center\">")
                        AlignmentType.RIGHT -> sb.append("<p style=\"text-align:right\">")
                        AlignmentType.LEFT -> sb.append("<p>")
                    }
                }
            }
            para.spans.forEach { span ->
                var text = span.text
                if (span.style.isBold) text = "<b>$text</b>"
                if (span.style.isItalic) text = "<i>$text</i>"
                if (span.style.isUnderline) text = "<u>$text</u>"
                if (span.style.isStrikethrough) text = "<s>$text</s>"
                sb.append(text)
            }
            when (para.type) {
                ParagraphType.H1 -> sb.appendLine("</h1>")
                ParagraphType.H2 -> sb.appendLine("</h2>")
                ParagraphType.H3 -> sb.appendLine("</h3>")
                ParagraphType.BULLET_LIST, ParagraphType.ORDERED_LIST -> sb.appendLine("</li>")
                ParagraphType.NORMAL -> sb.appendLine("</p>")
            }
        }
        return sb.toString()
    }

    fun fromHtml(html: String): RichTextDocument {
        if (html.isBlank()) return RichTextDocument()

        val paragraphs = mutableListOf<ParagraphData>()
        val regex = Regex("<(h[1-3]|p|li)([^>]*)>([\\s\\S]*?)</\\1>")
        regex.findAll(html).forEach { match ->
            val tag = match.groupValues[1]
            val attrs = match.groupValues[2]
            val inner = match.groupValues[3]
            val alignment = when {
                "text-align:center" in attrs -> AlignmentType.CENTER
                "text-align:right" in attrs -> AlignmentType.RIGHT
                else -> AlignmentType.LEFT
            }
            val type = when (tag) {
                "h1" -> ParagraphType.H1
                "h2" -> ParagraphType.H2
                "h3" -> ParagraphType.H3
                "li" -> ParagraphType.BULLET_LIST
                else -> ParagraphType.NORMAL
            }
            val spans = parseSpans(inner)
            paragraphs.add(ParagraphData(type = type, alignment = alignment, spans = spans))
        }
        if (paragraphs.isEmpty()) paragraphs.add(ParagraphData())
        return RichTextDocument(paragraphs)
    }

    private fun parseSpans(html: String): List<SpanNode> {
        val spans = mutableListOf<SpanNode>()
        val regex = Regex("<([biustr/]+)>([^<]*)</\\1>")
        var lastEnd = 0
        regex.findAll(html).forEach { m ->
            val before = html.substring(lastEnd, m.range.first)
            if (before.isNotBlank()) spans.add(SpanNode(before))
            val tag = m.groupValues[1]
            val text = m.groupValues[2]
            val style = SpanStyle(
                isBold = tag == "b",
                isItalic = tag == "i",
                isUnderline = tag == "u",
                isStrikethrough = tag == "s"
            )
            spans.add(SpanNode(text, style))
            lastEnd = m.range.last + 1
        }
        val remaining = html.substring(lastEnd)
        if (remaining.isNotBlank()) spans.add(SpanNode(remaining))
        if (spans.isEmpty() && html.isNotBlank()) spans.add(SpanNode(html))
        return spans
    }
}
