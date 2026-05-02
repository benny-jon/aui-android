package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bennyjon.aui.compose.LocalAuiRenderState
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback

/**
 * Renders a `button_secondary` block.
 *
 * Displays an outlined secondary-action button. When [block.feedback] is set,
 * [onFeedback] is called on tap.
 */
@Composable
fun AuiButtonSecondary(
    block: AuiBlock.ButtonSecondary,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val renderState = LocalAuiRenderState.current
    OutlinedButton(
        onClick = {
            block.feedback?.let { feedback ->
                val allParams = renderState.inputValues + feedback.params
                onFeedback(feedback.copy(params = allParams))
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = theme.shapes.button,
        border = BorderStroke(width = theme.spacing.dividerThickness, color = theme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = theme.colors.primary,
        ),
    ) {
        Text(
            text = block.data.label,
            style = theme.typography.button,
        )
    }
}
