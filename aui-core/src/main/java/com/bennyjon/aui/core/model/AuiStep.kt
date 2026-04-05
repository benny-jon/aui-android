package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * A single step within a [AuiDisplay.SHEET] response.
 *
 * The library renders each step inside a persistent bottom sheet, automatically advancing
 * through steps as the user interacts. Step blocks should contain the input components and
 * a single `button_primary` as the submission trigger; navigation and accumulation are handled
 * by the library.
 *
 * @param blocks The content blocks to render for this step. Include a `button_primary` as
 *   the action trigger. The library derives the answer for this step from the first input
 *   block's value in [AuiFeedback.params] — no label field is needed.
 * @param label Short label shown in the auto-rendered stepper indicator (e.g. "Experience").
 *   Defaults to the step number if absent.
 * @param question Full question text stored in [AuiEntry.question] when the user answers
 *   this step. If null no entry is recorded for this step.
 * @param skippable When true the library renders a "Skip" button beneath the step content.
 */
@Serializable
data class AuiStep(
    val blocks: List<AuiBlock>,
    val label: String? = null,
    val question: String? = null,
    val skippable: Boolean = false,
)
