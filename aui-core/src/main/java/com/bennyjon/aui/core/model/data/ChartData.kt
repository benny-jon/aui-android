package com.bennyjon.aui.core.model.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data for the `chart` block.
 *
 * Drives a display-only visualization with one of three variants:
 * [ChartVariant.Bar], [ChartVariant.Line], or [ChartVariant.Pie]. The renderer
 * derives axes, scales, and legends from [series] — the AI never supplies layout
 * metadata.
 *
 * For [ChartVariant.Bar] and [ChartVariant.Line] all series must share the same
 * `x` labels in the same order. For [ChartVariant.Pie] each slice is a single
 * series with exactly one `{x, y}` point; the renderer computes percentages
 * from the raw `y` totals.
 */
@Serializable
data class ChartData(
    /** Chart variant — controls the drawing pipeline. */
    val variant: ChartVariant,
    /** Optional title rendered above the chart. */
    val title: String? = null,
    /** Optional x-axis label. Ignored for [ChartVariant.Pie]. */
    @SerialName("x_label") val xLabel: String? = null,
    /** Optional y-axis label. Ignored for [ChartVariant.Pie]. */
    @SerialName("y_label") val yLabel: String? = null,
    /** Data series. Must contain at least one entry. */
    val series: List<ChartSeries>,
)

/** Discriminator for [ChartData.variant]. */
@Serializable
enum class ChartVariant {
    @SerialName("bar") Bar,
    @SerialName("line") Line,
    @SerialName("pie") Pie,
}

/** A named series of [ChartPoint] values. */
@Serializable
data class ChartSeries(
    /** Legend label for this series (or slice, for pie). */
    val label: String,
    /** Data points. Empty series are tolerated but render nothing. */
    val values: List<ChartPoint>,
)

/** A single `(x, y)` data point inside a [ChartSeries]. */
@Serializable
data class ChartPoint(
    /** Category label on the x-axis, or slice label for pie. */
    val x: String,
    /** Numeric value on the y-axis, or raw slice weight for pie. */
    val y: Float,
)
