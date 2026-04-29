package com.bennyjon.aui.compose.components.text

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.internal.openDownloadsFolder
import com.bennyjon.aui.compose.internal.saveFileToDownloads
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.FileContentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Renders a `file_content` block as a copyable/downloadable artifact surface.
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
    modifier: Modifier = Modifier,
    filename: String? = null,
    language: String? = null,
    title: String? = null,
    description: String? = null,
) {
    val theme = LocalAuiTheme.current
    val bodyColor = LocalAuiBodyColor.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadNotice by remember { mutableStateOf<DownloadNotice?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = theme.shapes.card,
        color = theme.colors.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
        ) {
            FileContentHeader(
                filename = filename,
                language = language,
                title = title,
                description = description,
                onDownload = {
                    scope.launch {
                        val savedName = withContext(Dispatchers.IO) {
                            saveFileToDownloads(
                                context = context,
                                filename = filename,
                                language = language,
                                content = content,
                            )
                        }
                        downloadNotice = savedName?.let {
                            DownloadNotice.Saved(filename = it)
                        } ?: DownloadNotice.Failed
                    }
                },
                onCopy = { clipboard.setText(AnnotatedString(content)) },
            )

            downloadNotice?.let { notice ->
                FileDownloadNotice(
                    notice = notice,
                    onOpen = { openDownloadsFolder(context) },
                    onDismiss = { downloadNotice = null },
                )
            }

            FileContentBody(content = content, bodyColor = bodyColor)
        }
    }
}

@Composable
private fun FileContentHeader(
    filename: String?,
    language: String?,
    title: String?,
    description: String?,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
) {
    val theme = LocalAuiTheme.current
    val headline = title ?: filename ?: language?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
        ?: stringResource(R.string.aui_file_default_headline)
    val metadata = listOfNotNull(filename?.takeIf { it != title }, language)
        .joinToString(" · ")
        .ifBlank { null }

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
        FileContentActions(
            onDownload = onDownload,
            onCopy = onCopy,
        )
    }
}

@Composable
private fun FileContentActions(
    onDownload: () -> Unit,
    onCopy: () -> Unit,
) {
    val theme = LocalAuiTheme.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
    ) {
        IconButton(
            onClick = onDownload,
            modifier = Modifier.size(theme.spacing.minimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.aui_file_action_download),
                tint = theme.colors.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(theme.spacing.minimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.aui_file_action_copy),
                tint = theme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileDownloadNotice(
    notice: DownloadNotice,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = LocalAuiTheme.current
    val message = when (notice) {
        is DownloadNotice.Saved -> stringResource(R.string.aui_file_download_saved, notice.filename)
        DownloadNotice.Failed -> stringResource(R.string.aui_file_download_failed)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = theme.spacing.small),
        shape = theme.shapes.banner,
        color = theme.colors.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = theme.spacing.dividerThickness,
                    color = theme.colors.primary.copy(alpha = 0.35f),
                    shape = theme.shapes.banner,
                )
                .padding(
                    horizontal = theme.spacing.small,
                    vertical = theme.spacing.xSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.small),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = theme.typography.caption,
                color = theme.colors.onPrimaryContainer,
            )
            if (notice is DownloadNotice.Saved) {
                TextButton(
                    onClick = onOpen,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.textButtonColors(contentColor = theme.colors.primary),
                ) {
                    Text(
                        text = stringResource(R.string.aui_file_action_open),
                        style = theme.typography.label,
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(theme.spacing.minimumTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.aui_file_dismiss_notice),
                    tint = theme.colors.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun FileContentBody(
    content: String,
    bodyColor: androidx.compose.ui.graphics.Color,
) {
    val theme = LocalAuiTheme.current

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

private sealed interface DownloadNotice {
    data class Saved(val filename: String) : DownloadNotice
    data object Failed : DownloadNotice
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
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
