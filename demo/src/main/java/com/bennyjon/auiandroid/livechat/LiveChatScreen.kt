package com.bennyjon.auiandroid.livechat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.compose.display.AuiResponseCard
import com.bennyjon.aui.compose.text.parseInlineMarkdown
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatMessage
import com.bennyjon.auiandroid.data.llm.LlmProvider
import com.bennyjon.auiandroid.ui.ThemeDropdown
import com.bennyjon.auiandroid.ui.theme.DemoThemes

/** Width breakpoint at which the chat splits into a chat list + side detail pane. */
private val TwoPaneBreakpointDp = 600.dp

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
 * Routes assistant messages by their AUI [AuiDisplay] level:
 * - **INLINE** renders directly in the chat list via [AuiRenderer].
 * - **EXPANDED** renders as a tappable [AuiResponseCard] stub. On narrow windows the stub
 *   opens a [ModalBottomSheet] containing the full render; on wide windows
 *   ([TwoPaneBreakpointDp]+), the full render is shown in a persistent right-side detail
 *   pane and the stub stays visible in chat for navigation.
 * - **SURVEY** renders as a tappable [AuiResponseCard] stub and the library content is
 *   hosted in a [ModalBottomSheet] regardless of window size (surveys are modal input
 *   collection). The sheet auto-opens on arrival; swipe-down dismissal simply closes it —
 *   the card stays in the chat so the user can re-open the survey on tap. Only the Submit
 *   action generates an LLM turn.
 *
 * The screen pushes the current window size to the [LiveChatViewModel] on every composition
 * so the AI sees a `DEVICE` hint with width/height/layout in its system prompt for the next
 * message.
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
    val selectedDetailMessageId by viewModel.selectedDetailMessageId.collectAsState()
    val hasExpandedMessage = messages.any { it.auiResponse?.display == AuiDisplay.EXPANDED }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isLandscape = screenWidthDp >= screenHeightDp
    val isTwoPane = screenWidthDp >= TwoPaneBreakpointDp && isLandscape && hasExpandedMessage
    val singlePaneLandscapeInset by animateDpAsState(
        targetValue = if (!isTwoPane && isLandscape) screenWidthDp / 4 else 0.dp,
        label = "live_chat_single_pane_inset",
    )

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
                        isOpenAiAvailable = viewModel.isOpenAiAvailable,
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
                horizontalInset = singlePaneLandscapeInset,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val widthDp = maxWidth
            val heightDp = maxHeight
            LaunchedEffect(widthDp, heightDp) {
                viewModel.updateWindowSize(widthDp.value.toInt(), heightDp.value.toInt())
            }
            val auiTheme = resolveAuiTheme(theme)

            val pinnedMessage = messages.firstOrNull { it.id == selectedDetailMessageId }
            val pinnedIsSurvey = pinnedMessage?.auiResponse?.display == AuiDisplay.SURVEY

            // In two-pane mode, the detail pane shows the user-pinned EXPANDED message if
            // one is set, otherwise the latest EXPANDED message (sticky-latest behavior).
            // Surveys are always modal — they never occupy the detail pane.
            val detailPaneMessage = if (isTwoPane && !pinnedIsSurvey) {
                pinnedMessage?.takeIf { it.auiResponse?.display == AuiDisplay.EXPANDED }
                    ?: messages.lastOrNull { it.auiResponse?.display == AuiDisplay.EXPANDED }
            } else {
                null
            }

            // Sheet hosts a pinned SURVEY at any width, or a pinned EXPANDED on narrow windows.
            val sheetMessage = when {
                pinnedMessage == null -> null
                pinnedIsSurvey -> pinnedMessage
                !isTwoPane -> pinnedMessage
                else -> null
            }

            if (isTwoPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    ChatList(
                        messages = messages,
                        isSending = isSending,
                        pluginRegistry = pluginRegistry,
                        auiTheme = auiTheme,
                        onFeedback = viewModel::onFeedback,
                        onOpenDetail = viewModel::openDetail,
                        activeDetailMessageId = detailPaneMessage?.id,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    VerticalDivider()
                    DetailPane(
                        message = detailPaneMessage,
                        pluginRegistry = pluginRegistry,
                        auiTheme = auiTheme,
                        onFeedback = viewModel::onFeedback,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                ChatList(
                    messages = messages,
                    isSending = isSending,
                    pluginRegistry = pluginRegistry,
                    auiTheme = auiTheme,
                    onFeedback = viewModel::onFeedback,
                    onOpenDetail = viewModel::openDetail,
                    activeDetailMessageId = null,
                    horizontalInset = singlePaneLandscapeInset,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (sheetMessage?.auiResponse != null) {
                val messageIsSurvey = sheetMessage.auiResponse.display == AuiDisplay.SURVEY
                ResponseDetailSheet(
                    message = sheetMessage,
                    pluginRegistry = pluginRegistry,
                    auiTheme = auiTheme,
                    onFeedback = { feedback ->
                        viewModel.onFeedback(feedback)
                        // Survey step-level feedbacks (non-submit) keep the sheet open so the
                        // user can continue answering. Submit is structurally marked by
                        // stepsTotal; other display types close the sheet on any feedback.
                        if (!messageIsSurvey || feedback.stepsTotal != null) {
                            viewModel.closeDetail()
                        }
                    },
                    onDismiss = viewModel::closeDetail,
                )
            }
        }
    }
}

@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    isSending: Boolean,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    onFeedback: (AuiFeedback) -> Unit,
    onOpenDetail: (String) -> Unit,
    activeDetailMessageId: String?,
    horizontalInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp + horizontalInset,
            top = 8.dp,
            end = 16.dp + horizontalInset,
            bottom = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            when (message.role) {
                ChatMessage.Role.USER -> UserBubble(text = message.text ?: "")
                ChatMessage.Role.ASSISTANT -> AssistantMessage(
                    message = message,
                    pluginRegistry = pluginRegistry,
                    auiTheme = auiTheme,
                    onFeedback = onFeedback,
                    onOpenDetail = onOpenDetail,
                    isActiveDetail = message.id == activeDetailMessageId,
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

/**
 * Right-side detail pane shown in two-pane mode. Renders the full AUI of the active
 * EXPANDED message (user-pinned or sticky-latest), with feedback wired to the parent.
 */
@Composable
private fun DetailPane(
    message: ChatMessage?,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    onFeedback: (AuiFeedback) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val response = message?.auiResponse
        if (message == null || response == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Detail content will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            response.cardTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            response.cardDescription?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            AuiRenderer(
                response = response,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                theme = auiTheme,
                pluginRegistry = pluginRegistry,
                onFeedback = onFeedback,
                collectingFeedbackEnabled = !message.isAuiSpent,
            )
        }
    }
}

/**
 * Modal bottom sheet wrapping a single [AuiRenderer] for a pinned response.
 *
 * Used by both EXPANDED responses (on narrow windows) and SURVEY responses (at any width).
 * For SURVEY, the sheet stays open across step navigation and closes only on the consolidated
 * Submit feedback — routing handled by the caller via [onFeedback].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponseDetailSheet(
    message: ChatMessage,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    onFeedback: (AuiFeedback) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BackHandler(enabled = true, onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            message.auiResponse?.cardTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            message.auiResponse?.cardDescription?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            message.auiResponse?.let { response ->
                AuiRenderer(
                    response = response,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    theme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    onFeedback = onFeedback,
                    collectingFeedbackEnabled = !message.isAuiSpent,
                )
            }
        }
    }
}

/**
 * Dropdown button in the top bar for selecting the active [LlmProvider].
 *
 * Displays the current provider name. Providers that need keys are disabled when
 * their API key is not configured.
 */
@Composable
private fun ProviderDropdown(
    currentProvider: LlmProvider,
    isClaudeAvailable: Boolean,
    isOpenAiAvailable: Boolean,
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
                    LlmProvider.OPENAI -> isOpenAiAvailable
                    else -> true
                }
                DropdownMenuItem(
                    text = {
                        val label = when {
                            provider == LlmProvider.CLAUDE && !isClaudeAvailable ->
                                "${provider.displayName} (no key)"
                            provider == LlmProvider.OPENAI && !isOpenAiAvailable ->
                                "${provider.displayName} (no key)"
                            else -> provider.displayName
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

@Composable
private fun AssistantMessage(
    message: ChatMessage,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    onFeedback: (AuiFeedback) -> Unit,
    onOpenDetail: (String) -> Unit,
    isActiveDetail: Boolean,
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
    message.text?.takeIf { it.isNotBlank() }?.let { text ->
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

    // AUI content — branch on display level
    val response = message.auiResponse ?: return
    when (response.display) {
        AuiDisplay.EXPANDED, AuiDisplay.SURVEY -> {
            AuiResponseCard(
                response = response,
                onClick = { onOpenDetail(message.id) },
                modifier = Modifier.padding(top = 16.dp),
                theme = auiTheme,
                isActive = isActiveDetail,
            )
        }
        AuiDisplay.INLINE -> {
            AuiRenderer(
                response = response,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                theme = auiTheme,
                pluginRegistry = pluginRegistry,
                onFeedback = onFeedback,
                collectingFeedbackEnabled = !message.isAuiSpent,
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
    horizontalInset: Dp = 0.dp,
) {
    var text by rememberSaveable { mutableStateOf("") }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Surface(
        modifier = Modifier.padding(
            start = insets.calculateStartPadding(layoutDirection) + horizontalInset,
            bottom = insets.calculateBottomPadding(),
            end = insets.calculateEndPadding(layoutDirection) + horizontalInset
        ),
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
