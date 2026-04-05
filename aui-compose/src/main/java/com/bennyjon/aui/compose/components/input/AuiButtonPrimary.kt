package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.ButtonPrimaryData

/**
 * Renders a `button_primary` block.
 *
 * Displays a filled primary-color button. When [block.feedback] is set,
 * [onFeedback] is called on tap.
 */
@Composable
fun AuiButtonPrimary(
    block: AuiBlock.ButtonPrimary,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val registry = LocalAuiValueRegistry.current
    Button(
        onClick = {
            block.feedback?.let { feedback ->
                // Merge registry values into params so the AI receives all user inputs.
                // Explicit feedback.params (poll_id, step, etc.) take priority over registry.
                val allParams = registry.value + feedback.params
                onFeedback(feedback.copy(params = allParams))
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = theme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.colors.primary,
            contentColor = theme.colors.onPrimary,
        ),
    ) {
        Text(
            text = block.data.label,
            style = theme.typography.button,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiButtonPrimaryPreview() {
    AuiThemeProvider {
        AuiButtonPrimary(
            block = AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Submit Feedback"),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
