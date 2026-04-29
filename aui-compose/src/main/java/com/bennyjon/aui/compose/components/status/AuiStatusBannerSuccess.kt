package com.bennyjon.aui.compose.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.bennyjon.aui.core.model.data.StatusBannerSuccessData

/**
 * Renders a `status_banner_success` block.
 *
 * Displays a full-width success banner with a checkmark icon and message text.
 * Typically used as a confirmation after form submission.
 */
@Composable
fun AuiStatusBannerSuccess(
    block: AuiBlock.StatusBannerSuccess,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val announce = stringResource(
        R.string.aui_status_announce,
        stringResource(R.string.aui_status_intent_success),
        block.data.text,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = theme.colors.successContainer,
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
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = theme.colors.success,
            modifier = Modifier.size(theme.spacing.large),
        )
        Spacer(Modifier.width(theme.spacing.small))
        Text(
            text = block.data.text,
            style = theme.typography.body,
            color = theme.colors.onSuccessContainer,
        )
    }
}
