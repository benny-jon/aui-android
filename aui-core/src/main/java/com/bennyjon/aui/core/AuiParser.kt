package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.AuiStep
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

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
     * Parses [jsonString] into an [AuiResponse], returning `null` only when the top-level
     * response is unusable.
     *
     * Use this when the JSON source is untrusted or may be malformed. If the top-level
     * response can be understood, malformed known blocks are downgraded to
     * [com.bennyjon.aui.core.model.AuiBlock.Unknown] so other valid blocks still render.
     */
    fun parseOrNull(jsonString: String): AuiResponse? = try {
        parse(jsonString)
    } catch (e: SerializationException) {
        parseResilientOrNull(jsonString)
    } catch (e: IllegalArgumentException) {
        parseResilientOrNull(jsonString)
    }

    private fun parseResilientOrNull(jsonString: String): AuiResponse? {
        val root = try {
            json.parseToJsonElement(jsonString) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return null

        val display = try {
            root["display"]?.let { json.decodeFromJsonElement<AuiDisplay>(it) }
        } catch (_: Exception) {
            null
        } ?: return null

        return AuiResponse(
            display = display,
            blocks = root["blocks"].asJsonArrayOrEmpty().mapNotNull(::decodeBlockSafely),
            steps = root["steps"].asJsonArrayOrEmpty().mapNotNull(::decodeStepSafely),
            surveyTitle = root.primitiveContentOrNull("survey_title"),
            cardTitle = root.primitiveContentOrNull("card_title"),
            cardDescription = root.primitiveContentOrNull("card_description"),
        )
    }

    private fun decodeStepSafely(element: JsonElement): AuiStep? {
        val obj = element as? JsonObject ?: return null
        return try {
            json.decodeFromJsonElement<AuiStep>(obj)
        } catch (_: Exception) {
            AuiStep(
                blocks = obj["blocks"].asJsonArrayOrEmpty().mapNotNull(::decodeBlockSafely),
                question = obj.primitiveContentOrNull("question"),
                label = obj.primitiveContentOrNull("label"),
            )
        }
    }

    private fun decodeBlockSafely(element: JsonElement): AuiBlock? {
        val obj = element as? JsonObject ?: return null
        return try {
            json.decodeFromJsonElement<AuiBlock>(obj)
        } catch (_: Exception) {
            AuiBlock.Unknown(
                type = obj.primitiveContentOrNull("type") ?: "unknown",
                feedback = decodeFeedbackOrNull(obj["feedback"]),
                rawData = obj["data"],
            )
        }
    }

    private fun decodeFeedbackOrNull(element: JsonElement?): AuiFeedback? = try {
        element?.let { json.decodeFromJsonElement<AuiFeedback>(it) }
    } catch (_: Exception) {
        null
    }

    private fun JsonElement?.asJsonArrayOrEmpty(): JsonArray = (this as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.primitiveContentOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.content
}
