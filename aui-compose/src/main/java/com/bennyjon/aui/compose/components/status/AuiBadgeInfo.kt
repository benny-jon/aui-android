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
import com.bennyjon.aui.core.model.data.BadgeInfoData

/**
 * Renders a `badge_info` block.
 *
 * Displays a small info-colored pill with text inside. Typically used to surface neutral
 * status (e.g. "New", "Beta", "3 items").
 */
@Composable
fun AuiBadgeInfo(
    block: AuiBlock.BadgeInfo,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Box(
        modifier = modifier
            .background(
                color = theme.colors.infoContainer,
                shape = theme.shapes.badge,
            )
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.xSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = block.data.text,
            style = theme.typography.label,
            color = theme.colors.onInfoContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiBadgeInfoPreview() {
    AuiThemeProvider {
        AuiBadgeInfo(
            block = AuiBlock.BadgeInfo(
                data = BadgeInfoData(text = "New"),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
