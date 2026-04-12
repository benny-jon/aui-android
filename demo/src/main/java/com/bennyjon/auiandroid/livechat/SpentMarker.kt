package com.bennyjon.auiandroid.livechat

import com.bennyjon.aui.core.model.isReadOnly
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatMessage

/**
 * Marks all-but-last interactive AUI messages as spent.
 *
 * A message is marked [ChatMessage.isAuiSpent] = `true` when:
 * 1. It has a non-null [ChatMessage.auiResponse].
 * 2. It is **not** the last message with an AUI response.
 * 3. Its AUI response is **not** read-only (per [AuiResponse.isReadOnly]).
 *
 * Read-only responses (e.g. only open_url buttons) remain interactive forever.
 * The DB is never mutated — this is a derived, render-time transformation.
 */
internal fun List<ChatMessage>.markSpentInteractives(
    pluginRegistry: AuiPluginRegistry,
): List<ChatMessage> {
    val lastAuiIndex = indexOfLast { it.auiResponse != null }
    return mapIndexed { index, msg ->
        if (msg.auiResponse != null
            && index < lastAuiIndex
            && !msg.auiResponse.isReadOnly(pluginRegistry)
        ) {
            msg.copy(isAuiSpent = true)
        } else {
            msg
        }
    }
}
