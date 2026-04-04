package com.bennyjon.aui.core.model.data

import kotlinx.serialization.Serializable

/** Data for the `text` block. Supports basic markdown. */
@Serializable
data class TextData(val text: String)

/** Data for the `heading` block. Rendered as a bold section title. */
@Serializable
data class HeadingData(val text: String)

/** Data for the `caption` block. Rendered as small, muted metadata text. */
@Serializable
data class CaptionData(val text: String)
