package com.bennyjon.auiandroid.data.chat

import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat message persistence and LLM interaction.
 *
 * Implementations handle message storage and LLM client orchestration.
 * This interface has zero AUI library imports — all AUI-specific logic
 * (parsing, spent-marking) lives in the ViewModel layer.
 */
interface ChatRepository {

    /** Observes all messages in [conversationId], ordered by creation time. */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /**
     * Sends a user message and retrieves the assistant response.
     *
     * [contextHints], if non-blank, is appended to the static system prompt for this single
     * call. Use it to inject per-turn context that varies during a conversation (e.g. the
     * current window size, layout mode) without re-building the whole prompt each turn.
     */
    suspend fun sendUserMessage(
        conversationId: String,
        text: String,
        contextHints: String = "",
    )

    /** Deletes all messages in [conversationId]. */
    suspend fun clearConversation(conversationId: String)
}
