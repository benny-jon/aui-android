package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bennyjon.aui.compose.LocalAuiRenderState
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import kotlin.math.roundToInt

/**
 * Renders an `input_slider` block.
 *
 * Displays a labeled range slider between [InputSliderData.min] and [InputSliderData.max].
 * The current value is shown alongside the label. When [block.feedback] is set, [onFeedback]
 * is called when the user releases the thumb, with the final value added to params.
 */
@Composable
fun AuiInputSlider(
    block: AuiBlock.InputSlider,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val renderState = LocalAuiRenderState.current
    val data = block.data
    val sliderValue = renderState.uiState(data.key)?.toFloatOrNull() ?: (data.value ?: data.min)

    val step = data.step
    val steps = if (step != null && step > 0f) {
        ((data.max - data.min) / step - 1f).roundToInt().coerceAtLeast(0)
    } else {
        0
    }

    val displayValue = if (step != null && step >= 1f) {
        sliderValue.roundToInt().toString()
    } else {
        "%.1f".format(sliderValue)
    }

    Column(modifier = modifier) {
        Row {
            Text(
                text = data.label,
                style = theme.typography.label,
                color = LocalAuiCaptionColor.current,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = theme.spacing.xSmall),
            )
            Text(
                text = displayValue,
                style = theme.typography.label,
                color = theme.colors.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                val valueText = if (step != null && step >= 1f) {
                    it.toInt().toString()
                } else {
                    "%.1f".format(it)
                }
                renderState.setInputState(
                    key = data.key,
                    value = valueText,
                    uiStateValue = it.toString(),
                )
                renderState.setValue("value", valueText)
            },
            valueRange = data.min..data.max,
            steps = steps,
            onValueChangeFinished = {
                block.feedback?.let { feedback ->
                    val updatedParams = feedback.params + mapOf(data.key to displayValue, "value" to displayValue)
                    onFeedback(feedback.copy(params = updatedParams))
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = theme.colors.primary,
                activeTrackColor = theme.colors.primary,
                inactiveTrackColor = theme.colors.primaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
