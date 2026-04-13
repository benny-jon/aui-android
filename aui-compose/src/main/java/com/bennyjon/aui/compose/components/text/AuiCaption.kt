package com.bennyjon.aui.compose.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.CaptionData
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * Renders a `caption` block.
 *
 * Displays small, muted metadata text using [com.bennyjon.aui.compose.theme.AuiTheme]
 * caption typography and [com.bennyjon.aui.compose.theme.LocalAuiCaptionColor] color.
 */
@Composable
fun AuiCaption(
    block: AuiBlock.Caption,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Text(
        text = block.data.text,
        style = theme.typography.caption,
        color = LocalAuiCaptionColor.current,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AuiCaptionPreview() {
    AuiThemeProvider {
        AuiCaption(
            block = AuiBlock.Caption(data = CaptionData(text = "Step 2 of 3 · Quick Survey")),
        )
    }
}
