package com.bennyjon.aui.compose.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.TextData
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * Renders a `text` block.
 *
 * Displays plain body text using [AuiTheme] typography and color. Markdown is not
 * parsed in Phase 1 — the text is rendered as-is.
 */
@Composable
fun AuiText(
    block: AuiBlock.Text,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Text(
        text = block.data.text,
        style = theme.typography.body,
        color = LocalAuiBodyColor.current,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AuiTextPreview() {
    AuiThemeProvider {
        AuiText(
            block = AuiBlock.Text(data = TextData(text = "How would you rate your experience today?")),
        )
    }
}
