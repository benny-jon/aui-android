package com.bennyjon.aui.compose.internal

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * Shared mutable map of input key → current value for all input blocks rendered in the same
 * [BlockRenderer] call. Input components write here on value change; button components read here
 * to resolve `{{key}}` placeholders in feedback labels before emitting.
 */
internal val LocalAuiValueRegistry = compositionLocalOf<MutableState<Map<String, String>>> {
    mutableStateOf(emptyMap())
}

/**
 * Replaces every `{{key}}` token in [label] with the corresponding value from [values].
 * Tokens with no matching key are left as-is.
 */
internal fun resolvePlaceholders(label: String, values: Map<String, String>): String =
    values.entries.fold(label) { acc, (key, value) -> acc.replace("{{$key}}", value) }
