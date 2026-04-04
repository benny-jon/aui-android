package com.bennyjon.aui.core.model

import kotlinx.serialization.Serializable

/**
 * Describes what happens when the user interacts with an [AuiBlock].
 *
 * When interaction occurs, the host app receives this object via the `onFeedback` callback.
 * The [label] (with any `{{placeholder}}` values resolved) is shown as a user message bubble.
 */
@Serializable
data class AuiFeedback(
    /** Machine-readable identifier of the action that occurred (e.g. `"poll_submit"`). */
    val action: String,

    /**
     * Structured data about the interaction (e.g. `{"poll_id": "exp_rating", "value": "4"}`).
     * Supports `{{value}}` and `{{label}}` placeholder resolution in [label].
     */
    val params: Map<String, String> = emptyMap(),

    /** Human-readable text shown as the user's next chat message. Supports `{{value}}` placeholders. */
    val label: String? = null,
)
