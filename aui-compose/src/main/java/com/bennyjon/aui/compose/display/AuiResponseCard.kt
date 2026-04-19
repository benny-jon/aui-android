package com.bennyjon.aui.compose.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.AuiStep

/**
 * Tappable card stub that previews an [AuiResponse] the host has chosen to surface
 * through an out-of-flow container (sheet, dialog, or detail pane).
 *
 * Typical use cases:
 * - **EXPANDED** responses the host routes to a detail surface instead of rendering inline.
 * - **SURVEY** responses the host opens in a bottom sheet — the card keeps the survey
 *   discoverable in the chat so users can re-open it after an accidental dismiss.
 *
 * Title / description resolution:
 *
 * | Display    | Title (in order)                                           | Description (in order)                              |
 * |------------|------------------------------------------------------------|-----------------------------------------------------|
 * | `SURVEY`   | [AuiResponse.surveyTitle], [AuiResponse.cardTitle],        | [AuiResponse.cardDescription], step-count summary,  |
 * |            | first step question, literal `"Survey"`                    | nothing                                             |
 * | `EXPANDED` | [AuiResponse.cardTitle], first heading text, first text,   | [AuiResponse.cardDescription], first non-heading    |
 * |            | literal `"Tap to view"`                                    | text block, nothing                                 |
 *
 * @param response The [AuiResponse] to preview.
 * @param onClick Invoked when the user taps the card.
 * @param modifier Applied to the outermost [Surface].
 * @param theme The [AuiTheme] used for colors, typography, spacing, and shape. Defaults to
 *   [AuiTheme.Default]. Pass the same theme as the matching [com.bennyjon.aui.compose.AuiRenderer]
 *   so the card stays visually consistent with its expanded content.
 * @param isActive When `true`, the card is highlighted — e.g. it's the message currently
 *   shown in the detail pane or sheet.
 */
@Composable
fun AuiResponseCard(
    response: AuiResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    isActive: Boolean = false,
) {
    val title = response.cardStubTitle()
    val description = response.cardStubDescription()
    val containerColor = if (isActive) theme.colors.primaryContainer else theme.colors.surfaceVariant
    val titleColor = if (isActive) theme.colors.onPrimaryContainer else theme.colors.onSurface
    val descriptionColor = if (isActive) theme.colors.onPrimaryContainer else theme.colors.onSurfaceVariant
    val borderColor = if (isActive) theme.colors.primary else theme.colors.outline
    val borderWidth = if (isActive) 2.dp else 1.dp
    val arrowColor = if (isActive) theme.colors.primary else descriptionColor
    val labelColor = if (isActive) theme.colors.primary else theme.colors.onSurfaceVariant

    AuiThemeProvider(theme = theme) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = theme.shapes.card,
            color = containerColor,
            border = BorderStroke(borderWidth, borderColor),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = theme.spacing.medium, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isActive) {
                        Text(
                            text = "Viewing",
                            style = theme.typography.label,
                            color = labelColor,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        text = title,
                        style = theme.typography.subheading,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = theme.typography.caption,
                            color = descriptionColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = arrowColor,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

private fun AuiResponse.cardStubTitle(): String = when (display) {
    AuiDisplay.SURVEY -> surveyTitle
        ?: cardTitle
        ?: steps.firstStepQuestion()
        ?: "Survey"

    else -> cardTitle
        ?: blocks.firstHeadingText()
        ?: blocks.firstFileTitle()
        ?: blocks.firstTextText()
        ?: "Tap to view"
}

private fun AuiResponse.cardStubDescription(): String? = when (display) {
    AuiDisplay.SURVEY -> cardDescription ?: steps.stepCountSummary()
    else -> cardDescription ?: blocks.firstFileDescription() ?: blocks.firstNonHeadingText()
}

private fun List<AuiStep>.firstStepQuestion(): String? =
    firstNotNullOfOrNull { it.question?.takeIf { q -> q.isNotBlank() } }

private fun List<AuiStep>.stepCountSummary(): String? = when (size) {
    0 -> null
    1 -> "1 question"
    else -> "$size questions"
}

private fun List<AuiBlock>.firstHeadingText(): String? =
    firstNotNullOfOrNull { (it as? AuiBlock.Heading)?.data?.text }

private fun List<AuiBlock>.firstTextText(): String? =
    firstNotNullOfOrNull { (it as? AuiBlock.Text)?.data?.text }

private fun List<AuiBlock>.firstFileTitle(): String? =
    firstNotNullOfOrNull { block ->
        (block as? AuiBlock.FileContent)?.data?.title
            ?: (block as? AuiBlock.FileContent)?.data?.filename
    }

private fun List<AuiBlock>.firstFileDescription(): String? =
    firstNotNullOfOrNull { block ->
        (block as? AuiBlock.FileContent)?.data?.description
            ?: (block as? AuiBlock.FileContent)?.data?.language
    }

private fun List<AuiBlock>.firstNonHeadingText(): String? =
    firstNotNullOfOrNull { (it as? AuiBlock.Text)?.data?.text }

@Preview(showBackground = true, name = "AuiResponseCard — Expanded")
@Composable
private fun AuiResponseCardExpandedPreview() {
    AuiThemeProvider {
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                cardTitle = "Headphone picks",
                cardDescription = "Three top noise-cancelling models compared",
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "AuiResponseCard — Survey")
@Composable
private fun AuiResponseCardSurveyPreview() {
    AuiThemeProvider {
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.SURVEY,
                surveyTitle = "Quick feedback",
                steps = listOf(
                    AuiStep(blocks = emptyList(), question = "How was your experience?"),
                    AuiStep(blocks = emptyList(), question = "Any additional comments?"),
                ),
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "AuiResponseCard — Active")
@Composable
private fun AuiResponseCardActivePreview() {
    AuiThemeProvider {
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                cardTitle = "Currently viewed",
            ),
            onClick = {},
            isActive = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
