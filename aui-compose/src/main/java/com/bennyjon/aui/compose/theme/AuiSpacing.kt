package com.bennyjon.aui.compose.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale for an [AuiTheme].
 *
 * All AUI components read spacing values exclusively from this class via [LocalAuiTheme].
 * No component hardcodes a `Dp` value directly.
 */
data class AuiSpacing(
    /** 4 dp — tight gaps inside components. */
    val xSmall: Dp = 4.dp,
    /** 8 dp — gaps between related elements. */
    val small: Dp = 8.dp,
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
) {
    companion object {
        /** Default spacing scale. */
        val Default: AuiSpacing = AuiSpacing()
    }
}
