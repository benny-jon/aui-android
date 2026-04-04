package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.internal.resolvePlaceholders
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.ButtonSecondaryData

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
    val registry = LocalAuiValueRegistry.current
    OutlinedButton(
        onClick = {
            block.feedback?.let { feedback ->
                val allParams = registry.value + feedback.params
                val resolvedLabel = feedback.label?.let {
                    resolvePlaceholders(it, allParams)
                }
                onFeedback(feedback.copy(params = allParams, label = resolvedLabel))
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = theme.shapes.button,
        border = BorderStroke(width = 1.dp, color = theme.colors.outline),
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

@Preview(showBackground = true)
@Composable
private fun AuiButtonSecondaryPreview() {
    AuiThemeProvider {
        AuiButtonSecondary(
            block = AuiBlock.ButtonSecondary(
                data = ButtonSecondaryData(label = "Skip"),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
