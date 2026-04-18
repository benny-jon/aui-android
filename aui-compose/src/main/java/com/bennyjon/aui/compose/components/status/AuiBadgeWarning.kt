package com.bennyjon.aui.compose.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.BadgeWarningData

/**
 * Renders a `badge_warning` block.
 *
 * Displays a small warning-colored pill with text inside. Typically used to highlight
 * a caution state (e.g. "Low stock", "Expires soon").
 */
@Composable
fun AuiBadgeWarning(
    block: AuiBlock.BadgeWarning,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Box(
        modifier = modifier
            .background(
                color = theme.colors.warningContainer,
                shape = theme.shapes.badge,
            )
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.xSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = block.data.text,
            style = theme.typography.label,
            color = theme.colors.onWarningContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiBadgeWarningPreview() {
    AuiThemeProvider {
        AuiBadgeWarning(
            block = AuiBlock.BadgeWarning(
                data = BadgeWarningData(text = "Low stock"),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
