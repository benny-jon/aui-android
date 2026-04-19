package com.bennyjon.aui.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Color palette for an [AuiTheme].
 *
 * All AUI components read colors exclusively from this class via [LocalAuiTheme].
 * Use [AuiColors.Default] for Material3-inspired defaults, or [AuiColors.fromMaterialTheme]
 * to derive colors from the host app's active [MaterialTheme].
 *
 * The four semantic severity quads ([info], [success], [warning], [error] plus
 * `on*` / `*Container` / `on*Container`) are treated as semantic — the library picks
 * stable light or dark values independent of the host's brand palette so severity is
 * consistent across themes. Hosts can still override any individual token.
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
    /** Secondary brand/accent color sourced from Material's tertiary role. */
    val tertiary: Color = Color(0xFF7D5260),
    /** Content color on top of [tertiary]. */
    val onTertiary: Color = Color(0xFFFFFFFF),
    /** Tinted container surface associated with [tertiary]. */
    val tertiaryContainer: Color = Color(0xFFFFD8E4),
    /** Content color on top of [tertiaryContainer]. */
    val onTertiaryContainer: Color = Color(0xFF31111D),
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
    /** Info/neutral-emphasis foreground. */
    val info: Color = Color(0xFF1F6FEB),
    /** Content color on top of [info]. */
    val onInfo: Color = Color(0xFFFFFFFF),
    /** Tinted container surface for info states. */
    val infoContainer: Color = Color(0xFFD6E4FF),
    /** Content color on top of [infoContainer]. */
    val onInfoContainer: Color = Color(0xFF001A41),
    /** Warning/caution foreground. Defaults to a yellow-amber tuned for the container. */
    val warning: Color = Color(0xFF6B5200),
    /** Content color on top of [warning]. */
    val onWarning: Color = Color(0xFFFFFFFF),
    /** Tinted container surface for warning states. Defaults to Material Amber 200. */
    val warningContainer: Color = Color(0xFFFFE082),
    /** Content color on top of [warningContainer]. */
    val onWarningContainer: Color = Color(0xFF221B00),
    /** Error/destructive foreground. */
    val error: Color = Color(0xFFBA1A1A),
    /** Content color on top of [error]. */
    val onError: Color = Color(0xFFFFFFFF),
    /** Tinted container surface for error states. */
    val errorContainer: Color = Color(0xFFFFDAD6),
    /** Content color on top of [errorContainer]. */
    val onErrorContainer: Color = Color(0xFF410002),
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
        /** Material3-inspired default color palette tuned for light surfaces. */
        val Default: AuiColors = AuiColors()

        /**
         * Default palette tuned for dark surfaces.
         *
         * Swaps the severity quads (info/success/warning/error) to dark-mode-appropriate
         * tonal spots where the container is deep-saturated and the on-container text is
         * a light pastel — mirroring the Material 3 container pattern for dark schemes.
         */
        val DefaultDark: AuiColors = AuiColors(
            success = Color(0xFF9CD67D),
            onSuccess = Color(0xFF0A3900),
            successContainer = Color(0xFF1F5000),
            onSuccessContainer = Color(0xFFB7F397),
            info = Color(0xFFA6C8FF),
            onInfo = Color(0xFF002E69),
            infoContainer = Color(0xFF00458C),
            onInfoContainer = Color(0xFFD6E4FF),
            warning = Color(0xFFEBC343),
            onWarning = Color(0xFF3A2D00),
            warningContainer = Color(0xFF524100),
            onWarningContainer = Color(0xFFFFE082),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )

        /**
         * Derives an [AuiColors] from the current [MaterialTheme] color scheme.
         *
         * Use this in [AuiTheme.fromMaterialTheme] to keep AUI visually consistent
         * with the host app's theme.
         */
        @Composable
        fun fromMaterialTheme() = fromColorScheme(MaterialTheme.colorScheme)

        /**
         * Derives an [AuiColors] from a color scheme.
         *
         * Brand colors (primary, tertiary, surface, outline) come from [scheme]. Severity quads
         * (info/success/warning/error) are always sourced from the library's stable
         * semantic palettes so meaning is preserved across host themes: yellow stays
         * yellow, red stays red. The library picks [Default] or [DefaultDark] severities
         * by inspecting [scheme]'s surface luminance.
         */
        fun fromColorScheme(scheme: ColorScheme): AuiColors {
            val isDark = scheme.surface.luminance() < 0.5f
            val severity = if (isDark) DefaultDark else Default
            return AuiColors(
                primary = scheme.primary,
                onPrimary = scheme.onPrimary,
                primaryContainer = scheme.primaryContainer,
                onPrimaryContainer = scheme.onPrimaryContainer,
                tertiary = scheme.tertiary,
                onTertiary = scheme.onTertiary,
                tertiaryContainer = scheme.tertiaryContainer,
                onTertiaryContainer = scheme.onTertiaryContainer,
                surface = scheme.surface,
                onSurface = scheme.onSurface,
                surfaceVariant = scheme.surfaceVariant,
                onSurfaceVariant = scheme.onSurfaceVariant,
                outline = scheme.outline,
                success = severity.success,
                onSuccess = severity.onSuccess,
                successContainer = severity.successContainer,
                onSuccessContainer = severity.onSuccessContainer,
                info = severity.info,
                onInfo = severity.onInfo,
                infoContainer = severity.infoContainer,
                onInfoContainer = severity.onInfoContainer,
                warning = severity.warning,
                onWarning = severity.onWarning,
                warningContainer = severity.warningContainer,
                onWarningContainer = severity.onWarningContainer,
                error = severity.error,
                onError = severity.onError,
                errorContainer = severity.errorContainer,
                onErrorContainer = severity.onErrorContainer,
                headingColor = scheme.onSurface,
                bodyColor = scheme.onSurface,
                captionColor = scheme.onSurfaceVariant,
            )
        }
    }
}
