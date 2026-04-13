# Session: AuiCatalogPrompt — Tuning Knobs & Content Improvements

Read `CLAUDE.md` and `aui-core/src/main/kotlin/com/bennyjon/aui/core/AuiCatalogPrompt.kt` before starting.

This session adds host-configurable tuning to `AuiCatalogPrompt.generate()` and improves the default prompt content so AI models produce richer, more creative AUI responses out of the box.

---

## Motivation

Real-world testing showed two problems with the current generated prompt:

1. **Models play it safe.** The prompt's "AUI is OPTIONAL and ADDITIVE. Most responses should remain plain text" framing anchors the model toward quick_replies + occasional sheets, and away from richer uses like tappable link buttons, product cards, or expanded lists.
2. **Host apps have no way to tune this.** Some apps want a conservative assistant; others (shopping, discovery) want an eager one that reaches for components readily. The current `generate(registry)` API is a fixed artifact.

The fix has two parts: a small config API, and better default content.

---

## Part 1 — New API: `AuiPromptConfig`

Add a config parameter to `AuiCatalogPrompt.generate()` with two knobs.

### New types (in `aui-core`, same file as `AuiCatalogPrompt`)

```kotlin
/**
 * Tuning knobs for [AuiCatalogPrompt.generate].
 *
 * The library owns the schema (components, actions, envelope format).
 * Host apps own the voice — aggressiveness tunes framing tone, and
 * customExamples lets hosts teach the model domain-specific patterns.
 */
data class AuiPromptConfig(
    /**
     * Controls how eagerly the AI should reach for AUI components vs. plain text.
     * Swaps the framing paragraph near the top of the prompt. Defaults to [Balanced].
     */
    val aggressiveness: Aggressiveness = Aggressiveness.Balanced,

    /**
     * Additional examples appended to the built-in EXAMPLES section.
     * Useful for teaching the model domain-specific patterns (e.g. shopping
     * cards, product lists) without losing the library's default examples,
     * which teach envelope format and sheet mechanics.
     */
    val customExamples: List<AuiPromptExample> = emptyList()
)

enum class Aggressiveness {
    /** "Default to plain text. Only use AUI when it clearly adds value over prose." */
    Conservative,
    /** "Use AUI whenever a component makes the response more useful, actionable, or scannable." */
    Balanced,
    /** "Prefer rich components for responses involving links, lists, comparisons, or choices." */
    Eager
}

/**
 * A domain-specific example appended to the EXAMPLES section of the generated prompt.
 *
 * @param title Short description of what the example demonstrates (rendered as a section label).
 * @param json Raw AUI response JSON — the full envelope, including the "text" field.
 */
data class AuiPromptExample(
    val title: String,
    val json: String
)
```

### Updated `generate` signature

```kotlin
fun generate(
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    config: AuiPromptConfig = AuiPromptConfig()
): String
```

Keep the existing single-arg overload working (default config). No breaking change.

---

## Part 2 — How the knobs affect output

### `aggressiveness`

Replaces the current framing paragraph (the "AUI is OPTIONAL and ADDITIVE..." block near the top) with one of three variants:

- **Conservative** — keep the current conservative framing roughly as-is: "AUI is optional. Default to plain text. Only use components when they clearly add value over prose."
- **Balanced** (default) — "Use AUI whenever a component makes the response more useful, actionable, or scannable than prose. Plain text is fine for pure conversation, explanations, and single-sentence answers."
- **Eager** — "Prefer rich components for any response involving links, lists, comparisons, or choices. Plain text is for pure conversation and short explanations."

Pick the exact wording you think works best — the above is directional.

### `customExamples`

Appended **after** the built-in examples in the EXAMPLES section. Format each as:

```
Example: <title>
<json>
```

Built-in examples always remain — they teach envelope format and sheet mechanics, which host examples shouldn't have to re-teach.

---

## Part 3 — Content improvements to the default prompt

Regardless of config, the default generated prompt should be improved in the following ways. These address the "model plays it safe" problem at the root.

### 3a. Add a "When to use what" cheat sheet

After the `AVAILABLE COMPONENTS` / `AVAILABLE ACTIONS` sections, add a short guidance block. The model knows component signatures but not their *intent*. Something like:

```
WHEN TO REACH FOR WHICH COMPONENT:
  - Links / URLs → button_primary or button_secondary with action=open_url.
    Never render a URL as plain text when a tappable button is possible.
  - Lists of products / places / options → expanded display with a block
    per item, each with its own action button(s).
  - Comparing or picking between options → radio_list or chip_select_single + submit.
  - Multi-select preferences → checkbox_list or chip_select_multi + submit.
  - Rating or feedback collection → sheet with input_rating_stars.
  - Suggesting next actions or conversational branches → quick_replies at the end.
  - Numeric input within a range → input_slider.
```

Tune wording to match the rest of the prompt's voice.

### 3b. Expand the EXAMPLES section

The current examples (inline poll, sheet survey) only cover input collection. Add **two more built-in examples** that unlock creative usage:

**Example A — Expanded response with tappable link buttons (shopping / recommendations):**

```json
{
  "text": "Here are three solid options:",
  "aui": {
    "display": "expanded",
    "blocks": [
      { "type": "heading", "data": { "text": "Sony WH-1000XM5" } },
      { "type": "text", "data": { "text": "Top-tier noise cancellation, around $348." } },
      { "type": "button_primary",
        "data": { "label": "View on Amazon" },
        "feedback": { "action": "open_url", "params": { "url": "https://example.com/sony" } }
      },
      { "type": "divider" },
      { "type": "heading", "data": { "text": "Bose QuietComfort Ultra" } },
      { "type": "text", "data": { "text": "Excellent comfort for long wear, around $379." } },
      { "type": "button_primary",
        "data": { "label": "View on Amazon" },
        "feedback": { "action": "open_url", "params": { "url": "https://example.com/bose" } }
      }
    ]
  }
}
```

**Example B — Quick replies with per-option feedback (each chip fires its own action):**

Show that each quick_replies option can carry its own `feedback` object — tapping a chip can open a URL or submit different params. The current prompt never demonstrates this and the model doesn't discover it on its own.

```json
{
  "text": "Want to learn more?",
  "aui": {
    "display": "inline",
    "blocks": [
      { "type": "quick_replies", "data": {
          "options": [
            { "label": "Read the docs",
              "feedback": { "action": "open_url", "params": { "url": "https://example.com/docs" } } },
            { "label": "See examples",
              "feedback": { "action": "open_url", "params": { "url": "https://example.com/examples" } } },
            { "label": "Explain it like I'm 5",
              "feedback": { "action": "submit", "params": { "topic":"Explain it like I'm 5" } } }
          ]
      }}
    ]
  }
}
```

### 3c. Tighten the "Every interactive component MUST have a feedback object" note
 
This is correct but the model sometimes over-applies it (e.g., tries to put feedback on `radio_list`, which passively collects state). Clarify with a one-line aside — KDoc on `AuiCatalogPrompt` and the prompt itself should already be consistent on this, but double-check the wording matches our domain understanding: **collectors** (`radio_list`, `checkbox_list`, `chip_select_*`, `input_*`) don't need feedback on themselves; only **triggers** (`button_*`, `quick_replies` options) fire actions.
 
---

## Part 4 — Tests

Add unit tests in `aui-core`:

1. **Default output unchanged signature-wise.** `generate()` with no args still returns a valid, non-empty prompt containing all expected section headers.
2. **Aggressiveness swap.** Each of the three enum values produces a prompt with distinguishable framing text (assert on a signature phrase per level).
3. **Custom examples are appended.** Passing two `AuiPromptExample`s results in both titles and both JSON blobs appearing after the built-in examples, in order.
4. **Custom examples never replace built-ins.** Built-in example signature phrases (e.g. "feature_choice" from the poll example) still appear alongside custom ones.
5. **Plugin schemas still appear** when a registry is passed — existing behavior preserved.

---

## Part 5 — Documentation

1. **KDoc** on `AuiPromptConfig`, `Aggressiveness`, `AuiPromptExample`, and the updated `generate()` overload. Emphasize: *the library owns the schema; host apps own the voice*.
2. **README update.** In the "Customization" section (or wherever `AuiCatalogPrompt` is first introduced), add a short subsection showing:
   - Default usage (unchanged).
   - Picking an aggressiveness level.
   - Adding a domain-specific custom example.
   Keep it to ~15 lines of example code total.
3. **CLAUDE.md.** Add a bullet under Key Design Decisions:
   > `AuiCatalogPrompt` exposes two tuning knobs via `AuiPromptConfig`: `aggressiveness` (Conservative/Balanced/Eager) swaps framing tone; `customExamples` appends domain-specific examples to built-in ones. Philosophy: library owns schema, host owns voice.

---

## Deliverables checklist

- [x] `AuiPromptConfig`, `Aggressiveness`, `AuiPromptExample` added to `aui-core`
- [x] `AuiCatalogPrompt.generate()` accepts config; single-arg overload preserved
- [x] Three framing variants wired to `Aggressiveness`
- [x] Custom examples appended after built-ins
- [x] "When to reach for which component" cheat sheet added to default prompt
- [x] Two new built-in examples added (tappable link buttons, per-option quick_replies feedback)
- [x] Collector-vs-trigger feedback clarification verified
- [x] Unit tests covering all five cases in Part 4
- [x] KDoc + README + CLAUDE.md updated

