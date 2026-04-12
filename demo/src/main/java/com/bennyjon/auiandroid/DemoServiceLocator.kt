package com.bennyjon.auiandroid

import android.content.Context
import androidx.room.Room
import com.bennyjon.aui.core.AuiCatalogPrompt
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatRepository
import com.bennyjon.auiandroid.data.chat.DefaultChatRepository
import com.bennyjon.auiandroid.data.chat.db.ChatDatabase
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.llm.FakeLlmClient
import com.bennyjon.auiandroid.data.llm.LlmClient
import com.bennyjon.auiandroid.plugins.DemoPluginRegistry

/**
 * Simple object-based dependency injection for the demo app.
 *
 * Call [init] once from [MainActivity.onCreate] with the application context.
 * All dependencies are lazily created and shared across the app.
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

    /** Current LLM client. Defaults to [FakeLlmClient]. */
    val currentLlmClient: LlmClient by lazy {
        FakeLlmClient()
    }

    /** Chat repository backed by Room and the current LLM client. */
    val chatRepository: ChatRepository by lazy {
        DefaultChatRepository(
            llmClient = currentLlmClient,
            dao = dao,
            systemPrompt = systemPrompt,
        )
    }
}
