package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectMultiData

/**
 * Renders a `chip_select_multi` block.
 *
 * Displays a horizontal flow of chips where each chip can be toggled independently.
 * Selection state is managed locally. When [block.feedback] is set, [onFeedback] is called
 * on each toggle with the full set of selected values (comma-separated) added to params.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuiChipSelectMulti(
    block: AuiBlock.ChipSelectMulti,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val registry = LocalAuiValueRegistry.current
    var selectedValues by remember { mutableStateOf(block.data.selected.toSet()) }

    Column(modifier = modifier) {
        block.data.label?.let { label ->
            Text(
                text = label,
                style = theme.typography.label,
                color = theme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = theme.spacing.xSmall),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
        ) {
            block.data.options.forEach { option ->
                val isSelected = option.value in selectedValues
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newValues = if (isSelected) selectedValues - option.value else selectedValues + option.value
                        selectedValues = newValues
                        val joinedLabels = block.data.options
                            .filter { it.value in newValues }
                            .joinToString(", ") { it.label }
                        val joinedValues = newValues.joinToString(", ")
                        registry.value = registry.value + (block.data.key to joinedLabels)
                        block.feedback?.let { feedback ->
                            val updatedParams = feedback.params + mapOf(block.data.key to joinedValues)
                            onFeedback(feedback.copy(params = updatedParams))
                        }
                    },
                    label = {
                        Text(
                            text = option.label,
                            style = theme.typography.label,
                        )
                    },
                    shape = theme.shapes.chip,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.colors.primary,
                        selectedLabelColor = theme.colors.onPrimary,
                        labelColor = theme.colors.onSurface,
                    ),
                    leadingIcon = null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiChipSelectMultiPreview() {
    AuiThemeProvider {
        AuiChipSelectMulti(
            block = AuiBlock.ChipSelectMulti(
                data = ChipSelectMultiData(
                    key = "features",
                    label = "What features do you use most?",
                    options = listOf(
                        ChipOption(label = "Chat", value = "chat"),
                        ChipOption(label = "Search", value = "search"),
                        ChipOption(label = "Recommendations", value = "recs"),
                        ChipOption(label = "Order tracking", value = "tracking"),
                        ChipOption(label = "Account settings", value = "settings"),
                    ),
                    selected = listOf("chat", "search"),
                ),
            ),
            modifier = Modifier.padding(
                horizontal = LocalAuiTheme.current.spacing.medium,
                vertical = LocalAuiTheme.current.spacing.small,
            ),
        )
    }
}
