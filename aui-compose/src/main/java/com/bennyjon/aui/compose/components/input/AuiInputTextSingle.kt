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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.InputTextSingleData

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
    var text by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Text(
            text = block.data.label,
            style = theme.typography.label,
            color = theme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = theme.spacing.xSmall),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
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
            Spacer(modifier = Modifier.width(theme.spacing.small))
            Button(
                onClick = {
                    block.feedback?.let { feedback ->
                        onFeedback(
                            feedback.copy(
                                params = feedback.params + mapOf(block.data.key to text),
                            )
                        )
                    }
                },
                shape = theme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.colors.primary,
                    contentColor = theme.colors.onPrimary,
                ),
            ) {
                Text(
                    text = block.data.submitLabel ?: "Submit",
                    style = theme.typography.button,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiInputTextSinglePreview() {
    AuiThemeProvider {
        AuiInputTextSingle(
            block = AuiBlock.InputTextSingle(
                data = InputTextSingleData(
                    key = "open_feedback",
                    label = "Your feedback",
                    placeholder = "Optional — type anything here...",
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
