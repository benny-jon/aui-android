package com.bennyjon.auiandroid.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * can fully control the visual appearance of AUI components.
 */
object DemoThemes {

    /** Dark backgrounds with electric cyan/magenta accents. */
    val DarkNeon: AuiTheme = AuiTheme(
        colors = AuiColors(
            primary = Color(0xFF00E5FF),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF004D56),
            onPrimaryContainer = Color(0xFF00E5FF),
            surface = Color(0xFF1A1A2E),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB0BEC5),
            outline = Color(0xFF333355),
            success = Color(0xFF69F0AE),
            onSuccess = Color.Black,
            successContainer = Color(0xFF1B3A2A),
            onSuccessContainer = Color(0xFF69F0AE),
        ),
        typography = AuiTypography(
            heading = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
            body = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
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
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        ),
        spacing = AuiSpacing.Default,
        shapes = AuiShapes(
            chip = CircleShape,
            button = RoundedCornerShape(8.dp),
            card = RoundedCornerShape(8.dp),
            badge = CircleShape,
            banner = RoundedCornerShape(4.dp),
        ),
    )

    /** Earthy tones with serif headings and extra-rounded corners. */
    val WarmOrganic: AuiTheme = AuiTheme(
        colors = AuiColors(
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
        ),
        typography = AuiTypography(
            heading = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
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
        ),
        spacing = AuiSpacing.Default,
        shapes = AuiShapes(
            chip = CircleShape,
            button = RoundedCornerShape(20.dp),
            card = RoundedCornerShape(20.dp),
            badge = CircleShape,
            banner = RoundedCornerShape(16.dp),
        ),
    )
}
