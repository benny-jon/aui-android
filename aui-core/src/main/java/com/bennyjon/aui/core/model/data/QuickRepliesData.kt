package com.bennyjon.aui.core.model.data

import com.bennyjon.aui.core.model.AuiFeedback
import kotlinx.serialization.Serializable

/**
 * A single option in a `quick_replies` block.
 *
 * If [feedback] is omitted, tapping the option sends [label] as a plain-text user message.
 */
@Serializable
data class QuickReplyOption(
    /** Display label shown on the reply chip. */
    val label: String,
    /** Optional feedback triggered when this option is tapped. */
    val feedback: AuiFeedback? = null,
)

/** Data for the `quick_replies` block. A horizontal row of tappable suggestion chips. */
@Serializable
data class QuickRepliesData(
    /** The available reply options. */
    val options: List<QuickReplyOption>,
)
