package com.bennyjon.auiandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bennyjon.auiandroid.ui.theme.DemoThemes

/**
 * Demo landing screen with themed chat entry points.
 *
 * Each card launches the same [ChatScreen] with a different [com.bennyjon.aui.compose.theme.AuiTheme],
 * demonstrating that host apps can fully customise the AUI renderer's visual style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoHomeScreen(onThemeSelected: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AUI Demo") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Choose a theme to see how the same AUI content looks with different styles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ThemeCard(
                title = "Default Theme",
                subtitle = "Material 3 baseline — clean and familiar",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onThemeSelected("default") },
            )

            val darkNeonColors = DemoThemes.darkNeon().colors
            ThemeCard(
                title = "Dark Neon",
                subtitle = "Electric cyan accents with bold shapes",
                containerColor = darkNeonColors.primaryContainer,
                contentColor = darkNeonColors.onPrimaryContainer,
                onClick = { onThemeSelected("dark_neon") },
            )

            val warmOrganicColors = DemoThemes.warmOrganic().colors
            ThemeCard(
                title = "Warm Organic",
                subtitle = "Earthy tones, serif headings, rounded corners",
                containerColor = warmOrganicColors.primaryContainer,
                contentColor = warmOrganicColors.onPrimaryContainer,
                onClick = { onThemeSelected("warm_organic") },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Plugin System",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ThemeCard(
                title = "Plugin Showcase",
                subtitle = "Custom FunFact component + Navigate/OpenUrl actions",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onThemeSelected("plugins") },
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Complex Demos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemeCard(
                title = "All Components Showcase",
                subtitle = "Sheet flow using all available components and actions",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onThemeSelected("all_components") },
            )
        }
    }
}

@Composable
private fun ThemeCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}
