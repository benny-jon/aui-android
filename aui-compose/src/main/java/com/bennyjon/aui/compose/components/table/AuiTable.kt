package com.bennyjon.aui.compose.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.bennyjon.aui_compose.R
import com.bennyjon.aui.compose.components.text.AuiHeading
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.BadgeTone
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.TableAlign
import com.bennyjon.aui.core.model.data.TableCell
import com.bennyjon.aui.core.model.data.TableColumn
import com.bennyjon.aui.core.model.data.TableColumnType
import com.bennyjon.aui.core.model.data.TableData
import com.bennyjon.aui.core.model.data.TableNumberFormat
import java.text.NumberFormat
import java.util.Locale

private const val EmDash = "—"

/**
 * Renders a `table` block.
 *
 * Display-only tabular data with semantic cells: text, numbers (with optional formatting),
 * colored badges, and read-only star ratings. The table is wrapped in a horizontal scroll
 * container so wider tables overflow gracefully on narrow screens. Row-level feedback is
 * not supported in v1.
 */
@Composable
internal fun AuiTable(
    data: TableData,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAuiTheme.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(theme.spacing.small),
    ) {
        data.title?.let { title ->
            AuiHeading(block = AuiBlock.Heading(data = HeadingData(text = title)))
        }

        val columns = data.columns
        val columnWidths = remember(columns, data.rows, theme) { estimateColumnWidths(columns, data.rows, theme) }
        val tableWidth = columnWidths.fold(theme.spacing.zero) { total, width -> total + width }
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .border(
                    width = theme.spacing.dividerThickness,
                    color = theme.colors.outline.copy(alpha = 0.38f),
                    shape = theme.shapes.banner,
                )
                .clipToBounds()
                .horizontalScroll(scrollState),
        ) {
            Column {
                HeaderRow(columns = columns, columnWidths = columnWidths)
                HorizontalHairline(
                    color = theme.colors.outline.copy(alpha = 0.38f),
                    modifier = Modifier.width(tableWidth),
                    thickness = theme.spacing.dividerThickness,
                )
                data.rows.forEachIndexed { rowIndex, rawRow ->
                    val row = normalizeRowLength(rawRow, columns.size)
                    DataRow(
                        columns = columns,
                        columnWidths = columnWidths,
                        row = row,
                        isAlternate = rowIndex % 2 == 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(columns: List<TableColumn>, columnWidths: List<Dp>) {
    val theme = LocalAuiTheme.current
    Row(
        modifier = Modifier.background(theme.colors.surface),
    ) {
        columns.forEachIndexed { index, column ->
            Box(
                modifier = Modifier
                    .width(columnWidths.getOrElse(index) { theme.spacing.tableMinColumnWidth })
                    .padding(
                        horizontal = theme.spacing.medium,
                        vertical = theme.spacing.small,
                    ),
                contentAlignment = column.effectiveAlignment().toBoxAlignment(),
            ) {
                Text(
                    text = column.label,
                    style = theme.typography.label,
                    color = theme.colors.bodyColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DataRow(
    columns: List<TableColumn>,
    columnWidths: List<Dp>,
    row: List<TableCell>,
    isAlternate: Boolean,
) {
    val theme = LocalAuiTheme.current
    val background = if (isAlternate) theme.colors.surfaceVariant.copy(alpha = 0.18f) else Color.Transparent
    Row(
        modifier = Modifier.background(background),
    ) {
        columns.forEachIndexed { index, column ->
            val cell = row.getOrElse(index) { TableCell.Empty }
            Box(
                modifier = Modifier
                    .width(columnWidths.getOrElse(index) { theme.spacing.tableMinColumnWidth })
                    .padding(
                        horizontal = theme.spacing.medium,
                        vertical = theme.spacing.small,
                    ),
                contentAlignment = column.effectiveAlignment().toBoxAlignment(),
            ) {
                TableCellContent(column = column, cell = cell)
            }
        }
    }
}

@Composable
private fun TableCellContent(column: TableColumn, cell: TableCell) {
    when (column.type) {
        TableColumnType.Text -> TextCell(cell)
        TableColumnType.Number -> NumberCell(column = column, cell = cell)
        TableColumnType.Badge -> BadgeCell(cell)
        TableColumnType.RatingStars -> RatingStarsCell(cell)
    }
}

@Composable
private fun TextCell(cell: TableCell) {
    val theme = LocalAuiTheme.current
    val text = when (cell) {
        is TableCell.Text -> cell.text
        is TableCell.Number -> formatDouble(cell.value, TableNumberFormat.Decimal)
        is TableCell.Badge -> cell.text
        is TableCell.RatingStars -> cell.value.toString()
        TableCell.Empty -> EmDash
    }
    val color = if (cell is TableCell.Empty) theme.colors.captionColor else theme.colors.bodyColor
    Text(
        text = text,
        style = theme.typography.body,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NumberCell(column: TableColumn, cell: TableCell) {
    val theme = LocalAuiTheme.current
    val format = (cell as? TableCell.Number)?.format
        ?: column.format
        ?: TableNumberFormat.Decimal
    val value: Double? = when (cell) {
        is TableCell.Number -> cell.value
        is TableCell.Text -> cell.text.trim().toDoubleOrNull()
        is TableCell.RatingStars -> cell.value.toDouble()
        is TableCell.Badge, TableCell.Empty -> null
    }
    if (value == null) {
        EmptyText()
        return
    }
    Text(
        text = formatDouble(value, format),
        style = theme.typography.body,
        color = theme.colors.bodyColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun BadgeCell(cell: TableCell) {
    val (text, tone) = when (cell) {
        is TableCell.Badge -> cell.text to cell.tone
        is TableCell.Text -> cell.text to BadgeTone.Info
        is TableCell.Empty -> {
            EmptyText()
            return
        }
        else -> {
            EmptyText()
            return
        }
    }
    if (text.isBlank()) {
        EmptyText()
        return
    }
    TableBadgePill(text = text, tone = tone)
}

@Composable
private fun TableBadgePill(text: String, tone: BadgeTone) {
    val theme = LocalAuiTheme.current
    val (containerColor, contentColor) = when (tone) {
        BadgeTone.Info -> theme.colors.infoContainer to theme.colors.onInfoContainer
        BadgeTone.Success -> theme.colors.successContainer to theme.colors.onSuccessContainer
        BadgeTone.Warning -> theme.colors.warningContainer to theme.colors.onWarningContainer
        BadgeTone.Error -> theme.colors.errorContainer to theme.colors.onErrorContainer
    }
    Box(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = theme.shapes.badge,
            )
            .padding(horizontal = theme.spacing.medium, vertical = theme.spacing.xSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = theme.typography.label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RatingStarsCell(cell: TableCell) {
    val value: Float? = when (cell) {
        is TableCell.RatingStars -> cell.value
        is TableCell.Number -> cell.value.toFloat()
        is TableCell.Text -> cell.text.trim().toFloatOrNull()
        is TableCell.Badge, TableCell.Empty -> null
    }
    if (value == null) {
        EmptyText()
        return
    }
    ReadonlyRatingStars(value = value)
}

@Composable
private fun ReadonlyRatingStars(value: Float, max: Int = 5) {
    val theme = LocalAuiTheme.current
    val clamped = value.coerceIn(0f, max.toFloat())
    val formatted = if (clamped % 1f == 0f) clamped.toInt().toString() else clamped.toString()
    val description = stringResource(R.string.aui_table_rating_stars_content_description, formatted, max)
    Row(
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.tableRatingStarGap),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        for (i in 1..max) {
            val fillFraction = (clamped - (i - 1)).coerceIn(0f, 1f)
            Box(modifier = Modifier.size(theme.spacing.tableRatingStarSize)) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = theme.colors.outline,
                    modifier = Modifier.size(theme.spacing.tableRatingStarSize),
                )
                if (fillFraction > 0f) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = theme.colors.primary,
                        modifier = Modifier
                            .size(theme.spacing.tableRatingStarSize)
                            .clipLeftFraction(fillFraction),
                    )
                }
            }
        }
    }
}

/**
 * Clips a composable to the given horizontal fraction of its width (from the start edge).
 * Used to draw half-filled stars without bringing in a bitmap mask.
 */
private fun Modifier.clipLeftFraction(fraction: Float): Modifier = drawWithContent {
    clipRect(right = size.width * fraction.coerceIn(0f, 1f)) {
        this@drawWithContent.drawContent()
    }
}

@Composable
private fun EmptyText() {
    val theme = LocalAuiTheme.current
    Text(
        text = EmDash,
        style = theme.typography.body,
        color = theme.colors.captionColor,
        maxLines = 1,
    )
}

@Composable
private fun HorizontalHairline(color: Color, modifier: Modifier = Modifier, thickness: Dp) {
    Box(
        modifier = modifier
            .height(thickness)
            .background(color),
    )
}

private fun TableColumn.effectiveAlignment(): TableAlign = align ?: when (type) {
    TableColumnType.Number -> TableAlign.End
    else -> TableAlign.Start
}

private fun TableAlign.toBoxAlignment(): Alignment = when (this) {
    TableAlign.Start -> Alignment.CenterStart
    TableAlign.Center -> Alignment.Center
    TableAlign.End -> Alignment.CenterEnd
}

private fun normalizeRowLength(row: List<TableCell>, size: Int): List<TableCell> = when {
    row.size == size -> row
    row.size < size -> row + List(size - row.size) { TableCell.Empty }
    else -> row.subList(0, size)
}

private fun estimateColumnWidths(
    columns: List<TableColumn>,
    rows: List<List<TableCell>>,
    theme: com.bennyjon.aui.compose.theme.AuiTheme,
): List<Dp> {
    return columns.mapIndexed { index, column ->
        if (column.type == TableColumnType.RatingStars) return@mapIndexed theme.spacing.tableRatingColumnWidth

        val bodyCharWidth = if (column.type == TableColumnType.Number) {
            theme.spacing.tableEstimatedNumericCharWidth
        } else {
            theme.spacing.tableEstimatedBodyCharWidth
        }
        val headerWidth = estimateTextWidth(column.label, theme.spacing.tableEstimatedCaptionCharWidth)
        val maxCellWidth = rows.maxOfOrNull { row ->
            estimateTextWidth(
                text = cellTextForWidth(column, row.getOrElse(index) { TableCell.Empty }),
                charWidth = bodyCharWidth,
            )
        } ?: theme.spacing.zero
        val badgePadding = if (column.type == TableColumnType.Badge) theme.spacing.tableBadgeHorizontalPadding else theme.spacing.zero
        val rawWidth = maxOf(headerWidth, maxCellWidth + badgePadding) + theme.spacing.tableCellHorizontalPadding
        val maxWidth = when (column.type) {
            TableColumnType.Text -> theme.spacing.tableMaxTextColumnWidth
            TableColumnType.Number -> theme.spacing.tableMaxNumberColumnWidth
            TableColumnType.Badge -> theme.spacing.tableMaxCompactColumnWidth
            TableColumnType.RatingStars -> theme.spacing.tableRatingColumnWidth
        }
        rawWidth.coerceIn(theme.spacing.tableMinColumnWidth, maxWidth)
    }
}

private fun estimateTextWidth(text: String, charWidth: Dp): Dp = charWidth * text.length

private fun cellTextForWidth(column: TableColumn, cell: TableCell): String = when (column.type) {
    TableColumnType.Text -> when (cell) {
        is TableCell.Text -> cell.text
        is TableCell.Number -> formatDouble(cell.value, TableNumberFormat.Decimal)
        is TableCell.Badge -> cell.text
        is TableCell.RatingStars -> cell.value.toString()
        TableCell.Empty -> EmDash
    }
    TableColumnType.Number -> {
        val format = (cell as? TableCell.Number)?.format
            ?: column.format
            ?: TableNumberFormat.Decimal
        val value = when (cell) {
            is TableCell.Number -> cell.value
            is TableCell.Text -> cell.text.trim().toDoubleOrNull()
            is TableCell.RatingStars -> cell.value.toDouble()
            is TableCell.Badge, TableCell.Empty -> null
        }
        value?.let { formatDouble(it, format) }.orEmpty()
    }
    TableColumnType.Badge -> when (cell) {
        is TableCell.Badge -> cell.text
        is TableCell.Text -> cell.text
        else -> EmDash
    }
    TableColumnType.RatingStars -> "*****"
}

private fun Dp.coerceIn(minimumValue: Dp, maximumValue: Dp): Dp =
    Dp(value.coerceIn(minimumValue.value, maximumValue.value))

private fun formatDouble(value: Double, format: TableNumberFormat): String {
    val locale: Locale = Locale.getDefault()
    return when (format) {
        TableNumberFormat.Integer -> NumberFormat.getIntegerInstance(locale).format(value.toLong())
        TableNumberFormat.Decimal -> NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(value)
        TableNumberFormat.Currency -> NumberFormat.getCurrencyInstance(locale).format(value)
        TableNumberFormat.Percent -> NumberFormat.getPercentInstance(locale).apply {
            maximumFractionDigits = 1
        }.format(value)
    }
}
