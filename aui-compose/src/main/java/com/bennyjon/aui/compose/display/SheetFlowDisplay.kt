package com.bennyjon.aui.compose.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bennyjon.aui.compose.components.input.AuiButtonSecondary
import com.bennyjon.aui.compose.components.layout.AuiStepperHorizontal
import com.bennyjon.aui.compose.internal.BlockRenderer
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.StepperStep
import kotlinx.coroutines.launch

/**
 * Renders a multi-step survey inside a single persistent [ModalBottomSheet].
 *
 * The sheet stays open as the user moves between steps; it only closes when the final step
 * is submitted or skipped. The library handles:
 * - Step navigation (advancing on [AuiBlock.ButtonPrimary] feedback)
 * - Automatic stepper indicator (shown when there are more than one step)
 * - Optional Skip button (shown when [AuiStep.skippable] is true)
 * - Accumulation of [AuiEntry] Q+A pairs across steps
 * - A single consolidated [AuiFeedback] emitted to [onFeedback] at the end, with
 *   merged [AuiFeedback.params] and the full [AuiFeedback.entries] list
 *
 * **Skip tracking:** Skipped steps are counted separately (`skippedCount`). The internal
 * [buildSheetFormattedEntries] function produces the display string with fallbacks:
 * "Survey skipped" (all skipped), "Survey submitted" (no input answered),
 * or partial Q&A followed by "(N questions skipped)".
 *
 * **Dismiss behavior:** When the user dismisses the sheet via swipe-down (`onDismissRequest`),
 * the composable emits `onFeedback(action = "sheet_dismissed", stepsTotal = N)`.
 * [AuiFeedback.stepsSkipped] is `null` on dismiss because skip buttons were not used.
 *
 * The composable is inert once the sheet has been completed or dismissed. If the host app
 * does not clear its JSON after receiving [onFeedback], scrolling back to the message will not
 * reopen the sheet (provided the composable uses a stable key in [LazyColumn]).
 *
 * @see buildSheetFormattedEntries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetFlowDisplay(
    steps: List<AuiStep>,
    sheetTitle: String?,
    onFeedback: (AuiFeedback) -> Unit,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    if (steps.isEmpty()) return

    val theme = LocalAuiTheme.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var stepIndex by remember { mutableIntStateOf(0) }
    var skippedCount by remember { mutableIntStateOf(0) }
    val accumulatedParams = remember { mutableMapOf<String, String>() }
    val accumulatedEntries = remember { mutableListOf<AuiEntry>() }
    var showSheet by rememberSaveable { mutableStateOf(true) }

    fun finalize(terminalFeedback: AuiFeedback) {
        val entries = accumulatedEntries.toList()
        val formattedEntries = buildSheetFormattedEntries(entries, skippedCount)
        val additionalParams = mapOf(
            "steps_total" to steps.size.toString(),
            "steps_skipped" to skippedCount.toString(),
        )
        val finalFeedback = terminalFeedback.copy(
            params = accumulatedParams.toMap() + additionalParams + terminalFeedback.params,
            entries = entries,
            formattedEntries = formattedEntries,
            stepsSkipped = skippedCount,
            stepsTotal = steps.size,
        )
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showSheet = false
            onFeedback(finalFeedback)
        }
    }

    fun advance(feedback: AuiFeedback, isSkip: Boolean) {
        val step = steps[stepIndex]
        if (isSkip) {
            skippedCount++
        } else {
            accumulatedParams.putAll(feedback.params)
            val question = step.question
            val answer = getStepAnswer(step, feedback.params)
            if (question != null && answer != null) {
                accumulatedEntries.add(AuiEntry(question = question, answer = answer))
            }
        }
        if (stepIndex == steps.lastIndex) {
            finalize(feedback)
        } else {
            stepIndex++
        }
    }

    if (!showSheet) return

    ModalBottomSheet(
        onDismissRequest = {
            showSheet = false
            onFeedback(AuiFeedback(action = "sheet_dismissed", stepsTotal = steps.size))
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = theme.spacing.medium),
        ) {
            if (sheetTitle != null) {
                Text(
                    text = sheetTitle,
                    style = theme.typography.heading.copy(fontWeight = FontWeight.SemiBold),
                    color = theme.colors.onSurface,
                )
                Spacer(modifier = Modifier.height(theme.spacing.medium))
            }

            if (steps.size > 1) {
                AuiStepperHorizontal(
                    block = AuiBlock.StepperHorizontal(
                        data = StepperHorizontalData(
                            steps = steps.mapIndexed { i, s ->
                                StepperStep(label = s.label ?: (i + 1).toString())
                            },
                            current = stepIndex,
                        ),
                    ),
                )
                Spacer(modifier = Modifier.height(theme.spacing.medium))
            }

            val step = steps[stepIndex]

            // key() forces a fresh BlockRenderer (and registry) each time the step changes.
            key(stepIndex) {
                BlockRenderer(
                    blocks = step.blocks,
                    onFeedback = { feedback -> advance(feedback, isSkip = false) },
                    onUnknownBlock = onUnknownBlock,
                )
            }

            if (step.skippable) {
                val skipAction = step.blocks
                    .filterIsInstance<AuiBlock.ButtonPrimary>()
                    .firstOrNull()?.feedback?.action ?: "step_skipped"
                Spacer(modifier = Modifier.height(theme.spacing.small))
                AuiButtonSecondary(
                    block = AuiBlock.ButtonSecondary(
                        data = ButtonSecondaryData(label = "Skip"),
                        feedback = AuiFeedback(action = skipAction),
                    ),
                    onFeedback = { feedback -> advance(feedback, isSkip = true) },
                )
            }

            Spacer(modifier = Modifier.height(theme.spacing.large))
        }
    }
}

/**
 * Finds the first input block in [step] and returns its human-readable answer from [params].
 * The registry stores display labels (e.g. "😊 Great") under the input's key, so [params]
 * already contains the correct display value after the button merges the registry into params.
 */
internal fun getStepAnswer(step: AuiStep, params: Map<String, String>): String? {
    return step.blocks.firstNotNullOfOrNull { block ->
        when (block) {
            is AuiBlock.ChipSelectSingle -> params[block.data.key]
            is AuiBlock.ChipSelectMulti -> params[block.data.key]
            is AuiBlock.InputSlider -> params[block.data.key]
            is AuiBlock.InputRatingStars -> params[block.data.key]
            is AuiBlock.InputTextSingle -> params[block.data.key]
            is AuiBlock.RadioList -> params[block.data.key]
            is AuiBlock.CheckboxList -> params[block.data.key]
            else -> null
        }
    }?.takeIf { it.isNotBlank() }
}

/**
 * Builds the human-readable summary string for the feedback bubble after a sheet survey completes.
 *
 * - All steps answered → Q+A pairs joined by blank lines
 * - Some answered, some skipped → Q+A pairs followed by "(N questions skipped)"
 * - All steps skipped → "Survey skipped"
 * - Submitted with no input answered → "Survey submitted"
 */
internal fun buildSheetFormattedEntries(entries: List<AuiEntry>, skippedCount: Int): String {
    return when {
        entries.isEmpty() && skippedCount > 0 -> "Survey skipped"
        entries.isEmpty() -> "Survey submitted"
        skippedCount > 0 -> {
            val answered = entries.joinToString("\n\n") { "${it.question}\n${it.answer}" }
            val note = if (skippedCount == 1) "(1 question skipped)" else "($skippedCount questions skipped)"
            "$answered\n\n$note"
        }
        else -> entries.joinToString("\n\n") { "${it.question}\n${it.answer}" }
    }
}
