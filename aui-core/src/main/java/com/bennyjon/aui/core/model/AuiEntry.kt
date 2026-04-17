package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * A single question–answer pair captured during a [AuiDisplay.SURVEY] interaction.
 *
 * The library builds and populates [AuiFeedback.entries] from these as the user moves through
 * each step. The host app can format [entries] into a summary string for display or pass them
 * to the AI.
 */
@Serializable
data class AuiEntry(
    /** The question text declared on the step (e.g. "How was your experience?"). */
    val question: String,

    /** The resolved answer text (e.g. "😊 Great", "Speed, Design"). */
    val answer: String,
)
