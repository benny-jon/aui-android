package com.bennyjon.auiandroid.data.chat

/**
 * Runtime logging config for chat persistence / extraction diagnostics.
 *
 * A function is used instead of a fixed boolean so existing repository instances
 * can react immediately when the Settings toggle changes.
 */
fun interface ChatDebugLoggerConfig {
    fun isEnabled(): Boolean

    companion object {
        val Disabled = ChatDebugLoggerConfig { false }
    }
}
