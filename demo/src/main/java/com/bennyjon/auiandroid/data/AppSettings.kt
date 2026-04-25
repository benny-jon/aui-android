package com.bennyjon.auiandroid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bennyjon.auiandroid.data.llm.LlmProvider
import com.bennyjon.auiandroid.livechat.DemoAuiTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists user-facing settings via DataStore Preferences.
 *
 * Currently stores the selected [LlmProvider] so the demo app
 * remembers the user's choice across restarts.
 */
@Singleton
class AppSettings @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** Emits the persisted [LlmProvider], defaulting to [LlmProvider.FAKE]. */
    val llmProvider: Flow<LlmProvider> = dataStore.data.map { prefs ->
        val name = prefs[KEY_LLM_PROVIDER]
        LlmProvider.entries.firstOrNull { it.name == name } ?: LlmProvider.FAKE
    }

    /** Persists the selected [LlmProvider]. */
    suspend fun setLlmProvider(provider: LlmProvider) {
        dataStore.edit { prefs ->
            prefs[KEY_LLM_PROVIDER] = provider.name
        }
    }

    /** Emits the persisted [DemoAuiTheme], defaulting to [DemoAuiTheme.DEFAULT]. */
    val selectedTheme: Flow<DemoAuiTheme> = dataStore.data.map { prefs ->
        val name = prefs[KEY_SELECTED_THEME]
        DemoAuiTheme.entries.firstOrNull { it.name == name } ?: DemoAuiTheme.DEFAULT
    }

    /** Persists the selected [DemoAuiTheme]. */
    suspend fun setSelectedTheme(theme: DemoAuiTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_THEME] = theme.name
        }
    }

    /** Emits whether verbose chat repository/extractor logs are enabled. */
    val chatDebugLogsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CHAT_DEBUG_LOGS_ENABLED] ?: false
    }

    /** Persists whether verbose chat repository/extractor logs are enabled. */
    suspend fun setChatDebugLogsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CHAT_DEBUG_LOGS_ENABLED] = enabled
        }
    }

    private companion object {
        val KEY_LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme")
        val KEY_CHAT_DEBUG_LOGS_ENABLED = booleanPreferencesKey("chat_debug_logs_enabled")
    }
}
