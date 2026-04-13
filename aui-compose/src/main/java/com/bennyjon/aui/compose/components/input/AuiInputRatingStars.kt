package com.bennyjon.aui.compose.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.internal.LocalAuiValueRegistry
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.data.InputRatingStarsData

private const val STAR_COUNT = 5

/**
 * Renders an `input_rating_stars` block.
 *
 * Displays a row of 5 star icons. Tapping a star selects that rating; stars up to the
 * selected position are filled, the rest are outlined. When [block.feedback] is set,
 * [onFeedback] is called on each tap with the rating value added to params.
 */
@Composable
fun AuiInputRatingStars(
    block: AuiBlock.InputRatingStars,
    modifier: Modifier = Modifier,
    onFeedback: (AuiFeedback) -> Unit = {},
) {
    val theme = LocalAuiTheme.current
    val registry = LocalAuiValueRegistry.current
    var rating by remember { mutableIntStateOf(block.data.value ?: 0) }

    Column(modifier = modifier) {
        block.data.label?.let { label ->
            Text(
                text = label,
                style = theme.typography.label,
                color = LocalAuiCaptionColor.current,
                modifier = Modifier.padding(bottom = theme.spacing.xSmall),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
        ) {
            for (star in 1..STAR_COUNT) {
                val isFilled = star <= rating
                Icon(
                    imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star $star",
                    tint = if (isFilled) theme.colors.primary else theme.colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            rating = star
                            registry.value = registry.value + mapOf(block.data.key to star.toString(), "value" to star.toString())
                            block.feedback?.let { feedback ->
                                val updatedParams = feedback.params + mapOf(block.data.key to star.toString(), "value" to star.toString())
                                onFeedback(feedback.copy(params = updatedParams))
                            }
                        },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiInputRatingStarsPreview() {
    AuiThemeProvider {
        AuiInputRatingStars(
            block = AuiBlock.InputRatingStars(
                data = InputRatingStarsData(
                    key = "rating",
                    label = "Tap to rate",
                    value = 3,
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
