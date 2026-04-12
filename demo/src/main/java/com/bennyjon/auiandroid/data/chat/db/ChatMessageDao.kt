package com.bennyjon.auiandroid.data.chat.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data-access object for [ChatMessageEntity]. */
@Dao
interface ChatMessageDao {

    /** Observes all messages in [conversationId] ordered by creation time. */
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    /** Returns all messages in [conversationId] ordered by creation time (one-shot). */
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    /** Inserts a single message row. */
    @Insert
    suspend fun insert(entity: ChatMessageEntity)

    /** Deletes all messages in [conversationId]. */
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)
}
