package com.bennyjon.auiandroid.showcase

/**
 * A single entry in the All Blocks showcase.
 *
 * @param label The component name or combo title (e.g. "text", "Combo: Product Recommendations").
 * @param description A short one-liner about what the entry demonstrates.
 * @param auiJson The raw AUI JSON payload string (just the `aui` object, not the full envelope).
 * @param isSurvey Whether this entry uses the "survey" display mode.
 */
data class ShowcaseEntry(
    val label: String,
    val description: String?,
    val auiJson: String,
    val isSurvey: Boolean,
)
