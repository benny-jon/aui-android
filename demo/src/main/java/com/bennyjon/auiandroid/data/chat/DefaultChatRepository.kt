package com.bennyjon.auiandroid.data.chat

import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.chat.db.ChatMessageEntity
import com.bennyjon.auiandroid.data.llm.AuiResponseExtractor
import com.bennyjon.auiandroid.data.llm.LlmClient
import com.bennyjon.auiandroid.data.llm.LlmMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * [ChatRepository] backed by Room and an [LlmClient].
 *
 * Mints UUIDs for every message row. Sends user text to the LLM, then stores
 * the raw response content as-is. Parsing into text/AUI is deferred to
 * [toDomain] when loading from the database, so the original response is
 * always preserved for replay and debugging.
 *
 * @param llmClient The LLM provider to call for completions.
 * @param dao Room DAO for chat message persistence.
 * @param systemPrompt System prompt sent to the LLM on every request.
 * @param ioDispatcher Dispatcher for IO-bound work (default [Dispatchers.IO]).
 */
class DefaultChatRepository(
    private val llmClient: LlmClient,
    private val dao: ChatMessageDao,
    private val systemPrompt: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatRepository {

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun sendUserMessage(conversationId: String, text: String) {
        withContext(ioDispatcher) {
            // 1. Insert user row (rawContent = user's input text)
            val userEntity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = ROLE_USER,
                rawContent = text,
                createdAt = System.currentTimeMillis(),
            )
            dao.insert(userEntity)

            // 2. Build history from all rows
            val allEntities = dao.getMessages(conversationId)
            val history = allEntities.map { it.toLlmMessage() }

            // 3. Call LLM
            val result = llmClient.complete(systemPrompt, history)

            // 4. Insert assistant row (rawContent = unprocessed LLM response)
            val assistantEntity = ChatMessageEntity(
                id = result.messageId ?: UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = ROLE_ASSISTANT,
                rawContent = result.rawContent,
                errorMessage = result.errorMessage,
                createdAt = System.currentTimeMillis(),
            )
            dao.insert(assistantEntity)
        }
    }

    override suspend fun clearConversation(conversationId: String) {
        withContext(ioDispatcher) {
            dao.clearConversation(conversationId)
        }
    }

    /**
     * Maps an entity to the domain model. For user messages, [rawContent] is
     * the input text. For assistant messages, [AuiResponseExtractor] parses
     * [rawContent] into text, AUI JSON, and parsed [AuiResponse].
     */
    private fun ChatMessageEntity.toDomain(): ChatMessage {
        if (role == ROLE_USER) {
            return ChatMessage(
                id = id,
                createdAt = createdAt,
                role = ChatMessage.Role.USER,
                text = rawContent,
                rawContent = rawContent,
            )
        }

        val extracted = rawContent?.let { AuiResponseExtractor.fromRawResponse(it) }
        return ChatMessage(
            id = id,
            createdAt = createdAt,
            role = ChatMessage.Role.ASSISTANT,
            text = extracted?.text,
            auiResponse = extracted?.auiResponse,
            rawAuiJson = extracted?.auiJson,
            rawContent = rawContent,
            errorMessage = errorMessage ?: extracted?.errorMessage,
        )
    }

    /**
     * Maps an entity to an [LlmMessage] for conversation history. For user
     * messages, sends [rawContent] directly. For assistant messages, extracts
     * the meaningful content (AUI JSON or text) from [rawContent].
     */
    private fun ChatMessageEntity.toLlmMessage(): LlmMessage {
        if (role == ROLE_USER) {
            return LlmMessage(
                role = LlmMessage.Role.USER,
                content = rawContent ?: "",
            )
        }
        val extracted = rawContent?.let { AuiResponseExtractor.fromRawResponse(it) }
        return LlmMessage(
            role = LlmMessage.Role.ASSISTANT,
            content = extracted?.auiJson ?: extracted?.text ?: rawContent ?: "",
        )
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
