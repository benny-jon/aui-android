package com.bennyjon.aui.core.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data for the `input_rating_stars` block. A 1–5 star rating input. */
@Serializable
data class InputRatingStarsData(
    /** Key used to identify this input in feedback params. */
    override val key: String,
    /** Optional label displayed above the stars. */
    override val label: String? = null,
    /** Pre-selected star rating (1–5), if any. */
    val value: Int? = null,
) : AuiInputData

/** Data for the `input_text_single` block. A single-line text input with an optional submit action. */
@Serializable
data class InputTextSingleData(
    /** Key used to identify this input in feedback params. */
    override val key: String,
    /** Label displayed above the text field. */
    override val label: String,
    /** Placeholder text shown when the field is empty. */
    val placeholder: String? = null,
    /** Label for the submit button. Defaults to "Submit" if absent. */
    @SerialName("submit_label") val submitLabel: String? = null,
) : AuiInputData

/** Data for the `input_slider` block. A range slider for scale questions. */
@Serializable
data class InputSliderData(
    /** Key used to identify this input in feedback params. */
    override val key: String,
    /** Label displayed above the slider. */
    override val label: String,
    /** Minimum slider value. */
    val min: Float,
    /** Maximum slider value. */
    val max: Float,
    /** Initial slider value. Defaults to [min] if absent. */
    val value: Float? = null,
    /** Step increment between values. Defaults to 1 if absent. */
    val step: Float? = null,
) : AuiInputData
