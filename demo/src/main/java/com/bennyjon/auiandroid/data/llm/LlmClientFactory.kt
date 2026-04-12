package com.bennyjon.auiandroid.data.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates [LlmClient] instances for a given [LlmProvider].
 */
object LlmClientFactory {

    private val httpClient: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    /**
     * Creates an [LlmClient] for the given [provider].
     *
     * @param provider The LLM provider to use.
     * @param anthropicApiKey API key for [LlmProvider.CLAUDE]. Ignored for other providers.
     * @return A configured [LlmClient] instance.
     */
    fun create(
        provider: LlmProvider,
        anthropicApiKey: String = "",
    ): LlmClient = when (provider) {
        LlmProvider.FAKE -> FakeLlmClient()
        LlmProvider.CLAUDE -> ClaudeLlmClient(
            apiKey = anthropicApiKey,
            httpClient = httpClient,
        )
    }
}
