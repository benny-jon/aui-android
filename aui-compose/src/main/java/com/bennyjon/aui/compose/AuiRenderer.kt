package com.bennyjon.aui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.display.DisplayRouter
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.AuiParser
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.data.BadgeSuccessData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.StatusBannerSuccessData
import com.bennyjon.aui.core.model.data.TextData

/**
 * Renders an AUI JSON string as native Compose UI.
 *
 * Parses [json] and delegates to [DisplayRouter], which selects the appropriate layout
 * (inline, expanded, or bottom sheet) based on the `display` field. The host app supplies
 * [onFeedback] to receive interaction events.
 *
 * For sheet responses: the renderer opens a [ModalBottomSheet] overlay and emits no visible
 * content in the inline layout. The sheet navigates through all steps internally and calls
 * [onFeedback] once on submit or dismiss, then closes. The composable is inert after closing
 * (provided the host uses a stable key in [LazyColumn]).
 *
 * Example:
 * ```kotlin
 * AuiRenderer(
 *     json = aiResponseJson,
 *     onFeedback = { feedback -> viewModel.handleFeedback(feedback) },
 * )
 * ```
 *
 * @param json Raw AUI JSON string from the AI response.
 * @param modifier Modifier applied to the root layout.
 * @param theme The [AuiTheme] to apply. Defaults to [AuiTheme.Default].
 * @param onFeedback Called when the user interacts with a block that has feedback configured.
 *   For sheets: called once on submit/dismiss with consolidated feedback.
 * @param onParseError Called if the JSON cannot be parsed. The error message is passed as the argument.
 * @param onUnknownBlock Called for each unrecognized block type encountered during rendering.
 */
@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
    onParseError: ((String) -> Unit)? = null,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    val response = runCatching { AuiParser().parse(json) }
        .onFailure { onParseError?.invoke(it.message ?: "Failed to parse AUI JSON") }
        .getOrNull() ?: return

    AuiThemeProvider(theme = theme) {
        DisplayRouter(
            response = response,
            modifier = modifier,
            onFeedback = onFeedback,
            onUnknownBlock = onUnknownBlock,
        )
    }
}

/**
 * Renders a pre-parsed [AuiResponse] as native Compose UI.
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
