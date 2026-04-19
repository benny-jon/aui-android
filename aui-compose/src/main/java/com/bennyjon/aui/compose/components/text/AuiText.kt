package com.bennyjon.aui.compose.components.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.text.splitMarkdownBlocks
import com.bennyjon.aui.compose.text.MarkdownSegment
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
 * `[label](url)` (http/https/mailto only), and fenced triple-backtick blocks as
 * a fallback copyable artifact surface.
 *
 * Structural Markdown (headings, lists, blockquotes, tables, images, HTML) is **not**
 * supported — the AI uses dedicated AUI blocks (`heading`, `file_content`, `image_single`,
 * etc.) for structure. Exact file/document artifacts should use `file_content`.
 * Hosts needing full CommonMark rendering can register an
 * [com.bennyjon.aui.compose.plugin.AuiComponentPlugin] override for the `text` type.
 */
@Composable
fun AuiText(
    block: AuiBlock.Text,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val bodyColor = LocalAuiBodyColor.current
    val segments = remember(block.data.text) {
        splitMarkdownBlocks(block.data.text)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(theme.spacing.small),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Text -> {
                    if (segment.value.isNotEmpty()) {
                        val annotated = remember(segment.value, theme, bodyColor) {
                            parseInlineMarkdown(
                                source = segment.value,
                                codeStyle = theme.typography.code,
                                linkColor = theme.colors.primary,
                            )
                        }
                        Text(
                            text = annotated,
                            style = theme.typography.body,
                            color = bodyColor,
                        )
                    }
                }

                is MarkdownSegment.FencedCode -> {
                    AuiFileContentSurface(
                        content = segment.content,
                        language = segment.language,
                        title = segment.language?.replaceFirstChar { it.uppercase() } ?: "Text",
                    )
                }
            }
        }
    }
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
