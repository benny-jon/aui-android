package com.bennyjon.auiandroid.di

import android.content.Context
import androidx.room.Room
import com.bennyjon.aui.core.AuiCatalogPrompt
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.BuildConfig
import com.bennyjon.auiandroid.data.chat.db.ChatDatabase
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.plugins.DemoPluginRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnthropicApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SystemPrompt

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePluginRegistry(@ApplicationContext context: Context): AuiPluginRegistry =
        DemoPluginRegistry.create(context)

    @Provides
    @Singleton
    @SystemPrompt
    fun provideSystemPrompt(pluginRegistry: AuiPluginRegistry): String =
        AuiCatalogPrompt.generate(pluginRegistry)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, "aui_demo_chat.db")
            .build()

    @Provides
    @Singleton
    fun provideDao(database: ChatDatabase): ChatMessageDao =
        database.chatMessageDao()

    @Provides
    @Singleton
    @AnthropicApiKey
    fun provideAnthropicApiKey(): String = BuildConfig.ANTHROPIC_API_KEY

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
}
