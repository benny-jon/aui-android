package com.bennyjon.auiandroid.data.chat.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single chat message.
 *
 * Stores the raw LLM response content as-is in [rawContent], deferring any
 * parsing (text/AUI extraction) to the repository layer on load. This preserves
 * the original response for replay and debugging. A user row has [rawContent]
 * set to the user's input text; an assistant row has [rawContent] set to the
 * full LLM response string. [errorMessage] is set only on failure.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val rawContent: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
)
