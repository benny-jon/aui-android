package com.bennyjon.aui.compose.internal

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
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
import com.bennyjon.aui.core.model.AuiFeedback

private const val TAG = "BlockRenderer"

/**
 * Maps each [AuiBlock] to its composable via an exhaustive `when` expression.
 *
 * Unknown block types are skipped with a warning log — the renderer never crashes on
 * unrecognized input.
 */
@Composable
internal fun BlockRenderer(
    blocks: List<AuiBlock>,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is AuiBlock.Text -> AuiText(block = block)
                is AuiBlock.Heading -> AuiHeading(block = block)
                is AuiBlock.Caption -> AuiCaption(block = block)
                is AuiBlock.ChipSelectSingle -> AuiChipSelectSingle(block = block, onFeedback = onFeedback)
                is AuiBlock.ChipSelectMulti -> AuiChipSelectMulti(block = block, onFeedback = onFeedback)
                is AuiBlock.ButtonPrimary -> AuiButtonPrimary(block = block, onFeedback = onFeedback)
                is AuiBlock.ButtonSecondary -> AuiButtonSecondary(block = block, onFeedback = onFeedback)
                is AuiBlock.QuickReplies -> AuiQuickReplies(block = block, onFeedback = onFeedback)
                is AuiBlock.InputRatingStars -> AuiInputRatingStars(block = block, onFeedback = onFeedback)
                is AuiBlock.InputTextSingle -> AuiInputTextSingle(block = block, onFeedback = onFeedback)
                is AuiBlock.InputSlider -> AuiInputSlider(block = block, onFeedback = onFeedback)
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
