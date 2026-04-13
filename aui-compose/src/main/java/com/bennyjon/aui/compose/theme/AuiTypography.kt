package com.bennyjon.aui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography scale for an [AuiTheme].
 *
 * All AUI components read text styles exclusively from this class via [LocalAuiTheme].
 * Use [AuiTypography.Default] for Material3-inspired defaults, or [AuiTypography.fromMaterialTheme]
 * to derive styles from the host app's active [MaterialTheme].
 */
data class AuiTypography(
    /** Bold section headings (`heading` block). */
    val heading: TextStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    /** Secondary headings and step questions. */
    val subheading: TextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    /** Body / plain text (`text` block). */
    val body: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    /** Small muted metadata (`caption` block). */
    val caption: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    /** Small labels used inside components (chip text, slider labels). */
    val label: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    /** Inline code spans (monospace). */
    val code: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    /** Button labels. */
    val button: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
) {
    companion object {
        /** Material3-inspired default typography scale. */
        val Default: AuiTypography = AuiTypography()

        /**
         * Derives an [AuiTypography] from the current [MaterialTheme] typography.
         *
         * Use this in [AuiTheme.fromMaterialTheme] to keep AUI visually consistent
         * with the host app's theme.
         */
        @Composable
        fun fromMaterialTheme(): AuiTypography {
            val t = MaterialTheme.typography
            return AuiTypography(
                heading = t.headlineSmall,
                subheading = t.titleMedium,
                body = t.bodyMedium,
                code = t.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                caption = t.bodySmall,
                label = t.labelSmall,
                button = t.labelLarge,
            )
        }
    }
}
