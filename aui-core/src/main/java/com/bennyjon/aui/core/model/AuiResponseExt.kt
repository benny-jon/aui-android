package com.bennyjon.aui.core.model

import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Collects every [AuiFeedback] reachable from this block, including nested ones.
 *
 * Most blocks carry at most one top-level [AuiBlock.feedback]. [AuiBlock.QuickReplies]
 * also stores per-option feedback inside [QuickRepliesData][com.bennyjon.aui.core.model.data.QuickRepliesData].
 * This function returns all of them so callers don't need to know which block types
 * have nested feedback.
 */
fun AuiBlock.allFeedbacks(): List<AuiFeedback> = buildList {
    feedback?.let { add(it) }
    when (this@allFeedbacks) {
        is AuiBlock.QuickReplies -> data.options.mapNotNull { it.feedback }.let { addAll(it) }
        else -> { /* no other block types have nested feedback */ }
    }
}

/**
 * Returns `true` if every feedback in this response is either absent or handled
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
    val allFeedbacks = allBlocks.flatMap { it.allFeedbacks() }
    if (allFeedbacks.isEmpty()) return true
    return allFeedbacks.all { feedback ->
        when (feedback.action) {
            "submit" -> false
            else -> pluginRegistry.actionPlugin(feedback.action)?.isReadOnly ?: false
        }
    }
}
