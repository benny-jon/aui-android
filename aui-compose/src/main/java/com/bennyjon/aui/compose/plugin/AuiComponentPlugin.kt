package com.bennyjon.aui.compose.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bennyjon.aui.core.plugin.AuiPlugin
import kotlinx.serialization.KSerializer

/**
 * A plugin that renders a custom component type in Jetpack Compose.
 *
 * Component plugins let host apps introduce entirely new component types
 * or override built-in ones. The library handles JSON parsing automatically
 * using the plugin's [dataSerializer] — no manual field extraction needed.
 *
 * To **add a new type**, register a plugin with a unique [componentType].
 * To **override a built-in**, register a plugin whose [componentType] matches
 * the built-in's type (e.g. `"card_basic"`). The plugin takes priority.
 *
 * Example:
 * ```kotlin
 * @Serializable
 * data class FunFactData(val title: String, val fact: String, val source: String? = null)
 *
 * object FunFactPlugin : AuiComponentPlugin<FunFactData>() {
 *     override val id = "fun_fact"
 *     override val componentType = "demo_fun_fact"
 *     override val dataSerializer = FunFactData.serializer()
 *     override val promptSchema = "demo_fun_fact(title, fact, source?) — A colorful fun-fact card."
 *
 *     @Composable
 *     override fun Render(data: FunFactData, onFeedback: (() -> Unit)?, modifier: Modifier) {
 *         FunFactCard(data = data, onClick = { onFeedback?.invoke() }, modifier = modifier)
 *     }
 * }
 * ```
 *
 * @param T the data class that models this component's JSON fields.
 *          Must be `@Serializable` with a matching [KSerializer].
 */
abstract class AuiComponentPlugin<T : Any> : AuiPlugin {
    /** The component type string this plugin handles (e.g. `"card_product_review"`). */
    abstract val componentType: String

    /**
     * Kotlinx Serialization serializer for [T].
     *
     * The library uses this to parse the block's JSON data automatically via
     * `Json.decodeFromJsonElement(dataSerializer, rawData)`.
     */
    abstract val dataSerializer: KSerializer<T>

    /**
     * If this plugin renders an input component, return its registry key.
     *
     * Returning non-null tells the feedback pipeline (both [BlockRenderer][com.bennyjon.aui.compose.internal.BlockRenderer]
     * and [AuiSurveyContent][com.bennyjon.aui.compose.display.AuiSurveyContent]) to include this
     * component's value in entry accumulation. `null` means this plugin is not an input.
     */
    open val inputKey: String? get() = null

    /**
     * Human-readable label for this input, used as the entry question in feedback summaries.
     *
     * Only meaningful when [inputKey] is non-null. Falls back to [inputKey] if `null`.
     */
    open val inputLabel: String? get() = null

    /**
     * Render this component.
     *
     * @param data the parsed data object (deserialized via [dataSerializer]).
     * @param onFeedback called when the user interacts with the component.
     *        `null` when no feedback is configured for this block.
     * @param modifier the [Modifier] to apply to the root layout.
     */
    @Composable
    abstract fun Render(
        data: T,
        onFeedback: (() -> Unit)?,
        modifier: Modifier,
    )

    /**
     * Component plugins use [componentType] as their slot key so that
     * registering two plugins for the same type replaces the first.
     */
    override val slotKey: String get() = componentType
}
