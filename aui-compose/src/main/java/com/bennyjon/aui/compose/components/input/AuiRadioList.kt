package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.SelectionOption

/**
 * Renders a `radio_list` block.
 *
 * Displays a bordered vertical list of options where tapping one selects it and deselects any
 * previously selected option. Each option has a required label and optional description.
 * When [block.feedback] is set, [onFeedback] is called on each selection change with the
 * selected value added to params. The selected option's label is stored in the value registry
 * for Q+A entry building.
 */
@Composable
fun AuiRadioList(
    block: AuiBlock.RadioList,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val registry = LocalAuiValueRegistry.current
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(theme.spacing.dividerThickness, theme.colors.outline, theme.shapes.card)
                .clip(theme.shapes.card),
        ) {
            block.data.options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = theme.spacing.dividerThickness,
                        color = theme.colors.outline,
                    )
                }
                val isSelected = selectedValue == option.value
                SelectionRow(
                    selected = isSelected,
                    label = option.label,
                    description = option.description,
                    indicator = {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = theme.colors.primary,
                                unselectedColor = theme.colors.outline,
                            ),
                        )
                    },
                    onClick = {
                        selectedValue = option.value
                        registry.value = registry.value + (block.data.key to option.label)
                        block.feedback?.let { feedback ->
                            val updatedParams = feedback.params + mapOf(block.data.key to option.value)
                            onFeedback(feedback.copy(params = updatedParams))
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiRadioListPreview() {
    AuiThemeProvider {
        AuiRadioList(
            block = AuiBlock.RadioList(
                data = RadioListData(
                    key = "satisfaction",
                    label = "How satisfied are you?",
                    options = listOf(
                        SelectionOption(
                            label = "Very satisfied",
                            description = "I had a great experience overall",
                            value = "very_satisfied",
                        ),
                        SelectionOption(
                            label = "Somewhat satisfied",
                            description = "It was okay but could be better",
                            value = "somewhat_satisfied",
                        ),
                        SelectionOption(
                            label = "Neutral",
                            value = "neutral",
                        ),
                        SelectionOption(
                            label = "Not satisfied",
                            description = "I had issues that need to be addressed",
                            value = "not_satisfied",
                        ),
                    ),
                    selected = "somewhat_satisfied",
                ),
            ),
            modifier = Modifier.padding(
                horizontal = LocalAuiTheme.current.spacing.medium,
                vertical = LocalAuiTheme.current.spacing.small,
            ),
        )
    }
}
