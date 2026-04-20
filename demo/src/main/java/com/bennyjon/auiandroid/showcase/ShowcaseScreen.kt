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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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
    var activeDetailLabel by remember { mutableStateOf<String?>(null) }
    var sheetLabel by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Blocks Showcase") },
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
            val isTwoPane = maxWidth >= TwoPaneBreakpointDp && maxWidth >= maxHeight
            val detailPaneEntry = entries.firstOrNull { it.label == activeDetailLabel }
            val sheetEntry = entries.firstOrNull { it.label == sheetLabel }

            LaunchedEffect(isTwoPane, entries) {
                if (isTwoPane && detailPaneEntry == null) {
                    activeDetailLabel = entries.firstOrNull { it.response.display != AuiDisplay.SURVEY }?.label
                }
                if (!isTwoPane) {
                    activeDetailLabel = null
                }
            }

            val openEntry: (ShowcaseEntry) -> Unit = { entry ->
                if (isTwoPane && entry.response.display != AuiDisplay.SURVEY) {
                    activeDetailLabel = entry.label
                } else {
                    sheetLabel = entry.label
                }
            }

            if (isTwoPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    EntryList(
                        entries = entries,
                        auiTheme = auiTheme,
                        pluginRegistry = pluginRegistry,
                        activeLabel = detailPaneEntry?.label,
                        onInspectEntry = openEntry,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    VerticalDivider()
                    DetailPane(
                        entry = detailPaneEntry,
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
                    onInspectEntry = openEntry,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            sheetEntry?.let { entry ->
                ShowcaseDetailSheet(
                    entry = entry,
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
                            sheetLabel = null
                        }
                    },
                    onDismiss = { sheetLabel = null },
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
    onInspectEntry: (ShowcaseEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = entries.map { it.category }.distinct()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "showcase_overview") {
            ShowcaseOverview(entries = entries)
        }

        categories.forEach { category ->
            item(key = "category_$category") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(
                items = entries.filter { it.category == category },
                key = { it.label },
            ) { entry ->
                ShowcaseItem(
                    entry = entry,
                    auiTheme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    isActiveDetail = entry.label == activeLabel,
                    onInspectEntry = onInspectEntry,
                )
            }
        }
    }
}

@Composable
private fun ShowcaseOverview(entries: List<ShowcaseEntry>) {
    val displays = entries.map { it.response.display }.distinct()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Every built-in block, current display mode, and the demo plugin live here.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${entries.size} curated examples across ${displays.size} display modes. Inline entries render in place; use Preview & JSON to inspect the exact payload. Expanded examples open in the side pane on wide layouts and a bottom sheet on narrow layouts. Surveys stay modal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onInspectEntry: (ShowcaseEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleMedium,
            )
            DisplayPill(display = entry.response.display)
        }
        entry.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${entry.response.totalBlockCount()} block${if (entry.response.totalBlockCount() == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (entry.response.display) {
            AuiDisplay.EXPANDED, AuiDisplay.SURVEY -> {
                AuiResponseCard(
                    response = entry.response,
                    onClick = { onInspectEntry(entry) },
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
                TextButton(
                    onClick = { onInspectEntry(entry) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = if (isActiveDetail) "Showing Preview & JSON" else "Preview & JSON",
                    )
                }
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
    entry: ShowcaseEntry?,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (entry == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Select an example to inspect its rendered output and JSON here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Box
        }
        ShowcaseDetailContent(
            entry = entry,
            pluginRegistry = pluginRegistry,
            auiTheme = auiTheme,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            onFeedback = { Log.d("Showcase", "Feedback: ${it.action}") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowcaseDetailSheet(
    entry: ShowcaseEntry,
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
        ShowcaseDetailContent(
            entry = entry,
            pluginRegistry = pluginRegistry,
            auiTheme = auiTheme,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onFeedback = onFeedback,
        )
    }
}

@Composable
private fun ShowcaseDetailContent(
    entry: ShowcaseEntry,
    pluginRegistry: AuiPluginRegistry,
    auiTheme: AuiTheme,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = entry.label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DisplayPill(display = entry.response.display)
        Text(
            text = "Rendered Preview",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.response.cardTitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        entry.response.cardDescription?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AuiRenderer(
            response = entry.response,
            modifier = Modifier.fillMaxWidth(),
            theme = auiTheme,
            pluginRegistry = pluginRegistry,
            onFeedback = onFeedback,
        )
        Text(
            text = "Example JSON",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        JsonCodeBlock(json = entry.sourceJson)
    }
}

@Composable
private fun DisplayPill(display: AuiDisplay) {
    val label = when (display) {
        AuiDisplay.INLINE -> "INLINE"
        AuiDisplay.EXPANDED -> "EXPANDED"
        AuiDisplay.SURVEY -> "SURVEY"
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun JsonCodeBlock(json: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        SelectionContainer {
            Text(
                text = json,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

private fun AuiResponse.totalBlockCount(): Int = blocks.size + steps.sumOf { it.blocks.size }
