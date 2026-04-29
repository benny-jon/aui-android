package com.bennyjon.aui.compose.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.StepperStep

/**
 * Renders a `stepper_horizontal` block.
 *
 * Displays a horizontal row of step indicators with connecting lines. Completed steps show
 * a checkmark, the current step is highlighted in primary color, and upcoming steps are muted.
 */
@Composable
fun AuiStepperHorizontal(
    block: AuiBlock.StepperHorizontal,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val steps = block.data.steps
    val current = block.data.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index < current
            val isCurrent = index == current

            val activeColor = theme.colors.primary
            val inactiveColor = theme.colors.outline
            val leftLineColor = if (index <= current) activeColor else inactiveColor
            val rightLineColor = if (index < current) activeColor else inactiveColor

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Circle row with half-connector lines on each side
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left half-connector
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(theme.spacing.dividerThickness)
                                .background(leftLineColor),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    // Step circle
                    Box(
                        modifier = Modifier
                            .size(theme.spacing.stepperIndicatorSize)
                            .background(
                                color = if (isCompleted || isCurrent) activeColor else theme.colors.surface,
                                shape = theme.shapes.badge,
                            )
                            .border(
                                width = if (isCompleted || isCurrent) theme.spacing.zero else theme.spacing.dividerThickness,
                                color = if (isCompleted || isCurrent) activeColor else inactiveColor,
                                shape = theme.shapes.badge,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = theme.colors.onPrimary,
                                modifier = Modifier.size(theme.spacing.stepperIndicatorIconSize),
                            )
                        } else {
                            Text(
                                text = (index + 1).toString(),
                                style = theme.typography.label,
                                color = if (isCurrent) theme.colors.onPrimary else theme.colors.onSurfaceVariant,
                            )
                        }
                    }

                    // Right half-connector
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(theme.spacing.dividerThickness)
                                .background(rightLineColor),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(theme.spacing.xSmall))

                // Step label
                Text(
                    text = step.label,
                    style = theme.typography.caption,
                    color = if (isCurrent) activeColor else theme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuiStepperHorizontalPreview() {
    AuiThemeProvider {
        AuiStepperHorizontal(
            block = AuiBlock.StepperHorizontal(
                data = StepperHorizontalData(
                    steps = listOf(
                        StepperStep("Experience"),
                        StepperStep("Features"),
                        StepperStep("Feedback"),
                    ),
                    current = 1,
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
