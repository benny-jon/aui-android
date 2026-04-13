package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme

/**
 * A shared row layout used by [AuiRadioList] and [AuiCheckboxList].
 *
 * Renders a full-width tappable row with a leading [indicator] (radio button or checkbox),
 * a [label], and an optional [description]. When [selected] is true the row background gets
 * a subtle primary-color tint.
 */
@Composable
internal fun SelectionRow(
    selected: Boolean,
    label: String,
    description: String?,
    indicator: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val background = if (selected) theme.colors.primary.copy(alpha = 0.08f) else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        indicator()
        Column(
            modifier = Modifier
                .padding(start = theme.spacing.small)
                .weight(1f),
        ) {
            Text(
                text = label,
                style = theme.typography.body,
                color = LocalAuiBodyColor.current,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = theme.typography.caption,
                    color = LocalAuiCaptionColor.current,
                )
            }
        }
    }
}
