package com.bennyjon.auiandroid.livechat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * Demo-local inline Markdown parser for assistant text bubbles.
 *
 * Kept outside `aui-compose` so the demo does not depend on renderer internals.
 * Supports the subset the chat bubble needs: bold, italic, code spans, and safe links.
 */
internal fun parseAssistantInlineMarkdown(
    source: String,
    codeStyle: TextStyle,
    linkColor: Color,
    boldStyle: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val len = source.length

    while (i < len) {
        val ch = source[i]

        if (ch == '`') {
            val closeIndex = source.indexOf('`', i + 1)
            if (closeIndex != -1) {
                withStyle(codeStyle.toSpanStyle()) {
                    append(source.substring(i + 1, closeIndex))
                }
                i = closeIndex + 1
                continue
            }
        }

        if (ch == '[') {
            val closeBracket = source.indexOf(']', i + 1)
            if (closeBracket != -1 && closeBracket + 1 < len && source[closeBracket + 1] == '(') {
                val closeParen = source.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val label = source.substring(i + 1, closeBracket)
                    val url = source.substring(closeBracket + 2, closeParen)
                    val lower = url.lowercase()
                    if (
                        lower.startsWith("http://") ||
                        lower.startsWith("https://") ||
                        lower.startsWith("mailto:")
                    ) {
                        val linkStyle = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                        withLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(style = linkStyle),
                            )
                        ) {
                            withStyle(linkStyle) { append(label) }
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }
        }

        if (ch == '*' && i + 1 < len && source[i + 1] == '*') {
            val closeIndex = source.indexOf("**", i + 2)
            if (closeIndex != -1) {
                withStyle(boldStyle) {
                    append(source.substring(i + 2, closeIndex))
                }
                i = closeIndex + 2
                continue
            }
            append("**")
            i += 2
            continue
        }

        if (ch == '*') {
            val closeIndex = source.indexOf('*', i + 1)
            if (closeIndex != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(source.substring(i + 1, closeIndex))
                }
                i = closeIndex + 1
                continue
            }
        }

        if (ch == '_') {
            val leftFlanked = i > 0 && source[i - 1].isLetterOrDigit()
            if (!leftFlanked) {
                val closeIndex = source.indexOf('_', i + 1)
                if (closeIndex != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(source.substring(i + 1, closeIndex))
                    }
                    i = closeIndex + 1
                    continue
                }
            }
        }

        append(ch)
        i++
    }
}
