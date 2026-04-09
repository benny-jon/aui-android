package com.bennyjon.auiandroid

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Full-screen chat UI demonstrating AUI library integration.
 *
 * Renders a scrollable list of [DemoMessage]s. AI messages with `auiJson` are rendered via
 * [AuiRenderer] using the raw JSON overload — the library parses JSON internally. User
 * interactions trigger [DemoViewModel.onAuiFeedback], which handles sheet consumption
 * (setting `auiJson = null`) and appending the feedback as a user bubble.
 *
 * @param viewModel Drives the demo chat sequence.
 * @param title Text shown in the top app bar.
 * @param auiTheme The AUI theme applied to all rendered components.
 * @param pluginRegistry Plugin registry passed to [AuiRenderer] for custom component
 *   rendering and action handling. Defaults to [AuiPluginRegistry.Empty].
 * @param onBack Called when the user taps the back arrow. Pass `null` to hide the arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: DemoViewModel,
    title: String = "AUI Demo",
    auiTheme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onBack: (() -> Unit)? = null,
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            ChatInput(onSend = viewModel::onUserText)
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = messages,
                key = { message ->
                    when (message) {
                        is DemoMessage.Ai -> message.id
                        is DemoMessage.User -> message.id
                    }
                },
            ) { message ->
                when (message) {
                    is DemoMessage.Ai -> AiMessageItem(
                        message = message,
                        auiTheme = auiTheme,
                        pluginRegistry = pluginRegistry,
                        onFeedback = { feedback ->
                            viewModel.onAuiFeedback(message.id, feedback)
                        },
                    )
                    is DemoMessage.User -> UserBubble(text = message.text)
                }
            }
        }
    }
}

/**
 * Renders an AI message: optional text bubble followed by AUI content.
 *
 * When [DemoMessage.Ai.auiJson] is `null` (consumed sheet or no AUI), only the text is shown.
 * When present, the raw JSON is passed to [AuiRenderer] which handles parsing and display
 * mode routing internally.
 */
@Composable
private fun AiMessageItem(
    message: DemoMessage.Ai,
    auiTheme: AuiTheme,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit,
) {
    // Text portion (if any)
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
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    // AUI portion (if any — null after sheet consumption)
    message.auiJson?.let { json ->
        AuiRenderer(
            json = json,
            modifier = Modifier.fillMaxWidth(),
            theme = auiTheme,
            pluginRegistry = pluginRegistry,
            onFeedback = onFeedback,
            onParseError = { error ->
                Log.w("AuiDemo", "AUI parse error: $error")
            },
        )
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
private fun ChatInput(onSend: (String) -> Unit) {
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
                        onSend(text)
                        text = ""
                    },
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            TextButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = text.isNotBlank(),
            ) {
                Text("Send")
            }
        }
    }
}

@Preview
@Composable
fun AiMessageItemPreview() {
    AuiThemeProvider {
        AiMessageItem(
            message = DemoMessage.Ai(text = "Hi there, how are you?"),
            auiTheme = AuiTheme.fromMaterialTheme(),
            onFeedback = { }
        )
    }
}