# AUI Prompt Filter — Design Spec

## Problem

`AuiCatalogPrompt.generate()` always advertises the full built-in block catalog to
the AI. Host apps have no way to trim this to only the blocks they actually use.
This matters for two reasons:

1. **Prompt size.** The full catalog is large. A focused app (e.g. a shopping
   assistant) only needs a subset — advertising 25+ components wastes tokens every
   request and dilutes the AI's attention.
2. **AI focus.** Fewer advertised components → higher signal per component → the
   model picks more purposefully rather than reaching for whatever fits.

---

## Core Decision: Prompt-Only Filtering

**Filtering affects `AuiCatalogPrompt` only. `BlockRenderer` is unchanged.**

The renderer already follows a best-effort, never-crash contract. Blocks that exist
in stored JSON from old conversations always render — there is no render-time
suppression. This is intentional:

- A host updating their `includeInPrompt` list should not break past conversations.
- The AI stops emitting excluded types in new messages (because it's no longer told
  about them), but old messages that already contain those blocks continue to render
  correctly.
- `onUnknownBlock` is the existing escape hatch for blocks the renderer genuinely
  cannot handle. Filtered blocks are not "unknown" — they're known and welcome.

---

## API

### `includeInPrompt` on `AuiPluginRegistry`

```kotlin
registry
    .register(NavigatePlugin)
    .includeInPrompt(
        AuiBlock.Text::class,
        AuiBlock.Heading::class,
        AuiBlock.ButtonPrimary::class,
        AuiBlock.QuickReplies::class,
        AuiBlock.RadioList::class,
        AuiBlock.InputRatingStars::class,
    )
```

**Type-safe:** takes `KClass<out AuiBlock>` — references sealed subclasses directly.
Compile error if a type doesn't exist. No string typos.

**Default (no call):** full catalog advertised — identical to current behavior.
No migration needed for existing host apps.

**Allowlist, not denylist:** `excludeFromPrompt` is not added. Allowlist is the
right model because:
- Hosts think in terms of "what my app uses", not "what to subtract from the full
  catalog"
- New library versions adding block types don't silently expand an exclusion-based
  prompt — with `includeInPrompt`, the prompt stays exactly what the host declared

**`AuiBlock.Unknown` guard:** `Unknown` is not a real block type and must not appear
in `includeInPrompt` calls. It is excluded from `blockTypeMap` so the compiler
makes this impossible.

---

## Implementation

### 1. `blockTypeMap` in `AuiPluginRegistry` (`aui-core`)

A private mapping from `KClass` → wire type string. This is the single canonical
source of truth for all built-in wire type strings (today these are scattered between
`AuiBlockSerializer` and `AuiCatalogPrompt`).

```kotlin
private val blockTypeMap: Map<KClass<out AuiBlock>, String> = mapOf(
    AuiBlock.Text::class              to "text",
    AuiBlock.Heading::class           to "heading",
    AuiBlock.Caption::class           to "caption",
    AuiBlock.FileContent::class       to "file_content",
    AuiBlock.ButtonPrimary::class     to "button_primary",
    AuiBlock.ButtonSecondary::class   to "button_secondary",
    AuiBlock.QuickReplies::class      to "quick_replies",
    AuiBlock.ChipSelectSingle::class  to "chip_select_single",
    AuiBlock.ChipSelectMulti::class   to "chip_select_multi",
    AuiBlock.RadioList::class         to "radio_list",
    AuiBlock.CheckboxList::class      to "checkbox_list",
    AuiBlock.InputTextSingle::class   to "input_text_single",
    AuiBlock.InputSlider::class       to "input_slider",
    AuiBlock.InputRatingStars::class  to "input_rating_stars",
    AuiBlock.Divider::class           to "divider",
    AuiBlock.StepperHorizontal::class to "stepper_horizontal",
    AuiBlock.ProgressBar::class       to "progress_bar",
    AuiBlock.BadgeInfo::class         to "badge_info",
    AuiBlock.BadgeSuccess::class      to "badge_success",
    AuiBlock.BadgeWarning::class      to "badge_warning",
    AuiBlock.BadgeError::class        to "badge_error",
    AuiBlock.StatusBannerInfo::class  to "status_banner_info",
    AuiBlock.StatusBannerSuccess::class to "status_banner_success",
    AuiBlock.StatusBannerWarning::class to "status_banner_warning",
    AuiBlock.StatusBannerError::class to "status_banner_error",
    // Add new built-in types here as they are implemented.
    // AuiBlock.Unknown::class is intentionally excluded.
)
```

### 2. `includeInPrompt` method + `allowedPromptTypes` field

```kotlin
// stored internally as wire type strings after the KClass lookup
private var allowedPromptTypes: Set<String>? = null   // null = no filter = full catalog

fun includeInPrompt(vararg types: KClass<out AuiBlock>): AuiPluginRegistry {
    allowedPromptTypes = types.map {
        blockTypeMap[it] ?: error(
            "AuiBlock subclass ${it.simpleName} is not in blockTypeMap. " +
            "Add it, or do not pass it to includeInPrompt()."
        )
    }.toSet()
    return this
}
```

### 3. `isAllowedInPrompt` internal helper

```kotlin
internal fun isAllowedInPrompt(wireType: String): Boolean {
    val allowed = allowedPromptTypes ?: return true   // no filter → allow all
    return wireType in allowed
}
```

### 4. `AuiCatalogPrompt.generate()` — filter built-in list

`AuiCatalogPrompt` already iterates `ALL_COMPONENT_TYPES` (or equivalent) to build
the "Available Components" section. After this change, it filters through the
registry before building that section:

```kotlin
val advertisedBuiltIns = ALL_COMPONENT_TYPES.filter { type ->
    pluginRegistry.isAllowedInPrompt(type)
}
```

Plugin component schemas from `pluginRegistry.allComponentPlugins()` are **not
filtered** — `includeInPrompt` applies to built-ins only. Plugin types are always
advertised if registered.

---

## What Does Not Change

| Thing | Status |
|---|---|
| `BlockRenderer` resolution order | Unchanged — renders everything regardless |
| `AuiBlock` sealed class | Unchanged |
| `AuiParser` | Unchanged — parses everything |
| `onUnknownBlock` callback | Unchanged — not involved in filtering |
| Plugin registration API | Unchanged |
| `AuiPluginRegistry.Empty` | Unchanged — no filter, full catalog |
| Default `generate()` behavior | Unchanged — full catalog when no filter set |

---

## Module Boundary

`includeInPrompt` and `blockTypeMap` live in **`aui-core`** alongside
`AuiPluginRegistry`. `AuiCatalogPrompt` is also in `aui-core`. Neither touches
`aui-compose`. The Compose boundary is not affected.

---

## Example Usage

```kotlin
// Shopping assistant — only component types relevant to product discovery
val registry = AuiPluginRegistry()
    .register(AddToCartPlugin)
    .register(OpenUrlPlugin)
    .includeInPrompt(
        AuiBlock.Text::class,
        AuiBlock.Heading::class,
        AuiBlock.Caption::class,
        AuiBlock.ButtonPrimary::class,
        AuiBlock.QuickReplies::class,
        AuiBlock.RadioList::class,
        AuiBlock.InputRatingStars::class,
        AuiBlock.BadgeSuccess::class,
        AuiBlock.BadgeError::class,
    )

// Full catalog (default) — no call to includeInPrompt needed
val registry = AuiPluginRegistry()
    .register(NavigatePlugin)
```

---

## Non-Goals

- No `excludeFromPrompt` API — allowlist only.
- No render-time filtering — the renderer is a pure, forgiving renderer.
- No per-conversation filter versioning — out of scope.
- Plugin types are always advertised if registered — `includeInPrompt` is for
  built-ins only.
