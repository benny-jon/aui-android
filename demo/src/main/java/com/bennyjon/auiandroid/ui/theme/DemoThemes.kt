package com.bennyjon.auiandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.theme.AuiColors
import com.bennyjon.aui.compose.theme.AuiShapes
import com.bennyjon.aui.compose.theme.AuiSpacing
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiTypography
import com.bennyjon.auiandroid.ui.theme.green.GreenDarkScheme
import com.bennyjon.auiandroid.ui.theme.green.GreenLightScheme
import com.bennyjon.auiandroid.ui.theme.green.GreenTheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmDarkScheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmLightScheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmTheme

/**
 * Demo-specific [AuiTheme] definitions showcasing host app customization.
 *
 * These themes live in the demo module (NOT the library) to demonstrate that host apps
 * can fully control the visual appearance of AUI components. Each theme provides both
 * light and dark color variants, selected automatically via [isSystemInDarkTheme].
 */
object DemoThemes {

    // ── Warm Organic ──────────────────────────────────────────────────

    private val WarmOrganicLightColors = AuiColors.fromColorScheme(WarmLightScheme)
    private val WarmOrganicDarkColors = AuiColors.fromColorScheme(WarmDarkScheme)
    private val WarmOrganicTypography = AuiTypography.fromTypography(WarmTheme.WarmTypography)

    private val WarmOrganicShapes = AuiShapes(
        chip = CircleShape,
        button = RoundedCornerShape(20.dp),
        card = RoundedCornerShape(20.dp),
        badge = CircleShape,
        banner = RoundedCornerShape(16.dp),
    )

    /** Earthy tones with serif headings — adapts background to system dark/light mode. */
    @Composable
    fun warmOrganic(isDark: Boolean = isSystemInDarkTheme()): AuiTheme = AuiTheme(
        colors = if (isDark) WarmOrganicDarkColors else WarmOrganicLightColors,
        typography = WarmOrganicTypography,
        spacing = AuiSpacing.Default,
        shapes = WarmOrganicShapes,
    )

    // ── Earthy Green ─────────────────────────────────────────────────

    private val EarthyGreenLightColors = AuiColors.fromColorScheme(GreenLightScheme)
    private val EarthyGreenDarkColors = AuiColors.fromColorScheme(GreenDarkScheme)
    private val EarthyGreenTypography = AuiTypography.fromTypography(GreenTheme.GreenTypography)

    private val EarthyGreenShapes = AuiShapes(
        chip = RoundedCornerShape(12.dp),
        button = RoundedCornerShape(12.dp),
        card = RoundedCornerShape(16.dp),
        badge = RoundedCornerShape(8.dp),
        banner = RoundedCornerShape(12.dp),
    )

    /** Forest greens with sans-serif type — adapts background to system dark/light mode. */
    @Composable
    fun earthyGreen(isDark: Boolean = isSystemInDarkTheme()): AuiTheme = AuiTheme(
        colors = if (isDark) EarthyGreenDarkColors else EarthyGreenLightColors,
        typography = EarthyGreenTypography,
        spacing = AuiSpacing.Default,
        shapes = EarthyGreenShapes,
    )
}

/**
 * Maps [AuiColors] fields onto a Material 3 [ColorScheme], falling back to [base]
 * for any colors that [AuiColors] does not define (e.g. secondary, tertiary, error).
 *
 * This allows the entire screen — scaffold, bubbles, input bar — to reflect the
 * selected AUI theme, not just the AUI renderer blocks.
 */
fun AuiColors.toMaterialColorScheme(base: ColorScheme): ColorScheme = base.copy(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    surface = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    surfaceVariant = primaryContainer,
    background = surface,
    onBackground = onSurface,
)
