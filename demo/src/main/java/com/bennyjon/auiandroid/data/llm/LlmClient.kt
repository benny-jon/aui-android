package com.bennyjon.auiandroid.data.llm

/**
 * Provider-neutral interface for LLM completion.
 *
 * Implementations wrap a specific provider (Claude, OpenAI, fake) and return a
 * flat [LlmResponse]. The caller supplies a system prompt and conversation
 * history; the client handles serialization, HTTP, and response extraction.
 */
interface LlmClient {

    /**
     * Sends [history] to the LLM with the given [systemPrompt] and returns
     * the model's reply.
     *
     * On success the returned [LlmResponse] has a non-null [LlmResponse.text]
     * and optionally [LlmResponse.auiJson] / [LlmResponse.auiResponse].
     * On failure [LlmResponse.errorMessage] is set instead.
     */
    suspend fun complete(systemPrompt: String, history: List<LlmMessage>): LlmResponse
}
