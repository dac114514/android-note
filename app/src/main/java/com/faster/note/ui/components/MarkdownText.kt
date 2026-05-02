package com.faster.note.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = text.lines()
    Column(modifier = modifier) {
        for (line in lines) {
            when {
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                line.trimStart().startsWith("# ") -> Text(
                    text = parseBold(line.trimStart().removePrefix("# ").trimEnd()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                line.trimStart().startsWith("## ") -> Text(
                    text = parseBold(line.trimStart().removePrefix("## ").trimEnd()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                else -> Text(
                    text = parseBold(line.trimEnd()),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

private fun parseBold(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldStart = remaining.indexOf("**")
            if (boldStart == -1) {
                append(remaining)
                break
            }
            append(remaining.substring(0, boldStart))
            remaining = remaining.substring(boldStart + 2)

            val boldEnd = remaining.indexOf("**")
            if (boldEnd == -1) {
                append("**$remaining")
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(remaining.substring(0, boldEnd))
            }
            remaining = remaining.substring(boldEnd + 2)
        }
    }
}
