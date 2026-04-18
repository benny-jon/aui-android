package com.bennyjon.auiandroid.showcase

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.compose.display.AuiResponseCard
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.livechat.DemoAuiTheme
import com.bennyjon.auiandroid.ui.ThemeDropdown

/** Width breakpoint at which the showcase splits into a list + side detail pane. */
private val TwoPaneBreakpointDp = 600.dp

/**
 * Scrollable showcase screen that renders every AUI component type.
 *
 * INLINE entries render directly. EXPANDED and SURVEY entries render as a tappable
 * [AuiResponseCard] stub. Routing of an active card mirrors `LiveChatScreen`:
 * - On narrow windows (< [TwoPaneBreakpointDp]), tap opens a [ModalBottomSheet].
 * - On wide windows, EXPANDED responses occupy a persistent right-side detail pane while
 *   SURVEY responses always open in a modal sheet (surveys are modal input collection).
 *
 * @param viewModel The [ShowcaseViewModel] that provides the parsed showcase entries.
 * @param pluginRegistry Plugin registry passed to each [AuiRenderer] instance.
 * @param onBack Called when the user taps the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseScreen(
    viewModel: ShowcaseViewModel,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    selectedThemeName: DemoAuiTheme = DemoAuiTheme.DEFAULT,
    onChangeTheme: (DemoAuiTheme) -> Unit,
    onBack: () -> Unit,
) {
    val entries by viewModel.entries.collectAsState()
    var activeLabel by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Blocks") },
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
                        currentTheme = selectedThemeName,
                        onThemeSelected = onChangeTheme,
                    )
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val isTwoPane = maxWidth >= TwoPaneBreakpointDp
            val activeEntry = entries.firstOrNull { it.label == activeLabel }
            val activeResponse = activeEntry?.response
            val activeIsSurvey = activeResponse?.display == AuiDisplay.SURVEY

            // Detail pane only hosts EXPANDED responses on wide windows. Surveys always go
            // through the sheet regardless of width.
            val detailPaneEntry = if (isTwoPane && activeResponse?.display == AuiDisplay.EXPANDED) {
                activeEntry
            } else {
                null
            }

            // Sheet hosts surveys at any width, or expanded on narrow windows.
            val sheetEntry = when {
                activeEntry == null -> null
                activeIsSurvey -> activeEntry
                !isTwoPane -> activeEntry
                else -> null
            }

            if (isTwoPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    EntryList(
                        entries = entries,
                        auiTheme = auiTheme,
                        pluginRegistry = pluginRegistry,
                        activeLabel = detailPaneEntry?.label,
                        onOpenCard = { activeLabel = it.label },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    VerticalDivider()
                    DetailPane(
                        response = detailPaneEntry?.response,
                        pluginRegistry = pluginRegistry,
                        auiTheme = auiTheme,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                EntryList(
                    entries = entries,
                    auiTheme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    activeLabel = null,
                    onOpenCard = { activeLabel = it.label },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            sheetEntry?.let { entry ->
                ShowcaseDetailSheet(
                    response = entry.response,
                    pluginRegistry = pluginRegistry,
                    auiTheme = auiTheme,
                    onFeedback = { feedback ->
                        Log.d("Showcase", "Feedback: ${feedback.action}")
                        // Survey step-level feedbacks (non-submit) keep the sheet open so the
                        // user can continue answering. Submit is structurally marked by
                        // stepsTotal; other display types close the sheet on any feedback.
                        if (entry.response.display != AuiDisplay.SURVEY ||
                            feedback.stepsTotal != null
                        ) {
                            activeLabel = null
                        }
                    },
                    onDismiss = { activeLabel = null },
                )
            }
        }
    }
}

@Composable
private fun EntryList(
    entries: List<ShowcaseEntry>,
    auiTheme: AuiTheme,
    pluginRegistry: AuiPluginRegistry,
    activeLabel: String?,
    onOpenCard: (ShowcaseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(entries, key = { it.label }) { entry ->
            ShowcaseItem(
                entry = entry,
                auiTheme = auiTheme,
                pluginRegistry = pluginRegistry,
                isActiveDetail = entry.label == activeLabel,
                onOpenCard = onOpenCard,
            )
        }
    }
}

@Composable
private fun ShowcaseItem(
    entry: ShowcaseEntry,
    auiTheme: AuiTheme,
    pluginRegistry: AuiPluginRegistry,
    isActiveDetail: Boolean,
    onOpenCard: (ShowcaseEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = entry.label,
            style = MaterialTheme.typography.titleMedium,
        )
        entry.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (entry.response.display) {
            AuiDisplay.EXPANDED, AuiDisplay.SURVEY -> {
                AuiResponseCard(
                    response = entry.response,
                    onClick = { onOpenCard(entry) },
                    theme = auiTheme,
                    isActive = isActiveDetail,
                )
            }
            AuiDisplay.INLINE -> {
                AuiRenderer(
                    response = entry.response,
                    theme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    onFeedback = { Log.d("Showcase", "Feedback: ${it.action}") },
                )
            }
        }
    }
}

/**
 * Right-side detail pane shown in two-pane mode. Renders the full AUI of the active EXPANDED
 * entry, or a placeholder when nothing is selected.
 */
@Composable
private fun DetailPane(
    response: AuiResponse?,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (response == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Tap an expanded card to preview it here.",
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
                onFeedback = { Log.d("Showcase", "Feedback: ${it.action}") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseDetailSheet(
    response: AuiResponse,
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
                    .padding(top = 16.dp, bottom = 24.dp),
                theme = auiTheme,
                pluginRegistry = pluginRegistry,
                onFeedback = onFeedback,
            )
        }
    }
}
