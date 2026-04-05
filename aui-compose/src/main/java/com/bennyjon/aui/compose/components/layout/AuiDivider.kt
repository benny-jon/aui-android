package com.bennyjon.aui.compose.components.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * Renders a `divider` block.
 *
 * Displays a full-width horizontal separator line using [com.bennyjon.aui.compose.theme.AuiTheme]
 * outline color and divider thickness.
 */
@Composable
fun AuiDivider(modifier: Modifier = Modifier) {
    val theme = LocalAuiTheme.current
    HorizontalDivider(
        modifier = modifier.padding(vertical = theme.spacing.small),
        thickness = theme.spacing.dividerThickness,
        color = theme.colors.outline,
    )
}

@Preview(showBackground = true)
@Composable
private fun AuiDividerPreview() {
    AuiThemeProvider {
        AuiDivider()
    }
}
