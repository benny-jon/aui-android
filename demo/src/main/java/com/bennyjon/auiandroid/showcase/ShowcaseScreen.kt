package com.bennyjon.auiandroid.showcase

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.livechat.DemoAuiTheme
import com.bennyjon.auiandroid.ui.ThemeDropdown

/**
 * Scrollable showcase screen that renders every AUI component type.
 *
 * Inline and expanded entries are rendered directly. Sheet entries render as tappable
 * preview cards that open the bottom sheet when clicked.
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(entries, key = { it.label }) { entry ->
                ShowcaseItem(
                    entry = entry,
                    auiTheme = auiTheme,
                    pluginRegistry = pluginRegistry,
                )
            }
        }
    }
}

@Composable
private fun ShowcaseItem(
    entry: ShowcaseEntry,
    auiTheme: AuiTheme,
    pluginRegistry: AuiPluginRegistry,
) {
    var activeSheetJson by remember { mutableStateOf<String?>(null) }

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

        if (entry.isSheet) {
            Card(
                onClick = { activeSheetJson = entry.auiJson },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "Tap to preview sheet",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            activeSheetJson?.let { json ->
                AuiRenderer(
                    json = json,
                    theme = auiTheme,
                    pluginRegistry = pluginRegistry,
                    onFeedback = {
                        Log.d("Showcase", "Sheet feedback: ${it.action}")
                        activeSheetJson = null
                    },
                )
            }
        } else {
            AuiRenderer(
                json = entry.auiJson,
                theme = auiTheme,
                pluginRegistry = pluginRegistry,
                onFeedback = { Log.d("Showcase", "Feedback: ${it.action}") },
            )
        }
    }
}
