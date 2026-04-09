package com.bennyjon.auiandroid.plugins

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.plugin.AuiComponentPlugin
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class FunFactData(
    val title: String,
    val fact: String,
    val source: String? = null,
)

/**
 * Demo component plugin that renders a colorful fun-fact card.
 *
 * Demonstrates how host apps can add entirely new component types
 * via [AuiComponentPlugin]. The AI includes `demo_fun_fact` blocks
 * in its response, and this plugin renders them as styled cards.
 */
object DemoFunFactPlugin : AuiComponentPlugin<FunFactData>() {
    override val id = "demo_fun_fact"
    override val componentType = "demo_fun_fact"
    override val dataSerializer: KSerializer<FunFactData> = FunFactData.serializer()

    override val promptSchema =
        "demo_fun_fact(title, fact, source?) — A colorful fun-fact card with a title, fact text, and optional source attribution."

    @Composable
    override fun Render(
        data: FunFactData,
        onFeedback: (() -> Unit)?,
        modifier: Modifier,
    ) {
        val theme = LocalAuiTheme.current

        Card(
            onClick = { onFeedback?.invoke() },
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = theme.colors.primaryContainer,
                contentColor = theme.colors.onPrimaryContainer,
            ),
            shape = theme.shapes.card,
        ) {
            Column(modifier = Modifier.padding(theme.spacing.medium)) {
                Text(
                    text = data.title,
                    style = theme.typography.heading,
                    color = theme.colors.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(theme.spacing.small))
                Text(
                    text = data.fact,
                    style = theme.typography.body,
                    color = theme.colors.onPrimaryContainer,
                )
                data.source?.let { source ->
                    Spacer(modifier = Modifier.height(theme.spacing.small))
                    Text(
                        text = "Source: $source",
                        style = theme.typography.label,
                        fontStyle = FontStyle.Italic,
                        color = theme.colors.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
