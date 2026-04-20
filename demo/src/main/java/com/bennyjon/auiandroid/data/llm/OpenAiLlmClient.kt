package com.bennyjon.auiandroid.data.llm

import android.util.Log
import com.bennyjon.auiandroid.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * [LlmClient] implementation that calls the OpenAI Chat Completions API.
 *
 * Sends conversation history to `POST /v1/chat/completions` and extracts the
 * first assistant message payload as [LlmRawResult.rawContent]. Parsing into
 * text/AUI is deferred to the repository layer via [AuiResponseExtractor].
 *
 * If the assistant responds via a tool call, this client normalizes the first
 * matching tool call into the standard `{ "text": "...", "aui": { ... } }`
 * envelope so the rest of the app can stay provider-neutral.
 */
class OpenAiLlmClient(
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
                Log.d("OpenAiLlmClient", systemPrompt)
            }
            val requestBody = buildRequestBody(systemPrompt, history)

            val response = httpClient.post(CHAT_COMPLETIONS_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            if (BuildConfig.DEBUG) {
                Log.d("OpenAiLlmClient", responseText)
            }
            val root = Json.parseToJsonElement(responseText).jsonObject
            extractErrorMessage(root)?.let { errorMessage ->
                return LlmRawResult(
                    messageId = root.primitiveContentOrNull("id"),
                    errorMessage = errorMessage,
                )
            }
            val messageId = root["id"]?.jsonPrimitive?.content
            val message = root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?: return LlmRawResult(
                    messageId = messageId,
                    errorMessage = "OpenAI response missing assistant message",
                )

            val rawContent = extractRawContent(message)
                ?: return LlmRawResult(
                    messageId = messageId,
                    errorMessage = "OpenAI response missing content",
                )

            LlmRawResult(messageId = messageId, rawContent = rawContent)
        } catch (e: Exception) {
            LlmRawResult(errorMessage = "OpenAI API error: ${e.message}", cause = e)
        }
    }

    internal fun extractErrorMessage(root: JsonObject): String? {
        val error = root["error"]?.jsonObject ?: return null
        val message = error.primitiveContentOrNull("message")
        val type = error.primitiveContentOrNull("type")
        val code = error.primitiveContentOrNull("code")

        return buildString {
            append("OpenAI API error")
            if (!type.isNullOrBlank()) {
                append(" (").append(type).append(")")
            }
            if (!message.isNullOrBlank()) {
                append(": ").append(message)
            }
            if (!code.isNullOrBlank()) {
                append(" [").append(code).append("]")
            }
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): String {
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                history.forEach { msg ->
                    add(buildJsonObject {
                        put("role", when (msg.role) {
                            LlmMessage.Role.USER -> "user"
                            LlmMessage.Role.ASSISTANT -> "assistant"
                        })
                        put("content", msg.content)
                    })
                }
            })
            put("response_format", buildJsonObject {
                put("type", "json_object")
            })
        }
        return body.toString()
    }

    private fun extractRawContent(message: JsonObject): String? {
        extractTextContent(message["content"])?.let { return it }

        val toolCalls = message["tool_calls"]?.jsonArray.orEmpty()
        return toolCalls
            .firstOrNull { call ->
                call.jsonObject["function"]
                    ?.jsonObject
                    ?.get("name")
                    ?.jsonPrimitive
                    ?.content == AUI_ENVELOPE_TOOL_NAME
            }
            ?.jsonObject
            ?.get("function")
            ?.jsonObject
            ?.get("arguments")
            ?.jsonPrimitive
            ?.content
    }

    private fun extractTextContent(content: JsonElement?): String? {
        return when (content) {
            null -> null
            is JsonPrimitive -> content.content
            is JsonArray -> content.mapNotNull { item ->
                val itemObject = item as? JsonObject ?: return@mapNotNull null
                if (itemObject["type"]?.jsonPrimitive?.content != "text") {
                    return@mapNotNull null
                }
                itemObject["text"]?.jsonPrimitive?.content
            }.joinToString(separator = "").ifBlank { null }
            else -> null
        }
    }

    private fun JsonObject.primitiveContentOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    internal companion object {
        const val CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o"
        private const val AUI_ENVELOPE_TOOL_NAME = "respond_with_aui_envelope"
        private const val AUI_ENVELOPE_TOOL_DESCRIPTION =
            "Return the assistant reply as a JSON object with required text and optional aui fields."
    }
}
