package com.bennyjon.auiandroid.data.llm

/**
 * Available LLM provider backends.
 *
 * @property displayName Human-readable name shown in the provider dropdown.
 */
enum class LlmProvider(val displayName: String) {
    /** Cycles through scripted responses locally — no API key needed. */
    FAKE("Fake"),

    /** Anthropic Claude via the Messages API. Requires an API key. */
    CLAUDE("Claude"),

    /** OpenAI Chat Completions. Requires an API key. */
    OPENAI("OpenAI"),
}
