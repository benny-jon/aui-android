package com.bennyjon.auiandroid.livechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.AppSettings
import com.bennyjon.auiandroid.data.chat.ChatDebugLoggerConfig
import com.bennyjon.auiandroid.data.chat.ChatMessage
import com.bennyjon.auiandroid.data.chat.ChatRepository
import com.bennyjon.auiandroid.data.chat.DefaultChatRepository
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.llm.ClaudeLlmClient
import com.bennyjon.auiandroid.data.llm.FakeLlmClient
import com.bennyjon.auiandroid.data.llm.LlmClient
import com.bennyjon.auiandroid.data.llm.LlmProvider
import com.bennyjon.auiandroid.data.llm.OpenAiLlmClient
import com.bennyjon.auiandroid.di.AnthropicApiKey
import com.bennyjon.auiandroid.di.OpenAiApiKey
import com.bennyjon.auiandroid.di.SystemPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the live chat screen.
 *
 * Observes messages from [ChatRepository], applies spent-marking via
 * [markSpentInteractives], and exposes a [StateFlow] of [ChatMessage]s.
 * Feedback from AUI interactions is converted to plain text before being
 * sent as a user message.
 *
 * Supports switching [LlmProvider] at runtime. Switching clears the current
 * conversation and rebuilds the repository with the new provider's client.
 * The selected provider is persisted via [AppSettings] so it survives
 * app restarts.
 */
@HiltViewModel
class LiveChatViewModel @Inject constructor(
    private val dao: ChatMessageDao,
    val pluginRegistry: AuiPluginRegistry,
    @param:SystemPrompt private val systemPrompt: String,
    @param:AnthropicApiKey private val anthropicApiKey: String,
    @param:OpenAiApiKey private val openAiApiKey: String,
    private val httpClient: HttpClient,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val conversationId: String = "live"

    private val _repositoryVersion = MutableStateFlow(0)

    private var repository: ChatRepository = createRepository(LlmProvider.FAKE)
    private val _currentProvider = MutableStateFlow(LlmProvider.FAKE)
    private val _chatDebugLogsEnabled = MutableStateFlow(false)

    /** All messages in the conversation, with spent-marking applied. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> =
        _repositoryVersion.flatMapLatest {
            repository.observeMessages(conversationId)
                .map { it.markSpentInteractives(pluginRegistry) }
                .onEach { maybeAutoOpenLatestSurvey(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Survey message ids we've already auto-opened. Prevents re-opening after a dismiss.
    private val autoOpenedSurveyIds = mutableSetOf<String>()

    private fun maybeAutoOpenLatestSurvey(messages: List<ChatMessage>) {
        val latest = messages.lastOrNull() ?: return
        if (latest.auiResponse?.display == AuiDisplay.SURVEY
            && latest.id !in autoOpenedSurveyIds
        ) {
            autoOpenedSurveyIds.add(latest.id)
            _selectedDetailMessageId.value = latest.id
        }
    }

    private val _isSending = MutableStateFlow(false)
    private val _lastUserMessage = MutableStateFlow<String?>(null)

    /** True while a message is being sent and the LLM response is pending. */
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    /** Whether the latest failed assistant turn can be retried. */
    val canRetryLastMessage: StateFlow<Boolean> = _lastUserMessage
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** The currently active LLM provider. */
    val currentProvider: StateFlow<LlmProvider> = _currentProvider.asStateFlow()

    private val _selectedDetailMessageId = MutableStateFlow<String?>(null)

    /**
     * Message id currently surfaced in the detail area.
     *
     * For EXPANDED messages, this is what the user has pinned (or null → sticky-latest
     * fallback in two-pane mode). For SURVEY messages, the ViewModel auto-pins each newly
     * arrived survey once so the sheet pops up on arrival; users can re-open the card later
     * to re-surface it. Dismissal simply clears this, with no LLM turn.
     */
    val selectedDetailMessageId: StateFlow<String?> = _selectedDetailMessageId.asStateFlow()

    private val _windowWidthDp = MutableStateFlow(0)
    private val _windowHeightDp = MutableStateFlow(0)

    /** Whether the Claude provider is available (API key configured). */
    val isClaudeAvailable: Boolean = anthropicApiKey.isNotBlank()

    /** Whether the OpenAI provider is available (API key configured). */
    val isOpenAiAvailable: Boolean = openAiApiKey.isNotBlank()

    private val _selectedTheme = MutableStateFlow(DemoAuiTheme.DEFAULT)

    /** The currently selected AUI theme. */
    val selectedTheme: StateFlow<DemoAuiTheme> = _selectedTheme.asStateFlow()

    /** Whether verbose repository/extractor logs are enabled. */
    val chatDebugLogsEnabled: StateFlow<Boolean> = _chatDebugLogsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = appSettings.llmProvider.first()
            val provider = when {
                saved == LlmProvider.CLAUDE && !isClaudeAvailable -> LlmProvider.FAKE
                saved == LlmProvider.OPENAI && !isOpenAiAvailable -> LlmProvider.FAKE
                else -> saved
            }
            if (provider != _currentProvider.value) {
                repository = createRepository(provider)
                _currentProvider.value = provider
                _repositoryVersion.value++
            }
        }
        viewModelScope.launch {
            _selectedTheme.value = appSettings.selectedTheme.first()
        }
        viewModelScope.launch {
            appSettings.chatDebugLogsEnabled.collect { enabled ->
                _chatDebugLogsEnabled.value = enabled
            }
        }
    }

    /** Sends a user text message and waits for the LLM response. */
    fun send(text: String) {
        if (text.isBlank()) return
        _lastUserMessage.value = text
        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendUserMessage(conversationId, text, contextHints = buildContextHints())
            } finally {
                _isSending.value = false
            }
        }
    }

    /** Re-sends the last user-authored message after a failed assistant turn. */
    fun retryLastSend() {
        val lastMessage = _lastUserMessage.value ?: return
        if (_isSending.value) return
        send(lastMessage)
    }

    /** Converts AUI feedback to text and sends it as a user message. */
    fun onFeedback(feedback: AuiFeedback) {
        send(feedback.toUserMessageText())
    }

    /**
     * Records the latest measured window size from the screen. Called on every composition; the
     * value is read by [send] when dispatching the next user message so the AI sees current
     * device context.
     */
    fun updateWindowSize(widthDp: Int, heightDp: Int) {
        _windowWidthDp.value = widthDp
        _windowHeightDp.value = heightDp
    }

    /** Pins [messageId] as the active detail message, opening it in the sheet/detail pane. */
    fun openDetail(messageId: String) {
        _selectedDetailMessageId.value = messageId
    }

    /** Clears the user's detail-pane pin so the sticky-latest expanded message takes over. */
    fun closeDetail() {
        _selectedDetailMessageId.value = null
    }

    private fun buildContextHints(): String {
        val width = _windowWidthDp.value
        val height = _windowHeightDp.value
        if (width <= 0 || height <= 0) return ""
        val layout = if (width >= TWO_PANE_BREAKPOINT_DP && width >= height) "two_pane" else "single_column"
        return buildString {
            append("DEVICE: layout=").append(layout).append(".\n")
            append("Prefer \"inline\" for chat-flow messages; use \"expanded\" when the ")
            append("response is focused content the user may want to linger on.")
        }
    }

    /** Clears all messages in the current conversation. */
    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation(conversationId)
            autoOpenedSurveyIds.clear()
            _selectedDetailMessageId.value = null
            _lastUserMessage.value = null
        }
    }

    /**
     * Switches the AUI theme. The selection is persisted to [AppSettings].
     */
    fun switchTheme(theme: DemoAuiTheme) {
        if (theme == _selectedTheme.value) return
        _selectedTheme.value = theme
        viewModelScope.launch {
            appSettings.setSelectedTheme(theme)
        }
    }

    /**
     * Switches to a new [LlmProvider], clearing the conversation.
     *
     * No-op if the provider is already active or if switching to Claude
     * without an API key. The selection is persisted to [AppSettings].
     */
    fun switchProvider(provider: LlmProvider) {
        if (provider == _currentProvider.value) return
        if (!isProviderAvailable(provider)) return

        viewModelScope.launch {
            repository.clearConversation(conversationId)
            autoOpenedSurveyIds.clear()
            _selectedDetailMessageId.value = null
            _lastUserMessage.value = null
            repository = createRepository(provider)
            _currentProvider.value = provider
            _repositoryVersion.value++
            appSettings.setLlmProvider(provider)
        }
    }

    private fun createLlmClient(provider: LlmProvider): LlmClient = when (provider) {
        LlmProvider.FAKE -> FakeLlmClient()
        LlmProvider.CLAUDE -> ClaudeLlmClient(
            apiKey = anthropicApiKey,
            httpClient = httpClient,
        )
        LlmProvider.OPENAI -> OpenAiLlmClient(
            apiKey = openAiApiKey,
            httpClient = httpClient,
        )
    }

    private fun createRepository(provider: LlmProvider): ChatRepository =
        DefaultChatRepository(
            llmClient = createLlmClient(provider),
            dao = dao,
            systemPrompt = systemPrompt,
            loggingConfig = ChatDebugLoggerConfig { _chatDebugLogsEnabled.value },
        )

    private companion object {
        const val TWO_PANE_BREAKPOINT_DP = 600
    }

    private fun isProviderAvailable(provider: LlmProvider): Boolean = when (provider) {
        LlmProvider.FAKE -> true
        LlmProvider.CLAUDE -> isClaudeAvailable
        LlmProvider.OPENAI -> isOpenAiAvailable
    }
}
