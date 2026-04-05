package com.bennyjon.aui.compose.internal

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bennyjon.aui.compose.components.input.AuiButtonPrimary
import com.bennyjon.aui.compose.components.input.AuiButtonSecondary
import com.bennyjon.aui.compose.components.input.AuiChipSelectMulti
import com.bennyjon.aui.compose.components.input.AuiChipSelectSingle
import com.bennyjon.aui.compose.components.input.AuiInputRatingStars
import com.bennyjon.aui.compose.components.input.AuiInputSlider
import com.bennyjon.aui.compose.components.input.AuiInputTextSingle
import com.bennyjon.aui.compose.components.input.AuiQuickReplies
import com.bennyjon.aui.compose.components.layout.AuiDivider
import com.bennyjon.aui.compose.components.layout.AuiProgressBar
import com.bennyjon.aui.compose.components.layout.AuiSpacer
import com.bennyjon.aui.compose.components.layout.AuiStepperHorizontal
import com.bennyjon.aui.compose.components.status.AuiBadgeSuccess
import com.bennyjon.aui.compose.components.status.AuiStatusBannerSuccess
import com.bennyjon.aui.compose.components.text.AuiCaption
import com.bennyjon.aui.compose.components.text.AuiHeading
import com.bennyjon.aui.compose.components.text.AuiText
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback

private const val TAG = "BlockRenderer"

/**
 * Scans [blocks] for heading→input pairs and maps them to [AuiEntry] instances using the
 * current [registry] values. Each [AuiBlock.Heading] sets the current question; the next
 * input block whose key has a value in the registry produces an entry.
 */
internal fun buildEntriesFromBlocks(blocks: List<AuiBlock>, registry: Map<String, String>): List<AuiEntry> {
    val entries = mutableListOf<AuiEntry>()
    var currentQuestion: String? = null
    for (block in blocks) {
        if (block is AuiBlock.Heading) {
            currentQuestion = block.data.text
            continue
        }
        val key = block.inputKey() ?: continue
        val question = currentQuestion ?: continue
        val answer = registry[key]?.takeIf { it.isNotBlank() } ?: continue
        entries.add(AuiEntry(question = question, answer = answer))
        currentQuestion = null
    }
    return entries
}

private fun AuiBlock.inputKey(): String? = when (this) {
    is AuiBlock.ChipSelectSingle -> data.key
    is AuiBlock.ChipSelectMulti -> data.key
    is AuiBlock.InputSlider -> data.key
    is AuiBlock.InputRatingStars -> data.key
    is AuiBlock.InputTextSingle -> data.key
    else -> null
}

/**
 * Maps each [AuiBlock] to its composable via an exhaustive `when` expression.
 *
 * Unknown block types are skipped with a warning log — the renderer never crashes on
 * unrecognized input.
 *
 * @param registryOverride If provided, this registry is shared with sibling renderers (e.g. the
 *   two split renderers in EXPANDED display). If null, a fresh local registry is created.
 * @param allBlocksForEntries If provided, [buildEntriesFromBlocks] scans this list instead of
 *   [blocks] when building Q+A entries on feedback. Use this when the heading that precedes an
 *   input lives in a sibling renderer (EXPANDED split).
 */
@Composable
internal fun BlockRenderer(
    blocks: List<AuiBlock>,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
    registryOverride: MutableState<Map<String, String>>? = null,
    allBlocksForEntries: List<AuiBlock>? = null,
) {
    val localRegistry = remember { mutableStateOf(emptyMap<String, String>()) }
    val registry = registryOverride ?: localRegistry
    val entryBlocks = allBlocksForEntries ?: blocks
    val wrappedOnFeedback: (AuiFeedback) -> Unit = { feedback ->
        val entries = buildEntriesFromBlocks(entryBlocks, registry.value)
        val formattedEntries = entries
            .joinToString("\n\n") { "${it.question}\n${it.answer}" }
            .ifBlank { null }
        onFeedback(feedback.copy(entries = entries, formattedEntries = formattedEntries))
    }
    CompositionLocalProvider(LocalAuiValueRegistry provides registry) {
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is AuiBlock.Text -> AuiText(block = block)
                is AuiBlock.Heading -> AuiHeading(block = block)
                is AuiBlock.Caption -> AuiCaption(block = block)
                is AuiBlock.ChipSelectSingle -> AuiChipSelectSingle(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.ChipSelectMulti -> AuiChipSelectMulti(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.ButtonPrimary -> AuiButtonPrimary(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.ButtonSecondary -> AuiButtonSecondary(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.QuickReplies -> AuiQuickReplies(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.InputRatingStars -> AuiInputRatingStars(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.InputTextSingle -> AuiInputTextSingle(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.InputSlider -> AuiInputSlider(block = block, onFeedback = wrappedOnFeedback)
                is AuiBlock.Divider -> AuiDivider()
                is AuiBlock.Spacer -> AuiSpacer()
                is AuiBlock.StepperHorizontal -> AuiStepperHorizontal(block = block)
                is AuiBlock.ProgressBar -> AuiProgressBar(block = block)
                is AuiBlock.BadgeSuccess -> AuiBadgeSuccess(block = block)
                is AuiBlock.StatusBannerSuccess -> AuiStatusBannerSuccess(block = block)
                is AuiBlock.Unknown -> Log.w(TAG, "Skipping unknown block type: ${block.type}")
            }
        }
    }
    }
}
