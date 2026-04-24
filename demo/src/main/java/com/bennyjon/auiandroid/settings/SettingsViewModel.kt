package com.bennyjon.auiandroid.settings

import androidx.lifecycle.ViewModel
import com.bennyjon.auiandroid.di.SystemPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:SystemPrompt val generatedPrompt: String,
) : ViewModel()
