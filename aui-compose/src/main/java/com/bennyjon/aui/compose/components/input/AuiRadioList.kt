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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback

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
                    role = Role.RadioButton,
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
                        registry.value = registry.value
                            .updateValue(block.data.key, option.label)
                            .updateUiStateValue(block.data.key, option.value)
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
