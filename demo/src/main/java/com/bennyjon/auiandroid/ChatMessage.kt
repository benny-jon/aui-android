package com.bennyjon.auiandroid

import com.bennyjon.aui.core.model.AuiResponse

/** A single entry in the chat conversation. */
sealed class ChatMessage {
    /** A text message sent by the user. */
    data class UserText(val text: String) : ChatMessage()

    /** A feedback action taken by the user (e.g., submitting a poll answer). */
    data class UserFeedback(val label: String) : ChatMessage()

    /** An AI response rendered via [com.bennyjon.aui.compose.AuiRenderer]. */
    data class AiResponse(val response: AuiResponse) : ChatMessage()
}
