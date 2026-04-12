package com.bennyjon.auiandroid.livechat

import com.bennyjon.aui.core.model.AuiFeedback

/**
 * Converts an [AuiFeedback] into plain text suitable for sending as a user message.
 *
 * Uses [AuiFeedback.formattedEntries] when available (the library-computed Q&A summary).
 * Falls back to the param values joined together (e.g. button label stored in params),
 * and finally to the [AuiFeedback.action] if nothing else is available.
 */
internal fun AuiFeedback.toUserMessageText(): String =
    formattedEntries
        ?: params.values.filter { it.isNotBlank() }.joinToString(", ").ifEmpty { null }
        ?: action
