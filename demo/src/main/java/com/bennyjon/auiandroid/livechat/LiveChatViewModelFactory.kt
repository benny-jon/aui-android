package com.bennyjon.auiandroid.livechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bennyjon.auiandroid.DemoServiceLocator

/**
 * Factory that wires [LiveChatViewModel] from [DemoServiceLocator].
 *
 * @param conversationId The conversation ID to use for this chat session.
 */
class LiveChatViewModelFactory(
    private val conversationId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LiveChatViewModel(
            repository = DemoServiceLocator.chatRepository,
            conversationId = conversationId,
            pluginRegistry = DemoServiceLocator.pluginRegistry,
        ) as T
}
