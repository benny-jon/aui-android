package com.bennyjon.aui.compose.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.bennyjon.aui.compose.internal.BlockRenderer
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.TextData

/**
 * Routes an [AuiResponse] to the appropriate display mode.
 *
 * - **INLINE**: All blocks rendered in a [Column]. Suitable for chat bubbles.
 * - **EXPANDED**: Leading text/heading/caption blocks rendered inline; remaining content
 *   blocks rendered full-width below.
 * - **SHEET**: Renders a persistent bottom sheet that navigates through each [AuiStep]
 *   without closing between steps. The library manages step navigation, the stepper indicator,
 *   and accumulation — emitting a single [AuiFeedback] with all Q+A entries at the end.
 *
 * The split logic for INLINE/EXPANDED: blocks are scanned from the start. Contiguous leading
 * `text`, `heading`, and `caption` blocks accumulate into the "bubble" list. The first
 * non-text block and everything after it form the "content" list.
 *
 * @param response The parsed [AuiResponse] to route and render.
 * @param modifier Modifier applied to the outermost layout.
 * @param onFeedback Called when the user interacts with a block that has feedback configured.
 */
@Composable
fun DisplayRouter(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    when (response.display) {
        AuiDisplay.INLINE -> {
            BlockRenderer(
                blocks = response.blocks,
                modifier = modifier,
                onFeedback = onFeedback,
            )
        }

        AuiDisplay.EXPANDED -> {
            val (bubbleBlocks, contentBlocks) = splitBlocks(response.blocks)
            // Shared registry so all inputs across both split renderers are visible to
            // buildEntriesFromBlocks. allBlocksForEntries = response.blocks ensures headings in
            // bubbleBlocks are correctly associated with inputs in contentBlocks.
            val sharedRegistry = remember { mutableStateOf(emptyMap<String, String>()) }
            Column(modifier = modifier.fillMaxWidth()) {
                if (bubbleBlocks.isNotEmpty()) {
                    BlockRenderer(
                        blocks = bubbleBlocks,
                        onFeedback = onFeedback,
                        registryOverride = sharedRegistry,
                        allBlocksForEntries = response.blocks,
                    )
                }
                if (contentBlocks.isNotEmpty()) {
                    BlockRenderer(
                        blocks = contentBlocks,
                        modifier = Modifier.fillMaxWidth(),
                        onFeedback = onFeedback,
                        registryOverride = sharedRegistry,
                        allBlocksForEntries = response.blocks,
                    )
                }
            }
        }

        AuiDisplay.SHEET -> {
            SheetFlowDisplay(
                steps = response.steps,
                sheetTitle = response.sheetTitle,
                onFeedback = onFeedback,
            )
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

@Preview(showBackground = true, name = "DisplayRouter — Inline")
@Composable
private fun DisplayRouterInlinePreview() {
    AuiThemeProvider {
        DisplayRouter(
            response = AuiResponse(
                display = AuiDisplay.INLINE,
                blocks = listOf(
                    AuiBlock.Text(data = TextData(text = "Was this response helpful?")),
                    AuiBlock.ChipSelectSingle(
                        data = ChipSelectSingleData(
                            key = "helpful",
                            options = listOf(
                                ChipOption(label = "Yes", value = "yes"),
                                ChipOption(label = "No", value = "no"),
                            ),
                        ),
                    ),
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
            onFeedback = {},
        )
    }
}

@Preview(showBackground = true, name = "DisplayRouter — Expanded")
@Composable
private fun DisplayRouterExpandedPreview() {
    AuiThemeProvider {
        DisplayRouter(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                blocks = listOf(
                    AuiBlock.Heading(data = HeadingData(text = "What features do you use most?")),
                    AuiBlock.Text(data = TextData(text = "Select all that apply.")),
                    AuiBlock.ChipSelectSingle(
                        data = ChipSelectSingleData(
                            key = "feature",
                            options = listOf(
                                ChipOption(label = "Chat", value = "chat"),
                                ChipOption(label = "Search", value = "search"),
                                ChipOption(label = "Orders", value = "orders"),
                            ),
                        ),
                    ),
                    AuiBlock.ButtonPrimary(
                        data = ButtonPrimaryData(label = "Submit"),
                        feedback = AuiFeedback(action = "poll_submit", params = mapOf("poll_id" to "features")),
                    ),
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
            onFeedback = {},
        )
    }
}

@Preview(showBackground = true, name = "DisplayRouter — Sheet Flow")
@Composable
private fun DisplayRouterSheetFlowPreview() {
    AuiThemeProvider {
        DisplayRouter(
            response = AuiResponse(
                display = AuiDisplay.SHEET,
                sheetTitle = "Quick Survey",
                steps = listOf(
                    AuiStep(
                        label = "Experience",
                        question = "How was your experience?",
                        skippable = true,
                        blocks = listOf(
                            AuiBlock.ChipSelectSingle(
                                data = ChipSelectSingleData(
                                    key = "experience",
                                    options = listOf(
                                        ChipOption(label = "😊 Great", value = "great"),
                                        ChipOption(label = "🙂 Good", value = "good"),
                                    ),
                                ),
                            ),
                            AuiBlock.ButtonPrimary(
                                data = ButtonPrimaryData(label = "Next"),
                                feedback = AuiFeedback(action = "poll_next_step"),
                            ),
                        ),
                    ),
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
            onFeedback = {},
        )
    }
}
