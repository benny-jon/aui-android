package com.bennyjon.aui.core.model

import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Returns `true` if every block in this response is either feedback-free or handled
 * by a read-only action plugin.
 *
 * A "submit" action is always considered interactive (returns `false`). Unknown
 * actions with no matching plugin are also considered interactive.
 *
 * Host apps use this to decide whether an AUI response can remain clickable in a
 * chat history (read-only) or should be grayed out after a newer response arrives
 * (interactive / "spent").
 */
fun AuiResponse.isReadOnly(pluginRegistry: AuiPluginRegistry): Boolean {
    val allBlocks = blocks + steps.flatMap { it.blocks }
    return allBlocks.all { block ->
        val feedback = block.feedback ?: return@all true
        when (feedback.action) {
            "submit" -> false
            else -> pluginRegistry.actionPlugin(feedback.action)?.isReadOnly ?: false
        }
    }
}
