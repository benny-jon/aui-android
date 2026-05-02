package com.bennyjon.aui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bennyjon.aui.compose.components.input.uiStateKey
import com.bennyjon.aui.compose.components.input.updateUiStateValue
import com.bennyjon.aui.compose.components.input.updateValue

/**
 * Explicit state holder for one logical AUI response render.
 *
 * Hosts can keep one instance per message / response id and pass that same object back into
 * [AuiRenderer] anywhere the same response is rendered (inline, sheet, detail pane, etc.).
 * Sharing the same instance shares input values and survey progress; using a different instance
 * isolates them.
 *
 * The host is responsible for choosing the identity boundary. Reusing one [AuiRenderState]
 * across unrelated responses will intentionally carry state forward.
 *
 * @param inputValues Initial input registry contents for this response render.
 * @param surveyStepIndex Initial survey step index for survey responses.
 */
@Stable
class AuiRenderState(
    inputValues: Map<String, String> = emptyMap(),
    surveyStepIndex: Int = 0,
) {
    private val registry = mutableStateOf(inputValues)
    internal var surveyStepIndex by mutableIntStateOf(surveyStepIndex)

    /** Current human-readable input values collected by the renderer. */
    val inputValues: Map<String, String>
        get() = registry.value

    /** Returns the feedback-facing value stored for [key], or `null` when absent. */
    fun value(key: String): String? = registry.value[key]

    /**
     * Returns the renderer-local UI state for [key], or `null` when absent.
     *
     * This is separate from [value] so components can preserve implementation-specific state
     * (for example selected ids) while still storing human-readable feedback text separately.
     */
    fun uiState(key: String): String? = registry.value[uiStateKey(key)]

    /** Stores the feedback-facing value for [key]. Passing `null` removes it. */
    fun setValue(key: String, value: String?) {
        registry.value = registry.value.updateValue(key, value)
    }

    /** Stores renderer-local UI state for [key]. Passing `null` removes it. */
    fun setUiState(key: String, value: String?) {
        registry.value = registry.value.updateUiStateValue(key, value)
    }

    /**
     * Updates both the feedback-facing [value] and renderer-local [uiStateValue] for [key].
     *
     * When [uiStateValue] is omitted, the same value is used for both slots.
     */
    fun setInputState(
        key: String,
        value: String?,
        uiStateValue: String? = value,
    ) {
        registry.value = registry.value
            .updateValue(key, value)
            .updateUiStateValue(key, uiStateValue)
    }

    /** Clears collected input values and resets survey progress to the first step. */
    fun reset() {
        registry.value = emptyMap()
        surveyStepIndex = 0
    }

    companion object {
        private const val InputValuesKey = "inputValues"
        private const val SurveyStepIndexKey = "surveyStepIndex"

        /** [Saver] for persisting [AuiRenderState] with `rememberSaveable`. */
        val Saver: Saver<AuiRenderState, Any> = mapSaver(
            save = { state ->
                mapOf(
                    InputValuesKey to state.registry.value.toMap(),
                    SurveyStepIndexKey to state.surveyStepIndex,
                )
            },
            restore = { restored ->
                @Suppress("UNCHECKED_CAST")
                AuiRenderState(
                    inputValues = restored[InputValuesKey] as? Map<String, String> ?: emptyMap(),
                    surveyStepIndex = restored[SurveyStepIndexKey] as? Int ?: 0,
                )
            },
        )
    }
}

/**
 * Remembers an [AuiRenderState] for the current composition and restores it across configuration
 * changes. Hosts with their own state holders can construct and retain [AuiRenderState]
 * instances directly instead of using this helper.
 */
@Composable
fun rememberAuiRenderState(): AuiRenderState =
    rememberSaveable(saver = AuiRenderState.Saver) { AuiRenderState() }

/**
 * The [AuiRenderState] currently backing an [AuiRenderer].
 *
 * Built-in input components and custom component plugins can read this to persist UI state in
 * the same typed response-level state object that the host supplied to the renderer.
 */
val LocalAuiRenderState = compositionLocalOf<AuiRenderState> {
    error("LocalAuiRenderState not provided. Wrap content in AuiRenderer or AuiSurveyContent.")
}
