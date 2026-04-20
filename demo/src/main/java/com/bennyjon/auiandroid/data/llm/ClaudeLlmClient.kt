package com.bennyjon.auiandroid.data.llm

import android.util.Log
import com.bennyjon.auiandroid.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * [LlmClient] implementation that calls the Anthropic Messages API.
 *
 * Sends conversation history to `POST /v1/messages` and extracts the text
 * from all `text`-type content blocks, returning the concatenated result as
 * [LlmRawResult.rawContent].
 * Parsing into text/AUI is deferred to the repository layer via [AuiResponseExtractor].
 *
 * @param apiKey Anthropic API key (x-api-key header).
 * @param httpClient Ktor HTTP client configured with content negotiation.
 * @param model Model identifier to use (defaults to claude-sonnet-4-5).
 */
class ClaudeLlmClient(
    private val apiKey: String,
    private val httpClient: HttpClient,
    private val model: String = DEFAULT_MODEL,
) : LlmClient {

    override suspend fun complete(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): LlmRawResult {
        return try {
            if (BuildConfig.DEBUG) {
                Log.d("ClaudeLlmClient", systemPrompt)
            }
            val requestBody = buildRequestBody(systemPrompt, history)

            val response = httpClient.post(MESSAGES_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", API_VERSION)
                    append("anthropic-beta", "prompt-caching-2024-07-31")
                }
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            if (BuildConfig.DEBUG) {
                Log.d("ClaudeLlmClient", responseText)
            }
            val root = Json.parseToJsonElement(responseText).jsonObject
            extractErrorMessage(root)?.let { errorMessage ->
                return LlmRawResult(
                    messageId = root.primitiveContentOrNull("request_id"),
                    errorMessage = errorMessage,
                )
            }
            val messageId = root["id"]?.jsonPrimitive?.content
            val contentText = extractContentText(root)
                ?: return LlmRawResult(
                    messageId = messageId,
                    errorMessage = "Claude response missing text content",
                )
            LlmRawResult(messageId = messageId, rawContent = contentText)
        } catch (e: Exception) {
            LlmRawResult(errorMessage = "Claude API error: ${e.message}", cause = e)
        }
    }

    internal fun extractContentText(root: JsonObject): String? {
        val content = root["content"]?.jsonArray ?: return null
        return content.mapNotNull { block ->
            val obj = block.jsonObject
            if (obj["type"]?.jsonPrimitive?.content != "text") {
                return@mapNotNull null
            }
            obj["text"]?.jsonPrimitive?.content
        }.joinToString(separator = "").ifBlank { null }
    }

    internal fun extractErrorMessage(root: JsonObject): String? {
        if (root.primitiveContentOrNull("type") != "error") return null

        val error = root["error"]?.jsonObject
        val message = error?.primitiveContentOrNull("message")
        val type = error?.primitiveContentOrNull("type")
        val requestId = root.primitiveContentOrNull("request_id")

        return buildString {
            append("Claude API error")
            if (!type.isNullOrBlank()) {
                append(" (").append(type).append(")")
            }
            if (!message.isNullOrBlank()) {
                append(": ").append(message)
            }
            if (!requestId.isNullOrBlank()) {
                append(" [").append(requestId).append("]")
            }
        }
    }

    private fun JsonObject?.primitiveContentOrNull(key: String): String? =
        this?.let { (it[key] as? JsonPrimitive)?.content }

    private fun buildRequestBody(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): String {
        val messages = history.map { msg ->
            buildJsonObject {
                put("role", when (msg.role) {
                    LlmMessage.Role.USER -> "user"
                    LlmMessage.Role.ASSISTANT -> "assistant"
                })
                put("content", msg.content)
            }
        }

        val system = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", systemPrompt)
                put("cache_control", buildJsonObject { put("type", "ephemeral") })
            })
        }

        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", MAX_TOKENS)
            put("system", system)
            put("messages", buildJsonArray { messages.forEach { add(it) } })
        }

        return body.toString()
    }

    internal companion object {
        const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
        const val API_VERSION = "2023-06-01"
        const val DEFAULT_MODEL = "claude-sonnet-4-5"
        const val MAX_TOKENS = 4096
    }
}
