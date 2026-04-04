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
import com.bennyjon.aui.core.model.data.BadgeSuccessData

/**
 * Renders a `badge_success` block.
 *
 * Displays a small success-colored pill with text inside. Typically used to confirm
 * a completed action (e.g. "Submitted", "3 of 3 completed").
 */
@Composable
fun AuiBadgeSuccess(
    block: AuiBlock.BadgeSuccess,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Box(
        modifier = modifier
            .background(
                color = theme.colors.successContainer,
                shape = theme.shapes.badge,
            )
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.xSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = block.data.text,
            style = theme.typography.label,
            color = theme.colors.onSuccessContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiBadgeSuccessPreview() {
    AuiThemeProvider {
        AuiBadgeSuccess(
            block = AuiBlock.BadgeSuccess(
                data = BadgeSuccessData(text = "3 of 3 completed"),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
