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
 * Renders a `chip_select_single` block.
 *
 * Displays a horizontal flow of chips where tapping one selects it and deselects any
 * previously selected chip. Selection state is managed locally. When [block.feedback] is set,
 * [onFeedback] is called on each selection change with the selected value added to params.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuiChipSelectSingle(
    block: AuiBlock.ChipSelectSingle,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val registry = LocalAuiValueRegistry.current
    val selectedValue = registry.value.uiStateValue(block.data.key) ?: block.data.selected

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
                val isSelected = selectedValue == option.value
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        registry.value = registry.value
                            .updateValue(block.data.key, option.label)
                            .updateUiStateValue(block.data.key, option.value)
                        block.feedback?.let { feedback ->
                            val updatedParams = feedback.params + mapOf(block.data.key to option.value)
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
