# Session — Status Variants Expansion (Info / Warning / Error)

**Date:** 2026-04-17
**Purpose:** Expand the `status` component family from the single existing `success` variant to the full four-variant set (`info`, `success`, `warning`, `error`) for both `badge_*` and `status_banner_*` blocks.
**iOS reuse:** This doc is the canonical spec for the AUI-iOS companion library. Use it to mirror the wire format, color tokens, and renderer behavior on iOS.

---

## 1. What is being added

Six new block types (two families × three new severities):

| Family       | Existing            | New                                                   |
| ------------ | ------------------- | ----------------------------------------------------- |
| Badge        | `badge_success`     | `badge_info`, `badge_warning`, `badge_error`          |
| Status banner| `status_banner_success` | `status_banner_info`, `status_banner_warning`, `status_banner_error` |

All share the same JSON shape:

```json
{ "type": "<block_type>", "data": { "text": "string" } }
```

No `feedback` — these are display-only (read-only). Matches the existing `badge_success` / `status_banner_success` contract.

**Bumps built-in component count:** 18 → 24.

---

## 2. Wire format (JSON schema)

Each new block follows the identical data shape to its success counterpart — only the `type` discriminator differs.

```
badge_info            { type: "badge_info",            data: { text: string } }
badge_success         { type: "badge_success",         data: { text: string } }      // existing
badge_warning         { type: "badge_warning",         data: { text: string } }
badge_error           { type: "badge_error",           data: { text: string } }

status_banner_info    { type: "status_banner_info",    data: { text: string } }
status_banner_success { type: "status_banner_success", data: { text: string } }      // existing
status_banner_warning { type: "status_banner_warning", data: { text: string } }
status_banner_error   { type: "status_banner_error",   data: { text: string } }
```

Already described in `spec/aui-spec-v1.md` §"Status / Progress" — Android implementation is catching up to the written spec.

---

## 3. Visual semantics

### Badge (`badge_*`)
- **Shape:** pill (`AuiShapes.badge`, defaults to `CircleShape`).
- **Size:** compact — `horizontal = spacing.medium`, `vertical = spacing.xSmall`.
- **Typography:** `typography.label`.
- **Layout:** `Box` with text centered. Self-sized (no `fillMaxWidth`).
- **Icon:** none.

### Status banner (`status_banner_*`)
- **Shape:** soft-rounded (`AuiShapes.banner`, defaults to `RoundedCornerShape(8.dp)`).
- **Size:** full-width row, padding `spacing.medium`.
- **Typography:** `typography.body`.
- **Layout:** `Row` — icon (`spacing.large`), `spacing.small` gap, text.
- **Icons (Material Icons filled, from `material-icons-core` only):**
  - info → `Icons.Default.Info`
  - success → `Icons.Default.CheckCircle` (existing)
  - warning → `Icons.Default.Warning`
  - error → `Icons.Default.Warning` (reused — error-red tint + container color carries the severity. The `Error` glyph lives only in `material-icons-extended`, which we avoid pulling in for a single icon. iOS can use the proper `xmark.octagon.fill` SF Symbol since that one's free.)

---

## 4. Theme color tokens

Each severity gets a four-color quad on `AuiColors`, matching the existing `success` shape:

| Semantic       | Foreground     | On-foreground     | Container           | On-container          |
| -------------- | -------------- | ----------------- | ------------------- | --------------------- |
| **info**       | `info`         | `onInfo`          | `infoContainer`     | `onInfoContainer`     |
| **success**    | `success`      | `onSuccess`       | `successContainer`  | `onSuccessContainer`  |
| **warning**    | `warning`      | `onWarning`       | `warningContainer`  | `onWarningContainer`  |
| **error**      | `error`        | `onError`         | `errorContainer`    | `onErrorContainer`    |

### Default palette — LIGHT surfaces (`AuiColors.Default`)

| Token | Hex | Role |
| --- | --- | --- |
| `info` | `#1F6FEB` | Icon tint on `infoContainer` |
| `onInfo` | `#FFFFFF` | Content on `info` (unused by banners — reserved for future filled variants) |
| `infoContainer` | `#D6E4FF` | Banner/badge background |
| `onInfoContainer` | `#001A41` | Banner/badge text |
| `success` | `#386A20` | Icon tint on `successContainer` |
| `onSuccess` | `#FFFFFF` | Reserved |
| `successContainer` | `#B7F397` | Banner/badge background |
| `onSuccessContainer` | `#072100` | Banner/badge text |
| `warning` | `#6B5200` | Icon tint on `warningContainer` (dark amber) |
| `onWarning` | `#FFFFFF` | Reserved |
| `warningContainer` | `#FFE082` | Banner/badge background (Material Amber 200 — unambiguously yellow) |
| `onWarningContainer` | `#221B00` | Banner/badge text |
| `error` | `#BA1A1A` | Icon tint on `errorContainer` |
| `onError` | `#FFFFFF` | Reserved |
| `errorContainer` | `#FFDAD6` | Banner/badge background |
| `onErrorContainer` | `#410002` | Banner/badge text |

### Default palette — DARK surfaces (`AuiColors.DefaultDark`)

Dark-mode variant swaps each severity so the container is the deep-saturated side and the on-container is the light pastel — the Material 3 dark container pattern.

| Token | Hex | Role |
| --- | --- | --- |
| `info` | `#A6C8FF` | Icon tint |
| `onInfo` | `#002E69` | Reserved |
| `infoContainer` | `#00458C` | Banner/badge background |
| `onInfoContainer` | `#D6E4FF` | Banner/badge text |
| `success` | `#9CD67D` | Icon tint |
| `onSuccess` | `#0A3900` | Reserved |
| `successContainer` | `#1F5000` | Banner/badge background |
| `onSuccessContainer` | `#B7F397` | Banner/badge text |
| `warning` | `#EBC343` | Icon tint (bright amber) |
| `onWarning` | `#3A2D00` | Reserved |
| `warningContainer` | `#524100` | Banner/badge background (dark amber) |
| `onWarningContainer` | `#FFE082` | Banner/badge text |
| `error` | `#FFB4AB` | Icon tint |
| `onError` | `#690005` | Reserved |
| `errorContainer` | `#93000A` | Banner/badge background |
| `onErrorContainer` | `#FFDAD6` | Banner/badge text |

### Contrast sanity check (WCAG — text 4.5:1, icons/large 3:1)

| Severity | Mode | text on container | icon on container |
| --- | --- | --- | --- |
| info    | light | ~15:1 | ~3.5:1 |
| info    | dark  | ~11:1 | ~5:1 |
| success | light | ~12:1 | ~4:1 |
| success | dark  | ~11:1 | ~5:1 |
| warning | light | ~16:1 | ~5:1 |
| warning | dark  | ~10:1 | ~5:1 |
| error   | light | ~14:1 | ~4:1 |
| error   | dark  | ~12:1 | ~6:1 |

All pass AA for text (4.5:1) and comfortably exceed the 3:1 icon threshold.

### `fromColorScheme()` mapping

Severity quads are **not** pulled from the scheme — they come from `Default` (light) or `DefaultDark` (dark), selected by inspecting `scheme.surface.luminance() < 0.5f`. Brand tokens still come from the scheme:

```
primary / onPrimary / primaryContainer / onPrimaryContainer → scheme
surface / onSurface / surfaceVariant / onSurfaceVariant    → scheme
outline                                                     → scheme
info / success / warning / error quads                      → Default or DefaultDark
                                                              based on scheme luminance
headingColor, bodyColor                                     → scheme.onSurface
captionColor                                                → scheme.onSurfaceVariant
```

This decouples severity from brand so info stays blue, warning stays yellow, and error stays red — regardless of whether the host's brand is teal, olive, or purple. Hosts that *do* want severity colors tied to their brand can still construct `AuiColors(...)` manually or `.copy()` the result.

---

## 5. Rendering rules

- Badges use `{severity}Container` as background, `on{Severity}Container` as text.
- Banners use `{severity}Container` as background, `on{Severity}Container` as text, with the icon tinted `{severity}` (the bold foreground) to draw the eye.
- All components pass through `LocalAuiTheme`. Never reach into `MaterialTheme` directly.
- Components are pure display — no `feedback` is accepted from JSON (the library simply ignores it since `data.feedback` is a pass-through field on `AuiBlock`).

---

## 6. iOS parity notes

When porting to AUI-iOS:

1. **Wire format is stable** — do not rename the `type` strings. Any JSON that renders correctly on Android must render on iOS.
2. **Color tokens** — introduce matching properties on the iOS equivalent of `AuiColors` (`AuiColorScheme` in SwiftUI). Default palette hex values above are authoritative.
3. **Icons** — use SF Symbols:
   - info → `info.circle.fill`
   - success → `checkmark.circle.fill`
   - warning → `exclamationmark.triangle.fill`
   - error → `xmark.octagon.fill`
4. **Shapes & spacing** — badge is a `Capsule()`, banner is `RoundedRectangle(cornerRadius: 8)`. Padding matches Android (medium horizontal + xSmall vertical for badge; medium for banner).
5. **No feedback** — all 8 variants are read-only. On iOS, mark them non-interactive (`allowsHitTesting(false)` is not required, but don't wire any button/tap recognizer).
6. **Sealed type hierarchy** — the Swift equivalent should be an enum case per block type on the `AUIBlock` enum, mirroring the Kotlin sealed class.
7. **AUI catalog prompt** — the iOS library's prompt generator must list all 8 status types verbatim — the hosted LLM should see an identical component menu regardless of platform.

---

## 7. Implementation checklist (Android)

- [ ] `aui-compose/.../theme/AuiColors.kt` — add 12 tokens (3 severities × 4 roles) to the data class, `Default`; add dark-mode companion `DefaultDark`; refactor `fromColorScheme()` to detect light/dark via `scheme.surface.luminance()` and pick stable severity palettes instead of pulling from brand tokens.
- [ ] `aui-core/.../model/data/StatusData.kt` — add 6 data classes (`BadgeInfoData`, `BadgeWarningData`, `BadgeErrorData`, `StatusBannerInfoData`, `StatusBannerWarningData`, `StatusBannerErrorData`).
- [ ] `aui-core/.../model/AuiBlock.kt` — add 6 subclasses + 6 serializer branches in `AuiBlockSerializer`.
- [ ] `aui-compose/.../components/status/` — add 6 composable files, each with a `@Preview`.
- [ ] `aui-compose/.../internal/BlockRenderer.kt` — add 6 `when` branches.
- [ ] `aui-core/.../AuiCatalogPrompt.kt` — extend `COMPONENTS` section and `ALL_COMPONENT_TYPES` list.
- [ ] `demo/src/main/assets/all-blocks-showcase.json` — add showcase cards for all 6 new variants.
- [ ] `README.md`, `docs/architecture.md` — update block count (18 → 24).
- [ ] Tests: parser round-trip + catalog prompt inclusion.
- [ ] `./gradlew build :aui-core:test :aui-compose:testDebugUnitTest` green.

---

## 8. Out of scope
- No new `feedback`/interactivity on any status component.
- No new animation or entrance/exit transitions.
- No "dismissible" banner variant (future work if needed).
- No change to `badge_success` or `status_banner_success` visual behavior.
