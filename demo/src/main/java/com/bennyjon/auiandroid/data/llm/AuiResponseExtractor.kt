package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.AuiParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Deserializes LLM responses into an [LlmResponse].
 *
 * Supports two formats:
 *
 * **Structured envelope** (used when the LLM follows the system prompt):
 * ```json
 * {
 *   "text": "Conversational message",
 *   "aui": { "display": "expanded", "blocks": [ ... ] }
 * }
 * ```
 *
 * **Claude Messages API response** (raw API JSON):
 * ```json
 * {
 *   "id": "msg_...",
 *   "content": [ { "type": "text", "text": "..." } ],
 *   ...
 * }
 * ```
 *
 * The format is detected automatically. When a Claude API response contains a
 * structured envelope inside its text content, the envelope is unwrapped so
 * that [LlmResponse.auiJson] and [LlmResponse.auiResponse] are populated.
 */
internal object AuiResponseExtractor {

    private val json = Json { ignoreUnknownKeys = true }
    private val auiParser = AuiParser()

    /**
     * Parses [rawText] and returns an [LlmResponse].
     *
     * [rawText] may be any of:
     *  - A structured envelope `{ "text": "...", "aui": { ... } }`
     *  - Raw AUI JSON (has `display`), optionally wrapped in a ```json fence
     *  - A Claude Messages API response (detected by the `content` array)
     *  - Mixed content: preamble text followed by a fenced or unfenced JSON block
     *  - Plain text — returned as [LlmResponse.text] with no AUI
     */
    fun fromRawResponse(rawText: String): LlmResponse {
        return try {
            val root = try {
                json.parseToJsonElement(rawText).jsonObject
            } catch (_: Exception) {
                null
            }
            if (root != null) {
                when {
                    isClaudeErrorResponse(root) -> parseClaudeErrorResponse(root)
                    isClaudeApiResponse(root) -> parseClaudeApiResponse(root)
                    else -> parseContentText(rawText)
                }
            } else {
                parseContentText(rawText)
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

    /**
     * Detects whether [root] is a Claude Messages API response by checking for
     * the `content` array field (which the envelope format never uses).
     */
    private fun isClaudeApiResponse(root: JsonObject): Boolean {
        val content = root["content"] ?: return false
        return content is JsonArray
    }

    private fun isClaudeErrorResponse(root: JsonObject): Boolean =
        root.primitiveContentOrNull("type") == "error" &&
            root["error"] is JsonObject

    /**
     * Parses the original structured envelope format: `{ "text": "...", "aui": { ... } }`.
     */
    private fun parseEnvelope(root: JsonObject): LlmResponse {
        val text = root["text"]?.jsonPrimitive?.content
            ?: return error("Missing 'text' field in LLM response")
        val id = root["id"]?.jsonPrimitive?.content

        val auiElement = root["aui"]
        if (auiElement != null && auiElement is JsonObject) {
            val auiJsonString = auiElement.toString()
            val auiResponse = auiParser.parseOrNull(auiJsonString)
            return LlmResponse(
                id = id,
                text = text,
                auiJson = auiJsonString,
                auiResponse = auiResponse,
            )
        }
        return LlmResponse(id = id, text = text)
    }

    /**
     * Parses a Claude Messages API response, extracting text from the first
     * `text` content block and `id` from the root.
     *
     * Tries to unwrap content text in this priority order:
     * 1. Structured envelope `{ "text": "...", "aui": { ... } }`
     * 2. Raw AUI JSON (detected by `display` field) — may be wrapped in markdown code fences
     * 3. Embedded JSON within mixed content (e.g. text preceding the JSON)
     * 4. Plain text fallback
     */
    private fun parseClaudeApiResponse(root: JsonObject): LlmResponse {
        val id = root["id"]?.jsonPrimitive?.content
        val contentText = extractContentText(root)
            ?: return error("No text content in Claude response")
        val parsed = parseContentText(contentText)
        return parsed.copy(id = id ?: parsed.id)
    }

    private fun parseClaudeErrorResponse(root: JsonObject): LlmResponse {
        val error = root["error"]?.jsonObject
        val message = error?.primitiveContentOrNull("message")
        val type = error?.primitiveContentOrNull("type")
        val requestId = root.primitiveContentOrNull("request_id")

        val errorMessage = buildString {
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

        return LlmResponse(id = requestId, errorMessage = errorMessage)
    }

    private fun JsonObject?.primitiveContentOrNull(key: String): String? =
        this?.let { (it[key] as? JsonPrimitive)?.content }

    /**
     * Parses the text content of an assistant message, which may be:
     *
     *  1. A structured envelope `{ "text": "...", "aui": { ... } }`
     *  2. Raw AUI JSON (has `display`, no `text`) — optionally wrapped in a ```json fence
     *  3. Mixed content: preamble text followed by a fenced or unfenced JSON block
     *  4. Plain text
     */
    private fun parseContentText(contentText: String): LlmResponse {
        val stripped = stripMarkdownCodeFence(contentText)

        tryParseEnvelope(stripped)?.let { return it }
        tryParseRawAui(stripped)?.let { return it }
        tryExtractEmbeddedJson(contentText)?.let { return it }

        if (stripped.contains("\"aui\"")) {
            logWarning(
                "Content looks like it contains AUI but could not be parsed. " +
                    "First 200 chars: ${stripped.take(200)}"
            )
        }

        return LlmResponse(text = contentText)
    }

    /**
     * Extracts the text value from the first `text`-type content block.
     */
    private fun extractContentText(root: JsonObject): String? {
        val content = root["content"]?.jsonArray ?: return null
        for (block in content) {
            val obj = block.jsonObject
            if (obj["type"]?.jsonPrimitive?.content == "text") {
                return obj["text"]?.jsonPrimitive?.content
            }
        }
        return null
    }

    /**
     * Attempts to parse [text] as a structured envelope. Returns null if it
     * is not valid JSON or does not contain the required `text` field.
     */
    private fun tryParseEnvelope(text: String): LlmResponse? {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            // Only treat as envelope if it has the `text` key
            if (root.containsKey("text")) parseEnvelope(root) else null
        } catch (e: Exception) {
            logWarning("tryParseEnvelope failed: ${e.message}")
            null
        }
    }

    /**
     * Attempts to parse [text] as raw AUI JSON (has `display` field but no `text` field).
     * This handles the case where the LLM outputs the AUI payload directly instead of
     * wrapping it in the `{ "text", "aui" }` envelope.
     */
    private fun tryParseRawAui(text: String): LlmResponse? {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            if (root.containsKey("display") && !root.containsKey("text")) {
                val auiJsonString = root.toString()
                val auiResponse = auiParser.parseOrNull(auiJsonString)
                LlmResponse(
                    text = "",
                    auiJson = auiJsonString,
                    auiResponse = auiResponse,
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logWarning("tryParseRawAui failed: ${e.message}")
            null
        }
    }

    /**
     * Tries to find a JSON object embedded within [text] that may have a leading
     * or trailing text prefix/suffix (e.g. the model wrote conversational text
     * before the JSON envelope).
     *
     * Extracts the substring from the first `{` to the last `}` and attempts to
     * parse it as an envelope or raw AUI JSON.
     */
    private fun tryExtractEmbeddedJson(text: String): LlmResponse? {
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) return null
        // Only attempt if the JSON is actually embedded (has a prefix or suffix)
        if (firstBrace == 0 && lastBrace == text.length - 1) return null

        val candidate = text.substring(firstBrace, lastBrace + 1)
        return tryParseEnvelope(candidate) ?: tryParseRawAui(candidate)
    }

    /**
     * Strips markdown code fences (` ```json ... ``` ` or ` ``` ... ``` `) from [text].
     * Returns the inner content if fences are found, or [text] unchanged otherwise.
     */
    private fun stripMarkdownCodeFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return text
        val openEnd = trimmed.indexOf('\n')
        if (openEnd == -1) return text
        if (!trimmed.endsWith("```")) return text
        return trimmed.substring(openEnd + 1, trimmed.length - 3).trim()
    }

    private fun logWarning(message: String) {
        System.err.println("AuiResponseExtractor: $message")
    }
}
