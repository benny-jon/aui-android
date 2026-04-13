package com.bennyjon.aui.compose.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.ProgressBarData

/**
 * Renders a `progress_bar` block.
 *
 * Displays a label above a linear progress indicator. Progress is computed as
 * `data.progress / (data.max ?: 100f)`, clamped to [0, 1].
 */
@Composable
fun AuiProgressBar(
    block: AuiBlock.ProgressBar,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val fraction = (block.data.progress / (block.data.max ?: 100f)).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = block.data.label,
            style = theme.typography.caption,
            color = LocalAuiCaptionColor.current,
        )
        Spacer(Modifier.height(theme.spacing.xSmall))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = theme.colors.primary,
            trackColor = theme.colors.primaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiProgressBarPreview() {
    AuiThemeProvider {
        AuiProgressBar(
            block = AuiBlock.ProgressBar(
                data = ProgressBarData(
                    label = "Step 2 of 3",
                    progress = 2f,
                    max = 3f,
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
