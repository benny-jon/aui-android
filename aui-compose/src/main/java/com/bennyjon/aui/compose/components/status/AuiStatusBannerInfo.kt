package com.bennyjon.aui.compose.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.StatusBannerInfoData

/**
 * Renders a `status_banner_info` block.
 *
 * Displays a full-width info banner with an info icon and message text.
 * Typically used for neutral announcements or tips.
 */
@Composable
fun AuiStatusBannerInfo(
    block: AuiBlock.StatusBannerInfo,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = theme.colors.infoContainer,
                shape = theme.shapes.banner,
            )
            .padding(theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = theme.colors.info,
            modifier = Modifier.size(theme.spacing.large),
        )
        Spacer(Modifier.width(theme.spacing.small))
        Text(
            text = block.data.text,
            style = theme.typography.body,
            color = theme.colors.onInfoContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiStatusBannerInfoPreview() {
    AuiThemeProvider {
        AuiStatusBannerInfo(
            block = AuiBlock.StatusBannerInfo(
                data = StatusBannerInfoData(text = "Heads up: we updated our privacy policy."),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
