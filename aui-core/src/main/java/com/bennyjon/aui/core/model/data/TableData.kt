package com.bennyjon.aui.core.model.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Data for the `table` block.
 *
 * Display-only tabular data with semantic cells. Each column declares a [TableColumnType]
 * that drives alignment and how bare cell values are interpreted. Cells may be supplied
 * as bare JSON primitives (string or number) or as objects — see [TableCell] for the
 * supported shapes.
 *
 * Tables wider than the available width are rendered with horizontal scroll. Row-level
 * feedback is not supported in v1 — the block is purely a display component.
 */
@Serializable
data class TableData(
    /** Optional title rendered above the table. */
    val title: String? = null,
    /** Column definitions, in display order. Minimum 1. */
    val columns: List<TableColumn>,
    /**
     * Rows of cells. Each row is an ordered list of cells whose length should match
     * [columns]. Length mismatches are padded with [TableCell.Empty] or truncated by the
     * renderer — the parser preserves whatever the AI emitted.
     */
    val rows: List<List<TableCell>>,
)

/** A single column definition. */
@Serializable
data class TableColumn(
    /** Header label shown on the column. */
    val label: String,
    /** Cell type. Drives alignment and bare-value interpretation. */
    val type: TableColumnType,
    /** Number format. Only meaningful when [type] is [TableColumnType.Number]. */
    val format: TableNumberFormat? = null,
    /** Alignment override. If unset, defaults per [type] (see `AuiTable`). */
    val align: TableAlign? = null,
)

/** Cell type for a [TableColumn]. */
@Serializable(with = TableColumnTypeSerializer::class)
enum class TableColumnType {
    Text,
    Number,
    Badge,
    RatingStars,
}

internal object TableColumnTypeSerializer : KSerializer<TableColumnType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.bennyjon.aui.core.model.data.TableColumnType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TableColumnType) {
        encoder.encodeString(
            when (value) {
                TableColumnType.Text -> "text"
                TableColumnType.Number -> "number"
                TableColumnType.Badge -> "badge"
                TableColumnType.RatingStars -> "rating"
            }
        )
    }

    override fun deserialize(decoder: Decoder): TableColumnType = when (decoder.decodeString().lowercase()) {
        "text" -> TableColumnType.Text
        "number" -> TableColumnType.Number
        "badge" -> TableColumnType.Badge
        "rating", "rating_stars" -> TableColumnType.RatingStars
        else -> error("Unknown table column type")
    }
}

/** Number formatting options for a [TableColumn] of type [TableColumnType.Number]. */
@Serializable
enum class TableNumberFormat {
    @SerialName("integer") Integer,
    @SerialName("decimal") Decimal,
    @SerialName("currency") Currency,
    @SerialName("percent") Percent,
}

/** Alignment options for a [TableColumn]. */
@Serializable(with = TableAlignSerializer::class)
enum class TableAlign {
    Start,
    Center,
    End,
}

internal object TableAlignSerializer : KSerializer<TableAlign> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.bennyjon.aui.core.model.data.TableAlign", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TableAlign) {
        encoder.encodeString(
            when (value) {
                TableAlign.Start -> "start"
                TableAlign.Center -> "center"
                TableAlign.End -> "end"
            }
        )
    }

    // Accept common English aliases ("left", "right") alongside the canonical
    // "start"/"end" values — LLMs consistently reach for the directional words,
    // and rejecting the whole column would drop the entire table via the
    // tolerant-parse fallback to Unknown.
    override fun deserialize(decoder: Decoder): TableAlign = when (decoder.decodeString().lowercase()) {
        "start", "left" -> TableAlign.Start
        "center", "centre", "middle" -> TableAlign.Center
        "end", "right" -> TableAlign.End
        else -> TableAlign.Start
    }
}

/** Tone for a [TableCell.Badge]. */
@Serializable
enum class BadgeTone {
    @SerialName("info") Info,
    @SerialName("success") Success,
    @SerialName("warning") Warning,
    @SerialName("error") Error,
}

/**
 * A single cell in a [TableData] row.
 *
 * The parser is column-agnostic: it decodes cells purely from JSON shape. The renderer
 * is then responsible for reinterpreting a cell in the context of its column's
 * [TableColumn.type] — e.g. a [Text] cell in a `number` column is parsed as a number,
 * and a [Number] cell in a `rating` column is rendered as stars.
 *
 * Malformed cells (unrecognized shapes) decode to [Empty] and render as an em-dash.
 */
@Serializable(with = TableCellSerializer::class)
sealed class TableCell {
    /** Plain text. */
    data class Text(val text: String) : TableCell()

    /**
     * A numeric value. [format] overrides the column's format when non-null.
     *
     * When a `Text` cell lands in a `number` column the renderer attempts to parse the
     * text; successful parses are rendered using the column's effective format.
     */
    data class Number(val value: Double, val format: TableNumberFormat? = null) : TableCell()

    /** A coloured status pill. */
    data class Badge(val text: String, val tone: BadgeTone = BadgeTone.Info) : TableCell()

    /** A 0–5 star rating. Half-values render as a half-filled star. */
    data class RatingStars(val value: Float) : TableCell()

    /** Placeholder for unrecognized or empty cells — rendered as em-dash. */
    object Empty : TableCell()
}

/**
 * Decodes [TableCell] values from JSON.
 *
 * The serializer is column-agnostic — it decides a cell's subtype from JSON shape alone,
 * not from the containing column. The renderer reinterprets cells based on the column's
 * [TableColumnType] (see `TableCellContent` in aui-compose).
 *
 * Rules:
 * - A bare string → [TableCell.Text].
 * - A bare number → [TableCell.Number] with `format = null`.
 * - A bare boolean or `null` → [TableCell.Empty].
 * - An object with a `tone` key → [TableCell.Badge] (text defaults to empty if missing).
 * - An object with a `value` key that is a number → [TableCell.Number] (reading optional
 *   `format`). In a rating column the renderer will reinterpret it as stars.
 * - An object with a `text` key → [TableCell.Text] (or [TableCell.Badge] if `tone` is
 *   also present).
 * - Anything else → [TableCell.Empty].
 */
internal object TableCellSerializer : KSerializer<TableCell> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.bennyjon.aui.core.model.data.TableCell")

    override fun serialize(encoder: Encoder, value: TableCell) {
        error("TableCell is parse-only in v1 — serialization is not required")
    }

    override fun deserialize(decoder: Decoder): TableCell {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("TableCellSerializer requires JsonDecoder")
        return fromJsonElement(jsonDecoder.decodeJsonElement())
    }

    fun fromJsonElement(element: JsonElement): TableCell {
        return when (element) {
            is JsonNull -> TableCell.Empty
            is JsonPrimitive -> fromPrimitive(element)
            is JsonObject -> fromObject(element)
            else -> TableCell.Empty
        }
    }

    private fun fromPrimitive(primitive: JsonPrimitive): TableCell {
        if (primitive.isString) return TableCell.Text(primitive.content)
        val asDouble = primitive.doubleOrNull
        if (asDouble != null) return TableCell.Number(asDouble)
        if (primitive.booleanOrNull != null) return TableCell.Empty
        return TableCell.Empty
    }

    private fun fromObject(obj: JsonObject): TableCell {
        val tone = (obj["tone"] as? JsonPrimitive)?.contentOrNullIfNotString()
        if (tone != null) {
            val text = (obj["text"] as? JsonPrimitive)?.contentOrNullIfNotString().orEmpty()
            return TableCell.Badge(text = text, tone = toneFromString(tone))
        }
        val valueEl = obj["value"]
        if (valueEl is JsonPrimitive) {
            val asDouble = valueEl.doubleOrNull
            if (asDouble != null) {
                val format = (obj["format"] as? JsonPrimitive)
                    ?.contentOrNullIfNotString()
                    ?.let(::numberFormatFromString)
                return TableCell.Number(asDouble, format)
            }
        }
        val text = (obj["text"] as? JsonPrimitive)?.contentOrNullIfNotString()
        if (text != null) return TableCell.Text(text)
        return TableCell.Empty
    }

    private fun toneFromString(raw: String): BadgeTone = when (raw.lowercase()) {
        "success" -> BadgeTone.Success
        "warning" -> BadgeTone.Warning
        "error" -> BadgeTone.Error
        else -> BadgeTone.Info
    }

    private fun numberFormatFromString(raw: String): TableNumberFormat? = when (raw.lowercase()) {
        "integer" -> TableNumberFormat.Integer
        "decimal" -> TableNumberFormat.Decimal
        "currency" -> TableNumberFormat.Currency
        "percent" -> TableNumberFormat.Percent
        else -> null
    }

    private fun JsonPrimitive.contentOrNullIfNotString(): String? =
        if (isString) content else null
}
