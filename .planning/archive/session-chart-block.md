# Session — Chart Block (`chart`)

## Goal

Add a built-in `chart` block supporting three variants — `bar`, `line`, and `pie`
— drawn entirely with Jetpack Compose `Canvas`. No third-party chart dependencies.
Renders with axis labels and a legend. Themes entirely through `AuiTheme`.

---

## Context

Read `CLAUDE.md` and `docs/architecture.md` before starting.

The current built-in component count is 25 (see `README.md` component catalog table
and `AuiCatalogPrompt.ALL_COMPONENT_TYPES`). This session adds 1 new block type
(`chart`) that covers all three variants via a `variant` discriminator field.
Update the count to 26 everywhere it appears.

Do not add any new Gradle dependencies. All chart rendering uses `androidx.compose.ui.graphics.Canvas`
and `androidx.compose.foundation.Canvas` primitives already present in `aui-compose`.

---

## Wire Format

The JSON shape (already confirmed — use this exactly):

```json
{
  "type": "chart",
  "data": {
    "variant": "bar",
    "title": "Your Quiz Scores This Week",
    "x_label": "Day",
    "y_label": "Score %",
    "series": [
      {
        "label": "Score",
        "values": [
          { "x": "Mon", "y": 72 },
          { "x": "Tue", "y": 85 },
          { "x": "Wed", "y": 78 },
          { "x": "Fri", "y": 91 }
        ]
      }
    ]
  }
}
```

Field rules:
- `variant`: required. One of `"bar"`, `"line"`, `"pie"`.
- `title`: optional. Displayed above the chart.
- `x_label`: optional. Axis label beneath the x-axis. Ignored for `pie`.
- `y_label`: optional. Rotated label along the y-axis. Ignored for `pie`.
- `series`: required. Array of one or more series. Each series has:
  - `label`: required. Used in the legend.
  - `values`: required. Array of `{ x: string, y: number }` points.
    - For `pie`, `x` is the slice label, `y` is the raw value (library computes percentages).
    - For `bar` and `line`, all series must share the same `x` values (same labels, same order).
- `chart` has no `feedback` — it is display-only. The library ignores any `feedback`
  field present on this block type (standard `AuiBlock` pass-through behaviour).

---

## Data Classes — `aui-core`

Create `aui-core/.../model/data/ChartData.kt`:

```kotlin
@Serializable
data class ChartData(
    val variant: ChartVariant,
    val title: String? = null,
    @SerialName("x_label") val xLabel: String? = null,
    @SerialName("y_label") val yLabel: String? = null,
    val series: List<ChartSeries>,
)

@Serializable
enum class ChartVariant {
    @SerialName("bar") Bar,
    @SerialName("line") Line,
    @SerialName("pie") Pie,
}

@Serializable
data class ChartSeries(
    val label: String,
    val values: List<ChartPoint>,
)

@Serializable
data class ChartPoint(
    val x: String,
    val y: Float,
)
```

---

## Sealed Class + Serializer — `aui-core`

In `AuiBlock.kt`, add:

```kotlin
data class Chart(val data: ChartData, override val feedback: AuiFeedback? = null) : AuiBlock()
```

In `AuiBlockSerializer` (the polymorphic `selectDeserializer`), add a branch:

```kotlin
"chart" -> ChartData.serializer().let { ChartSerializer }
```

Wire it exactly as the existing block types are wired (follow the existing pattern
in the file — do not deviate).

---

## Composable — `aui-compose`

Create `aui-compose/.../components/chart/AuiChart.kt`.

The composable signature:

```kotlin
@Composable
internal fun AuiChart(data: ChartData, modifier: Modifier = Modifier)
```

### Layout structure (all variants)

```
[ title (optional, typography.heading) ]
[ chart canvas area                    ]
[ x_label (optional, centered caption) ]   ← bar + line only
[ legend row                           ]
```

For `y_label` (bar + line only): draw it as rotated text along the left edge of the
canvas area using `Canvas` + `rotate`. Do not use a separate composable for this —
keep it within the same `Canvas` call as the chart body.

### Theming

Use only `LocalAuiTheme.current`. Never reference `MaterialTheme` directly.

Series colours: cycle through a fixed palette derived from the theme. Use this
palette order (express as theme token offsets or hardcoded ARGB — your call, but
they must look good on both `AuiTheme.Default` light and dark surfaces):

```
series[0] → theme.colors.primary
series[1] → theme.colors.secondary  (if AuiColors exposes one; otherwise a fixed teal)
series[2] → a warm accent (amber-ish)
series[3..] → repeat with reduced alpha
```

If `AuiColors` does not currently expose a `secondary` token, add one as an
`internal val` default in `AuiColors` — do not add it to the public constructor
yet (that's a separate API decision).

Axis lines, tick marks, and grid lines: `theme.colors.outline` at 38% alpha.
Axis label text: `theme.colors.captionColor`, `theme.typography.caption`.
Legend dots/lines: match series colour. Legend text: `theme.colors.bodyColor`, `theme.typography.caption`.

### Bar chart

- Vertical bars, one bar per `x` value per series. Multi-series bars are grouped
  side-by-side with a small gap between them.
- Y-axis: auto-scaled from 0 to `ceil(maxY / niceStep) * niceStep`. Use 4–5 y-axis
  ticks. Draw horizontal grid lines at each tick (dashed, low alpha).
- X-axis: one label per x value, centered under its bar group. Clip long labels
  with ellipsis if they would overlap.
- Bar corners: `4.dp` rounded top corners only (use `Path` + `addRoundRect` with
  `topLeft` and `topRight` radii set, bottom radii = 0).

### Line chart

- One polyline per series, drawn with `drawPath`. Fill the area under each line
  with a vertical gradient from series colour at 30% alpha at the top to transparent
  at the bottom.
- Data points: small filled circle at each point (`radius = 4.dp`). On single-point
  series, still draw the circle.
- Y-axis and x-axis: same rules as bar chart.
- Stroke width: `2.dp`.

### Pie chart

- Full filled pie (not a ring). Draw with `drawArc(useCenter = true)`.
- Slices are proportional to their `y` value across all series entries. The AI
  supplies one series per slice, each with a single `{ x, y }` value — the `x`
  is the slice label and `y` is the raw value. The library computes each slice's
  angle as `(y / totalY) * 360°`.
- Start angle: -90° (first slice starts at the top).
- Gap between slices: `2.dp` expressed as a small angle (in degrees) subtracted
  symmetrically from each arc sweep. Draw the gap by slightly shrinking each arc
  rather than drawing a separator line.
- No x/y axis, no axis labels, no grid lines for pie.
- Legend: one filled circle per slice, slice label + percentage (`"%.0f%%".format(pct)`).
  Percentages are computed from the raw `y` values.

### Legend

Draw below the chart canvas (not inside it) as a `FlowRow`-style horizontal wrapping
layout using a `Row` with `Modifier.horizontalScroll` if items overflow, or a simple
wrapping approach with `FlowRow` if it is already available in the Compose version
in use. Each legend item: a small filled circle (bar/pie) or short horizontal line (line
chart) in the series colour, followed by the label text. Spacing between items:
`theme.spacing.medium`.

### Sizing

Default chart canvas height: `200.dp`. Do not make this configurable in this session.
The composable takes `modifier` — callers can override height if needed.

---

## BlockRenderer — `aui-compose`

In `BlockRenderer.kt`, add a `when` branch for `AuiBlock.Chart`:

```kotlin
is AuiBlock.Chart -> AuiChart(data = block.data, modifier = modifier)
```

Place it in the **Display** section alongside `Text`, `Heading`, `Caption`,
`FileContent` — chart is display-only.

---

## AuiCatalogPrompt — `aui-core`

In `AuiCatalogPrompt.kt`:

1. Add `"chart"` to `ALL_COMPONENT_TYPES`.

2. In the `COMPONENTS` string (the Display section), add:

```
chart(variant, title?, x_label?, y_label?, series[]{label, values[]{x, y}})
  — Native chart. variant: "bar" | "line" | "pie".
    title: optional heading above the chart.
    x_label / y_label: optional axis labels (ignored for pie).
    series: one or more data series. Each series has a label (shown in the legend)
    and a values array of {x, y} points. For pie, supply one series per slice —
    each with a single {x, y} where x is the slice label and y is the raw value;
    the library computes percentages. For bar/line, all series must share the same
    x labels in the same order. Chart is display-only — do not add feedback.
  Use chart when the user asks to visualise data, compare values over time,
  or show a breakdown. Prefer pie for part-of-whole breakdowns (≤6 slices), bar for
  comparisons across categories, line for trends over time.
```

---

## Showcase JSON — `demo`

Add a new card to `demo/src/main/assets/all-blocks-showcase.json` (or whichever
showcase asset file exists). Add three consecutive showcase entries — one per variant:

```json
{
  "type": "chart",
  "data": {
    "variant": "bar",
    "title": "Quiz Scores This Week",
    "x_label": "Day",
    "y_label": "Score %",
    "series": [
      {
        "label": "Score",
        "values": [
          { "x": "Mon", "y": 72 },
          { "x": "Tue", "y": 85 },
          { "x": "Wed", "y": 78 },
          { "x": "Fri", "y": 91 }
        ]
      }
    ]
  }
},
{
  "type": "chart",
  "data": {
    "variant": "line",
    "title": "Daily Active Users",
    "x_label": "Week",
    "y_label": "Users",
    "series": [
      {
        "label": "Android",
        "values": [
          { "x": "W1", "y": 1200 },
          { "x": "W2", "y": 1500 },
          { "x": "W3", "y": 1350 },
          { "x": "W4", "y": 1800 }
        ]
      },
      {
        "label": "iOS",
        "values": [
          { "x": "W1", "y": 900 },
          { "x": "W2", "y": 1100 },
          { "x": "W3", "y": 1050 },
          { "x": "W4", "y": 1400 }
        ]
      }
    ]
  }
},
{
  "type": "chart",
  "data": {
    "variant": "pie",
    "title": "Traffic Sources",
    "series": [
      {
        "label": "Organic",
        "values": [{ "x": "Organic", "y": 45 }]
      },
      {
        "label": "Direct",
        "values": [{ "x": "Direct", "y": 28 }]
      },
      {
        "label": "Referral",
        "values": [{ "x": "Referral", "y": 17 }]
      },
      {
        "label": "Social",
        "values": [{ "x": "Social", "y": 10 }]
      }
    ]
  }
}
```

---

## Documentation Updates

- `README.md` component catalog table: add `chart` to the **Display** row.
  Update built-in count from 25 → 26.
- `docs/architecture.md` Catalog Roadmap: remove `chart` from the priority wishlist
  (item 3). Update any component count references.
- `CLAUDE.md`: add one-line entry for this session in the session log.
  Update the "Next:" pointer.

Do NOT modify `spec/aui-spec-v1.md` or any `.planning/` files.

---

## Tests — `aui-core`

In the core test module, add `ChartBlockParserTest`:

```
- Parses bar chart with single series correctly
- Parses line chart with two series correctly
- Parses pie chart correctly
- Optional fields (title, x_label, y_label) default to null when absent
- Unknown variant value → parse fails gracefully (AuiBlock.Unknown, no crash)
- Chart block round-trips through AuiBlockSerializer (serialize → deserialize)
- "chart" appears in AuiCatalogPrompt.ALL_COMPONENT_TYPES
- AuiCatalogPrompt output contains the string "chart" in the COMPONENTS section
```

---

## Verification

```bash
./gradlew build :aui-core:test :aui-compose:testDebugUnitTest
```

All tests must pass. Build must be clean (no warnings introduced by this session).
Run the demo app on an emulator and confirm all three chart variants render correctly
in the showcase screen with visible titles, axis labels, legends, and correct colours.
