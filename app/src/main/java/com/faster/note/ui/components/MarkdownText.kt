package com.faster.note.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            val line = lines[i]
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
                isTableRow(line) -> {
                    val (tableLines, consumed) = collectTableLines(lines, i)
                    renderTable(tableLines)
                    i += consumed - 1
                }
                else -> Text(
                    text = parseBold(line.trimEnd()),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            i++
        }
    }
}

private fun isTableRow(line: String): Boolean {
    val trimmed = line.trimStart()
    return trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2
}

private fun collectTableLines(lines: List<String>, startIdx: Int): Pair<List<String>, Int> {
    val tableLines = mutableListOf<String>()
    var idx = startIdx
    while (idx < lines.size && isTableRow(lines[idx])) {
        tableLines.add(lines[idx].trim())
        idx++
    }
    return tableLines to (idx - startIdx)
}

@Composable
private fun renderTable(rows: List<String>) {
    if (rows.size < 2) return  // Need at least header + separator

    val parsedRows = rows.map { row ->
        row.split("|").drop(1).dropLast(1).map { it.trim() }
    }
    val header = parsedRows[0]
    val dataRows = parsedRows.drop(2)  // Skip header + separator rows
    if (header.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                header.forEach { cell ->
                    Text(
                        text = parseBold(cell),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Data rows
            dataRows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (rowIdx % 2 == 1) Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ) else Modifier
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pad or trim row to match header column count
                    val paddedRow = row + List(maxOf(0, header.size - row.size)) { "" }
                    paddedRow.take(header.size).forEach { cell ->
                        Text(
                            text = parseBold(cell),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (rowIdx < dataRows.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
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
