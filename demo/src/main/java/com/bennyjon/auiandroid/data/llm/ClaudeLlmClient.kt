package com.bennyjon.auiandroid.data.llm

import android.os.Build
import android.util.Log
import com.bennyjon.auiandroid.BuildConfig
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
            if (BuildConfig.DEBUG) {
                Log.d("ClaudeLlmClient", systemPrompt)
            }
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
            if (BuildConfig.DEBUG) {
                Log.d("ClaudeLlmClient", responseText)
            }
            AuiResponseExtractor.fromRawResponse(responseText)
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
