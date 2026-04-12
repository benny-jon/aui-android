package com.bennyjon.auiandroid

import android.content.Context
import androidx.room.Room
import com.bennyjon.aui.core.AuiCatalogPrompt
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatRepository
import com.bennyjon.auiandroid.data.chat.DefaultChatRepository
import com.bennyjon.auiandroid.data.chat.db.ChatDatabase
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.llm.LlmClient
import com.bennyjon.auiandroid.data.llm.LlmClientFactory
import com.bennyjon.auiandroid.data.llm.LlmProvider
import com.bennyjon.auiandroid.plugins.DemoPluginRegistry

/**
 * Simple object-based dependency injection for the demo app.
 *
 * Call [init] once from [MainActivity.onCreate] with the application context.
 * All dependencies are lazily created and shared across the app.
 *
 * Use [setProvider] to switch between LLM backends at runtime. Switching
 * providers rebuilds the [LlmClient] and [ChatRepository].
 */
object DemoServiceLocator {

    private lateinit var appContext: Context

    /** Initialize with the application context. Must be called before accessing any property. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Plugin registry shared across the app. */
    val pluginRegistry: AuiPluginRegistry by lazy {
        DemoPluginRegistry.create(appContext)
    }

    /** System prompt generated from the plugin registry. */
    val systemPrompt: String by lazy {
        AuiCatalogPrompt.generate(pluginRegistry)
    }

    private val database: ChatDatabase by lazy {
        Room.databaseBuilder(appContext, ChatDatabase::class.java, "aui_demo_chat.db")
            .build()
    }

    private val dao: ChatMessageDao by lazy {
        database.chatMessageDao()
    }

    /** The Anthropic API key from BuildConfig. Empty string if not configured. */
    val anthropicApiKey: String by lazy {
        BuildConfig.ANTHROPIC_API_KEY
    }

    /** Whether the Claude provider is available (API key is configured). */
    val isClaudeAvailable: Boolean
        get() = anthropicApiKey.isNotBlank()

    /** The currently active LLM provider. */
    var currentProvider: LlmProvider = LlmProvider.FAKE
        private set

    /** Current LLM client, rebuilt on provider switch. */
    var currentLlmClient: LlmClient = LlmClientFactory.create(LlmProvider.FAKE)
        private set

    /** Chat repository backed by Room and the current LLM client. */
    var chatRepository: ChatRepository = createRepository(currentLlmClient)
        private set

    /**
     * Switches to a new [LlmProvider], rebuilding the client and repository.
     *
     * The caller is responsible for clearing the conversation if desired —
     * typically done via [ChatRepository.clearConversation] before or after switching.
     */
    fun setProvider(provider: LlmProvider) {
        if (provider == currentProvider) return
        currentProvider = provider
        currentLlmClient = LlmClientFactory.create(
            provider = provider,
            anthropicApiKey = anthropicApiKey,
        )
        chatRepository = createRepository(currentLlmClient)
    }

    private fun createRepository(client: LlmClient): ChatRepository =
        DefaultChatRepository(
            llmClient = client,
            dao = dao,
            systemPrompt = systemPrompt,
        )
}
