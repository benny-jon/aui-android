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
import androidx.compose.runtime.Stable
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
import com.bennyjon.aui.compose.plugin.componentPlugin
import com.bennyjon.aui.compose.theme.LocalAuiBodyColor
import com.bennyjon.aui.compose.theme.LocalAuiHeadingColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiInputBlock
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.StepperStep
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
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
 * **Skip tracking:** Skipped steps are counted separately. The internal
 * [buildSheetFormattedEntries] function produces the display string with fallbacks:
 * "Survey skipped" (all skipped), "Survey submitted" (no input answered),
 * or partial Q&A followed by "(N questions skipped)".
 *
 * **Dismiss behavior:** When the user dismisses the sheet via swipe-down (`onDismissRequest`),
 * the composable emits `onFeedback(action = "sheet_dismissed", stepsTotal = N)`.
 * [AuiFeedback.stepsSkipped] is `null` on dismiss because skip buttons were not used.
 *
 * **Terminal action gating:** Only actions in the terminal set (`submit`, `poll_complete`,
 * `poll_submit`) advance the step — or finalize the sheet on the last step. As a fallback, if
 * a step has at most one [AuiBlock.ButtonPrimary], any click on it is also treated as terminal
 * (covers AI-hallucinated action names). Non-terminal actions (`open_url`, `navigate`, etc.)
 * are passed through to [onFeedback] immediately without advancing or dismissing.
 *
 * **Multi-input steps:** When a step contains more than one input block, entries are created for
 * each input using its `label` (or `key` as fallback) as the question. Single-input steps
 * still use the step-level [AuiStep.question] for backward compatibility.
 *
 * **Structural completion signal:** Hosts that want to handle sheet flow completions uniformly
 * (regardless of what action name the AI chose) should branch on [AuiFeedback.stepsTotal]
 * `!= null` in their `onFeedback` callback. This is the reliable structural signal that a
 * feedback came from a finalized sheet flow. Action-name dispatch via `AuiActionPlugin` is best
 * suited to single-component interactions, not multi-step flows.
 *
 * The composable is inert once the sheet has been completed or dismissed. If the host app
 * does not clear its JSON after receiving [onFeedback], scrolling back to the message will not
 * reopen the sheet (provided the composable uses a stable key in [LazyColumn]).
 *
 * @param steps The list of survey steps to render, each containing its own blocks.
 * @param sheetTitle Optional title displayed at the top of the sheet.
 * @param pluginRegistry Registry of component plugins, passed through to [BlockRenderer]
 *   for rendering custom or overridden block types within each step.
 * @param onFeedback Called once when the sheet flow completes (submit or dismiss) with a
 *   consolidated [AuiFeedback]. Action plugin routing is handled upstream by
 *   [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer].
 * @param onUnknownBlock If provided, called for each unrecognized block type that has no
 *   matching component plugin.
 * @see buildSheetFormattedEntries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetFlowDisplay(
    steps: List<AuiStep>,
    sheetTitle: String?,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    if (steps.isEmpty()) return

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by rememberSaveable { mutableStateOf(true) }
    val flowState = remember { SheetFlowState(steps, pluginRegistry) }

    if (!showSheet) return

    ModalBottomSheet(
        onDismissRequest = {
            showSheet = false
            onFeedback(AuiFeedback(action = "sheet_dismissed", stepsTotal = steps.size))
        },
        sheetState = sheetState,
    ) {
        SheetFlowContent(
            steps = steps,
            flowState = flowState,
            sheetTitle = sheetTitle,
            pluginRegistry = pluginRegistry,
            onStepCompleted = { feedback, isSkip ->
                val step = steps[flowState.stepIndex]
                if (!isSkip && !isTerminalSheetAction(feedback.action, step)) {
                    // Non-terminal action (open_url, navigate, etc.): pass through
                    // without advancing or dismissing.
                    onFeedback(feedback)
                } else {
                    val finalFeedback = flowState.advance(feedback, isSkip)
                    if (finalFeedback != null) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            onFeedback(finalFeedback)
                        }
                    }
                }
            },
            onUnknownBlock = onUnknownBlock,
        )
    }
}

// ── State holder ─────────────────────────────────────────────────────────────

/**
 * Mutable state holder for a multi-step sheet flow.
 *
 * Tracks the current step index, accumulated Q+A entries, merged params, and skip count.
 * Call [advance] after each step to record the answer (or skip) and move forward.
 * On the last step, [advance] returns the consolidated [AuiFeedback]; otherwise it returns `null`.
 */
@Stable
internal class SheetFlowState(
    private val steps: List<AuiStep>,
    private val pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
) {

    var stepIndex by mutableIntStateOf(0)
        private set

    private var skippedCount = 0
    private val accumulatedParams = mutableMapOf<String, String>()
    private val accumulatedEntries = mutableListOf<AuiEntry>()

    /**
     * Records the current step's answer (or skip) and advances to the next step.
     *
     * @return The consolidated [AuiFeedback] if the flow is now complete, or `null` to continue.
     */
    fun advance(feedback: AuiFeedback, isSkip: Boolean): AuiFeedback? {
        val step = steps[stepIndex]
        if (isSkip) {
            skippedCount++
        } else {
            accumulatedParams.putAll(feedback.params)
            accumulatedEntries.addAll(getAllStepEntries(step, feedback.params, pluginRegistry))
        }
        return if (stepIndex == steps.lastIndex) {
            buildFinalFeedback(feedback)
        } else {
            stepIndex++
            null
        }
    }

    private fun buildFinalFeedback(terminalFeedback: AuiFeedback): AuiFeedback {
        val entries = accumulatedEntries.toList()
        val additionalParams = mapOf(
            "steps_total" to steps.size.toString(),
            "steps_skipped" to skippedCount.toString(),
        )
        return terminalFeedback.copy(
            params = accumulatedParams.toMap() + additionalParams + terminalFeedback.params,
            entries = entries,
            formattedEntries = buildSheetFormattedEntries(entries, skippedCount),
            stepsSkipped = skippedCount,
            stepsTotal = steps.size,
        )
    }
}

// ── Sheet body ───────────────────────────────────────────────────────────────

/**
 * The content rendered inside the [ModalBottomSheet]: title, stepper, current step blocks,
 * and optional skip button.
 */
@Composable
private fun SheetFlowContent(
    steps: List<AuiStep>,
    flowState: SheetFlowState,
    sheetTitle: String?,
    pluginRegistry: AuiPluginRegistry,
    onStepCompleted: (feedback: AuiFeedback, isSkip: Boolean) -> Unit,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)?,
) {
    val theme = LocalAuiTheme.current
    val step = steps[flowState.stepIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = theme.spacing.medium),
    ) {
        if (sheetTitle != null) {
            Text(
                text = sheetTitle,
                style = theme.typography.heading.copy(fontWeight = FontWeight.SemiBold),
                color = LocalAuiHeadingColor.current,
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
                        current = flowState.stepIndex,
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(theme.spacing.medium))
        }

        val question = step.question
        if (question != null) {
            Text(
                text = question,
                style = theme.typography.subheading,
                color = LocalAuiBodyColor.current,
            )
            Spacer(modifier = Modifier.height(theme.spacing.small))
        }

        // key() forces a fresh BlockRenderer (and registry) each time the step changes.
        key(flowState.stepIndex) {
            BlockRenderer(
                blocks = step.blocks,
                pluginRegistry = pluginRegistry,
                onFeedback = { feedback -> onStepCompleted(feedback, false) },
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
                onFeedback = { feedback -> onStepCompleted(feedback, true) },
            )
        }

        Spacer(modifier = Modifier.height(theme.spacing.large))
    }
}

// ── Helper functions ─────────────────────────────────────────────────────────

/**
 * Extracts [AuiEntry] instances for **all** input blocks in [step] that have a non-blank
 * answer in [params].
 *
 * Both built-in input blocks ([AuiInputBlock]) and plugin-provided inputs
 * ([AuiComponentPlugin.inputKey][com.bennyjon.aui.compose.plugin.AuiComponentPlugin.inputKey])
 * are recognized. Each input's `label` (falling back to its `key`) becomes the entry's
 * question. For single-input steps, the step-level [AuiStep.question] is preferred over
 * the input label to preserve backward-compatible summary text.
 */
internal fun getAllStepEntries(
    step: AuiStep,
    params: Map<String, String>,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
): List<AuiEntry> {
    val entries = mutableListOf<AuiEntry>()
    for (block in step.blocks) {
        val (key, label) = block.inputKeyAndLabel(pluginRegistry) ?: continue
        val answer = params[key]?.takeIf { it.isNotBlank() } ?: continue
        entries.add(AuiEntry(question = label ?: key, answer = answer))
    }
    val stepQuestion = step.question
    if (entries.size == 1 && stepQuestion != null) {
        return listOf(entries[0].copy(question = stepQuestion))
    }
    return entries
}

/**
 * Returns the registry key and human-readable label for this block if it is an input,
 * or `null` otherwise.
 *
 * Checks [AuiInputBlock] for built-ins and falls back to
 * [AuiComponentPlugin][com.bennyjon.aui.compose.plugin.AuiComponentPlugin] properties
 * for [AuiBlock.Unknown] blocks backed by a plugin.
 */
private fun AuiBlock.inputKeyAndLabel(
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
): Pair<String, String?>? = when {
    this is AuiInputBlock -> inputData.key to inputData.label
    this is AuiBlock.Unknown -> {
        val plugin = pluginRegistry.componentPlugin(type)
        plugin?.inputKey?.let { key -> key to plugin.inputLabel }
    }
    else -> null
}

/** Actions that signal step completion / sheet finalization. */
private val TERMINAL_SHEET_ACTIONS = setOf("submit", "complete", "poll_complete", "poll_submit")

/**
 * Returns `true` when [action] should advance (or finalize) the sheet flow.
 *
 * An action is terminal if it is in [TERMINAL_SHEET_ACTIONS], **or** the step has at most
 * one [AuiBlock.ButtonPrimary] whose action matches — this fallback covers AI-generated
 * action names that don't follow the convention but are clearly the step's only submit path.
 */
internal fun isTerminalSheetAction(action: String, step: AuiStep): Boolean {
    if (action in TERMINAL_SHEET_ACTIONS) return true
    val primaryButtons = step.blocks.filterIsInstance<AuiBlock.ButtonPrimary>()
    return primaryButtons.size <= 1 && primaryButtons.any { it.feedback?.action == action }
}

/**
 * Builds the human-readable summary string for the feedback bubble after a sheet survey completes.
 *
 * - All steps answered -> Q+A pairs joined by blank lines
 * - Some answered, some skipped -> Q+A pairs followed by "(N questions skipped)"
 * - All steps skipped -> "Survey skipped"
 * - Submitted with no input answered -> "Survey submitted"
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
