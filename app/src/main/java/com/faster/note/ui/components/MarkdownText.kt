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
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trimEnd()

            when {
                // Table block: consecutive pipe lines with a separator row
                line.startsWith("|") && i + 1 < lines.size && lines[i + 1].trim().startsWith("|") -> {
                    val tableLines = mutableListOf(line)
                    i++
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        tableLines.add(lines[i].trimEnd())
                        i++
                    }
                    MarkdownTable(tableLines)
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInline(line.removePrefix("# ").trim()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    i++
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInline(line.removePrefix("## ").trim()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    i++
                }
                line.isBlank() -> {
                    Spacer(Modifier.height(4.dp))
                    i++
                }
                else -> {
                    Text(
                        text = parseInline(line),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    i++
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(lines: List<String>) {
    // Filter out empty pipe lines and the separator line (containing only ---)
    val dataRows = lines
        .dropWhile { it.trim() == "|" }
        .filter { it.trim() != "|" }

    if (dataRows.isEmpty()) return

    // Split into header separator and actual rows
    val sepIndex = dataRows.indexOfFirst { r ->
        val cells = r.split("|").filter { it.isNotBlank() }
        cells.all { it.trim().all { c -> c == '-' || c == ':' || c == ' ' } }
    }

    val headerRow: String?
    val bodyRows: List<String>
    if (sepIndex >= 0) {
        headerRow = dataRows.getOrNull(sepIndex - 1)
        bodyRows = dataRows.drop(sepIndex + 1).filter { it.trim() != "|" }
    } else {
        headerRow = null
        bodyRows = dataRows
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (headerRow != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val cells = headerRow.split("|").filter { it.isNotBlank() }
                cells.forEach { cell ->
                    Text(
                        text = parseInline(cell.trim()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        bodyRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val cells = row.split("|").filter { it.isNotBlank() }
                cells.forEach { cell ->
                    Text(
                        text = parseInline(cell.trim()),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun parseInline(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldStart = remaining.indexOf("**")
            if (boldStart == -1) {
                append(remaining)
                break
            }
            // Text before bold
            append(remaining.substring(0, boldStart))
            remaining = remaining.substring(boldStart + 2)

            val boldEnd = remaining.indexOf("**")
            if (boldEnd == -1) {
                // Unclosed bold, treat as plain text
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
