package com.bennyjon.aui.compose.components.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.bennyjon.aui.compose.theme.AuiTheme
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.data.ChartData
import com.bennyjon.aui.core.model.data.ChartPoint
import com.bennyjon.aui.core.model.data.ChartSeries
import com.bennyjon.aui.core.model.data.ChartVariant
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Default on-canvas chart height. Callers may override via [Modifier]. */
private val ChartCanvasHeight = 200.dp

/**
 * Renders a `chart` block as a native Canvas drawing.
 *
 * Supports [ChartVariant.Bar], [ChartVariant.Line], and [ChartVariant.Pie]. Axis labels and
 * the legend render above/below the drawing area; the drawing area itself is a single
 * [Canvas] that handles axis ticks, grid lines, and rotated y-axis text.
 */
@Composable
internal fun AuiChart(
    data: ChartData,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    val textMeasurer = rememberTextMeasurer()
    val palette = remember(theme) { seriesPalette(theme) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
    ) {
        data.title?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                text = title,
                style = theme.typography.subheading,
                color = theme.colors.headingColor,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartCanvasHeight),
        ) {
            when (data.variant) {
                ChartVariant.Bar -> drawBarOrLineChart(
                    data = data,
                    palette = palette,
                    theme = theme,
                    textMeasurer = textMeasurer,
                    isLine = false,
                )
                ChartVariant.Line -> drawBarOrLineChart(
                    data = data,
                    palette = palette,
                    theme = theme,
                    textMeasurer = textMeasurer,
                    isLine = true,
                )
                ChartVariant.Pie -> drawPieChart(
                    data = data,
                    palette = palette,
                    theme = theme,
                    textMeasurer = textMeasurer,
                )
            }
        }

        if (data.variant != ChartVariant.Pie) {
            data.xLabel?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    style = theme.typography.caption,
                    color = theme.colors.captionColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ChartLegend(
            data = data,
            palette = palette,
        )
    }
}

// ── Legend ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(
    data: ChartData,
    palette: List<Color>,
) {
    val theme = LocalAuiTheme.current
    val items = legendItems(data)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.xSmall),
    ) {
        items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette[index % palette.size]),
                )
                Text(
                    text = item,
                    modifier = Modifier.padding(start = theme.spacing.xSmall),
                    style = theme.typography.caption,
                    color = theme.colors.bodyColor,
                )
            }
        }
    }
}

private fun legendItems(data: ChartData): List<String> {
    if (data.variant != ChartVariant.Pie) {
        return data.series.map { it.label }
    }
    val total = data.series.sumOf { series ->
        series.values.sumOf { it.y.toDouble() }
    }.toFloat()
    if (total <= 0f) return data.series.map { it.label }
    return data.series.map { series ->
        val value = series.values.sumOf { it.y.toDouble() }.toFloat()
        val pct = value / total * 100f
        "${series.label} ${"%.0f".format(pct)}%"
    }
}

// ── Bar + Line chart ────────────────────────────────────────────────────

private fun DrawScope.drawBarOrLineChart(
    data: ChartData,
    palette: List<Color>,
    theme: AuiTheme,
    textMeasurer: TextMeasurer,
    isLine: Boolean,
) {
    val series = data.series.filter { it.values.isNotEmpty() }
    if (series.isEmpty()) return

    val xLabels = series.first().values.map { it.x }
    if (xLabels.isEmpty()) return

    val axisColor = theme.colors.outline.copy(alpha = 0.38f)
    val captionStyle = theme.typography.caption.copy(color = theme.colors.captionColor)

    val maxY = series.maxOf { s -> s.values.maxOf { it.y } }.coerceAtLeast(0f)
    val (niceMax, step) = niceAxisRange(if (maxY <= 0f) 1f else maxY, targetTicks = 4)
    val tickValues = generateSequence(0f) { prev -> (prev + step).takeIf { it <= niceMax + step * 0.0001f } }
        .toList()

    // Gutters: space reserved for y-labels, x-labels, rotated y_label.
    val yLabelWidths = tickValues.map { value ->
        textMeasurer.measure(formatTick(value), captionStyle).size.width.toFloat()
    }
    val yLabelMaxWidth = (yLabelWidths.maxOrNull() ?: 0f)
    val yRotatedLabelWidth = if (!data.yLabel.isNullOrBlank()) {
        textMeasurer.measure(data.yLabel!!, captionStyle).size.height.toFloat() + 4.dp.toPx()
    } else 0f
    val xLabelHeight = textMeasurer.measure("X", captionStyle).size.height.toFloat()

    val leftGutter = yRotatedLabelWidth + yLabelMaxWidth + 6.dp.toPx()
    val rightGutter = 8.dp.toPx()
    val topGutter = 4.dp.toPx()
    val bottomGutter = xLabelHeight + 6.dp.toPx()

    val plotLeft = leftGutter
    val plotRight = size.width - rightGutter
    val plotTop = topGutter
    val plotBottom = size.height - bottomGutter
    val plotWidth = plotRight - plotLeft
    val plotHeight = plotBottom - plotTop
    if (plotWidth <= 0f || plotHeight <= 0f) return

    // Horizontal grid + y tick labels.
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    tickValues.forEach { value ->
        val y = plotBottom - (value / niceMax) * plotHeight
        drawLine(
            color = axisColor,
            start = Offset(plotLeft, y),
            end = Offset(plotRight, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash.takeIf { value > 0f },
        )
        val label = formatTick(value)
        val measured = textMeasurer.measure(label, captionStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = plotLeft - 4.dp.toPx() - measured.size.width,
                y = y - measured.size.height / 2f,
            ),
        )
    }

    // Rotated y_label.
    if (!data.yLabel.isNullOrBlank()) {
        val measured = textMeasurer.measure(data.yLabel!!, captionStyle)
        val centerY = plotTop + plotHeight / 2f
        rotate(degrees = -90f, pivot = Offset(measured.size.height.toFloat() / 2f + 2.dp.toPx(), centerY)) {
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = 2.dp.toPx() + measured.size.height.toFloat() / 2f - measured.size.width / 2f,
                    y = centerY - measured.size.height / 2f,
                ),
            )
        }
    }

    // X-axis line.
    drawLine(
        color = axisColor,
        start = Offset(plotLeft, plotBottom),
        end = Offset(plotRight, plotBottom),
        strokeWidth = 1.dp.toPx(),
    )

    val groupCount = xLabels.size
    val groupWidth = plotWidth / groupCount

    // X labels.
    xLabels.forEachIndexed { i, label ->
        val centerX = plotLeft + groupWidth * (i + 0.5f)
        val measured = textMeasurer.measure(
            text = label,
            style = captionStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = groupWidth.toInt().coerceAtLeast(1)),
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = centerX - measured.size.width / 2f,
                y = plotBottom + 4.dp.toPx(),
            ),
        )
    }

    if (isLine) {
        drawLineSeries(series, palette, plotLeft, plotTop, plotRight, plotBottom, niceMax, groupWidth)
    } else {
        drawBarSeries(series, palette, plotLeft, plotTop, plotBottom, niceMax, groupWidth)
    }
}

private fun DrawScope.drawBarSeries(
    series: List<ChartSeries>,
    palette: List<Color>,
    plotLeft: Float,
    plotTop: Float,
    plotBottom: Float,
    niceMax: Float,
    groupWidth: Float,
) {
    val plotHeight = plotBottom - plotTop
    val seriesCount = series.size
    val groupPadding = groupWidth * 0.2f
    val barsArea = groupWidth - groupPadding
    val innerGap = if (seriesCount > 1) 2.dp.toPx() else 0f
    val barWidth = ((barsArea - innerGap * (seriesCount - 1)) / seriesCount).coerceAtLeast(1f)
    val radius = 4.dp.toPx()

    val pointCount = series.first().values.size
    for (groupIndex in 0 until pointCount) {
        val groupLeft = plotLeft + groupWidth * groupIndex + groupPadding / 2f
        series.forEachIndexed { seriesIndex, s ->
            val point = s.values.getOrNull(groupIndex) ?: return@forEachIndexed
            val color = palette[seriesIndex % palette.size]
            val barLeft = groupLeft + seriesIndex * (barWidth + innerGap)
            val barHeight = (point.y.coerceAtLeast(0f) / niceMax) * plotHeight
            val barTop = plotBottom - barHeight
            val path = roundedTopRect(
                left = barLeft,
                top = barTop,
                right = barLeft + barWidth,
                bottom = plotBottom,
                radius = radius,
            )
            drawPath(path = path, color = color)
        }
    }
}

private fun DrawScope.drawLineSeries(
    series: List<ChartSeries>,
    palette: List<Color>,
    plotLeft: Float,
    plotTop: Float,
    plotRight: Float,
    plotBottom: Float,
    niceMax: Float,
    groupWidth: Float,
) {
    val plotHeight = plotBottom - plotTop
    val pointRadius = 4.dp.toPx()
    series.forEachIndexed { seriesIndex, s ->
        val color = palette[seriesIndex % palette.size]
        val points = s.values.mapIndexed { i, point ->
            Offset(
                x = plotLeft + groupWidth * (i + 0.5f),
                y = plotBottom - (point.y.coerceAtLeast(0f) / niceMax) * plotHeight,
            )
        }
        if (points.isEmpty()) return@forEachIndexed

        // Filled area under the line.
        val fillPath = Path().apply {
            moveTo(points.first().x, plotBottom)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, plotBottom)
            close()
        }
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0f)),
                startY = plotTop,
                endY = plotBottom,
            ),
        )

        // Line stroke.
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Data point dots.
        points.forEach { p ->
            drawCircle(color = color, radius = pointRadius, center = p)
        }
    }
}

private fun roundedTopRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
): Path {
    val r = radius.coerceAtMost((right - left) / 2f).coerceAtMost((bottom - top))
    return Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + r)
        if (r > 0f) {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = left,
                    top = top,
                    right = left + 2 * r,
                    bottom = top + 2 * r,
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        }
        lineTo(right - r, top)
        if (r > 0f) {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = right - 2 * r,
                    top = top,
                    right = right,
                    bottom = top + 2 * r,
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        }
        lineTo(right, bottom)
        close()
    }
}

// ── Pie chart ───────────────────────────────────────────────────────────

private fun DrawScope.drawPieChart(
    data: ChartData,
    palette: List<Color>,
    theme: AuiTheme,
    textMeasurer: TextMeasurer,
) {
    val slices = data.series.mapNotNull { s ->
        val value = s.values.sumOf { it.y.toDouble() }.toFloat()
        if (value <= 0f) null else s.label to value
    }
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0f) return

    val diameter = minOf(size.width, size.height) - 8.dp.toPx()
    if (diameter <= 0f) return
    val topLeft = Offset(
        x = (size.width - diameter) / 2f,
        y = (size.height - diameter) / 2f,
    )
    val arcSize = Size(diameter, diameter)
    val radius = diameter / 2f

    // Session plan: gap of 2.dp along circumference, expressed as an angle.
    val gapDegrees = (2.dp.toPx() / radius.coerceAtLeast(1f)) * (180f / PI.toFloat())

    var startAngle = -90f
    slices.forEachIndexed { index, (_, value) ->
        val sweep = value / total * 360f
        val drawSweep = (sweep - gapDegrees).coerceAtLeast(0f)
        val offset = (sweep - drawSweep) / 2f
        drawArc(
            color = palette[index % palette.size],
            startAngle = startAngle + offset,
            sweepAngle = drawSweep,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
        startAngle += sweep
    }
}

// ── Palette + formatting ────────────────────────────────────────────────

private fun seriesPalette(theme: AuiTheme): List<Color> {
    val primary = theme.colors.primary
    val secondary = Color(0xFF009688) // Teal 500 — stable on light + dark surfaces.
    val warm = Color(0xFFFFB300) // Amber 600.
    return listOf(
        primary,
        secondary,
        warm,
        primary.copy(alpha = 0.6f),
        secondary.copy(alpha = 0.6f),
        warm.copy(alpha = 0.6f),
    )
}

private fun niceAxisRange(maxValue: Float, targetTicks: Int): Pair<Float, Float> {
    val rawStep = maxValue / targetTicks
    val step = niceStep(rawStep)
    val niceMax = ceil(maxValue / step) * step
    return (if (niceMax <= 0f) step else niceMax) to step
}

private fun niceStep(rawStep: Float): Float {
    if (rawStep <= 0f) return 1f
    val exponent = floor(log10(rawStep.toDouble())).toInt()
    val magnitude = 10.0.pow(exponent).toFloat()
    val fraction = rawStep / magnitude
    val niceFraction = when {
        fraction <= 1f -> 1f
        fraction <= 2f -> 2f
        fraction <= 5f -> 5f
        else -> 10f
    }
    return niceFraction * magnitude
}

private fun formatTick(value: Float): String {
    val rounded = value.toDouble()
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.1f".format(rounded)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AuiChartBarPreview() {
    AuiThemeProvider {
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Bar,
                title = "Quiz Scores This Week",
                xLabel = "Day",
                yLabel = "Score %",
                series = listOf(
                    ChartSeries(
                        label = "Score",
                        values = listOf(
                            ChartPoint("Mon", 72f),
                            ChartPoint("Tue", 85f),
                            ChartPoint("Wed", 78f),
                            ChartPoint("Fri", 91f),
                        ),
                    ),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AuiChartLinePreview() {
    AuiThemeProvider {
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Line,
                title = "Daily Active Users",
                xLabel = "Week",
                yLabel = "Users",
                series = listOf(
                    ChartSeries(
                        label = "Android",
                        values = listOf(
                            ChartPoint("W1", 1200f),
                            ChartPoint("W2", 1500f),
                            ChartPoint("W3", 1350f),
                            ChartPoint("W4", 1800f),
                        ),
                    ),
                    ChartSeries(
                        label = "iOS",
                        values = listOf(
                            ChartPoint("W1", 900f),
                            ChartPoint("W2", 1100f),
                            ChartPoint("W3", 1050f),
                            ChartPoint("W4", 1400f),
                        ),
                    ),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AuiChartPiePreview() {
    AuiThemeProvider {
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Pie,
                title = "Traffic Sources",
                series = listOf(
                    ChartSeries("Organic", listOf(ChartPoint("Organic", 45f))),
                    ChartSeries("Direct", listOf(ChartPoint("Direct", 28f))),
                    ChartSeries("Referral", listOf(ChartPoint("Referral", 17f))),
                    ChartSeries("Social", listOf(ChartPoint("Social", 10f))),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

