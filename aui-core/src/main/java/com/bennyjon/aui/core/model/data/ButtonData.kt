package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** Data for the `button_primary` block. Rendered as a filled CTA button. */
@Serializable
data class ButtonPrimaryData(
    /** Label displayed on the button. */
    val label: String,
    /** Optional icon name (Material icon identifier). */
    val icon: String? = null,
)

/** Data for the `button_secondary` block. Rendered as an outlined secondary action button. */
@Serializable
data class ButtonSecondaryData(
    /** Label displayed on the button. */
    val label: String,
    /** Optional icon name (Material icon identifier). */
    val icon: String? = null,
)
