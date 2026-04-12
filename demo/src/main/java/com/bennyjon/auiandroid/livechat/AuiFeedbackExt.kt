package com.bennyjon.auiandroid.livechat

import com.bennyjon.aui.core.model.AuiFeedback

/**
 * Converts an [AuiFeedback] into plain text suitable for sending as a user message.
 *
 * Uses [AuiFeedback.formattedEntries] when available (the library-computed Q&A summary),
 * falling back to the feedback [AuiFeedback.action] label if no entries were collected
 * (e.g. a simple button tap).
 */
internal fun AuiFeedback.toUserMessageText(): String =
    formattedEntries ?: action
