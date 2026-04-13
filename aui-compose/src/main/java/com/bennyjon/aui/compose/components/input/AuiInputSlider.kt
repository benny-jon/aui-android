package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.InputSliderData
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
    val registry = LocalAuiValueRegistry.current
    val data = block.data
    var sliderValue by remember { mutableFloatStateOf(data.value ?: data.min) }

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

    // Register initial value so buttons on the same page can read it before the user drags.
    LaunchedEffect(data.key) {
        registry.value = registry.value + mapOf(data.key to displayValue, "value" to displayValue)
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
            onValueChange = { sliderValue = it },
            valueRange = data.min..data.max,
            steps = steps,
            onValueChangeFinished = {
                registry.value = registry.value + mapOf(data.key to displayValue, "value" to displayValue)
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

@Preview(showBackground = true)
@Composable
private fun AuiInputSliderPreview() {
    AuiThemeProvider {
        AuiInputSlider(
            block = AuiBlock.InputSlider(
                data = InputSliderData(
                    key = "nps",
                    label = "0 = Not likely, 10 = Very likely",
                    min = 0f,
                    max = 10f,
                    value = 5f,
                    step = 1f,
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
