package com.bennyjon.aui.compose.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.StepperStep
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import kotlinx.coroutines.launch

/**
 * Renders a multi-page survey inside a single persistent [ModalBottomSheet].
 *
 * The library owns the entire survey shell: step navigation (Back / Next / Submit), the
 * stepper indicator, accumulation of answers across steps, and the final submission.
 * Step blocks should contain only collector components (inputs, chip selects, radio / checkbox
 * lists) — never navigation buttons. Any [AuiBlock.ButtonPrimary] the AI places inside a step
 * is still rendered, but passes its feedback straight through to the host without advancing
 * or finalizing the survey.
 *
 * The sheet stays open as the user moves between steps; it only closes on **Submit** (from
 * the last step) or when the user dismisses the sheet via swipe-down. Every step is
 * implicitly optional: users can tap **Next** without answering, and steps with no collected
 * answer are simply omitted from [AuiFeedback.entries].
 *
 * A single shared registry persists across all steps, so moving back to a prior step still
 * shows the user's earlier answers. On **Submit**, [AuiFeedback.entries] is built from every
 * step's inputs in declaration order, and [AuiFeedback.stepsSkipped] reports how many steps
 * produced no entry.
 *
 * **Dismiss behavior:** When the user dismisses the sheet via swipe-down, the composable emits
 * `onFeedback(action = "survey_dismissed", stepsTotal = N)`. [AuiFeedback.stepsSkipped] is
 * `null` on dismiss.
 *
 * **Structural completion signal:** Hosts that want to handle survey completions uniformly
 * should branch on [AuiFeedback.stepsTotal] `!= null` in their `onFeedback` callback — this
 * is the reliable structural signal that a feedback came from a finalized survey.
 *
 * The composable is inert once the survey has been submitted or dismissed. If the host app
 * does not clear its JSON after receiving [onFeedback], scrolling back to the message will
 * not re-open the sheet (provided the composable uses a stable key in [LazyColumn]).
 *
 * @param steps The list of survey steps to render, each containing its own blocks.
 * @param surveyTitle Optional title displayed at the top of the sheet.
 * @param pluginRegistry Registry of component plugins, passed through to [BlockRenderer]
 *   for rendering custom or overridden block types within each step.
 * @param onFeedback Called with the final consolidated [AuiFeedback] on submit or dismiss,
 *   and with any non-terminal feedback fired by blocks inside a step (e.g. an `open_url`
 *   button the AI added to a step). Action plugin routing is handled upstream by
 *   [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer].
 * @param onUnknownBlock If provided, called for each unrecognized block type that has no
 *   matching component plugin.
 * @see buildSurveyFormattedEntries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SurveyFlowDisplay(
    steps: List<AuiStep>,
    surveyTitle: String?,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
) {
    if (steps.isEmpty()) return

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by rememberSaveable { mutableStateOf(true) }
    val flowState = remember { SurveyFlowState(steps, pluginRegistry) }

    if (!showSheet) return

    ModalBottomSheet(
        onDismissRequest = {
            showSheet = false
            onFeedback(AuiFeedback(action = "survey_dismissed", stepsTotal = steps.size))
        },
        sheetState = sheetState,
    ) {
        SurveyContent(
            steps = steps,
            flowState = flowState,
            surveyTitle = surveyTitle,
            pluginRegistry = pluginRegistry,
            onBack = { flowState.back() },
            onNext = { flowState.next() },
            onSubmit = {
                val finalFeedback = flowState.finalize()
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                    onFeedback(finalFeedback)
                }
            },
            onStepFeedback = onFeedback,
            onUnknownBlock = onUnknownBlock,
        )
    }
}

// ── State holder ─────────────────────────────────────────────────────────────

/**
 * Mutable state holder for a multi-page survey.
 *
 * Tracks the current step index and holds the shared input registry used across every step,
 * so moving back to a prior step still renders the user's earlier answers. Call [finalize]
 * on Submit to produce the consolidated [AuiFeedback].
 */
@Stable
internal class SurveyFlowState(
    private val steps: List<AuiStep>,
    private val pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
) {

    var stepIndex by mutableIntStateOf(0)
        private set

    val registry: MutableState<Map<String, String>> = mutableStateOf(emptyMap())

    fun back() {
        if (stepIndex > 0) stepIndex--
    }

    fun next() {
        if (stepIndex < steps.lastIndex) stepIndex++
    }

    /**
     * Builds the terminal [AuiFeedback] for the survey. Scans every step's inputs against the
     * shared registry. Steps with no collected answer increment [AuiFeedback.stepsSkipped];
     * steps with answers contribute [AuiEntry] rows (one per input).
     */
    fun finalize(): AuiFeedback {
        val registryValue = registry.value
        val entries = mutableListOf<AuiEntry>()
        var skipped = 0
        for (step in steps) {
            val stepEntries = getAllStepEntries(step, registryValue, pluginRegistry)
            if (stepEntries.isEmpty()) skipped++
            entries.addAll(stepEntries)
        }
        val params = registryValue + mapOf(
            "steps_total" to steps.size.toString(),
            "steps_skipped" to skipped.toString(),
        )
        return AuiFeedback(
            action = "submit",
            params = params,
            entries = entries,
            formattedEntries = buildSurveyFormattedEntries(entries, skipped),
            stepsSkipped = skipped,
            stepsTotal = steps.size,
        )
    }
}

// ── Sheet body ───────────────────────────────────────────────────────────────

/**
 * The content rendered inside the [ModalBottomSheet]: title, stepper, the current step's
 * question and collector blocks, and the library-injected navigation row.
 */
@Composable
private fun SurveyContent(
    steps: List<AuiStep>,
    flowState: SurveyFlowState,
    surveyTitle: String?,
    pluginRegistry: AuiPluginRegistry,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onStepFeedback: (AuiFeedback) -> Unit,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)?,
) {
    val theme = LocalAuiTheme.current
    val step = steps[flowState.stepIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = theme.spacing.medium),
    ) {
        if (surveyTitle != null) {
            Text(
                text = surveyTitle,
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

        // key() forces a fresh BlockRenderer each time the step changes, while the shared
        // registry passed via registryOverride persists across steps.
        key(flowState.stepIndex) {
            BlockRenderer(
                blocks = step.blocks,
                pluginRegistry = pluginRegistry,
                registryOverride = flowState.registry,
                onFeedback = onStepFeedback,
                onUnknownBlock = onUnknownBlock,
            )
        }

        Spacer(modifier = Modifier.height(theme.spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (flowState.stepIndex > 0) {
                SurveyNavSecondaryButton(
                    label = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag(SurveyTestTags.BACK),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (flowState.stepIndex < steps.lastIndex) {
                SurveyNavPrimaryButton(
                    label = "Next",
                    onClick = onNext,
                    modifier = Modifier.testTag(SurveyTestTags.NEXT),
                )
            } else {
                SurveyNavPrimaryButton(
                    label = "Submit",
                    onClick = onSubmit,
                    modifier = Modifier.testTag(SurveyTestTags.SUBMIT),
                )
            }
        }

        Spacer(modifier = Modifier.height(theme.spacing.large))
    }
}

// ── Nav buttons ──────────────────────────────────────────────────────────────

/**
 * Themed primary button used for the library-injected Next / Submit controls.
 *
 * Deliberately sized to content (no `fillMaxWidth`) so the nav [Row] — which pushes Back
 * to the start and Next/Submit to the end via a weighted Spacer — lays out correctly.
 */
@Composable
private fun SurveyNavPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = theme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.colors.primary,
            contentColor = theme.colors.onPrimary,
        ),
    ) {
        Text(text = label, style = theme.typography.button)
    }
}

/**
 * Themed secondary button used for the library-injected Back control. Content-sized for
 * the same reason as [SurveyNavPrimaryButton].
 */
@Composable
private fun SurveyNavSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = theme.shapes.button,
        border = BorderStroke(width = 1.dp, color = theme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = theme.colors.primary,
        ),
    ) {
        Text(text = label, style = theme.typography.button)
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
 * the input label to preserve richer summary text.
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

/**
 * Builds the human-readable summary string for the feedback bubble after a survey completes.
 *
 * - All steps answered -> Q+A pairs joined by blank lines
 * - Some answered, some skipped -> Q+A pairs followed by "(N questions skipped)"
 * - All steps skipped -> "Survey skipped"
 * - Submitted with no input answered -> "Survey submitted"
 */
internal fun buildSurveyFormattedEntries(entries: List<AuiEntry>, skippedCount: Int): String {
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
