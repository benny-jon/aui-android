package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.AuiParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Deserializes the structured JSON envelope returned by an LLM into an [LlmResponse].
 *
 * The expected envelope format is:
 * ```json
 * {
 *   "text": "Conversational message",
 *   "aui": { "display": "inline", "blocks": [ ... ] }
 * }
 * ```
 *
 * `text` is always present on success. `aui` is optional — when present it is
 * parsed via [AuiParser] and both [LlmResponse.auiJson] and
 * [LlmResponse.auiResponse] are populated.
 */
internal object AuiResponseExtractor {

    private val json = Json { ignoreUnknownKeys = true }
    private val auiParser = AuiParser()

    /**
     * Parses [rawText] as the structured envelope and returns an [LlmResponse].
     *
     * Returns an error [LlmResponse] if [rawText] is not valid JSON or is
     * missing the required `text` field.
     */
    fun fromRawResponse(rawText: String): LlmResponse {
        return try {
            val root = json.parseToJsonElement(rawText).jsonObject
            val text = root["text"]?.jsonPrimitive?.content
                ?: return error("Missing 'text' field in LLM response")

            val auiElement = root["aui"]
            if (auiElement != null && auiElement is JsonObject) {
                val auiJsonString = auiElement.toString()
                val auiResponse = auiParser.parseOrNull(auiJsonString)
                LlmResponse(
                    text = text,
                    auiJson = auiJsonString,
                    auiResponse = auiResponse,
                )
            } else {
                LlmResponse(text = text)
            }
        } catch (e: Exception) {
            error("Failed to parse LLM response: ${e.message}", e)
        }
    }

    /**
     * Creates an error [LlmResponse] with the given [message] and optional [cause].
     */
    fun error(message: String, cause: Throwable? = null): LlmResponse =
        LlmResponse(errorMessage = message, cause = cause)
}
