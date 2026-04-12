package com.bennyjon.auiandroid.data.llm

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * [LlmClient] implementation that calls the Anthropic Messages API.
 *
 * Sends conversation history to `POST /v1/messages` and parses the response
 * through [AuiResponseExtractor]. The system prompt instructs the model to
 * respond with the structured JSON envelope `{ "text": "...", "aui": { ... } }`.
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

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): LlmResponse {
        return try {
            val requestBody = buildRequestBody(systemPrompt, history)

            val response = httpClient.post(MESSAGES_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", API_VERSION)
                }
                setBody(requestBody)
            }

            val responseText = response.bodyAsText()
            val contentText = extractContentText(responseText)
                ?: return AuiResponseExtractor.error("No text content in Claude response")

            AuiResponseExtractor.fromRawResponse(contentText)
        } catch (e: Exception) {
            AuiResponseExtractor.error("Claude API error: ${e.message}", e)
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): String {
        val messages = history.map { msg ->
            ApiMessage(
                role = when (msg.role) {
                    LlmMessage.Role.USER -> "user"
                    LlmMessage.Role.ASSISTANT -> "assistant"
                },
                content = msg.content,
            )
        }

        val body = ApiRequest(
            model = model,
            max_tokens = MAX_TOKENS,
            system = systemPrompt,
            messages = messages,
        )

        return json.encodeToString(body)
    }

    /**
     * Extracts the text from the first `text` content block in the API response.
     */
    private fun extractContentText(responseJson: String): String? {
        return try {
            val root = json.parseToJsonElement(responseJson).jsonObject
            val content = root["content"]?.jsonArray ?: return null
            for (block in content) {
                val obj = block.jsonObject
                if (obj["type"]?.jsonPrimitive?.content == "text") {
                    return obj["text"]?.jsonPrimitive?.content
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    private data class ApiMessage(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class ApiRequest(
        val model: String,
        val max_tokens: Int,
        val system: String,
        val messages: List<ApiMessage>,
    )

    internal companion object {
        const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
        const val API_VERSION = "2023-06-01"
        const val DEFAULT_MODEL = "claude-sonnet-4-5"
        const val MAX_TOKENS = 4096
    }
}
