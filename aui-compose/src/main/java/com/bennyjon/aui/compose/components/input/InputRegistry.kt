package com.bennyjon.aui.compose.components.input

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver

private const val UiStatePrefix = "__aui_ui__:"
private const val MultiValueSeparator = '\u001F'

internal fun Map<String, String>.updateValue(key: String, value: String?): Map<String, String> =
    if (value == null) this - key else this + (key to value)

internal fun uiStateKey(key: String): String = UiStatePrefix + key

internal fun Map<String, String>.uiStateValue(key: String): String? = this[uiStateKey(key)]

internal fun Map<String, String>.updateUiStateValue(key: String, value: String?): Map<String, String> =
    updateValue(uiStateKey(key), value)

internal fun encodeUiStateValues(values: Collection<String>): String? =
    values.takeIf { it.isNotEmpty() }?.joinToString(separator = MultiValueSeparator.toString())

internal fun decodeUiStateValues(value: String?): Set<String> =
    value
        ?.takeIf { it.isNotEmpty() }
        ?.split(MultiValueSeparator)
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()

internal val RegistryStateSaver: Saver<MutableState<Map<String, String>>, Any> = Saver(
    save = { it.value },
    restore = { restored ->
        @Suppress("UNCHECKED_CAST")
        mutableStateOf(restored as? Map<String, String> ?: emptyMap())
    },
)
