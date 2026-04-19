package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * A single step within a [AuiDisplay.SURVEY] response.
 *
 * The library renders each step as flat content and injects all navigation controls
 * (Back / Next on intermediate steps, Submit on the final step). Hosts choose the surrounding
 * container — sheet, dialog, pane, or inline content. The AI only declares the [question] and
 * the collector [blocks] for that question — never navigation buttons, submit actions, or
 * skip controls.
 *
 * Every step is implicitly optional: users can advance past any step without answering, and
 * steps without a collected answer are simply omitted from [AuiFeedback.entries].
 *
 * @param blocks The collector component(s) for this question. Should contain input
 *   components only (radio_list, checkbox_list, chip_select_*, input_*). The library
 *   provides its own Back / Next / Submit buttons — do not add button_primary blocks here.
 * @param question Full question text shown at the top of the step and used as the question
 *   text for any entry captured from a single-input step. If null the library falls back to
 *   the input's `label` or `key`.
 * @param label Optional short label shown inside the stepper indicator (e.g. `"Rating"`,
 *   `"Details"`). Purely cosmetic — does not affect entries or feedback. When null the
 *   stepper falls back to the step number.
 */
@Serializable
data class AuiStep(
    val blocks: List<AuiBlock>,
    val question: String? = null,
    val label: String? = null,
)
