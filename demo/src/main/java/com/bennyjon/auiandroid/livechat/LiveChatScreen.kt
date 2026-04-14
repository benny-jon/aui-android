package com.bennyjon.auiandroid.livechat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.compose.text.parseInlineMarkdown
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatMessage
import com.bennyjon.auiandroid.data.llm.LlmProvider
import com.bennyjon.auiandroid.ui.theme.DemoThemes

/**
 * Available AUI themes in the demo app.
 *
 * Each entry maps to an [AuiTheme] instance used to wrap the [AuiRenderer].
 */
enum class DemoAuiTheme(val displayName: String) {
    DEFAULT("Default"),
    WARM_ORGANIC("Warm Organic"),
    EARTHY_GREEN("Earthy Green"),
}

/**
 * Full-screen chat UI for live conversations with an LLM.
 *
 * Renders [ChatMessage]s from [LiveChatViewModel.messages]. User messages appear as
 * right-aligned bubbles, assistant messages as left-aligned bubbles with optional
 * [AuiRenderer] content. Spent AUI responses are grayed out. A text input bar at
 * the bottom allows the user to send messages.
 *
 * The top app bar includes a theme dropdown for switching AUI themes, a provider
 * dropdown for switching between LLM backends, and a clear button.
 *
 * @param viewModel Drives the chat state.
 * @param pluginRegistry Plugin registry for AUI rendering and action handling.
 * @param onBack Called when the back arrow is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatScreen(
    viewModel: LiveChatViewModel,
    pluginRegistry: AuiPluginRegistry,
    theme: DemoAuiTheme = DemoAuiTheme.DEFAULT,
    onChangeTheme: (DemoAuiTheme) -> Unit,
    onBack: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val currentProvider by viewModel.currentProvider.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    ThemeDropdown(
                        currentTheme = theme,
                        onThemeSelected = onChangeTheme,
                    )
                    ProviderDropdown(
                        currentProvider = currentProvider,
                        isClaudeAvailable = viewModel.isClaudeAvailable,
                        onProviderSelected = viewModel::switchProvider,
                    )
                    TextButton(onClick = viewModel::clearConversation) {
                        Text("Clear")
                    }
                },
            )
        },
        bottomBar = {
            LiveChatInput(
                onSend = viewModel::send,
                isSending = isSending,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items = messages, key = { it.id }) { message ->
                when (message.role) {
                    ChatMessage.Role.USER -> UserBubble(text = message.text ?: "")
                    ChatMessage.Role.ASSISTANT -> AssistantMessage(
                        message = message,
                        pluginRegistry = pluginRegistry,
                        auiTheme = resolveAuiTheme(theme),
                        onFeedback = viewModel::onFeedback,
                    )
                }
            }

            if (isSending) {
                item(key = "sending_indicator") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dropdown button in the top bar for selecting the active [LlmProvider].
 *
 * Displays the current provider name. Claude is disabled if no API key is configured.
 */
@Composable
private fun ProviderDropdown(
    currentProvider: LlmProvider,
    isClaudeAvailable: Boolean,
    onProviderSelected: (LlmProvider) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentProvider.displayName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LlmProvider.entries.forEach { provider ->
                val enabled = when (provider) {
                    LlmProvider.CLAUDE -> isClaudeAvailable
                    else -> true
                }
                DropdownMenuItem(
                    text = {
                        val label = if (provider == LlmProvider.CLAUDE && !isClaudeAvailable) {
                            "${provider.displayName} (no key)"
                        } else {
                            provider.displayName
                        }
                        Text(label)
                    },
                    onClick = {
                        expanded = false
                        onProviderSelected(provider)
                    },
                    enabled = enabled,
                )
            }
        }
    }
}

/**
 * Resolves a [DemoAuiTheme] selection to the corresponding [AuiTheme] instance.
 */
@Composable
private fun resolveAuiTheme(theme: DemoAuiTheme): AuiTheme = when (theme) {
    DemoAuiTheme.DEFAULT -> AuiTheme.fromMaterialTheme()
    DemoAuiTheme.WARM_ORGANIC -> DemoThemes.warmOrganic()
    DemoAuiTheme.EARTHY_GREEN -> DemoThemes.earthyGreen()
}

/**
 * Dropdown button in the top bar for selecting the active [AuiTheme].
 *
 * Displays the current theme name. Tapping opens a menu with all [DemoAuiTheme] entries.
 */
@Composable
private fun ThemeDropdown(
    currentTheme: DemoAuiTheme,
    onThemeSelected: (DemoAuiTheme) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentTheme.displayName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DemoAuiTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.displayName) },
                    onClick = {
                        expanded = false
                        onThemeSelected(theme)
                    },
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(
    message: ChatMessage,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    onFeedback: (com.bennyjon.aui.core.model.AuiFeedback) -> Unit,
) {
    // Error banner
    message.errorMessage?.let { error ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    // Text bubble
    message.text?.let { text ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                val spannedText = parseInlineMarkdown(
                    source = text,
                    codeStyle = auiTheme.typography.code,
                    linkColor = auiTheme.colors.primary
                )
                Text(
                    text = spannedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    // AUI content
    message.auiResponse
        ?.takeIf { shouldRenderAui(it, message.isAuiSpent) }
        ?.let { response ->
            val spentAlpha = if (message.isAuiSpent) 0.6f else 1f
            Box(modifier = Modifier.alpha(spentAlpha)) {
                AuiRenderer(
                    response = response,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    theme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    onFeedback = { feedback ->
                        if (!message.isAuiSpent) {
                            onFeedback(feedback)
                        }
                    },
                )
            }
        }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun LiveChatInput(
    onSend: (String) -> Unit,
    isSending: Boolean,
) {
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && !isSending) {
                            onSend(text)
                            text = ""
                        }
                    },
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                enabled = !isSending,
            )
            TextButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = text.isNotBlank() && !isSending,
            ) {
                Text("Send")
            }
        }
    }
}

/**
 * Whether an AUI response should be rendered in the chat list.
 *
 * Spent sheet responses are hidden entirely (they've already been submitted and
 * re-rendering would re-open the sheet). Spent inline/expanded responses are
 * still shown (grayed out) so the user can see what was there.
 */
internal fun shouldRenderAui(response: AuiResponse, isSpent: Boolean): Boolean =
    !isSpent || response.display != AuiDisplay.SHEET
