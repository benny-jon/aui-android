package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bennyjon.aui.compose.LocalAuiRenderState
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback

/**
 * Renders an `input_text_single` block.
 *
 * Displays a labeled single-line text field with an optional placeholder. A submit button
 * appears inline next to the field. When [block.feedback] is set, [onFeedback] is called
 * on submit with the entered text added to params under [block.data.key].
 */
@Composable
fun AuiInputTextSingle(
    block: AuiBlock.InputTextSingle,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val renderState = LocalAuiRenderState.current
    val text = renderState.uiState(block.data.key) ?: renderState.value(block.data.key).orEmpty()

    Column(modifier = modifier) {
        Text(
            text = block.data.label,
            style = theme.typography.label,
            color = LocalAuiCaptionColor.current,
            modifier = Modifier.padding(bottom = theme.spacing.xSmall),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    renderState.setInputState(
                        key = block.data.key,
                        value = it.ifBlank { null },
                    )
                },
                placeholder = block.data.placeholder?.let {
                    { Text(text = it, style = theme.typography.body) }
                },
                singleLine = true,
                textStyle = theme.typography.body,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.colors.primary,
                    unfocusedBorderColor = theme.colors.outline,
                    focusedLabelColor = theme.colors.primary,
                    unfocusedLabelColor = theme.colors.onSurfaceVariant,
                    cursorColor = theme.colors.primary,
                ),
                modifier = Modifier.weight(1f),
            )
            val blockFeedback = block.feedback
            if (blockFeedback != null) {
                Spacer(modifier = Modifier.width(theme.spacing.small))
                Button(
                    onClick = {
                        val updatedParams = blockFeedback.params + mapOf(block.data.key to text)
                        onFeedback(blockFeedback.copy(params = updatedParams))
                    },
                    shape = theme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.colors.primary,
                        contentColor = theme.colors.onPrimary,
                    ),
                ) {
                    Text(
                        text = block.data.submitLabel ?: stringResource(R.string.aui_input_text_submit_default),
                        style = theme.typography.button,
                    )
                }
            }
        }
    }
}
