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
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.StatusBannerErrorData

/**
 * Renders a `status_banner_error` block.
 *
 * Displays a full-width error banner with an error icon and message text.
 * Typically used to surface a failure or blocking state that requires user action.
 */
@Composable
fun AuiStatusBannerError(
    block: AuiBlock.StatusBannerError,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = theme.colors.errorContainer,
                shape = theme.shapes.banner,
            )
            .padding(theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = theme.colors.error,
            modifier = Modifier.size(theme.spacing.large),
        )
        Spacer(Modifier.width(theme.spacing.small))
        Text(
            text = block.data.text,
            style = theme.typography.body,
            color = theme.colors.onErrorContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiStatusBannerErrorPreview() {
    AuiThemeProvider {
        AuiStatusBannerError(
            block = AuiBlock.StatusBannerError(
                data = StatusBannerErrorData(text = "Couldn't reach the server. Check your connection."),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
