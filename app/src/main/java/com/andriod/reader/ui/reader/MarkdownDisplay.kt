package com.andriod.reader.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Text(
        text = buildMarkdownAnnotatedString(text, style),
        style = style,
        lineHeight = style.lineHeight * 1.4f,
        modifier = modifier,
    )
}

internal fun buildMarkdownAnnotatedString(
    text: String,
    baseStyle: TextStyle,
): AnnotatedString {
    val bodyColor = baseStyle.color.takeIf { it != Color.Unspecified }
        ?: Color.Unspecified
    val lines = text.split('\n')

    return buildAnnotatedString {
        lines.forEachIndexed { index, rawLine ->
            if (index > 0) append('\n')
            appendMarkdownLine(rawLine, baseStyle, bodyColor)
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownLine(
    rawLine: String,
    baseStyle: TextStyle,
    bodyColor: Color,
) {
    val heading = Regex("""^(#{1,6})\s+(.*)$""").find(rawLine)
    if (heading != null) {
        val level = heading.groupValues[1].length
        val content = heading.groupValues[2]
        val headingStyle = SpanStyle(
            fontSize = when (level) {
                1 -> 26.sp
                2 -> 22.sp
                else -> 20.sp
            },
            fontWeight = FontWeight.Bold,
            color = bodyColor,
        )
        withStyle(headingStyle) {
            appendInlineMarkdown(content, baseStyle, bodyColor)
        }
        return
    }

    val todoUnchecked = Regex("""^- \[ \] (.*)$""").find(rawLine)
    if (todoUnchecked != null) {
        append("☐ ")
        appendInlineMarkdown(todoUnchecked.groupValues[1], baseStyle, bodyColor)
        return
    }

    val todoChecked = Regex("""^- \[[xX]\] (.*)$""").find(rawLine)
    if (todoChecked != null) {
        append("☑ ")
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = bodyColor)) {
            appendInlineMarkdown(todoChecked.groupValues[1], baseStyle, bodyColor)
        }
        return
    }

    val ordered = Regex("""^(\d+)\.\s+(.*)$""").find(rawLine)
    if (ordered != null) {
        append("${ordered.groupValues[1]}. ")
        appendInlineMarkdown(ordered.groupValues[2], baseStyle, bodyColor)
        return
    }

    val bullet = Regex("""^-\s+(.*)$""").find(rawLine)
    if (bullet != null) {
        append("• ")
        appendInlineMarkdown(bullet.groupValues[1], baseStyle, bodyColor)
        return
    }

    appendInlineMarkdown(rawLine, baseStyle, bodyColor)
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    baseStyle: TextStyle,
    bodyColor: Color,
) {
    var index = 0
    while (index < text.length) {
        val slice = text.substring(index)

        val strike = Regex("""^~~([^~]*)~~""").find(slice)
        if (strike != null) {
            withStyle(
                SpanStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = bodyColor.copy(alpha = 0.55f),
                ),
            ) {
                append(strike.groupValues[1])
            }
            index += strike.value.length
            continue
        }

        val bold = Regex("""^\*\*([^*]+)\*\*""").find(slice)
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = bodyColor)) {
                append(bold.groupValues[1])
            }
            index += bold.value.length
            continue
        }

        val italic = Regex("""^\*([^*]+)\*""").find(slice)
        if (italic != null) {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = bodyColor)) {
                append(italic.groupValues[1])
            }
            index += italic.value.length
            continue
        }

        append(text[index])
        index++
    }
}
