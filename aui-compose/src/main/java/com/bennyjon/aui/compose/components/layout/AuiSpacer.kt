package com.bennyjon.aui.compose.components.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * Renders a `spacer` block.
 *
 * Inserts vertical breathing room using [com.bennyjon.aui.compose.theme.AuiSpacing.spacerHeight].
 */
@Composable
fun AuiSpacer(modifier: Modifier = Modifier) {
    val theme = LocalAuiTheme.current
    Spacer(modifier = modifier.height(theme.spacing.spacerHeight))
}

@Preview(showBackground = true)
@Composable
private fun AuiSpacerPreview() {
    AuiThemeProvider {
        AuiSpacer()
    }
}
