package com.bennyjon.auiandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
}
