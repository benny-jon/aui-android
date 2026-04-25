package com.bennyjon.auiandroid.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bennyjon.auiandroid.data.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
) : ViewModel() {

    val chatDebugLogsEnabled: StateFlow<Boolean> = appSettings.chatDebugLogsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setChatDebugLogsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setChatDebugLogsEnabled(enabled)
        }
    }
}
