# AUI Android

> **🚧 Work in Progress — Not ready for production use.**
> This library is under active development and has **not been published to Maven Central** yet. APIs may change without notice. Feel free to explore the code and the demo app, but do not add it as a dependency in your project.

An open-source Kotlin library for rendering AI-driven interactive UI in Jetpack Compose.

AI assistants respond with JSON describing pre-built native components instead of plain text.
AUI parses the JSON and renders native Compose UI — cards, forms, chips, buttons, surveys — inside your app.

## Visual Examples

| AI-Generated Survey | All Blocks Showcase |
|:---:|:---:|
| The AI builds a multi-step survey from JSON and presents it as a native bottom sheet. Each step collects user input and the consolidated result is delivered to your app via a single callback. | A scrollable gallery of every built-in AUI component — text, cards, lists, inputs, status, media, and layout blocks — rendered from JSON in their inline, expanded, and survey display modes. Use it to preview the catalog, switch themes on the fly, and see how each block reacts to user interaction. |
| <img src="docs/assets/ai-generated-survey-example.gif" width="300" /> | <img src="docs/assets/all-blocks-show-case.gif" width="300" /> |

## How It Works

AUI connects your app and an AI assistant through three steps:

```
┌──────────┐          ┌─────────┐          ┌─────────────┐
│ AUI      │  prompt  │   AI    │   JSON   │ AUI         │
│ Core     │ ───────▶ │ (Cloud) │ ───────▶ │ Compose     │
│ (prompt) │          │         │          │ (renderer)  │
└──────────┘          └─────────┘          └──────┬──────┘
                                                  │
                                           native Compose UI
                                                  │
                                                  ▼
                                           ┌──────────────┐
                                           │   Your App   │◀── user taps
                                           └──────────────┘    (AuiFeedback)
```

**Step 1 — AUI generates a prompt describing its components.** Your app includes it in the AI's system prompt.

```kotlin
val systemPrompt = "You are a helpful assistant.\n\n" +
    AuiCatalogPrompt.generate()
```

The generated prompt tells the AI what components exist (buttons, chips, forms, rating inputs, etc.) and how to format responses. It stays in sync with the library automatically.

**Step 2 — The AI responds with structured JSON.** Instead of plain text, the AI returns a JSON envelope with an optional `aui` field containing native UI components:

```json
{
  "text": "Which feature should we build next?",
  "aui": {
    "display": "expanded",
    "blocks": [
      { "type": "radio_list", "data": {
          "key": "feature",
          "options": [
            { "label": "Dark mode", "value": "dark_mode" },
            { "label": "Export to PDF", "value": "export_pdf" }
          ]
      }},
      { "type": "button_primary", "data": { "label": "Vote" },
        "feedback": { "action": "submit", "params": {} }
      }
    ]
  }
}
```

For text-only replies, the AI simply omits the `aui` field: `{ "text": "Sure, happy to help!" }`.

**Step 3 — AUI renders native Compose UI.** Your app passes the JSON to `AuiRenderer`, which parses it and renders native components. When the user interacts (taps a button, submits a form), your app receives an `AuiFeedback` callback:

```kotlin
AuiRenderer(
    json = auiJson,
    onFeedback = { feedback ->
        // feedback.action  → "submit"
        // feedback.params  → { "feature": "dark_mode" }
        sendToAI(feedback)
    }
)
```

The library is a **pure renderer with a callback**. It does not manage chat history, conversation state, networking, or message models — those are your app's domain.

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
    append(AuiCatalogPrompt.generate(pluginRegistry = myPluginRegistry))
}
```

`AuiCatalogPrompt.generate()` returns the full component catalog so your AI knows
what components are available and how to format responses. When you pass a
`pluginRegistry`, plugin component schemas and action schemas are included
automatically. It stays in sync with the library automatically.

You can tune the prompt tone and add domain-specific examples via `AuiPromptConfig`:

```kotlin
val systemPrompt = buildString {
    append("You are a shopping assistant.\n\n")
    append(AuiCatalogPrompt.generate(
        pluginRegistry = myPluginRegistry,
        config = AuiPromptConfig(
            aggressiveness = Aggressiveness.Eager,
            customExamples = listOf(
                AuiPromptExample(
                    title = "Product comparison",
                    json = """{ "text": "Compare:", "aui": { "display": "expanded", "blocks": [...] } }"""
                )
            )
        )
    ))
}
```

- **Aggressiveness**: `Conservative` (plain text default), `Balanced` (default — use components when helpful), `Eager` (prefer components for links, lists, choices).
- **Custom examples**: Appended after built-in examples. Teach the model your domain patterns without losing the library's foundational examples.

### 4. Enable prompt caching (recommended)

The AUI catalog prompt is large but identical across every request in a conversation,
making it an ideal candidate for **prompt caching**. With caching enabled, the catalog
tokens are processed once and reused on subsequent requests — significantly reducing
cost and latency.

Most LLM providers support this by marking the system prompt as cacheable:

- **Anthropic (Claude):** [Prompt Caching](https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching) — send the system prompt as a content block with `cache_control` and add the `anthropic-beta: prompt-caching-2024-07-31` header. Cached tokens cost 90% less.
- **OpenAI:** [Prompt Caching](https://platform.openai.com/docs/guides/prompt-caching) — caching is automatic for prompts longer than 1,024 tokens. No code changes needed. Cached tokens cost 50% less.

## Display Levels

The AI chooses how prominently to present each response:

| Level | When to use | Behavior |
|-------|-------------|----------|
| **expanded** | Quick info, status badges, polls, rich content, product cards | Full-width in the chat feed |
| **survey** | Multi-step surveys, forms, bookings | Bottom sheet overlay with library-injected Back/Next/Submit |

## Handling Surveys

Surveys open automatically and the library injects Back/Next/Submit navigation around
each step. When the user submits or dismisses, `onFeedback` fires once with the
consolidated result.

**Important:** After `onFeedback` fires for a survey, set the AUI JSON to `null` so it
doesn't re-open if the user scrolls back:

```kotlin
fun onAuiFeedback(messageId: String, feedback: AuiFeedback) {
    // Mark survey as consumed
    if (feedback.stepsTotal != null) {
        markAuiConsumed(messageId)  // e.g., set auiJson = null
    }

    // Use the feedback however your app needs
    addUserMessage(feedback.formattedEntries ?: "Submitted")
    sendToAI(feedback)
}
```

If the host app forgets to consume the survey, it won't crash or re-open — the composable
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

## Customization

AUI's plugin system lets you add custom components, override built-ins, and register app-specific actions.

### Custom Component

Define a data class, implement `AuiComponentPlugin`, and register it:

```kotlin
@Serializable
data class FunFactData(val title: String, val fact: String, val source: String? = null)

object FunFactPlugin : AuiComponentPlugin<FunFactData>() {
    override val id = "fun_fact"
    override val componentType = "demo_fun_fact"
    override val dataSerializer = FunFactData.serializer()
    override val promptSchema = "demo_fun_fact(title, fact, source?) — A colorful fun-fact card."

    @Composable
    override fun Render(data: FunFactData, onFeedback: (() -> Unit)?, modifier: Modifier) {
        Card(onClick = { onFeedback?.invoke() }, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(data.title, style = LocalAuiTheme.current.typography.heading)
                Text(data.fact, style = LocalAuiTheme.current.typography.body)
            }
        }
    }
}
```

### Custom Action

Action plugins handle side effects like navigation or opening URLs:

```kotlin
class OpenUrlPlugin(private val context: Context) : AuiActionPlugin() {
    override val id = "open_url"
    override val action = "open_url"
    override val promptSchema = "open_url(url) — Open the given URL in the device browser."

    override fun handle(feedback: AuiFeedback): Boolean {
        val url = feedback.params["url"] ?: return false
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        return true  // claimed — onFeedback will not be called
    }
}
```

Action plugins use chain-of-responsibility: return `true` to claim the feedback (host `onFeedback` skipped), or `false` to pass through.

### Override a Built-in

Register a plugin whose `componentType` matches a built-in's type — the plugin takes priority:

```kotlin
object MyCardBasicPlugin : AuiComponentPlugin<CardBasicData>() {
    override val id = "my_card_basic"
    override val componentType = "card_basic"  // shadows built-in card_basic
    override val dataSerializer = CardBasicData.serializer()
    override val promptSchema = ""  // empty — the built-in schema is already in the catalog

    @Composable
    override fun Render(data: CardBasicData, onFeedback: (() -> Unit)?, modifier: Modifier) {
        // Your custom rendering for card_basic
    }
}
```

### Building the Registry

Build one `AuiPluginRegistry` and pass it to both the renderer and the prompt generator:

```kotlin
val pluginRegistry = AuiPluginRegistry().registerAll(
    FunFactPlugin,
    OpenUrlPlugin(context),
    MyCardBasicPlugin,
)

// Renderer — plugins render custom blocks and handle actions
AuiRenderer(
    json = json,
    pluginRegistry = pluginRegistry,
    onFeedback = { /* only called for unclaimed feedback */ }
)

// Prompt — plugin schemas are included automatically
val systemPrompt = AuiCatalogPrompt.generate(pluginRegistry = pluginRegistry)
```

## Component Catalog

18 component types across these categories:

| Category | Components |
|----------|-----------|
| **Display** | `text`, `heading`, `caption` |
| **Input** | `chip_select_single`, `chip_select_multi`, `button_primary`, `button_secondary`, `quick_replies`, `input_rating_stars`, `input_text_single`, `input_slider`, `radio_list`, `checkbox_list` |
| **Layout** | `divider` |
| **Progress** | `stepper_horizontal`, `progress_bar` |
| **Status** | `badge_success`, `status_banner_success` |

The `text` component renders inline Markdown: `**bold**`, `*italic*`, `` `code` ``, and `[links](url)`. Structural Markdown (headings, lists, etc.) uses dedicated block types instead.

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
- **`AuiPluginRegistry`** — Register and look up plugins. Pass to both renderer and prompt generator.
- **`AuiComponentPlugin<T>`** — Add or override component types with custom Compose rendering.
- **`AuiActionPlugin`** — Handle named actions (navigation, URLs, etc.) with chain-of-responsibility routing.

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
