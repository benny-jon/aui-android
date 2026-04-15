package com.bennyjon.auiandroid.data.llm

/**
 * Provider-neutral interface for LLM completion.
 *
 * Implementations wrap a specific provider (Claude, OpenAI, fake) and return a
 * raw [LlmRawResult]. The caller supplies a system prompt and conversation
 * history; the client handles serialization and HTTP. Parsing the raw content
 * into text/AUI is deferred to the repository layer.
 */
interface LlmClient {

    /**
     * Sends [history] to the LLM with the given [systemPrompt] and returns
     * the model's raw reply.
     *
     * On success the returned [LlmRawResult] has a non-null
     * [LlmRawResult.rawContent] containing the unprocessed response string.
     * On failure [LlmRawResult.errorMessage] is set instead.
     */
    suspend fun complete(systemPrompt: String, history: List<LlmMessage>): LlmRawResult
}
