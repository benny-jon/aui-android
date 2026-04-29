package com.bennyjon.aui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale for an [AuiTheme].
 *
 * All AUI components read spacing values exclusively from this class via [LocalAuiTheme].
 * No component hardcodes a `Dp` value directly.
 */
@Immutable
data class AuiSpacing(
    /** Zero dp — convenient token for components that should not hardcode `0.dp`. */
    val zero: Dp = 0.dp,
    /** 2 dp — ultra-tight offsets, borders, and micro-gaps. */
    val xxSmall: Dp = 2.dp,
    /** 4 dp — tight gaps inside components. */
    val xSmall: Dp = 4.dp,
    /** 8 dp — gaps between related elements. */
    val small: Dp = 8.dp,
    /** 12 dp — medium-tight spacing for compact cards and block internals. */
    val smallMedium: Dp = 12.dp,
    /** 16 dp — standard content padding and gaps between components. */
    val medium: Dp = 16.dp,
    /** 24 dp — section-level separation. */
    val large: Dp = 24.dp,
    /** 32 dp — generous breathing room. */
    val xLarge: Dp = 32.dp,
    /** 48 dp - accessible minimum size for clickable elements */
    val minimumTouchTarget: Dp = 48.dp,
    /** Thickness of `divider` blocks. */
    val dividerThickness: Dp = 1.dp,
    /** Vertical gap between sibling blocks in a `blocks` array. */
    val blockSpacing: Dp = 12.dp,
    /** Additional top padding above `section_header` blocks, on top of [blockSpacing]. */
    val sectionHeaderTopSpacing: Dp = 8.dp,
    /** Default chart canvas height. */
    val chartCanvasHeight: Dp = 200.dp,
    /** Dot size for chart legend markers. */
    val chartLegendMarkerSize: Dp = 10.dp,
    /** Minimum width for table columns. */
    val tableMinColumnWidth: Dp = 72.dp,
    /** Maximum width for free-form text table columns. */
    val tableMaxTextColumnWidth: Dp = 220.dp,
    /** Maximum width for compact badge-style table columns. */
    val tableMaxCompactColumnWidth: Dp = 144.dp,
    /** Maximum width for numeric table columns. */
    val tableMaxNumberColumnWidth: Dp = 184.dp,
    /** Horizontal padding budget applied per table cell. */
    val tableCellHorizontalPadding: Dp = 32.dp,
    /** Additional horizontal padding budget reserved for badge pills in tables. */
    val tableBadgeHorizontalPadding: Dp = 32.dp,
    /** Estimated average body glyph width for conservative table sizing. */
    val tableEstimatedBodyCharWidth: Dp = 8.dp,
    /** Estimated average caption glyph width for conservative table sizing. */
    val tableEstimatedCaptionCharWidth: Dp = 7.dp,
    /** Estimated average numeric glyph width for conservative table sizing. */
    val tableEstimatedNumericCharWidth: Dp = 10.dp,
    /** Rating-star icon size inside read-only table cells. */
    val tableRatingStarSize: Dp = 16.dp,
    /** Gap between rating stars inside read-only table cells. */
    val tableRatingStarGap: Dp = 2.dp,
    /** Fixed width for read-only rating columns. */
    val tableRatingColumnWidth: Dp = 120.dp,
    /** Stepper indicator circle diameter. */
    val stepperIndicatorSize: Dp = 24.dp,
    /** Check/icon size used inside compact stepper indicators. */
    val stepperIndicatorIconSize: Dp = 14.dp,
) {
    companion object {
        /** Default spacing scale. */
        val Default: AuiSpacing = AuiSpacing()
    }
}
