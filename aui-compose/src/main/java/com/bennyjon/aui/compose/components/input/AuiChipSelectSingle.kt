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
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectSingleData

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
    var selectedValue by remember { mutableStateOf(block.data.selected) }

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
                val isSelected = selectedValue == option.value
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedValue = option.value
                        block.feedback?.let { feedback ->
                            onFeedback(
                                feedback.copy(
                                    params = feedback.params + mapOf(block.data.key to option.value),
                                )
                            )
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
private fun AuiChipSelectSinglePreview() {
    AuiThemeProvider {
        AuiChipSelectSingle(
            block = AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(
                    key = "experience",
                    label = "How was your experience?",
                    options = listOf(
                        ChipOption(label = "Great", value = "great"),
                        ChipOption(label = "Good", value = "good"),
                        ChipOption(label = "Okay", value = "okay"),
                        ChipOption(label = "Poor", value = "poor"),
                    ),
                    selected = "good",
                ),
            ),
            modifier = Modifier.padding(
                horizontal = LocalAuiTheme.current.spacing.medium,
                vertical = LocalAuiTheme.current.spacing.small,
            ),
        )
    }
}
