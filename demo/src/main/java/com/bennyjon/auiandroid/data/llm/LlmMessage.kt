package com.bennyjon.auiandroid.data.llm

/**
 * A single message in a conversation history sent to an [LlmClient].
 *
 * @property role Whether this message is from the user or the assistant.
 * @property content The text content of the message.
 */
data class LlmMessage(
    val role: Role,
    val content: String,
) {
    /** The sender of a conversation message. */
    enum class Role { USER, ASSISTANT }
}
