# AUI Android

> **Released on Maven Central.**
> Current first public version: `com.bennyjon:aui-compose:0.1.0-alpha01`.
> APIs are pre-1.0 and may change between alpha versions. Release mechanics and
> follow-up checklist notes live in [`.planning/release-checklist.md`](.planning/release-checklist.md).

An open-source Kotlin library for rendering AI-driven interactive UI in Jetpack Compose.

AI assistants respond with JSON describing pre-built native components instead of plain text.
AUI parses the JSON and renders native Compose UI — cards, forms, chips, buttons, surveys — inside your app.

## Visual Examples

| AI-Generated Survey |
|:---:|
| The AI builds a multi-step survey from JSON. The library manages step navigation and consolidation; the demo hosts it in a bottom sheet, but hosts are free to pick any container. A single callback delivers the final answers to your app. |
| <img src="docs/assets/ai-generated-survey-example.gif" width="300" /> |

### AUI Blocks Examples

Grouped from foundational content and inputs through richer layouts, status UI, data views, and host-routed expanded flows.

<table>
  <tr>
    <td width="50%" valign="top">
      <strong>Foundations + Expanded Card + Bar Chart</strong><br/>
      <code>text</code>, <code>heading</code>, <code>caption</code>, <code>expanded</code>, <code>chart</code> (<code>bar</code>)
    </td>
    <td width="50%" valign="top">
      <strong>Actions + Table</strong><br/>
      <code>table</code>, <code>button_primary</code>, <code>button_secondary</code>, <code>quick_replies</code>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/display%20text,heading,caption,expanded-card,bar-chart.png" height="640" />
    </td>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/table,inputs%20button%20primary,%20secondary,%20quick%20actions.png" height="640" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Selection Inputs</strong><br/>
      <code>chip_select_single</code>, <code>chip_select_multi</code>, <code>radio_list</code>, <code>checkbox_list</code>
    </td>
    <td width="50%" valign="top">
      <strong>Text Input + Slider + Ratings + Layout</strong><br/>
      <code>input_text_single</code>, <code>input_slider</code>, <code>input_rating_stars</code>, layout examples, <code>stepper_horizontal</code>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/single%20select%20chips,%20multiple%20select%20chips,%20radio%20button%20list,%20checkbox%20list.png" height="640" />
    </td>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/textinput,%20slider,%20ratings,%20layout%20examples,%20stepper.png" height="640" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Status Badges + Progress</strong><br/>
      <code>badge_info</code>, <code>badge_success</code>, <code>badge_warning</code>, <code>badge_error</code>, <code>progress_bar</code>
    </td>
    <td width="50%" valign="top">
      <strong>Status Banners + Plugins</strong><br/>
      <code>status_banner_info</code>, <code>status_banner_success</code>, <code>status_banner_warning</code>, <code>status_banner_error</code>, plugin-rendered blocks
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/status%20badges,%20progress%20bar.png" height="640" />
    </td>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/status%20banners,%20plugins.png" height="640" />
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Chart Variants</strong><br/>
      <code>chart</code> (<code>line</code>, <code>pie</code>)
    </td>
    <td width="50%" valign="top">
      <strong>Composite Flows</strong><br/>
      Mixed compositions, <code>expanded</code> cards, and host-routed <code>survey</code> flows
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/line-chart,pie-chart.png" height="640" />
    </td>
    <td align="center">
      <img src="docs/assets/screenshots-all-blocks/compositions%20expandable%20cards,%20survey.png" height="640" />
    </td>
  </tr>
</table>

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
    "display": "inline",
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
For interactive responses, keep one `AuiRenderState` per logical response/message id and pass it
back into `AuiRenderer` anywhere that same response is shown. That lets input values and survey
progress survive remounts and stay in sync across inline, sheet, and detail-pane surfaces.

## Quick Start

### 1. Add the dependency

`0.1.0-alpha01` is available on Maven Central:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.bennyjon:aui-compose:0.1.0-alpha01")
}
```

`aui-compose` transitively depends on `aui-core`, so a single line is enough.

If you need to work from unreleased source instead of the published artifact,
either include the modules via Gradle's
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html)
feature, or copy `aui-core` and `aui-compose` into your project and add:

```kotlin
dependencies {
    implementation(project(":aui-compose"))
}
```

### 2. Render AI responses

```kotlin
@Composable
fun AiMessageBubble(
    response: AuiResponse,
    onFeedback: (AuiFeedback) -> Unit,
) {
    val renderState = rememberAuiRenderState()

    AuiRenderer(
        response = response,
        state = renderState,
        theme = AuiTheme.fromMaterialTheme(),
        onFeedback = onFeedback,
    )
}
```

That's it. Three lines: dependency, `AuiRenderer`, `onFeedback` callback.

Simple cases can use `rememberAuiRenderState()`. Hosts with a ViewModel typically keep
`Map<messageId, AuiRenderState>` and reuse the same instance for the same response.

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

## Canonical Host Integration

The recommended host model is one assistant turn with always-present text plus
optional AUI:

```kotlin
data class AssistantTurn(
    val id: String,
    val text: String,
    val auiResponse: AuiResponse? = null,
)
```

That one shape covers:

- text-only reply: `text != ""`, `auiResponse == null`
- AUI-only reply: `text == ""`, `auiResponse != null`
- text + AUI in one turn: both fields present

For interactive responses, keep one `AuiRenderState` per `AssistantTurn.id`
and reuse that same instance anywhere the same response is shown.

```kotlin
class ChatViewModel : ViewModel() {
    private val renderStates = mutableMapOf<String, AuiRenderState>()

    var activeDetailTurnId by mutableStateOf<String?>(null)
        private set

    fun renderStateFor(turnId: String): AuiRenderState =
        renderStates.getOrPut(turnId) { AuiRenderState() }

    fun openDetail(turnId: String) {
        activeDetailTurnId = turnId
    }

    fun closeDetail() {
        activeDetailTurnId = null
    }

    fun onAuiFeedback(feedback: AuiFeedback) {
        sendUserMessage(
            text = feedback.formattedEntries ?: feedback.action,
            metadata = feedback.params,
        )
    }
}
```

Render assistant text inline, then choose the AUI surface from
`response.display`:

```kotlin
@Composable
fun AssistantTurnRow(
    turn: AssistantTurn,
    renderState: AuiRenderState,
    isDetailOpen: Boolean,
    onOpenDetail: () -> Unit,
    onCloseDetail: () -> Unit,
    onFeedback: (AuiFeedback) -> Unit,
) {
    Column {
        if (turn.text.isNotBlank()) {
            AssistantTextBubble(turn.text)
        }

        val response = turn.auiResponse ?: return@Column
        when (response.display) {
            AuiDisplay.INLINE -> AuiRenderer(
                response = response,
                state = renderState,
                theme = AuiTheme.fromMaterialTheme(),
                onFeedback = onFeedback,
            )

            AuiDisplay.EXPANDED,
            AuiDisplay.SURVEY -> AuiResponseCard(
                response = response,
                isActive = isDetailOpen,
                onClick = onOpenDetail,
            )
        }
    }

    if (isDetailOpen) {
        val response = turn.auiResponse ?: return
        ModalBottomSheet(onDismissRequest = onCloseDetail) {
            AuiRenderer(
                response = response,
                state = renderState,
                theme = AuiTheme.fromMaterialTheme(),
                onFeedback = { feedback ->
                    onFeedback(feedback)
                    if (response.display == AuiDisplay.SURVEY) onCloseDetail()
                },
            )
        }
    }
}
```

Recommended routing:

- `inline`: render directly in the timeline
- `expanded`: show an `AuiResponseCard`, then open the full `AuiRenderer` in a
  sheet, dialog, or side pane when tapped
- `survey`: also host-owned; the library manages step navigation and final
  consolidated submit, but the host owns the container lifecycle

The same `AuiRenderState` instance is what makes a dismissed survey reopen at
the same step or a detail-pane render stay in sync with any duplicate surface.

### 4. Enable prompt caching (recommended)

The AUI catalog prompt is large but identical across every request in a conversation,
making it an ideal candidate for **prompt caching**. With caching enabled, the catalog
tokens are processed once and reused on subsequent requests — significantly reducing
cost and latency.

Most LLM providers support this by marking the system prompt as cacheable:

- **Anthropic (Claude):** [Prompt Caching](https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching) — send the system prompt as a content block with `cache_control` and add the `anthropic-beta: prompt-caching-2024-07-31` header. Cached tokens cost 90% less.
- **OpenAI:** [Prompt Caching](https://platform.openai.com/docs/guides/prompt-caching) — caching is automatic for prompts longer than 1,024 tokens. No code changes needed. Cached tokens cost 50% less.

## Display Levels

The AI chooses how prominently to present each response. The library itself renders
`inline` and `expanded` identically — both signal the AI's **intent**. Hosts decide
whether to surface `expanded` responses in a separate detail surface.

| Level | When to use | Library behavior |
|-------|-------------|------------------|
| **inline** | Quick replies, polls, short confirmations, single cards — anything that belongs in the chat flow. | Renders in place. Leading `text` / `heading` / `caption` blocks form the chat bubble; the rest render full-width below. |
| **expanded** | 3+ rich cards, image galleries, comparisons, long-form content the user may want to study. | Renders identically to `inline`. Hosts may route it to a bottom sheet (narrow windows) or a side detail pane (wide windows) using the included [`AuiResponseCard`](#expanded-content) stub. |
| **survey** | Multi-page structured input — 2+ questions, feedback forms, onboarding flows. | Flat content with library-injected Back / Next / Submit navigation, stepper indicator, and consolidated submission. The library does **not** wrap itself in a bottom sheet — hosts own the container. |

## Error Handling

AUI has two parser modes, and they serve different host needs:

- `AuiParser.parse(json)`: strict. Use when AUI JSON is already trusted or pre-validated. It throws if the top-level response is unusable.
- `AuiParser.parseOrNull(json)`: tolerant. Use when the JSON comes directly from an LLM or any other untrusted source. It returns `null` only when the top-level response cannot be understood at all.

`parseOrNull` salvages partial responses instead of failing the entire render:

- Unknown block `type` values become `AuiBlock.Unknown`.
- Malformed known blocks are downgraded to `AuiBlock.Unknown` when the top-level response is still valid.
- Unknown fields on known block types are ignored.
- Missing or unknown top-level `display` still fails the whole parse and returns `null`.

The renderer follows the same fail-soft policy:

- `AuiRenderer(json = ...)` uses strict parsing internally. If the whole payload is unusable, it renders nothing and calls `onParseError`.
- When parsing succeeds but some blocks are unknown, the renderer checks the plugin registry first. If no component plugin can render that block, it skips just that block and calls `onUnknownBlock`.
- If a plugin-backed unknown block has missing or malformed `rawData`, the renderer also skips just that block and calls `onUnknownBlock`.

Recommended host pattern:

```kotlin
val parser = AuiParser()
val response = parser.parseOrNull(auiJson)

if (response != null) {
    AuiRenderer(
        response = response,
        state = renderState,
        theme = AuiTheme.fromMaterialTheme(),
        pluginRegistry = pluginRegistry,
        onFeedback = onFeedback,
    )
} else {
    AssistantTextBubble("I couldn't render that interactive response.")
}
```

If you prefer the raw JSON overload, wire `onParseError` and `onUnknownBlock` explicitly:

```kotlin
AuiRenderer(
    json = auiJson,
    state = renderState,
    theme = AuiTheme.fromMaterialTheme(),
    pluginRegistry = pluginRegistry,
    onFeedback = onFeedback,
    onParseError = { error -> logger.warn("AUI parse failed: $error") },
    onUnknownBlock = { block -> logger.warn("Skipped AUI block type=${block.type}") },
)
```

Host guidance:

- Show normal assistant text even when AUI parsing fails. Treat AUI as additive, not required for the turn to make sense.
- Use `parseOrNull` when you want the host to decide the fallback UI before composition.
- Use `onParseError` for telemetry and for a generic fallback surface when rendering directly from raw JSON.
- Use `onUnknownBlock` for forward-compatibility telemetry. Unknown future block types should not break the rest of the response.

## Expanded content

For `expanded` responses, include `card_title` and `card_description` so the host can
render a meaningful preview stub:

```json
{
  "display": "expanded",
  "card_title": "Headphone picks",
  "card_description": "Three top noise-cancelling models compared",
  "blocks": [ ... ]
}
```

AUI ships with `AuiResponseCard` — a tappable card stub for hosts that want to surface
`expanded` (or dismissed `survey`) responses through a detail surface:

```kotlin
AuiResponseCard(
    response = parsedResponse,
    onClick = { activeMessageId = messageId },
    isActive = messageId == activeMessageId,
)
```

It reads `card_title` / `card_description` (falling back to the first heading, text, or
`survey_title`) so the stub preview is always meaningful.

The host decides what happens on tap: open a `ModalBottomSheet` on narrow windows, show the
full `AuiRenderer` in a side pane on wide windows, or anything else. The library stays out
of layout decisions.

## Handling Surveys

The library renders surveys as **flat content** — it manages step navigation and
consolidates answers, but it does not provide a sheet or dialog. The host picks the
container:

```kotlin
if (showSurveySheet) {
    ModalBottomSheet(onDismissRequest = { showSurveySheet = false }) {
        AuiRenderer(
            response = surveyResponse,
            onFeedback = { feedback ->
                showSurveySheet = false
                sendToAI(feedback)
            },
        )
    }
}
```

When the user taps the library-injected Submit button, `onFeedback` fires once with
`stepsTotal != null`, `formattedEntries` containing the full Q+A summary, and `params`
holding the merged key-value data plus `steps_total` / `steps_skipped`. Unanswered steps
are simply excluded from `entries` — users can submit any subset.

Host-driven dismissal (closing the sheet without Submit) is a host concern — the library
emits no feedback for it. You decide whether a dismissed survey stays re-openable in the
chat (via `AuiResponseCard`) or is discarded.

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

AUI's plugin system lets you add custom components and register app-specific actions.

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

If your custom component collects user input, override `inputMetadata(data)` so feedback
accumulation can include that block's per-instance `key` and optional `label`.

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

### Building the Registry

Build one `AuiPluginRegistry` and pass it to both the renderer and the prompt generator:

```kotlin
val pluginRegistry = AuiPluginRegistry().registerAll(
    FunFactPlugin,
    OpenUrlPlugin(context),
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

27 built-in component types across these categories:

| Category | Components |
|----------|-----------|
| **Display** | `text`, `heading`, `caption`, `file_content`, `chart`, `table` |
| **Input** | `button_primary`, `button_secondary`, `quick_replies`, `chip_select_single`, `chip_select_multi`, `radio_list`, `checkbox_list`, `input_text_single`, `input_slider`, `input_rating_stars` |
| **Layout** | `divider` |
| **Progress** | `stepper_horizontal`, `progress_bar` |
| **Status** | `badge_info`, `badge_success`, `badge_warning`, `badge_error`, `status_banner_info`, `status_banner_success`, `status_banner_warning`, `status_banner_error` |

The `text` component renders inline Markdown: `**bold**`, `*italic*`, `` `code` ``, and `[links](url)`. Structural Markdown (headings, lists, etc.) uses dedicated block types instead. For exact copyable artifacts like `.md`, `.json`, config files, or source files, use `file_content` rather than decomposing them into multiple presentation blocks.

Need something outside the catalog (e.g. product cards, maps, weather widgets)? Register
an `AuiComponentPlugin` — see [Customization](#customization). Unknown component types are
silently skipped (never crash). You can observe them via `onUnknownBlock`.

## Modules

| Module | Description | Dependencies |
|--------|-------------|-------------|
| `aui-core` | Pure Kotlin. JSON parsing, data models, validation. | Kotlinx Serialization |
| `aui-compose` | Jetpack Compose renderer, theme, components. | aui-core, Compose, Coil |
| `demo` | Sample chat app. NOT part of the library. | aui-compose |

`aui-compose` transitively includes `aui-core`, so most apps just add one dependency.

## Public API

The library exposes a deliberately small surface:

- **`AuiRenderer`** — The main composable. Two overloads: `(json: String, ...)` and `(response: AuiResponse, ...)`. Handles `inline`, `expanded`, and `survey` responses.
- **`AuiResponseCard`** — Optional host-rendered card stub for surfacing `expanded` or dismissed `survey` responses through a detail surface. Uses `card_title` / `card_description`, falling back to the first heading / text block or survey title.
- **`AuiTheme`** — Theme data class with `AuiColors`, `AuiTypography`, `AuiSpacing`, `AuiShapes`. Includes `AuiTheme.fromMaterialTheme()` for zero-config MaterialTheme bridging.
- **`AuiFeedback`** — Callback data: `action`, `params`, `formattedEntries`, `entries`, `stepsSkipped`, `stepsTotal`.
- **`AuiCatalogPrompt`** — Generates the AI system prompt text from the component catalog. Tune via `AuiPromptConfig` (aggressiveness + custom examples).
- **`AuiParser`** — JSON parser (used internally by `AuiRenderer`, but available if you need pre-parsing).
- **`AuiResponse`** / **`AuiBlock`** / **`AuiStep`** — Data models for parsed responses.
- **`AuiPluginRegistry`** — Register and look up plugins. Pass to both renderer and prompt generator.
- **`AuiComponentPlugin<T>`** — Add custom component types with custom Compose rendering.
- **`AuiActionPlugin`** — Handle named actions (navigation, URLs, etc.) with chain-of-responsibility routing. Mark `isReadOnly = true` so the renderer keeps pass-through actions enabled even after a collecting block has been spent.

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
- Min SDK 26, Target SDK 36

## Documentation

- [Architecture](docs/architecture.md) — Module structure, public API, design decisions
- [Live Chat Demo](docs/livechat.md) — Demo-only architecture, provider switching, persistence, and UX behavior
- [File Content Block](docs/file-content-block.md) — Reusable contract for copyable file/document artifacts across Android and future iOS renderers
- [AUI Spec](spec/aui-spec-v1.md) — Full JSON format specification
- [JSON Examples](spec/examples/) — Sample responses for each display level

## License

Apache 2.0
