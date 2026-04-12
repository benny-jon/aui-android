package com.bennyjon.auiandroid.data.chat

import com.bennyjon.aui.core.AuiParser
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.chat.db.ChatMessageEntity
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
 * the response. Entity-to-domain mapping parses [ChatMessageEntity.auiJson]
 * via [AuiParser] when non-null.
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

    private val auiParser = AuiParser()

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        dao.observeMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun sendUserMessage(conversationId: String, text: String) {
        withContext(ioDispatcher) {
            // 1. Insert user row
            val userEntity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = ROLE_USER,
                text = text,
                createdAt = System.currentTimeMillis(),
            )
            dao.insert(userEntity)

            // 2. Build history from all rows
            val allEntities = dao.getMessages(conversationId)
            val history = allEntities.map { it.toLlmMessage() }

            // 3. Call LLM
            val llmResponse = llmClient.complete(systemPrompt, history)

            // 4. Insert assistant row
            val assistantEntity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = ROLE_ASSISTANT,
                text = llmResponse.text,
                auiJson = llmResponse.auiJson,
                errorMessage = llmResponse.errorMessage,
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

    private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        createdAt = createdAt,
        role = if (role == ROLE_USER) ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
        text = text,
        auiResponse = auiJson?.let { auiParser.parseOrNull(it) },
        rawAuiJson = auiJson,
        errorMessage = errorMessage,
    )

    private fun ChatMessageEntity.toLlmMessage(): LlmMessage = LlmMessage(
        role = if (role == ROLE_USER) LlmMessage.Role.USER else LlmMessage.Role.ASSISTANT,
        content = auiJson ?: text ?: "",
    )

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
