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
    /** Rendered full-width in the chat feed, below the AI bubble. Best for rich content. */
    @SerialName("expanded") EXPANDED,

    /**
     * Rendered as a persistent bottom sheet that navigates through multiple steps without
     * closing between them. The library manages step advancement, accumulation, and the
     * stepper indicator automatically. Content is declared as a list of [AuiStep]s.
     */
    @SerialName("sheet") SHEET,
}
