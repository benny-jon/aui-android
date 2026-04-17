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
    /** How to present this response (inline, expanded, or survey). */
    val display: AuiDisplay,

    /**
     * Ordered list of content blocks to render. Used for [AuiDisplay.INLINE] and
     * [AuiDisplay.EXPANDED]. Empty by default — [AuiDisplay.SURVEY] uses [steps] instead.
     */
    val blocks: List<AuiBlock> = emptyList(),

    /**
     * Ordered list of steps for [AuiDisplay.SURVEY]. The library renders each step inside
     * a persistent bottom sheet, navigating between them without closing.
     */
    val steps: List<AuiStep> = emptyList(),

    /** Title shown in the survey header when [display] is [AuiDisplay.SURVEY]. */
    @SerialName("survey_title") val surveyTitle: String? = null,

    /**
     * Short title for a host-rendered card stub when [display] is [AuiDisplay.EXPANDED].
     * Hosts that surface EXPANDED content through a tappable stub use this as the stub
     * heading. If null, hosts may fall back to the first `heading` or `text` block in
     * [blocks].
     */
    @SerialName("card_title") val cardTitle: String? = null,

    /**
     * Short description for a host-rendered card stub when [display] is [AuiDisplay.EXPANDED].
     * Hosts that surface EXPANDED content through a tappable stub use this as the stub
     * subtitle. If null, hosts may fall back to the first `text` block (after any heading)
     * in [blocks].
     */
    @SerialName("card_description") val cardDescription: String? = null,
)
