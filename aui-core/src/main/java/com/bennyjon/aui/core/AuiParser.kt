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
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
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
    fun parse(jsonString: String): AuiResponse {
        val normalizedRoot = normalizeResponseRoot(json.parseToJsonElement(jsonString))
        return json.decodeFromJsonElement(normalizedRoot)
    }

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
            normalizeResponseRoot(json.parseToJsonElement(jsonString)) as? JsonObject
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

    /**
     * Normalizes a common malformed shape emitted by some models:
     *
     * { "type": "...", "data": { ..., "feedback": { ... } } }
     *
     * into the canonical sibling form:
     *
     * { "type": "...", "data": { ... }, "feedback": { ... } }
     *
     * This only applies to top-level blocks (response blocks and survey step blocks).
     * Nested item data such as quick_replies option feedback is left untouched.
     */
    private fun normalizeResponseRoot(root: JsonElement): JsonElement {
        val obj = root as? JsonObject ?: return root
        return buildJsonObject {
            obj.forEach { (key, value) ->
                when (key) {
                    "blocks" -> put(key, normalizeBlocksArray(value))
                    "steps" -> put(key, normalizeStepsArray(value))
                    else -> put(key, value)
                }
            }
        }
    }

    private fun normalizeBlocksArray(element: JsonElement): JsonElement {
        val array = element as? JsonArray ?: return element
        return buildJsonArray {
            array.forEach { add(normalizeTopLevelBlockFeedback(it)) }
        }
    }

    private fun normalizeStepsArray(element: JsonElement): JsonElement {
        val array = element as? JsonArray ?: return element
        return buildJsonArray {
            array.forEach { stepElement ->
                val step = stepElement as? JsonObject
                if (step == null) {
                    add(stepElement)
                } else {
                    add(
                        buildJsonObject {
                            step.forEach { (key, value) ->
                                if (key == "blocks") {
                                    put(key, normalizeBlocksArray(value))
                                } else {
                                    put(key, value)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun normalizeTopLevelBlockFeedback(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return element
        if (obj.containsKey("feedback")) return obj

        val data = obj["data"] as? JsonObject ?: return obj
        val nestedFeedback = data["feedback"] ?: return obj

        return buildJsonObject {
            obj.forEach { (key, value) ->
                when (key) {
                    "data" -> put(
                        key,
                        buildJsonObject {
                            data.forEach { (dataKey, dataValue) ->
                                if (dataKey != "feedback") put(dataKey, dataValue)
                            }
                        },
                    )

                    else -> put(key, value)
                }
            }
            put("feedback", nestedFeedback)
        }
    }

    private fun JsonElement?.asJsonArrayOrEmpty(): JsonArray = (this as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.primitiveContentOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.content
}
