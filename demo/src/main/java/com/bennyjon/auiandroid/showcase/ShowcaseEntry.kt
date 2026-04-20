package com.bennyjon.auiandroid.showcase

import com.bennyjon.aui.core.model.AuiResponse

/**
 * A single entry in the All Blocks showcase.
 *
 * @param label The component name or combo title (e.g. "text", "Combo: Product Recommendations").
 * @param description A short one-liner about what the entry demonstrates.
 * @param response The parsed AUI payload. Drives both rendering and stub-card resolution.
 */
data class ShowcaseEntry(
    val category: String,
    val label: String,
    val description: String?,
    val response: AuiResponse,
    val sourceJson: String,
)
