package com.bennyjon.aui.compose.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * Parses basic inline Markdown into a Compose [AnnotatedString].
 *
 * Supported syntax:
 * - `**bold**`
 * - `*italic*` and `_italic_` (underscore only when not word-flanked)
 * - `` `code` ``
 * - `[label](url)` (http, https, and mailto schemes only)
 *
 * Graceful degradation: if a delimiter is never closed, the opening delimiter and
 * everything after it are emitted as literal text. No exceptions are thrown.
 *
 * **Out of scope:** headings, lists, blockquotes, tables, images, HTML, reference-style
 * links, escapes, and nested emphasis. The AI uses separate AUI blocks for structural
 * elements. Hosts needing full CommonMark can register an `AuiComponentPlugin` override
 * for the `text` component type.
 */
fun parseInlineMarkdown(
    source: String,
    bodyStyle: TextStyle,
    codeStyle: TextStyle,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val len = source.length

    while (i < len) {
        val ch = source[i]

        // 1. Code span: ` ... `
        if (ch == '`') {
            val closeIndex = source.indexOf('`', i + 1)
            if (closeIndex != -1) {
                val code = source.substring(i + 1, closeIndex)
                withStyle(codeStyle.toSpanStyle()) {
                    append(code)
                }
                i = closeIndex + 1
                continue
            }
            // Unclosed — fall through to literal
        }

        // 2. Link: [label](url)
        if (ch == '[') {
            val closeBracket = source.indexOf(']', i + 1)
            if (closeBracket != -1 && closeBracket + 1 < len && source[closeBracket + 1] == '(') {
                val closeParen = source.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val label = source.substring(i + 1, closeBracket)
                    val url = source.substring(closeBracket + 2, closeParen)
                    if (isSupportedUrl(url)) {
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
                            withStyle(linkStyle) {
                                append(label)
                            }
                        }
                        i = closeParen + 1
                        continue
                    }
                    // Unsupported URL scheme — fall through to literal
                }
            }
            // Malformed link — fall through to literal
        }

        // 3. Bold: **...**
        if (ch == '*' && i + 1 < len && source[i + 1] == '*') {
            val closeIndex = source.indexOf("**", i + 2)
            if (closeIndex != -1) {
                val boldText = source.substring(i + 2, closeIndex)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(boldText)
                }
                i = closeIndex + 2
                continue
            }
            // Unclosed bold — emit both * as literal so italic doesn't mis-match them
            append("**")
            i += 2
            continue
        }

        // 4. Italic: *...* or _..._
        if (ch == '*') {
            val closeIndex = source.indexOf('*', i + 1)
            if (closeIndex != -1) {
                val italicText = source.substring(i + 1, closeIndex)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(italicText)
                }
                i = closeIndex + 1
                continue
            }
            // Unclosed — fall through to literal
        }

        if (ch == '_') {
            // Only treat as italic when the opening _ is not flanked by a word character
            // on the left (prevents snake_case_names from triggering italic).
            val leftFlanked = i > 0 && source[i - 1].isLetterOrDigit()
            if (!leftFlanked) {
                val closeIndex = source.indexOf('_', i + 1)
                if (closeIndex != -1) {
                    val italicText = source.substring(i + 1, closeIndex)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicText)
                    }
                    i = closeIndex + 1
                    continue
                }
            }
            // Word-flanked or unclosed — fall through to literal
        }

        // 5. Literal character
        append(ch)
        i++
    }
}

private fun isSupportedUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.startsWith("http://") ||
        lower.startsWith("https://") ||
        lower.startsWith("mailto:")
}
