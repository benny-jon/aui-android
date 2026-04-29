package com.bennyjon.aui.compose.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.bennyjon.aui.compose.components.input.RegistryStateSaver
import com.bennyjon.aui.compose.internal.BlockRenderer
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Routes an [AuiResponse] to the appropriate display mode.
 *
 * - **INLINE** and **EXPANDED**: Rendered identically by the library. Leading
 *   text/heading/caption blocks are rendered inline; remaining content blocks are rendered
 *   full-width below. The two levels carry the AI's *intent* — `INLINE` means "belongs in
 *   the chat flow," `EXPANDED` means "focused content the user may want to study." Host
 *   apps decide whether to surface EXPANDED responses in a separate detail surface (a
 *   bottom sheet on narrow windows, a side detail pane on wider windows). The library
 *   does not enforce this — both render the same here.
 * - **SURVEY**: Renders flat survey content via [AuiSurveyContent] — the library manages step
 *   navigation, the stepper indicator, and accumulation, emitting a single [AuiFeedback] with
 *   all Q+A entries on submit. It does **not** wrap itself in a bottom sheet; hosts choose
 *   the container (sheet, dialog, pane, inline). See [AuiSurveyContent] for details.
 *
 * The split logic for INLINE/EXPANDED: blocks are scanned from the start. Contiguous leading
 * `text`, `heading`, and `caption` blocks accumulate into the "bubble" list. The first
 * non-text block and everything after it form the "content" list.
 *
 * @param response The parsed [AuiResponse] to route and render.
 * @param modifier Modifier applied to the outermost layout.
 * @param pluginRegistry Registry of component and action plugins. Component plugins are checked
 *   before built-ins when rendering unknown block types. Passed through to [BlockRenderer].
 *   Action plugin routing is handled upstream by [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer].
 * @param onFeedback Called when the user interacts with a block that has feedback configured.
 *   By the time this is called, [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer] has already
 *   applied chain-of-responsibility routing with action plugins.
 * @param collectingFeedbackEnabled When `false`, blocks that collect conversational feedback
 *   are rendered at reduced alpha with their feedback suppressed. Pass-through blocks remain
 *   fully visible and functional. Defaults to `true`.
 * @param onUnknownBlock If provided, called for each [AuiBlock.Unknown] the renderer skips,
 *   including unmatched block types and plugin-backed unknown blocks whose data is missing or
 *   malformed.
 */
@Composable
internal fun DisplayRouter(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    collectingFeedbackEnabled: Boolean = true,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    when (response.display) {
        AuiDisplay.SURVEY -> {
            AuiSurveyContent(
                steps = response.steps,
                surveyTitle = response.surveyTitle,
                onSubmit = onFeedback,
                modifier = modifier,
                pluginRegistry = pluginRegistry,
                onStepFeedback = onFeedback,
                onUnknownBlock = onUnknownBlock,
            )
        }

        else -> {
            val (bubbleBlocks, contentBlocks) = splitBlocks(response.blocks)
            // Shared registry so all inputs across both split renderers are visible to
            // buildEntriesFromBlocks. allBlocksForEntries = response.blocks ensures headings in
            // bubbleBlocks are correctly associated with inputs in contentBlocks.
            val sharedRegistry = rememberSaveable(saver = RegistryStateSaver) {
                mutableStateOf(emptyMap<String, String>())
            }
            Column(modifier = modifier.fillMaxWidth()) {
                if (bubbleBlocks.isNotEmpty()) {
                    BlockRenderer(
                        blocks = bubbleBlocks,
                        pluginRegistry = pluginRegistry,
                        onFeedback = onFeedback,
                        registryOverride = sharedRegistry,
                        allBlocksForEntries = response.blocks,
                        collectingFeedbackEnabled = collectingFeedbackEnabled,
                        onUnknownBlock = onUnknownBlock,
                    )
                }
                if (contentBlocks.isNotEmpty()) {
                    BlockRenderer(
                        blocks = contentBlocks,
                        modifier = Modifier.fillMaxWidth(),
                        pluginRegistry = pluginRegistry,
                        onFeedback = onFeedback,
                        registryOverride = sharedRegistry,
                        allBlocksForEntries = response.blocks,
                        collectingFeedbackEnabled = collectingFeedbackEnabled,
                        onUnknownBlock = onUnknownBlock,
                    )
                }
            }
        }
    }
}

/**
 * Splits [blocks] into a bubble list (leading text/heading/caption blocks) and a content list
 * (remaining blocks starting at the first non-text block).
 */
internal fun splitBlocks(blocks: List<AuiBlock>): Pair<List<AuiBlock>, List<AuiBlock>> {
    val splitIndex = blocks.indexOfFirst { block ->
        block !is AuiBlock.Text && block !is AuiBlock.Heading && block !is AuiBlock.Caption
    }
    return if (splitIndex == -1) {
        Pair(blocks, emptyList())
    } else {
        Pair(blocks.subList(0, splitIndex), blocks.subList(splitIndex, blocks.size))
    }
}
