package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses AUI JSON strings into [AuiResponse] objects.
 *
 * Unknown component `type` values produce [com.bennyjon.aui.core.model.AuiBlock.Unknown]
 * and never throw. Unknown JSON fields on known types are silently ignored.
 *
 * Example usage:
 * ```kotlin
 * val parser = AuiParser()
 * val response = parser.parse(jsonString)
 * ```
 */
class AuiParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses [jsonString] into an [AuiResponse].
     *
     * @throws SerializationException if the JSON is structurally invalid or missing required fields.
     */
    fun parse(jsonString: String): AuiResponse = json.decodeFromString(jsonString)

    /**
     * Parses [jsonString] into an [AuiResponse], returning `null` on any parse error.
     *
     * Use this when the JSON source is untrusted or may be malformed.
     */
    fun parseOrNull(jsonString: String): AuiResponse? = try {
        parse(jsonString)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
