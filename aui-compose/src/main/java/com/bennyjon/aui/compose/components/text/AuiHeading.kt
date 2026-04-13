package com.bennyjon.aui.compose.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiHeadingColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * Renders a `heading` block.
 *
 * Displays a bold section title using [AuiTheme] heading typography.
 */
@Composable
fun AuiHeading(
    block: AuiBlock.Heading,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Text(
        text = block.data.text,
        style = theme.typography.heading,
        color = LocalAuiHeadingColor.current,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AuiHeadingPreview() {
    AuiThemeProvider {
        AuiHeading(
            block = AuiBlock.Heading(data = HeadingData(text = "What features do you use most?")),
        )
    }
}
