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

