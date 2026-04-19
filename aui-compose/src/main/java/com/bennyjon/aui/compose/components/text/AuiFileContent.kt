package com.bennyjon.aui.compose.components.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.FileContentData

/**
 * Renders a `file_content` block as a copyable artifact surface.
 */
@Composable
fun AuiFileContent(
    block: AuiBlock.FileContent,
    modifier: Modifier = Modifier,
) {
    AuiFileContentSurface(
        content = block.data.content,
        filename = block.data.filename,
        language = block.data.language,
        title = block.data.title,
        description = block.data.description,
        modifier = modifier,
    )
}

@Composable
internal fun AuiFileContentSurface(
    content: String,
    filename: String? = null,
    language: String? = null,
    title: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val bodyColor = LocalAuiBodyColor.current
    val clipboard = LocalClipboardManager.current
    val headline = title ?: filename ?: language?.replaceFirstChar { it.uppercase() } ?: "File"
    val metadata = listOfNotNull(filename?.takeIf { it != title }, language).joinToString(" · ").ifBlank { null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = theme.shapes.card,
        color = theme.colors.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.colors.surfaceVariant.copy(alpha = 0.5f))
                    .padding(
                        horizontal = theme.spacing.small,
                        vertical = theme.spacing.xSmall,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
                ) {
                    Text(
                        text = headline,
                        style = theme.typography.subheading,
                        color = theme.colors.onSurface,
                    )
                    metadata?.let {
                        Text(
                            text = it,
                            style = theme.typography.caption,
                            color = theme.colors.onSurfaceVariant,
                        )
                    }
                    description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = theme.typography.caption,
                            color = theme.colors.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(content)) },
                    modifier = Modifier.size(theme.spacing.large),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy file contents",
                        tint = theme.colors.onSurfaceVariant,
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = content,
                    style = theme.typography.code,
                    color = bodyColor,
                    modifier = Modifier.padding(
                        start = theme.spacing.small,
                        end = theme.spacing.small,
                        bottom = theme.spacing.small,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "AuiFileContent")
@Composable
private fun AuiFileContentPreview() {
    AuiThemeProvider {
        AuiFileContent(
            block = AuiBlock.FileContent(
                data = FileContentData(
                    filename = "README.md",
                    language = "markdown",
                    title = "Project README",
                    description = "Setup and usage guide",
                    content = "# Hello\n\nRun `./gradlew build`.",
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
