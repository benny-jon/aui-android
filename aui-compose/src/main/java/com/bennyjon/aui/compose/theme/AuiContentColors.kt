package com.bennyjon.aui.compose.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Content color for heading text in the current display context.
 *
 * Provided by [AuiThemeProvider] from [AuiColors.headingColor] and overridden by display
 * contexts (inline bubble, expanded, sheet) to match their background surface.
 * Read by [AuiHeading][com.bennyjon.aui.compose.components.text.AuiHeading].
 */
val LocalAuiHeadingColor = compositionLocalOf { Color.Black }

/**
 * Content color for body text in the current display context.
 *
 * Provided by [AuiThemeProvider] from [AuiColors.bodyColor] and overridden by display
 * contexts to match their background surface.
 * Read by [AuiText][com.bennyjon.aui.compose.components.text.AuiText] and input content text.
 */
val LocalAuiBodyColor = compositionLocalOf { Color.Black }

/**
 * Content color for caption and label text in the current display context.
 *
 * Provided by [AuiThemeProvider] from [AuiColors.captionColor] and overridden by display
 * contexts to match their background surface.
 * Read by [AuiCaption][com.bennyjon.aui.compose.components.text.AuiCaption] and input labels.
 */
val LocalAuiCaptionColor = compositionLocalOf { Color.Black }
