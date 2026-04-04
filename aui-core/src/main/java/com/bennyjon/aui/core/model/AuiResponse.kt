package com.bennyjon.aui.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The top-level AUI response produced by an AI assistant.
 *
 * Contains the presentation level and the ordered list of content blocks to render.
 * Parse from JSON using [com.bennyjon.aui.core.AuiParser].
 */
@Serializable
data class AuiResponse(
    /** How to present this response (inline, expanded, or sheet). */
    val display: AuiDisplay,

    /** Ordered list of content blocks to render. */
    val blocks: List<AuiBlock>,

    /** Title shown in the sheet header when [display] is [AuiDisplay.SHEET]. */
    @SerialName("sheet_title") val sheetTitle: String? = null,

    /**
     * Whether the sheet can be dismissed by swiping down.
     * Defaults to `true`. Only relevant when [display] is [AuiDisplay.SHEET].
     */
    @SerialName("sheet_dismissable") val sheetDismissable: Boolean = true,
)
