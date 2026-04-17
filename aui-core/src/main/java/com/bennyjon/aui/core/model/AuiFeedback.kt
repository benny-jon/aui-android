package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * Describes the result of a user interaction with an [AuiBlock].
 *
 * Received by the host app via the `onFeedback` callback. For [AuiDisplay.SURVEY]
 * responses the library accumulates interactions across all steps and emits a single
 * consolidated [AuiFeedback] at the end — [entries] contains the full list of
 * question–answer pairs for display, and [params] contains all collected key-value data.
 */
@Serializable
data class AuiFeedback(
    /** Machine-readable identifier of the action (e.g. `"poll_submit"`, `"poll_complete"`). */
    val action: String,

    /**
     * Structured key-value data about the interaction.
     * For [AuiDisplay.SURVEY] this is the merged params from all steps.
     */
    val params: Map<String, String> = emptyMap(),

    /**
     * Library-computed summary of all question–answer pairs, separated by blank lines.
     *
     * Ready to send directly to the AI as the user's reply, e.g.:
     * ```
     * How was your experience?
     * 😊 Great
     *
     * What would you like improved?
     * Speed, Design
     * ```
     *
     * `null` when no question–answer pairs were collected (e.g. a simple button tap with no inputs).
     */
    val formattedEntries: String? = null,

    /**
     * Ordered list of question–answer pairs captured from user input.
     *
     * For [AuiDisplay.SURVEY], one entry per step that had both a question and a non-empty answer.
     * For [AuiDisplay.EXPANDED], one entry per heading→input pair.
     *
     * Use this when you want to build a custom summary instead of using [formattedEntries].
     */
    val entries: List<AuiEntry> = emptyList(),

    /**
     * Number of steps the user left unanswered in a [AuiDisplay.SURVEY] interaction.
     * `null` for non-survey interactions.
     */
    val stepsSkipped: Int? = null,

    /**
     * Total number of steps in a [AuiDisplay.SURVEY] interaction.
     * `null` for non-survey interactions.
     */
    val stepsTotal: Int? = null,
)
