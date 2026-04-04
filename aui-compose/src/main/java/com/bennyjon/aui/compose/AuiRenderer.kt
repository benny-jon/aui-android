package com.bennyjon.aui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.display.DisplayRouter
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.data.BadgeSuccessData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.StatusBannerSuccessData
import com.bennyjon.aui.core.model.data.TextData

/**
 * The primary entry point for rendering an [AuiResponse].
 *
 * Wraps all content in an [AuiThemeProvider] and delegates routing to [DisplayRouter], which
 * selects the appropriate layout (inline, expanded, or bottom sheet) based on
 * [AuiResponse.display]. The host app supplies [onFeedback] to receive interaction events.
 *
 * Example:
 * ```kotlin
 * AuiRenderer(
 *     response = parsedResponse,
 *     onFeedback = { feedback -> viewModel.handleFeedback(feedback) },
 * )
 * ```
 *
 * @param response The parsed [AuiResponse] to render.
 * @param modifier Modifier applied to the root layout.
 * @param theme The [AuiTheme] to apply. Defaults to [AuiTheme.Default].
 * @param onFeedback Called when the user interacts with a block that has feedback configured.
 */
@Composable
fun AuiRenderer(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    AuiThemeProvider(theme = theme) {
        DisplayRouter(
            response = response,
            modifier = modifier,
            onFeedback = onFeedback,
        )
    }
}

@Preview(showBackground = true, name = "AuiRenderer — Inline Confirmation")
@Composable
private fun AuiRendererPreview() {
    val response = AuiResponse(
        display = AuiDisplay.INLINE,
        blocks = listOf(
            AuiBlock.StatusBannerSuccess(
                data = StatusBannerSuccessData(text = "Survey complete!"),
            ),
            AuiBlock.Text(
                data = TextData(text = "Thanks for your feedback. This helps us make the app better for you."),
            ),
            AuiBlock.Heading(
                data = HeadingData(text = "What's next?"),
            ),
            AuiBlock.Text(
                data = TextData(text = "We review all feedback weekly and prioritize based on your input."),
            ),
            AuiBlock.BadgeSuccess(
                data = BadgeSuccessData(text = "3 of 3 completed"),
            ),
        ),
    )
    AuiThemeProvider {
        AuiRenderer(
            response = response,
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
            onFeedback = {},
        )
    }
}
