# Session — Table Block (`table`)

> Renamed from `table_simple` → `table`. There is no sibling variant in the
> catalog roadmap, and with semantic cells this block is not really "simple"
> anyway. Future variants (e.g. `table_editable`, `table_pivot`) can use suffixes.

## Goal

Add a built-in `table` block: rows × columns tabular data with
**semantic cells** (typed values, not just strings). Renderer uses column types
to control alignment and formatting, and supports a small set of cell "shapes"
that unlock styling beyond plain text (status badges, readonly star ratings,
formatted numbers).

Tables wider than the available width **scroll horizontally**. No vertical
scroll — the host already scrolls the chat feed.

Row-level `feedback` is **deferred** for a future session; `table` is
display-only in v1.

---

## Context

Read `CLAUDE.md`, `AGENTS.md`, and `docs/architecture.md` before starting.

The current built-in component count is 26 (see `README.md` component catalog
table and `AuiCatalogPrompt.ALL_COMPONENT_TYPES`). This session adds 1 new block
type (`table`). Update the count to 27 everywhere it appears.

The Priority Wishlist in `docs/architecture.md` lists this as `table_simple`
(item 6). It ships as `table` — remove the `table_simple` entry from the
wishlist when this session lands.

Do not add any new Gradle dependencies. The renderer composes `Row` + `Column` +
`Modifier.horizontalScroll` — no third-party table library.

---

## Design Decisions (locked)

1. **Cell shapes supported in v1:** `text`, `number`, `badge`, `rating_stars`
   (readonly). No inline icons, no links, no images.
2. **Row-level `feedback`:** deferred to a later session. `table` is
   display-only in v1 and the renderer ignores any `feedback` field on rows or
   on the block itself.
3. **Overflow behavior:** horizontal scroll via `Modifier.horizontalScroll`
   when the content width exceeds available width. Cells do NOT wrap; long text
   is rendered single-line with ellipsis.
4. **Cell shape — string or object:** cells accept either a bare string (shortest
   path, reused as text) or an object with a typed payload. Column `type`
   drives the default alignment and supplies a parse hint when the cell is a
   bare string (e.g. `"12"` in a `number` column is parsed as 12).

---

## Wire Format

```json
{
  "type": "table",
  "data": {
    "title": "Daily Standings",
    "columns": [
      { "label": "Player", "type": "text" },
      { "label": "Score",  "type": "number", "format": "integer" },
      { "label": "Rating", "type": "rating_stars" },
      { "label": "Status", "type": "badge" }
    ],
    "rows": [
      [
        "Alice",
        1280,
        { "value": 5 },
        { "text": "Leading", "tone": "success" }
      ],
      [
        "Bob",
        { "value": 980, "format": "integer" },
        4.5,
        { "text": "Rising", "tone": "info" }
      ],
      [
        "Carol",
        720,
        3,
        { "text": "Trailing", "tone": "warning" }
      ]
    ]
  }
}
```

### Top-level fields

| Field     | Required | Description                                                                 |
|-----------|----------|-----------------------------------------------------------------------------|
| `title`   | no       | Displayed above the table (`typography.heading`).                           |
| `columns` | yes      | Ordered list of column definitions. Minimum 1, recommended max 6.           |
| `rows`    | yes      | Ordered list of rows. Each row is an ordered array of cells with length == `columns.size`. Rows with a length mismatch are trimmed/padded with empty text cells at parse time (non-fatal). |

### Column fields

| Field    | Required | Description                                                                                                |
|----------|----------|------------------------------------------------------------------------------------------------------------|
| `label`  | yes      | Header text for the column.                                                                                |
| `type`   | yes      | One of `"text"`, `"number"`, `"badge"`, `"rating_stars"`. Drives alignment + bare-string interpretation.   |
| `format` | no       | Only valid when `type == "number"`. One of `"integer"`, `"decimal"`, `"currency"`, `"percent"`. Default: `"decimal"`. |
| `align`  | no       | Override default alignment. One of `"start"`, `"center"`, `"end"`. Defaults per type (see below).          |

### Cell shapes

A cell may be **either** a bare JSON primitive (string or number) **or** an
object. The object shape depends on the column `type`:

- `text` column
  - Bare: `"Alice"` → text cell with that content.
  - Object: `{ "text": "Alice" }`.
- `number` column
  - Bare: `1280` or `"1280"` → parsed as `Double`.
  - Object: `{ "value": 1280, "format": "currency" }`. `format` on the cell
    overrides the column's `format`.
- `badge` column
  - Bare: `"Leading"` → badge with tone `"info"` (default).
  - Object: `{ "text": "Leading", "tone": "info" | "success" | "warning" | "error" }`.
- `rating_stars` column
  - Bare: `5` or `"4.5"` → stars with that value (0–5, half-steps rendered as
    half-filled stars).
  - Object: `{ "value": 4.5 }`. `max` is fixed at 5 in v1.

Unknown cell object keys are ignored. Malformed cells (e.g. string in a
`number` column that does not parse) render as empty text rather than throwing —
consistent with tolerant parsing in Session 44.

### Default alignment per column `type`

| `type`         | Header align | Cell align |
|----------------|--------------|------------|
| `text`         | start        | start      |
| `number`       | end          | end        |
| `badge`        | start        | start      |
| `rating_stars` | start        | start      |

`column.align`, if provided, overrides both header and cell alignment for that
column.

### Number `format` rules

| `format`   | Rendering                                                     |
|------------|---------------------------------------------------------------|
| `integer`  | `"%.0f"` locale-aware.                                        |
| `decimal`  | `"%.2f"` locale-aware (default).                              |
| `currency` | Locale currency (`NumberFormat.getCurrencyInstance()`).       |
| `percent`  | Value is treated as 0–1; `NumberFormat.getPercentInstance()`. |

Use `java.text.NumberFormat` with the default system locale. Do not take the
locale as a parameter in this session.

---

## Data Classes — `aui-core`

Create `aui-core/.../model/data/TableData.kt`:

```kotlin
@Serializable
data class TableData(
    val title: String? = null,
    val columns: List<TableColumn>,
    val rows: List<List<TableCell>>,
)

@Serializable
data class TableColumn(
    val label: String,
    val type: TableColumnType,
    val format: TableNumberFormat? = null,
    val align: TableAlign? = null,
)

@Serializable
enum class TableColumnType {
    @SerialName("text") Text,
    @SerialName("number") Number,
    @SerialName("badge") Badge,
    @SerialName("rating_stars") RatingStars,
}

@Serializable
enum class TableNumberFormat {
    @SerialName("integer") Integer,
    @SerialName("decimal") Decimal,
    @SerialName("currency") Currency,
    @SerialName("percent") Percent,
}

@Serializable
enum class TableAlign {
    @SerialName("start") Start,
    @SerialName("center") Center,
    @SerialName("end") End,
}

@Serializable(with = TableCellSerializer::class)
sealed class TableCell {
    data class Text(val text: String) : TableCell()
    data class Number(val value: Double, val format: TableNumberFormat? = null) : TableCell()
    data class Badge(val text: String, val tone: BadgeTone = BadgeTone.Info) : TableCell()
    data class RatingStars(val value: Float) : TableCell()
    /** Parser could not interpret the cell; renderer shows blank. */
    object Empty : TableCell()
}

@Serializable
enum class BadgeTone {
    @SerialName("info") Info,
    @SerialName("success") Success,
    @SerialName("warning") Warning,
    @SerialName("error") Error,
}
```

### `TableCellSerializer`

A custom `KSerializer<TableCell>` that reads `JsonElement` and branches on:
- primitive string → `Text` (also used as a fallback when no column context)
- primitive number → `Number` with `format = null`
- object with `"tone"` key → `Badge`
- object with `"value"` key and numeric → `Number` or `RatingStars` depending
  on presence of `"format"` key (ambiguous — prefer `Number` if `format` is
  present, else leave as `Number` and let the renderer reinterpret based on
  the column type)
- object with `"text"` key → `Text` or `Badge` if `tone` is present
- anything else → `Empty`

Because the serializer does not have column context, the **renderer** is
responsible for the final interpretation: e.g. a `TableCell.Number(4.5)` in a
`rating_stars` column is rendered as stars; a `TableCell.Text("1280")` in a
`number` column is parsed as 1280 and formatted. This keeps the serializer
column-agnostic and the row array uniform.

Put a short KDoc on the serializer explaining this two-phase approach.

---

## Sealed Class + Serializer — `aui-core`

In `AuiBlock.kt`, add:

```kotlin
/** Tabular data with semantic cells. Display-only in v1. */
@Serializable
data class Table(
    val data: TableData,
    override val feedback: AuiFeedback? = null,
) : AuiBlock()
```

In `AuiBlockSerializer.selectDeserializer`, add:

```kotlin
"table" -> AuiBlock.Table.serializer()
```

Place it in the Display section of the `when`, adjacent to `Chart`.

---

## Composable — `aui-compose`

Create `aui-compose/.../components/table/AuiTable.kt`.

Signature:

```kotlin
@Composable
internal fun AuiTable(data: TableData, modifier: Modifier = Modifier)
```

### Layout structure

```
[ title (optional, typography.heading)        ]
[ ┌──────────────────────────────────────┐    ]
[ │ header row (sticky-left not required)│    ]   ← horizontalScroll
[ ├──────────────────────────────────────┤    ]
[ │ row 1                                │    ]
[ │ row 2                                │    ]
[ │ ...                                  │    ]
[ └──────────────────────────────────────┘    ]
```

Outer structure:

```kotlin
Column(modifier) {
    title?.let { AuiHeading(...) }
    Box(
        Modifier
            .horizontalScroll(rememberScrollState())
            .border(1.dp, theme.colors.outline.copy(alpha = 0.38f), RoundedCornerShape(theme.shape.small))
    ) {
        Column {
            HeaderRow(columns)
            Divider()
            rows.forEachIndexed { index, row ->
                DataRow(columns, row, isAlternate = index % 2 == 1)
            }
        }
    }
}
```

### Column sizing

Each column is laid out with `Modifier.widthIn(min = 96.dp).padding(...)`.
Do **not** use `weight` inside the `horizontalScroll` (weights require a
bounded width, which scroll containers do not provide). Instead, let columns
size to their content using `Modifier.width(IntrinsicSize.Min)` wrapped cells
or a simple `Modifier.widthIn(min = 96.dp)` + `wrapContentWidth`.

Cell padding: `horizontal = theme.spacing.medium`, `vertical = theme.spacing.small`.

### Row striping

Alternate rows use `theme.colors.surface` background at 4% darker tint for
zebra striping. Use `Color.Black.copy(alpha = 0.04f)` drawn via `Modifier.background`.
First (non-header) row has the default surface background.

### Header row

Background: `theme.colors.surface` with a thin bottom divider (`1.dp`,
`theme.colors.outline` at 38% alpha).

Text style: `theme.typography.caption`, color `theme.colors.bodyColor`,
weight `FontWeight.Medium`. Align per column rules above.

### Cell rendering

Centralise cell rendering in a single `@Composable` `TableCellContent` that
takes `column: TableColumn` and `cell: TableCell`, applies the column-driven
reinterpretation (string-in-number, number-in-rating_stars), and dispatches:

- `text` column → `Text(cell.text, style = theme.typography.body, color = theme.colors.bodyColor, maxLines = 1, overflow = Ellipsis)`
- `number` column → `Text(formatted, ...)` using the effective format
  (cell.format ?? column.format ?? Decimal).
- `badge` column → reuse the existing badge composables. Map tone to:
  - `Info` → `AuiBadgeInfo`
  - `Success` → `AuiBadgeSuccess`
  - `Warning` → `AuiBadgeWarning`
  - `Error` → `AuiBadgeError`

  Build a `BadgeXxxData(text)` synthetically; the badge composables take
  `data` today. If any of them are `internal` and require an `AuiBlock`
  parameter, add a small package-private overload that takes `data` and
  renders without block context — follow the existing pattern used elsewhere
  in this module.
- `rating_stars` column → draw a read-only 5-star row using
  `Icons.Filled.Star` / `Icons.Outlined.Star`, sized `16.dp`. Half-values
  (e.g. 4.5) render as a half-filled fifth star — use the existing half-star
  drawing pattern if `AuiInputRatingStars` has one; otherwise draw the outlined
  star clipped to the left half + filled star clipped to the right half. Color
  filled stars with `theme.colors.primary`, outlined with
  `theme.colors.outline`.

  Do **not** reuse `AuiInputRatingStars` directly — it is an input and wires
  into the value registry. Create a small private `ReadonlyRatingStars` inside
  `AuiTable.kt`.

### Empty cells

`TableCell.Empty` → render an em-dash `"—"` in `theme.colors.captionColor`.

### Alignment

Each cell is wrapped in a `Box(modifier = Modifier.widthIn(min = 96.dp))` with
`contentAlignment` set per the column's effective alignment:

- `Start` → `Alignment.CenterStart`
- `Center` → `Alignment.Center`
- `End` → `Alignment.CenterEnd`

---

## BlockRenderer — `aui-compose`

In `BlockRenderer.kt`, add a `when` branch:

```kotlin
is AuiBlock.Table -> AuiTable(data = block.data, modifier = modifier)
```

Place it in the **Display** section alongside `Chart` and `FileContent`.

---

## AuiCatalogPrompt — `aui-core`

In `AuiCatalogPrompt.kt`:

1. Add `"table"` to `ALL_COMPONENT_TYPES`.

2. In the `COMPONENTS` string (Display section), add:

```
table(title?, columns[]{label, type, format?, align?}, rows[][])
  — Tabular data with semantic cells. columns.type is one of
    "text" | "number" | "badge" | "rating_stars" and drives alignment
    and bare-cell interpretation. Cells can be bare values (string or number)
    or objects:
      text:         "Alice"                         or { "text": "Alice" }
      number:       1280                            or { "value": 1280, "format": "currency" }
      badge:        "Active"                        or { "text": "Active", "tone": "success" }
      rating_stars: 4.5                             or { "value": 4.5 }
    Badge tones: "info" | "success" | "warning" | "error".
    Number formats: "integer" | "decimal" | "currency" | "percent".
    Tables scroll horizontally when wider than the chat area — prefer ≤6
    columns. Display-only: do not add feedback. Keep row count reasonable
    (≤20) for chat readability.
  Use table for structured comparisons, specs, leaderboards,
  receipts, metadata. Prefer key_value_list (when available) for single-row
  field/value layouts.
```

---

## Showcase JSON — `demo`

Add a new entry to `demo/src/main/assets/all-blocks-showcase.json` in the
appropriate Display group. One entry that exercises all four cell types:

```json
{
  "type": "table",
  "data": {
    "title": "Weekly Leaderboard",
    "columns": [
      { "label": "Player", "type": "text" },
      { "label": "Score",  "type": "number", "format": "integer" },
      { "label": "Rating", "type": "rating_stars" },
      { "label": "Status", "type": "badge" }
    ],
    "rows": [
      ["Alice", 1280, 5,   { "text": "Leading",  "tone": "success" }],
      ["Bob",   980,  4.5, { "text": "Rising",   "tone": "info" }],
      ["Carol", 720,  3,   { "text": "Trailing", "tone": "warning" }],
      ["Dave",  450,  2,   { "text": "Out",      "tone": "error" }]
    ]
  }
}
```

Update the showcase asset validation test (see Session 40) to expect the new
block count and to assert `table` parses cleanly.

---

## Documentation Updates

- `README.md` component catalog table: add `table` to the **Display**
  row. Update built-in count from 26 → 27.
- `docs/architecture.md` Catalog Roadmap:
  - remove `table` from the Priority Wishlist (item 6) and renumber
    subsequent items.
  - remove it from the "Lists" or "Display" bullet lists below the wishlist
    if present.
- `AGENTS.md`: append a one-line `Session NN` entry in Recent Progress and
  update the Next Task pointer to the next wishlist item (`code_block`).
- `CLAUDE.md`: no change required if it only points back at `AGENTS.md`.

Do NOT modify `spec/aui-spec-v1.md` in this session (the spec already lists
`table` as planned; a spec update for semantic cells is a separate
decision that should be made once v1 is in use). Add a TODO comment in the
session's commit message noting the spec mismatch for a follow-up.

---

## Tests — `aui-core`

Add `TableBlockParserTest`:

- Parses a table with all four column types and mixed bare/object cells.
- Bare string in a `number` column parses to the correct `Double`.
- Bare number in a `rating_stars` column renders via the renderer path (parser
  just stores `TableCell.Number`; column-driven reinterpretation is tested in
  the compose test below).
- Unknown column `type` → the whole block falls back to `AuiBlock.Unknown`
  (reuse the tolerant-parsing path).
- Missing `type` on a column → block falls back to `AuiBlock.Unknown`.
- Row length mismatch (too few / too many cells) → parsed block normalises
  row length to `columns.size`, padding with `TableCell.Empty` or truncating.
- Unknown badge `tone` → defaults to `Info`.
- Round-trips through `AuiBlockSerializer`.
- `"table"` appears in `AuiCatalogPrompt.ALL_COMPONENT_TYPES`.
- `AuiCatalogPrompt` output contains the string `"table"` in the
  COMPONENTS section.

## Tests — `aui-compose`

Add `AuiTableTest` (Robolectric / Compose UI test, matching the
conventions of other component tests in the module):

- Renders a 4-column table with 3 rows without crashing.
- Number column cell with `format = "currency"` renders with the current
  locale's currency symbol.
- Rating column bare value `4.5` renders 4 filled + 1 half star.
- Badge column cell with `tone = "warning"` resolves to `AuiBadgeWarning`.
- Table with overflowing width is horizontally scrollable (assert
  `horizontalScroll` modifier is applied; don't assert pixel layout).
- Malformed cell (e.g. `"abc"` in a `number` column) renders as em-dash and
  does not throw.

---

## Verification

```bash
./gradlew build :aui-core:test :aui-compose:testDebugUnitTest
```

All tests must pass. Build must be clean (no new warnings).

Run the demo app and confirm:
- Leaderboard showcase table renders with correct alignment (right-aligned
  Score column, left-aligned others).
- Badges render with the correct tone colors from `AuiTheme`.
- Stars render at 16dp with half-star support.
- Swipe horizontally on the table when the screen is narrow — content
  scrolls; rest of the chat feed does not scroll horizontally.
- Dark theme: verify header contrast, row striping, and badge legibility.
