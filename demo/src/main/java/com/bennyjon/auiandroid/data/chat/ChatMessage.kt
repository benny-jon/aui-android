package com.bennyjon.auiandroid.data.chat

import com.bennyjon.aui.core.model.AuiResponse

/**
 * Flat chat message. Optional fields describe what the message carries.
 *
 * A single assistant message can have both [text] and [auiResponse]. [isAuiSpent]
 * is derived by the ViewModel — never stored in the database.
 *
 * The database stores only [rawContent] (the unprocessed LLM response string).
 * Derived fields ([text], [auiResponse], [rawAuiJson]) are produced at load time
 * by running extraction on [rawContent], so parsing logic can be updated and
 * replayed against the preserved original.
 *
 * @property id Unique identifier (UUID).
 * @property createdAt Epoch millis when the message was created.
 * @property role Whether this message is from the user or the assistant.
 * @property text User text or assistant plain-text reply (derived from [rawContent]).
 * @property auiResponse Parsed AUI response, if the assistant included one (derived from [rawContent]).
 * @property rawAuiJson Raw AUI JSON string from the assistant, if present (derived from [rawContent]).
 * @property rawContent The original unprocessed content string as stored in the database.
 * @property errorMessage Error description when the assistant call failed.
 * @property isAuiSpent True when this AUI response has been superseded by a newer one.
 */
data class ChatMessage(
    val id: String,
    val createdAt: Long,
    val role: Role,
    val text: String? = null,
    val auiResponse: AuiResponse? = null,
    val rawAuiJson: String? = null,
    val rawContent: String? = null,
    val errorMessage: String? = null,
    val isAuiSpent: Boolean = false,
) {
    /** The sender of a chat message. */
    enum class Role { USER, ASSISTANT }
}
