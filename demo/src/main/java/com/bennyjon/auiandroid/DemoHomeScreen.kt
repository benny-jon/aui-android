package com.bennyjon.auiandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * Demo landing screen with live chat and themed showcase entry points.
 *
 * The live-chat card opens [com.bennyjon.auiandroid.livechat.LiveChatScreen] for
 * end-to-end LLM conversations. Theme cards launch [ChatScreen] with different
 * [com.bennyjon.aui.compose.theme.AuiTheme] instances.
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Live Chat with an LLM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ThemeCard(
                title = "Live Chat",
                subtitle = "End-to-end conversation with an LLM, persisted in Room",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onThemeSelected("live_chat") },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Themes Showcase (Fake chat)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val warmOrganicColors = DemoThemes.warmOrganic().colors
            ThemeCard(
                title = "Warm Organic",
                subtitle = "Earthy tones, serif headings, rounded corners",
                containerColor = warmOrganicColors.primaryContainer,
                contentColor = warmOrganicColors.onPrimaryContainer,
                onClick = { onThemeSelected("warm_organic") },
            )

            val earthyGreenColors = DemoThemes.earthyGreen().colors
            ThemeCard(
                title = "Earthy Green",
                subtitle = "Forest greens, clean sans-serif, softly rounded",
                containerColor = earthyGreenColors.primaryContainer,
                contentColor = earthyGreenColors.onPrimaryContainer,
                onClick = { onThemeSelected("earthy_green") },
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
