package com.bennyjon.aui.compose.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.BadgeErrorData

/**
 * Renders a `badge_error` block.
 *
 * Displays a small error-colored pill with text inside. Typically used to surface a
 * blocking or destructive state (e.g. "Failed", "Offline").
 */
@Composable
fun AuiBadgeError(
    block: AuiBlock.BadgeError,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val announce = stringResource(
        R.string.aui_status_announce,
        stringResource(R.string.aui_status_intent_error),
        block.data.text,
    )
    Box(
        modifier = modifier
            .background(
                color = theme.colors.errorContainer,
                shape = theme.shapes.badge,
            )
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.xSmall)
            .clearAndSetSemantics { contentDescription = announce },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = block.data.text,
            style = theme.typography.label,
            color = theme.colors.onErrorContainer,
        )
    }
}
