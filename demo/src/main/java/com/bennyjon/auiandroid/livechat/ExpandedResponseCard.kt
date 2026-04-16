package com.bennyjon.auiandroid.livechat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiResponse

/**
 * Tappable card stub that previews an [AuiResponse] with `display = "expanded"`.
 *
 * The host app shows this in the chat list. Tapping opens either a bottom sheet
 * (narrow windows) or sets the active detail-pane message (wide windows).
 *
 * Pulls [AuiResponse.cardTitle] and [AuiResponse.cardDescription] when set; otherwise
 * falls back to the first heading and the first non-heading text block in the response.
 *
 * @param response The expanded AUI response to preview.
 * @param onClick Invoked when the user taps the stub.
 * @param modifier Modifier applied to the outermost surface.
 * @param isSpent When `true`, the card renders at reduced alpha to match the spent
 *   styling used by inline AUI. Stays tappable so users can review the spent content.
 * @param isActive When `true`, the card is highlighted (e.g. it's the message currently
 *   shown in the detail pane). Defaults to `false`.
 */
@Composable
internal fun ExpandedResponseCard(
    response: AuiResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpent: Boolean = false,
    isActive: Boolean = false,
) {
    val title = response.cardTitle ?: response.firstHeadingText() ?: "Tap to view"
    val description = response.cardDescription ?: response.firstNonHeadingText()

    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private fun AuiResponse.firstHeadingText(): String? =
    blocks.firstNotNullOfOrNull { (it as? AuiBlock.Heading)?.data?.text }

private fun AuiResponse.firstNonHeadingText(): String? =
    blocks.firstNotNullOfOrNull { (it as? AuiBlock.Text)?.data?.text }

@Preview(showBackground = true)
@Composable
private fun ExpandedResponseCardPreview() {
    androidx.compose.material3.MaterialTheme {
        ExpandedResponseCard(
            response = AuiResponse(
                display = com.bennyjon.aui.core.model.AuiDisplay.EXPANDED,
                cardTitle = "Headphone picks",
                cardDescription = "Three top noise-cancelling models compared",
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
