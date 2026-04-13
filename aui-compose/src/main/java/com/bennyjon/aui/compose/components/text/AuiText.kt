package com.bennyjon.aui.compose.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.text.parseInlineMarkdown
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.TextData

/**
 * Renders a `text` block with inline Markdown support.
 *
 * Supported Markdown: `**bold**`, `*italic*`, `_italic_`, `` `code` ``,
 * and `[label](url)` (http/https/mailto only).
 *
 * Structural Markdown (headings, lists, blockquotes, tables, images, HTML) is **not**
 * supported — the AI uses dedicated AUI blocks (`heading`, `list_simple`, `image_single`,
 * etc.) for structure. Hosts needing full CommonMark rendering can register an
 * [com.bennyjon.aui.compose.plugin.AuiComponentPlugin] override for the `text` type.
 */
@Composable
fun AuiText(
    block: AuiBlock.Text,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val bodyColor = LocalAuiBodyColor.current
    val annotated = remember(block.data.text, theme, bodyColor) {
        parseInlineMarkdown(
            source = block.data.text,
            bodyStyle = theme.typography.body,
            codeStyle = theme.typography.code,
            linkColor = theme.colors.primary,
        )
    }
    Text(
        text = annotated,
        style = theme.typography.body,
        color = bodyColor,
        modifier = modifier,
    )
}

@Preview(showBackground = true, name = "AuiText — Inline Markdown")
@Composable
private fun AuiTextMarkdownPreview() {
    AuiThemeProvider {
        AuiText(
            block = AuiBlock.Text(
                data = TextData(
                    text = "Here is **bold**, *italic*, `code`, and a [link](https://example.com)."
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "AuiText — Unclosed Bold (Graceful)")
@Composable
private fun AuiTextUnterminatedPreview() {
    AuiThemeProvider {
        AuiText(
            block = AuiBlock.Text(
                data = TextData(text = "This has **unterminated bold that renders literally."),
            ),
        )
    }
}
