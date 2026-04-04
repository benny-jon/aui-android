package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * Describes the result of a user interaction with an [AuiBlock].
 *
 * Received by the host app via the `onFeedback` callback. For [AuiDisplay.SHEET]
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
     * For [AuiDisplay.SHEET] this is the merged params from all steps.
     */
    val params: Map<String, String> = emptyMap(),

    /**
     * Resolved answer text used internally by [AuiDisplay.SHEET] to populate [entries].
     * Supports `{{key}}` placeholder substitution from the value registry.
     */
    val label: String? = null,

    /**
     * Ordered list of question–answer pairs captured across all steps.
     * Populated by the library for [AuiDisplay.SHEET]; empty for all other display types.
     *
     * The host app can format these for the chat bubble:
     * ```kotlin
     * feedback.entries.joinToString("\n\n") { "${it.question}\n${it.answer}" }
     * ```
     */
    val entries: List<AuiEntry> = emptyList(),
)
