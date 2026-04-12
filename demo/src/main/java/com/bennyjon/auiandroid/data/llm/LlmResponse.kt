package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.model.AuiResponse

/**
 * Flat container for any LLM reply.
 *
 * [text] is always present for successful responses (the LLM's conversational
 * message). [auiJson] and [auiResponse] are optionally present when the LLM
 * also produced an AUI payload. [errorMessage] is set only on failure, in
 * which case [text] and [auiJson] are null.
 *
 * Constructed only via [AuiResponseExtractor].
 */
data class LlmResponse(
    val id: String? = null,
    val text: String? = null,
    val auiJson: String? = null,
    val auiResponse: AuiResponse? = null,
    val errorMessage: String? = null,
    val cause: Throwable? = null,
)
