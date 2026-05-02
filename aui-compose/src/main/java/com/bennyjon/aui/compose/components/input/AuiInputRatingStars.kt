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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.LocalAuiRenderState
import com.bennyjon.aui.compose.theme.LocalAuiCaptionColor
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback

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
    val renderState = LocalAuiRenderState.current
    val rating = renderState.uiState(block.data.key)?.toIntOrNull() ?: (block.data.value ?: 0)

    val rate: (Int) -> Unit = { star ->
        renderState.setInputState(
            key = block.data.key,
            value = star.toString(),
        )
        renderState.setValue("value", star.toString())
        block.feedback?.let { feedback ->
            val updatedParams = feedback.params + mapOf(block.data.key to star.toString(), "value" to star.toString())
            onFeedback(feedback.copy(params = updatedParams))
        }
    }

    val groupAnnounce = if (rating > 0) {
        stringResource(R.string.aui_input_rating_announce_rated, rating, STAR_COUNT)
    } else {
        stringResource(R.string.aui_input_rating_announce_unrated, STAR_COUNT)
    }
    val rateActions = (1..STAR_COUNT).map { star ->
        CustomAccessibilityAction(
            label = pluralStringResource(R.plurals.aui_input_rating_action_rate, star, star),
            action = { rate(star); true },
        )
    }

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
            modifier = Modifier.clearAndSetSemantics {
                role = Role.Button
                contentDescription = groupAnnounce
                customActions = rateActions
            },
        ) {
            for (star in 1..STAR_COUNT) {
                val isFilled = star <= rating
                Icon(
                    imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (isFilled) theme.colors.primary else theme.colors.primaryContainer,
                    modifier = Modifier
                        .size(theme.spacing.minimumTouchTarget)
                        .clickable { rate(star) },
                )
            }
        }
    }
}
