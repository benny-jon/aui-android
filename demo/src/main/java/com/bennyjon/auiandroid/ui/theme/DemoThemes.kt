package com.bennyjon.auiandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bennyjon.aui.compose.theme.AuiColors
import com.bennyjon.aui.compose.theme.AuiShapes
import com.bennyjon.aui.compose.theme.AuiSpacing
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiTypography

/**
 * Demo-specific [AuiTheme] definitions showcasing host app customization.
 *
 * These themes live in the demo module (NOT the library) to demonstrate that host apps
 * can fully control the visual appearance of AUI components. Each theme provides both
 * light and dark color variants, selected automatically via [isSystemInDarkTheme].
 */
object DemoThemes {

    // ── Warm Organic ──────────────────────────────────────────────────

    private val WarmOrganicLightColors = AuiColors(
        primary = Color(0xFF8D6E63),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7CCC8),
        onPrimaryContainer = Color(0xFF3E2723),
        surface = Color(0xFFFFF8E1),
        onSurface = Color(0xFF3E2723),
        onSurfaceVariant = Color(0xFF6D4C41),
        outline = Color(0xFFBCAAA4),
        success = Color(0xFF558B2F),
        onSuccess = Color.White,
        successContainer = Color(0xFFDCEDC8),
        onSuccessContainer = Color(0xFF1B5E20),
        headingColor = Color(0xFF3E2723),
        bodyColor = Color(0xFF3E2723),
        captionColor = Color(0xFF6D4C41),
    )

    private val WarmOrganicDarkColors = AuiColors(
        primary = Color(0xFFBCAAA4),
        onPrimary = Color(0xFF2E1F14),
        primaryContainer = Color(0xFF5D4037),
        onPrimaryContainer = Color(0xFFD7CCC8),
        surface = Color(0xFF2E1F14),
        onSurface = Color(0xFFEFEBE9),
        onSurfaceVariant = Color(0xFFBCAAA4),
        outline = Color(0xFF5D4037),
        success = Color(0xFF81C784),
        onSuccess = Color(0xFF1B3A2A),
        successContainer = Color(0xFF2E4A30),
        onSuccessContainer = Color(0xFFA5D6A7),
        headingColor = Color(0xFFEFEBE9),
        bodyColor = Color(0xFFEFEBE9),
        captionColor = Color(0xFFBCAAA4),
    )

    private val WarmOrganicTypography = AuiTypography(
        heading = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        subheading = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
        ),
        body = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        ),
        caption = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        label = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        button = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
    )

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

    private val EarthyGreenLightColors = AuiColors(
        primary = Color(0xFF4A7C59),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF1B3A1E),
        surface = Color(0xFFF1F8E9),
        onSurface = Color(0xFF1B3A1E),
        onSurfaceVariant = Color(0xFF4E6B52),
        outline = Color(0xFFA5C4AA),
        success = Color(0xFF2E7D32),
        onSuccess = Color.White,
        successContainer = Color(0xFFA5D6A7),
        onSuccessContainer = Color(0xFF1B5E20),
        headingColor = Color(0xFF1B3A1E),
        bodyColor = Color(0xFF2E3D30),
        captionColor = Color(0xFF4E6B52),
    )

    private val EarthyGreenDarkColors = AuiColors(
        primary = Color(0xFF81C784),
        onPrimary = Color(0xFF0D2610),
        primaryContainer = Color(0xFF2E5733),
        onPrimaryContainer = Color(0xFFC8E6C9),
        surface = Color(0xFF0D2610),
        onSurface = Color(0xFFE8F5E9),
        onSurfaceVariant = Color(0xFF81C784),
        outline = Color(0xFF2E5733),
        success = Color(0xFF66BB6A),
        onSuccess = Color(0xFF0D2610),
        successContainer = Color(0xFF1B5E20),
        onSuccessContainer = Color(0xFFA5D6A7),
        headingColor = Color(0xFFE8F5E9),
        bodyColor = Color(0xFFE8F5E9),
        captionColor = Color(0xFF81C784),
    )

    private val EarthyGreenTypography = AuiTypography(
        heading = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        subheading = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
        ),
        body = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        ),
        caption = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        label = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        button = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
    )

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
