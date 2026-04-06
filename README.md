# AUI Android

An open-source Kotlin library for rendering AI-driven interactive UI in Jetpack Compose.

AI assistants respond with JSON describing pre-built native components instead of plain text.
AUI parses the JSON and renders native Compose UI — cards, forms, chips, buttons, sheets — inside your app.

## Quick Start

### 1. Add the dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.bennyjon.aui:aui-compose:0.1.0")
}
```

### 2. Render AI responses

```kotlin
@Composable
fun AiMessageBubble(auiJson: String?) {
    auiJson?.let { json ->
        AuiRenderer(
            json = json,
            theme = AuiTheme.fromMaterialTheme(),
            onFeedback = { feedback ->
                // feedback.action — machine-readable action name
                // feedback.formattedEntries — human-readable Q&A summary
                // feedback.params — structured key-value data
                viewModel.handleFeedback(feedback)
            }
        )
    }
}
```

That's it. Three lines: dependency, `AuiRenderer`, `onFeedback` callback.

### 3. Include the AUI schema in your AI system prompt

```kotlin
val systemPrompt = buildString {
    append("You are a helpful assistant.\n\n")
    append(AuiCatalogPrompt.generate(
        availableActions = listOf("navigate", "submit", "open_url")
    ))
}
```

`AuiCatalogPrompt.generate()` returns the full component catalog so your AI knows
what components are available and how to format responses. It stays in sync with the
library automatically.

## How It Works

```
Your App                          AUI Library
─────────                         ───────────
AI Client  ──JSON string──▶  AuiRenderer (Composable)
                                   ├── Parses JSON
                                   ├── Routes to display level
                                   │   (inline / expanded / sheet)
                                   ├── Renders native Compose components
                                   └── Reports interactions via callback
                                          │
Chat UI  ◀──AuiFeedback────────────────────┘
```

The library is a **pure renderer with a callback**. It does not manage chat history,
conversation state, networking, or message models. Those are your app's domain.

## Display Levels

The AI chooses how prominently to present each response:

| Level | When to use | Behavior |
|-------|-------------|----------|
| **inline** | Quick info, status badges, simple replies | Renders inside the chat bubble |
| **expanded** | Rich content, polls, product cards | Full-width in the chat feed |
| **sheet** | Multi-step surveys, forms, bookings | Bottom sheet overlay with step navigation |

## Handling Sheets

Sheets open automatically and handle their own step navigation. When the user submits
or dismisses, `onFeedback` fires once with the consolidated result.

**Important:** After `onFeedback` fires for a sheet, set the AUI JSON to `null` so it
doesn't re-open if the user scrolls back:

```kotlin
fun onAuiFeedback(messageId: String, feedback: AuiFeedback) {
    // Mark sheet as consumed
    if (feedback.stepsTotal != null) {
        markAuiConsumed(messageId)  // e.g., set auiJson = null
    }

    // Use the feedback however your app needs
    addUserMessage(feedback.formattedEntries ?: "Submitted")
    sendToAI(feedback)
}
```

If the host app forgets to consume the sheet, it won't crash or re-open — the composable
is inert after the first submission (provided you use stable `LazyColumn` keys).

## Theming

AUI components never hardcode colors, fonts, or spacing. Everything goes through `AuiTheme`.

```kotlin
// Option A: Auto-map from your existing MaterialTheme
AuiRenderer(json = json, theme = AuiTheme.fromMaterialTheme(), ...)

// Option B: Provide a custom theme
val myTheme = AuiTheme(
    colors = AuiColors(primary = Color(0xFF6750A4), ...),
    typography = AuiTypography(heading = TextStyle(fontFamily = YourFont, ...), ...),
    spacing = AuiSpacing.Default,
    shapes = AuiShapes.Default
)
AuiRenderer(json = json, theme = myTheme, ...)
```

## Component Catalog

19 component types across these categories:

| Category | Components |
|----------|-----------|
| **Display** | `text`, `heading`, `caption` |
| **Input** | `chip_select_single`, `chip_select_multi`, `button_primary`, `button_secondary`, `quick_replies`, `input_rating_stars`, `input_text_single`, `input_slider`, `radio_list`, `checkbox_list` |
| **Layout** | `divider`, `spacer` |
| **Progress** | `stepper_horizontal`, `progress_bar` |
| **Status** | `badge_success`, `status_banner_success` |

Unknown component types are silently skipped (never crash). You can handle them with `onUnknownBlock`.

## Modules

| Module | Description | Dependencies |
|--------|-------------|-------------|
| `aui-core` | Pure Kotlin. JSON parsing, data models, validation. | Kotlinx Serialization |
| `aui-compose` | Jetpack Compose renderer, theme, components. | aui-core, Compose, Coil |
| `demo` | Sample chat app. NOT part of the library. | aui-compose |

`aui-compose` transitively includes `aui-core`, so most apps just add one dependency.

## Public API

The library exposes a deliberately small surface:

- **`AuiRenderer`** — The main composable. Two overloads: `(json: String, ...)` and `(response: AuiResponse, ...)`.
- **`AuiTheme`** — Theme data class with `AuiColors`, `AuiTypography`, `AuiSpacing`, `AuiShapes`.
- **`AuiFeedback`** — Callback data: `action`, `params`, `formattedEntries`, `entries`, `stepsSkipped`, `stepsTotal`.
- **`AuiCatalogPrompt`** — Generates AI system prompt text from the component catalog.
- **`AuiParser`** — JSON parser (used internally by `AuiRenderer`, but available if you need pre-parsing).
- **`AuiResponse`** / **`AuiBlock`** — Data models for parsed responses.

Everything else is `internal`.

## Building

```bash
./gradlew build                              # Build all
./gradlew :aui-core:test                     # Test core module
./gradlew :aui-compose:testDebugUnitTest     # Test compose module
./gradlew :demo:installDebug                 # Run demo app
```

## Requirements

- Kotlin 1.9+
- Jetpack Compose (BOM)
- Min SDK 26, Target SDK 35

## Documentation

- [Architecture](docs/architecture.md) — Module structure, public API, design decisions
- [AUI Spec](spec/aui-spec-v1.md) — Full JSON format specification
- [JSON Examples](spec/examples/) — Sample responses for each display level

## License

Apache 2.0
