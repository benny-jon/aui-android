package com.bennyjon.aui.compose.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape set for an [AuiTheme].
 *
 * All AUI components read shapes exclusively from this class via [LocalAuiTheme].
 * Use [AuiShapes.Default] for Material3-inspired defaults, or [AuiShapes.fromMaterialTheme]
 * to derive shapes from the host app's active [MaterialTheme].
 */
data class AuiShapes(
    /** Fully rounded pill shape used for chips and quick-reply options. */
    val chip: Shape = CircleShape,
    /** Fully rounded pill shape used for buttons. */
    val button: Shape = CircleShape,
    /** Moderately rounded shape used for cards. */
    val card: Shape = RoundedCornerShape(12.dp),
    /** Fully rounded pill shape used for badge components. */
    val badge: Shape = CircleShape,
    /** Lightly rounded shape used for status banners. */
    val banner: Shape = RoundedCornerShape(8.dp),
) {
    companion object {
        /** Default shape set. */
        val Default: AuiShapes = AuiShapes()

        /**
         * Derives an [AuiShapes] from the current [MaterialTheme] shapes.
         *
         * Use this in [AuiTheme.fromMaterialTheme] to keep AUI visually consistent
         * with the host app's theme.
         */
        @Composable
        fun fromMaterialTheme(): AuiShapes {
            val s = MaterialTheme.shapes
            return AuiShapes(
                chip = CircleShape,
                button = CircleShape,
                card = s.medium,
                badge = CircleShape,
                banner = s.extraSmall,
            )
        }
    }
}
