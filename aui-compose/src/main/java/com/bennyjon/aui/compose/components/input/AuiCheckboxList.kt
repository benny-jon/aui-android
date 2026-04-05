package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.bennyjon.aui.core.model.data.CheckboxListData
import com.bennyjon.aui.core.model.data.SelectionOption

/**
 * Renders a `checkbox_list` block.
 *
 * Displays a bordered vertical list of options where each option can be toggled independently.
 * Each option has a required label and optional description. When [block.feedback] is set,
 * [onFeedback] is called on each toggle with the full set of selected values (comma-separated)
 * added to params. The comma-separated labels of all checked options are stored in the value
 * registry for Q+A entry building.
 */
@Composable
fun AuiCheckboxList(
    block: AuiBlock.CheckboxList,
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
                val isChecked = option.value in selectedValues
                SelectionRow(
                    selected = isChecked,
                    label = option.label,
                    description = option.description,
                    indicator = {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = theme.colors.primary,
                                uncheckedColor = theme.colors.outline,
                                checkmarkColor = theme.colors.onPrimary,
                            ),
                        )
                    },
                    onClick = {
                        val newValues = if (isChecked) selectedValues - option.value else selectedValues + option.value
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
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiCheckboxListPreview() {
    AuiThemeProvider {
        AuiCheckboxList(
            block = AuiBlock.CheckboxList(
                data = CheckboxListData(
                    key = "improvements",
                    label = "What areas need improvement?",
                    options = listOf(
                        SelectionOption(
                            label = "Speed and performance",
                            description = "App feels slow in some areas",
                            value = "speed",
                        ),
                        SelectionOption(
                            label = "Design and usability",
                            description = "Some screens are hard to navigate",
                            value = "design",
                        ),
                        SelectionOption(
                            label = "Feature set",
                            description = "Missing features I need",
                            value = "features",
                        ),
                        SelectionOption(
                            label = "Pricing",
                            description = "Current pricing doesn't feel fair",
                            value = "pricing",
                        ),
                    ),
                    selected = listOf("speed", "design"),
                ),
            ),
            modifier = Modifier.padding(
                horizontal = LocalAuiTheme.current.spacing.medium,
                vertical = LocalAuiTheme.current.spacing.small,
            ),
        )
    }
}
