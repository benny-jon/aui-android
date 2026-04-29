package com.bennyjon.aui.compose.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.StatusBannerWarningData

/**
 * Renders a `status_banner_warning` block.
 *
 * Displays a full-width warning banner with a warning icon and message text.
 * Typically used to call attention to a caution state that requires awareness.
 */
@Composable
fun AuiStatusBannerWarning(
    block: AuiBlock.StatusBannerWarning,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val announce = stringResource(
        R.string.aui_status_announce,
        stringResource(R.string.aui_status_intent_warning),
        block.data.text,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = theme.colors.warningContainer,
                shape = theme.shapes.banner,
            )
            .padding(theme.spacing.medium)
            .clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announce
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = theme.colors.warning,
            modifier = Modifier.size(theme.spacing.large),
        )
        Spacer(Modifier.width(theme.spacing.small))
        Text(
            text = block.data.text,
            style = theme.typography.body,
            color = theme.colors.onWarningContainer,
        )
    }
}
