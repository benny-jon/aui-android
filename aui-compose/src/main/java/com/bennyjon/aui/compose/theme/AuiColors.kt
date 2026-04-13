package com.bennyjon.aui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Color palette for an [AuiTheme].
 *
 * All AUI components read colors exclusively from this class via [LocalAuiTheme].
 * Use [AuiColors.Default] for Material3-inspired defaults, or [AuiColors.fromMaterialTheme]
 * to derive colors from the host app's active [MaterialTheme].
 */
data class AuiColors(
    /** Brand/interactive primary color. */
    val primary: Color = Color(0xFF6750A4),
    /** Content color on top of [primary]. */
    val onPrimary: Color = Color(0xFFFFFFFF),
    /** Tinted container surface associated with [primary]. */
    val primaryContainer: Color = Color(0xFFEADDFF),
    /** Content color on top of [primaryContainer]. */
    val onPrimaryContainer: Color = Color(0xFF21005D),
    /** Default surface/card background. */
    val surface: Color = Color(0xFFD4CEF3),
    /** Primary content color on [surface]. */
    val onSurface: Color = Color(0xFF1C1B1F),
    val surfaceVariant: Color = Color(0xFFA199BD),
    /** Secondary/muted content color on [surface] (captions, placeholders). */
    val onSurfaceVariant: Color = Color(0xFF49454F),
    /** Subtle border and divider color. */
    val outline: Color = Color(0xFF79747E),
    /** Success/confirmation foreground. */
    val success: Color = Color(0xFF386A20),
    /** Content color on top of [success]. */
    val onSuccess: Color = Color(0xFFFFFFFF),
    /** Tinted container surface for success states. */
    val successContainer: Color = Color(0xFFB7F397),
    /** Content color on top of [successContainer]. */
    val onSuccessContainer: Color = Color(0xFF072100),
    /**
     * Default color for heading text.
     *
     * Display contexts (inline bubble, expanded, sheet) may override this via
     * [LocalAuiHeadingColor][com.bennyjon.aui.compose.theme.LocalAuiHeadingColor]
     * to ensure proper contrast against their specific background surface.
     */
    val headingColor: Color = onSurface,
    /**
     * Default color for body text.
     *
     * Display contexts may override this via
     * [LocalAuiBodyColor][com.bennyjon.aui.compose.theme.LocalAuiBodyColor].
     */
    val bodyColor: Color = onSurface,
    /**
     * Default color for caption and label text.
     *
     * Display contexts may override this via
     * [LocalAuiCaptionColor][com.bennyjon.aui.compose.theme.LocalAuiCaptionColor].
     */
    val captionColor: Color = onSurfaceVariant,
) {
    companion object {
        /** Material3-inspired default color palette. */
        val Default: AuiColors = AuiColors()

        /**
         * Derives an [AuiColors] from the current [MaterialTheme] color scheme.
         *
         * Use this in [AuiTheme.fromMaterialTheme] to keep AUI visually consistent
         * with the host app's theme.
         */
        @Composable
        fun fromMaterialTheme(): AuiColors {
            val scheme = MaterialTheme.colorScheme
            return AuiColors(
                primary = scheme.primary,
                onPrimary = scheme.onPrimary,
                primaryContainer = scheme.primaryContainer,
                onPrimaryContainer = scheme.onPrimaryContainer,
                surface = scheme.surface,
                onSurface = scheme.onSurface,
                surfaceVariant = scheme.surfaceVariant,
                onSurfaceVariant = scheme.onSurfaceVariant,
                outline = scheme.outline,
                success = scheme.tertiary,
                onSuccess = scheme.onTertiary,
                successContainer = scheme.tertiaryContainer,
                onSuccessContainer = scheme.onTertiaryContainer,
                headingColor = scheme.onSurface,
                bodyColor = scheme.onSurface,
                captionColor = scheme.onSurfaceVariant,
            )
        }
    }
}
