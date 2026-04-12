package com.bennyjon.auiandroid.livechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.DemoServiceLocator
import com.bennyjon.auiandroid.data.chat.ChatMessage
import com.bennyjon.auiandroid.data.chat.ChatRepository
import com.bennyjon.auiandroid.data.llm.LlmProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the live chat screen.
 *
 * Observes messages from [ChatRepository], applies spent-marking via
 * [markSpentInteractives], and exposes a [StateFlow] of [ChatMessage]s.
 * Feedback from AUI interactions is converted to plain text before being
 * sent as a user message.
 *
 * Supports switching [LlmProvider] at runtime. Switching clears the current
 * conversation and rebuilds the repository via [DemoServiceLocator].
 *
 * @param repository Chat repository for message persistence and LLM calls.
 * @param conversationId Identifier for the current conversation.
 * @param pluginRegistry Plugin registry used for read-only detection.
 */
class LiveChatViewModel(
    private var repository: ChatRepository,
    private val conversationId: String,
    private val pluginRegistry: AuiPluginRegistry,
) : ViewModel() {

    private val _repositoryVersion = MutableStateFlow(0)

    /** All messages in the conversation, with spent-marking applied. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> =
        _repositoryVersion.flatMapLatest {
            repository.observeMessages(conversationId)
                .map { it.markSpentInteractives(pluginRegistry) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)

    /** True while a message is being sent and the LLM response is pending. */
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _currentProvider = MutableStateFlow(DemoServiceLocator.currentProvider)

    /** The currently active LLM provider. */
    val currentProvider: StateFlow<LlmProvider> = _currentProvider.asStateFlow()

    /** Whether the Claude provider is available (API key configured). */
    val isClaudeAvailable: Boolean = DemoServiceLocator.isClaudeAvailable

    /** Sends a user text message and waits for the LLM response. */
    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendUserMessage(conversationId, text)
            } finally {
                _isSending.value = false
            }
        }
    }

    /** Converts AUI feedback to text and sends it as a user message. */
    fun onFeedback(feedback: AuiFeedback) {
        send(feedback.toUserMessageText())
    }

    /** Clears all messages in the current conversation. */
    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation(conversationId)
        }
    }

    /**
     * Switches to a new [LlmProvider], clearing the conversation.
     *
     * No-op if the provider is already active or if switching to Claude
     * without an API key.
     */
    fun switchProvider(provider: LlmProvider) {
        if (provider == _currentProvider.value) return
        if (provider == LlmProvider.CLAUDE && !isClaudeAvailable) return

        viewModelScope.launch {
            repository.clearConversation(conversationId)
            DemoServiceLocator.setProvider(provider)
            repository = DemoServiceLocator.chatRepository
            _currentProvider.value = provider
            _repositoryVersion.value++
        }
    }
}
