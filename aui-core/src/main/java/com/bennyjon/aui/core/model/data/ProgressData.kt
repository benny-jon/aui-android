package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** A single step in a [StepperHorizontalData]. */
@Serializable
data class StepperStep(val label: String)

/** Data for the `stepper_horizontal` block. Shows a horizontal step progress indicator. */
@Serializable
data class StepperHorizontalData(
    /** Ordered list of steps to display. */
    val steps: List<StepperStep>,
    /** Zero-based index of the currently active step. */
    val current: Int,
)

/** Data for the `progress_bar` block. A linear progress indicator with a label. */
@Serializable
data class ProgressBarData(
    /** Label displayed above or below the progress bar. */
    val label: String,
    /** Current progress value. */
    val progress: Float,
    /** Maximum progress value. Defaults to 100 if absent. */
    val max: Float? = null,
)
