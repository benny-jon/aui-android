package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiFeedback

/**
 * Accumulates [AuiFeedback] params and labels across multi-step flows and emits a single
 * consolidated callback when the terminal step is reached.
 *
 * Usage — instantiate once (e.g. in a ViewModel) and pass [process] as the `onFeedback`
 * lambda to `AuiRenderer`:
 *
 * ```kotlin
 * private val accumulator = AuiFeedbackAccumulator(
 *     onStep = { loadNextStep() },
 *     onComplete = { feedback -> submitToAi(feedback) },
 * )
 *
 * fun onFeedback(feedback: AuiFeedback) = accumulator.process(feedback)
 * ```
 *
 * @param onStep Called for every non-terminal feedback (i.e. `terminal = false`).
 *   Use this to advance the UI to the next step. The feedback passed here contains
 *   only the params declared on that step — not the accumulated total.
 * @param onComplete Called once when a terminal feedback arrives (`terminal = true`).
 *   The feedback passed here has:
 *   - [AuiFeedback.params] merged from all steps (prior + terminal)
 *   - [AuiFeedback.label] built by joining the non-blank resolved labels from every step
 *     (including the terminal step) with newlines, so the host receives a full summary.
 */
class AuiFeedbackAccumulator(
    private val onStep: (AuiFeedback) -> Unit = {},
    private val onComplete: (AuiFeedback) -> Unit,
) {
    private val accumulatedParams = mutableMapOf<String, String>()
    private val accumulatedLabels = mutableListOf<String>()

    /**
     * Routes [feedback] to [onStep] or [onComplete] based on [AuiFeedback.terminal].
     *
     * Non-terminal steps contribute their params and resolved label to the running totals.
     * The terminal step triggers [onComplete] with everything merged.
     *
     * Pass this as the `onFeedback` lambda to `AuiRenderer`.
     */
    fun process(feedback: AuiFeedback) {
        accumulatedParams.putAll(feedback.params)
        feedback.label?.takeIf { it.isNotBlank() }?.let { accumulatedLabels.add(it) }

        if (!feedback.terminal) {
            onStep(feedback)
        } else {
            val mergedLabel = accumulatedLabels
                .joinToString("\n\n")
                .takeIf { it.isNotBlank() }
            val merged = feedback.copy(
                params = accumulatedParams.toMap(),
                label = mergedLabel,
            )
            accumulatedParams.clear()
            accumulatedLabels.clear()
            onComplete(merged)
        }
    }

    /** Clears any accumulated state without emitting. Useful when a flow is cancelled. */
    fun reset() {
        accumulatedParams.clear()
        accumulatedLabels.clear()
    }
}
