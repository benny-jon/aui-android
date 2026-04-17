package com.bennyjon.aui.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The presentation level for an [AuiResponse].
 *
 * Describes the AI's intent for *where* the response belongs in the conversation. The library
 * itself renders [INLINE] and [EXPANDED] identically — the host app decides whether to surface
 * an [EXPANDED] response in a separate detail surface (a sheet on narrow windows, a side
 * detail pane on wider windows) based on its own layout.
 */
@Serializable
enum class AuiDisplay {
    /**
     * Belongs in the chat flow. Best for quick replies, short confirmations, small polls,
     * and other "keep the conversation moving" content. Hosts always render inline regardless
     * of screen size.
     */
    @SerialName("inline") INLINE,

    /**
     * Focused / detail content the user may want to study. Best for rich cards, long lists,
     * comparisons, and multi-block content. Hosts may surface this through a tappable card
     * stub in chat that opens a bottom sheet (narrow windows) or shows the full render in a
     * side detail pane (wide windows). The library itself renders [EXPANDED] identically to
     * [INLINE]; routing is the host's responsibility.
     */
    @SerialName("expanded") EXPANDED,

    /**
     * Multi-page structured input. Rendered as a persistent bottom sheet that navigates
     * between questions without closing. The library handles navigation (Back/Next/Submit),
     * the step indicator, answer accumulation, and the final submission. Content is declared
     * as a list of [AuiStep]s — one per question.
     */
    @SerialName("survey") SURVEY,
}
