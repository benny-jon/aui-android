package com.bennyjon.aui.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * The complete theme for AUI components.
 *
 * Holds [colors], [typography], [spacing], and [shapes]. Provide it to the composition via
 * [AuiThemeProvider]. Components access the active theme through [LocalAuiTheme].
 *
 * Example:
 * ```kotlin
 * AuiThemeProvider(theme = AuiTheme.fromMaterialTheme()) {
 *     AuiRenderer(response = response, onFeedback = onFeedback)
 * }
 * ```
 */
data class AuiTheme(
    val colors: AuiColors = AuiColors.Default,
    val typography: AuiTypography = AuiTypography.Default,
    val spacing: AuiSpacing = AuiSpacing.Default,
    val shapes: AuiShapes = AuiShapes.Default,
) {
    companion object {
        /** Material3-inspired default theme. */
        val Default: AuiTheme = AuiTheme()

        /**
         * Creates an [AuiTheme] derived from the current [androidx.compose.material3.MaterialTheme].
         *
         * Use this to keep AUI components visually consistent with the host app's design system.
         * Must be called inside a composition that has a [androidx.compose.material3.MaterialTheme] ancestor.
         */
        @Composable
        fun fromMaterialTheme(): AuiTheme = AuiTheme(
            colors = AuiColors.fromMaterialTheme(),
            typography = AuiTypography.fromMaterialTheme(),
            spacing = AuiSpacing.Default,
            shapes = AuiShapes.fromMaterialTheme(),
        )
    }
}

/**
 * [CompositionLocal] holding the current [AuiTheme].
 *
 * Access the active theme in any composable via `LocalAuiTheme.current`.
 * Defaults to [AuiTheme.Default] if no [AuiThemeProvider] ancestor is present.
 */
val LocalAuiTheme = compositionLocalOf { AuiTheme.Default }

/**
 * Provides [theme] to all AUI composables in [content].
 *
 * Wrap your [com.bennyjon.aui.compose.AuiRenderer] (or any AUI component) inside this
 * to apply a custom theme.
 *
 * @param theme The theme to apply. Defaults to [AuiTheme.Default].
 * @param content Composable content that will receive the theme.
 */
@Composable
fun AuiThemeProvider(
    theme: AuiTheme = AuiTheme.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAuiTheme provides theme,
        LocalAuiHeadingColor provides theme.colors.headingColor,
        LocalAuiBodyColor provides theme.colors.bodyColor,
        LocalAuiCaptionColor provides theme.colors.captionColor,
    ) {
        content()
    }
}
