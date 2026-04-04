package com.bennyjon.aui.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The presentation level for an [AuiResponse].
 *
 * Controls how the renderer surfaces the content to the user.
 */
@Serializable
enum class AuiDisplay {
    /** Rendered inside the AI chat bubble. Best for quick, conversational responses. */
    @SerialName("inline") INLINE,

    /** Rendered full-width in the chat feed, below the AI bubble. Best for rich content. */
    @SerialName("expanded") EXPANDED,

    /** Rendered as a bottom sheet overlay on top of the chat. Best for focused interactions. */
    @SerialName("sheet") SHEET,
}
