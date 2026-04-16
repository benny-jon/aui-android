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
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Renders an AUI JSON string as native Compose UI.
 *
 * Parses [json] and delegates to [DisplayRouter], which selects the appropriate layout
 * (expanded or bottom sheet) based on the `display` field. The host app supplies
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
 *     pluginRegistry = myAppRegistry,
 *     onFeedback = { feedback -> viewModel.handleFeedback(feedback) },
 * )
 * ```
 *
 * @param json Raw AUI JSON string from the AI response.
 * @param modifier Modifier applied to the root layout.
 * @param theme The [AuiTheme] to apply. Defaults to [AuiTheme.Default].
 * @param pluginRegistry Registry of component and action plugins. Component plugins are
 *   checked before built-ins; action plugins participate in chain-of-responsibility
 *   routing with [onFeedback].
 * @param onFeedback Called when the user interacts with a block whose feedback was not
 *   claimed by an action plugin. If a registered [AuiActionPlugin][com.bennyjon.aui.core.plugin.AuiActionPlugin]
 *   matches the feedback's action and returns `true` from `handle`, this callback is
 *   **not** called. If no plugin matches, or the plugin returns `false`, this callback
 *   **is** called. Hosts using no plugins receive every feedback event (the common case).
 *   For sheets: called once on submit/dismiss with consolidated feedback.
 * @param collectingFeedbackEnabled When `false`, blocks that collect conversational feedback
 *   (e.g. submit buttons, polls, chip selects) are rendered at reduced alpha with their
 *   feedback callbacks suppressed. Pass-through blocks (no feedback, or read-only plugin
 *   actions like `open_url`) remain fully visible and functional. Defaults to `true`.
 * @param onParseError Called if the JSON cannot be parsed. The error message is passed as the argument.
 * @param onUnknownBlock Called for each unrecognized block type encountered during rendering.
 */
@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    collectingFeedbackEnabled: Boolean = true,
    onParseError: ((String) -> Unit)? = null,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    val response = runCatching { AuiParser().parse(json) }
        .onFailure { onParseError?.invoke(it.message ?: "Failed to parse AUI JSON") }
        .getOrNull() ?: return

    val routedOnFeedback: (AuiFeedback) -> Unit = { feedback ->
        val claimed = pluginRegistry.actionPlugin(feedback.action)?.handle(feedback) ?: false
        if (!claimed) onFeedback(feedback)
    }

    AuiThemeProvider(theme = theme) {
        DisplayRouter(
            response = response,
            modifier = modifier,
            pluginRegistry = pluginRegistry,
            onFeedback = routedOnFeedback,
            collectingFeedbackEnabled = collectingFeedbackEnabled,
            onUnknownBlock = onUnknownBlock,
        )
    }
}

/**
 * Renders a pre-parsed [AuiResponse] as native Compose UI.
 *
 * Wraps all content in an [AuiThemeProvider] and delegates routing to [DisplayRouter], which
 * selects the appropriate layout (expanded or bottom sheet) based on [AuiResponse.display]. The host app supplies [onFeedback] to receive interaction events.
 *
 * Example:
 * ```kotlin
 * AuiRenderer(
 *     response = parsedResponse,
 *     pluginRegistry = myAppRegistry,
 *     onFeedback = { feedback -> viewModel.handleFeedback(feedback) },
 * )
 * ```
 *
 * @param response The parsed [AuiResponse] to render.
 * @param modifier Modifier applied to the root layout.
 * @param theme The [AuiTheme] to apply. Defaults to [AuiTheme.Default].
 * @param pluginRegistry Registry of component and action plugins. Component plugins are
 *   checked before built-ins; action plugins participate in chain-of-responsibility
 *   routing with [onFeedback].
 * @param onFeedback Called when the user interacts with a block whose feedback was not
 *   claimed by an action plugin. If a registered [AuiActionPlugin][com.bennyjon.aui.core.plugin.AuiActionPlugin]
 *   matches the feedback's action and returns `true` from `handle`, this callback is
 *   **not** called. If no plugin matches, or the plugin returns `false`, this callback
 *   **is** called. Hosts using no plugins receive every feedback event (the common case).
 * @param collectingFeedbackEnabled When `false`, blocks that collect conversational feedback
 *   (e.g. submit buttons, polls, chip selects) are rendered at reduced alpha with their
 *   feedback callbacks suppressed. Pass-through blocks (no feedback, or read-only plugin
 *   actions like `open_url`) remain fully visible and functional. Defaults to `true`.
 */
@Composable
fun AuiRenderer(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    collectingFeedbackEnabled: Boolean = true,
) {
    val routedOnFeedback: (AuiFeedback) -> Unit = { feedback ->
        val claimed = pluginRegistry.actionPlugin(feedback.action)?.handle(feedback) ?: false
        if (!claimed) onFeedback(feedback)
    }

    AuiThemeProvider(theme = theme) {
        DisplayRouter(
            response = response,
            modifier = modifier,
            pluginRegistry = pluginRegistry,
            onFeedback = routedOnFeedback,
            collectingFeedbackEnabled = collectingFeedbackEnabled,
        )
    }
}

@Preview(showBackground = true, name = "AuiRenderer — Expanded Confirmation")
@Composable
private fun AuiRendererPreview() {
    val response = AuiResponse(
        display = AuiDisplay.EXPANDED,
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
