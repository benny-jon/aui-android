package com.bennyjon.auiandroid.data.chat.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single chat message.
 *
 * Self-describing via nullable columns — no discriminator needed. A user row has
 * [text] set; an assistant row may have any combination of [text], [auiJson], and
 * [errorMessage].
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val text: String? = null,
    val auiJson: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long,
)
