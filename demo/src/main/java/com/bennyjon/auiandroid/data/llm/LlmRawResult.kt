package com.bennyjon.auiandroid.data.llm

/**
 * Raw result from an [LlmClient] completion call.
 *
 * Carries the unprocessed content string exactly as received from the provider.
 * Parsing into text, AUI JSON, etc. is deferred to the repository layer when
 * loading from the database, so the original response is always preserved for
 * replay and debugging.
 *
 * @property rawContent The raw response content string from the LLM provider.
 * @property errorMessage Error description when the call failed.
 * @property cause Optional throwable for error responses.
 */
data class LlmRawResult(
    val rawContent: String? = null,
    val errorMessage: String? = null,
    val cause: Throwable? = null,
)
