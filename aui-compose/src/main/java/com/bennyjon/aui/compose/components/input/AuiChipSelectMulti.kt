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
import androidx.compose.ui.Modifier
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback

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
    val selectedValues = decodeUiStateValues(registry.value.uiStateValue(block.data.key))
        .ifEmpty { block.data.selected.toSet() }

    Column(modifier = modifier) {
        block.data.label?.let { label ->
            Text(
                text = label,
                style = theme.typography.label,
                color = LocalAuiCaptionColor.current,
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
                        val joinedLabels = block.data.options
                            .filter { it.value in newValues }
                            .joinToString(", ") { it.label }
                        val joinedValues = newValues.joinToString(", ")
                        registry.value = registry.value
                            .updateValue(block.data.key, joinedLabels.ifBlank { null })
                            .updateUiStateValue(block.data.key, encodeUiStateValues(newValues))
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
                        labelColor = LocalAuiBodyColor.current,
                    ),
                    leadingIcon = null,
                )
            }
        }
    }
}
