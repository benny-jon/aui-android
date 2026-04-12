package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.QuickRepliesData
import com.bennyjon.aui.core.model.data.QuickReplyOption

/**
 * Renders a `quick_replies` block.
 *
 * Displays a wrapping row of outlined chips. Each option triggers its own
 * [AuiFeedback] immediately on tap. The tapped option stays highlighted to indicate selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuiQuickReplies(
    block: AuiBlock.QuickReplies,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.small),
    ) {
        block.data.options.forEachIndexed { index, option ->
            val isSelected = tappedIndex == index
            SuggestionChip(
                onClick = {
                    tappedIndex = index
                    option.feedback?.let { onFeedback(it) }
                },
                label = {
                    Text(
                        text = option.label,
                        style = theme.typography.label,
                    )
                },
                shape = theme.shapes.chip,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isSelected) theme.colors.primaryContainer else theme.colors.surface,
                    labelColor = if (isSelected) theme.colors.onPrimaryContainer else theme.colors.onSurface,
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = if (isSelected) theme.colors.primary else theme.colors.outline,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiQuickRepliesPreview() {
    AuiThemeProvider {
        AuiQuickReplies(
            block = AuiBlock.QuickReplies(
                data = QuickRepliesData(
                    options = listOf(
                        QuickReplyOption(
                            label = "Yes",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "yes")),
                        ),
                        QuickReplyOption(
                            label = "No",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "no")),
                        ),
                        QuickReplyOption(
                            label = "Maybe",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "maybe")),
                        ),
                    ),
                ),
            ),
        )
    }
}
