# AUI Android — Library Architecture

> An open-source library for rendering AI-driven interactive UI in native Jetpack Compose.  
> Drop-in. Provider-agnostic. Themeable. Still evolving.

---

## What This Library Is

AUI Android is a Compose library that takes a JSON response and renders rich,
interactive native UI — cards, lists, forms, buttons, chips — inside your app.
It's designed for AI chat interfaces but works anywhere you need server-driven UI.

```kotlin
// That's it. This is the integration.
AuiRenderer(
    json = """{ "display": "inline", "blocks": [...] }""",
    theme = myAppTheme,
    onFeedback = { feedback ->
        // User tapped something — handle it however you want
        sendToAI(feedback)
    }
)
```

## What This Library Is NOT

- Not an AI SDK. It doesn't know about Claude, OpenAI, Gemini, or any provider.
- Not a networking layer. It doesn't make HTTP requests.
- Not a chat framework. It renders individual responses, not conversations.
- Not a design system. It adapts to YOUR design system via theming.

---

## Module Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  YOUR APP                                                       │
│                                                                 │
│  ┌───────────────────┐    ┌──────────────────────────────────┐  │
│  │                   │    │                                  │  │
│  │  AI Client        │    │  Chat UI                         │  │
│  │  (Claude, GPT,    │───▶│  (your chat screen, messages,    │  │
│  │   Gemini, local)  │    │   whatever you want)             │  │
│  │                   │    │                                  │  │
│  └───────────────────┘    └──────────┬───────────────────────┘  │
│                                      │                          │
│                                      │ passes JSON string       │
│                                      ▼                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                                                          │   │
│  │  ┌─────────────────────────────────────────────────┐     │   │
│  │  │  aui-compose (library)                          │     │   │
│  │  │                                                 │     │   │
│  │  │  AuiRenderer composable                         │     │   │
│  │  │  ├── Theme system (AuiTheme + fromMaterialTheme)│     │   │
│  │  │  ├── Display router (inline / expanded / survey)│     │   │
│  │  │  ├── Block spacing (Arrangement.spacedBy)       │     │   │
│  │  │  ├── Component catalog (27 built-ins + plugins) │     │   │
│  │  │  ├── AuiResponseCard (host-rendered stub)       │     │   │
│  │  │  ├── Plugin system (component + action plugins) │     │   │
│  │  │  └── Feedback handler (tap → callback)          │     │   │
│  │  │                                                 │     │   │
│  │  │  Depends on:                                    │     │   │
│  │  │  ┌───────────────────────────────────────┐      │     │   │
│  │  │  │  aui-core (library)                   │      │     │   │
│  │  │  │                                       │      │     │   │
│  │  │  │  JSON parser                          │      │     │   │
│  │  │  │  Data models (AuiResponse, AuiBlock)  │      │     │   │
│  │  │  │  Schema validation                    │      │     │   │
│  │  │  │  Pure Kotlin — no Android deps        │      │     │   │
│  │  │  └───────────────────────────────────────┘      │     │   │
│  │  └─────────────────────────────────────────────────┘     │   │
│  │                                                          │   │
│  │  AUI LIBRARY (what you add as a dependency)              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Separate repo / separate module (NOT part of the library):

┌──────────────────────────────────────────────────┐
│  aui-demo (sample app)                           │
│                                                  │
│  Live chat + Fake / Claude / OpenAI integrations │
│  Shows how to use the library end-to-end         │
│  NOT a dependency — just a reference              │
└──────────────────────────────────────────────────┘
```

---

## Planned Artifacts

These are the intended consumer artifacts for the first release. The repo is
not published to Maven Central yet, but the first target is locked.

```kotlin
// build.gradle.kts (consumer's app)
dependencies {
    implementation("com.bennyjon:aui-compose:0.1.0-alpha01")
}
```

Advanced consumers that only want the parser/model layer can depend on:

```kotlin
dependencies {
    implementation("com.bennyjon:aui-core:0.1.0-alpha01")
}
```

| Artifact       | Contains                                    | Dependencies              |
|----------------|---------------------------------------------|---------------------------|
| `aui-core`     | Data models, JSON parser, schema validator  | Kotlinx Serialization     |
| `aui-compose`  | Compose renderer, theme, components         | aui-core, Compose, Coil   |

`aui-compose` transitively includes `aui-core`, so most apps just add one line.

---

## Public API Surface

The library exposes a deliberately small API. Everything else is internal.

### aui-core — Public API

```kotlin
// ── Parsing ──────────────────────────────────────────────

class AuiParser {
    /** Parse a JSON string into an AuiResponse. Throws on malformed input. */
    fun parse(json: String): AuiResponse

    /** Parse leniently; return null only when the top-level response is unusable. */
    fun parseOrNull(json: String): AuiResponse?
}

// ── Data Models ──────────────────────────────────────────

data class AuiResponse(
    val display: AuiDisplay,
    val blocks: List<AuiBlock> = emptyList(),
    val steps: List<AuiStep> = emptyList(),
    val surveyTitle: String? = null,
    val cardTitle: String? = null,        // host-rendered stub title (EXPANDED)
    val cardDescription: String? = null,  // host-rendered stub subtitle (EXPANDED)
)

enum class AuiDisplay { INLINE, EXPANDED, SURVEY }
// Library renders INLINE and EXPANDED identically. Hosts may surface EXPANDED responses
// via a separate detail surface (AuiResponseCard stub → bottom sheet on narrow windows,
// side detail pane on wide windows). Use card_title / card_description on AuiResponse
// to supply preview text for the host-rendered stub.
// SURVEY renders as flat content — the library manages step navigation and consolidation
// but does NOT wrap itself in a sheet. Hosts own the container.

sealed class AuiBlock {
    abstract val feedback: AuiFeedback?

    // Display
    data class Text(val data: TextData, ...) : AuiBlock()
    data class Heading(val data: HeadingData, ...) : AuiBlock()
    data class Caption(val data: CaptionData, ...) : AuiBlock()
    data class FileContent(val data: FileContentData, ...) : AuiBlock()

    // Input (implement AuiInputBlock where applicable)
    data class ButtonPrimary(val data: ButtonPrimaryData, ...) : AuiBlock()
    data class ButtonSecondary(val data: ButtonSecondaryData, ...) : AuiBlock()
    data class QuickReplies(val data: QuickRepliesData, ...) : AuiBlock()
    data class ChipSelectSingle(val data: ChipSelectSingleData, ...) : AuiBlock(), AuiInputBlock
    data class ChipSelectMulti(val data: ChipSelectMultiData, ...) : AuiBlock(), AuiInputBlock
    data class RadioList(val data: RadioListData, ...) : AuiBlock(), AuiInputBlock
    data class CheckboxList(val data: CheckboxListData, ...) : AuiBlock(), AuiInputBlock
    data class InputTextSingle(val data: InputTextSingleData, ...) : AuiBlock(), AuiInputBlock
    data class InputSlider(val data: InputSliderData, ...) : AuiBlock(), AuiInputBlock
    data class InputRatingStars(val data: InputRatingStarsData, ...) : AuiBlock(), AuiInputBlock

    // Layout / Progress / Status
    data class Divider(val data: DividerData = DividerData(), ...) : AuiBlock()
    data class StepperHorizontal(val data: StepperHorizontalData, ...) : AuiBlock()
    data class ProgressBar(val data: ProgressBarData, ...) : AuiBlock()
    data class BadgeInfo(val data: BadgeInfoData, ...) : AuiBlock()
    data class BadgeSuccess(val data: BadgeSuccessData, ...) : AuiBlock()
    data class BadgeWarning(val data: BadgeWarningData, ...) : AuiBlock()
    data class BadgeError(val data: BadgeErrorData, ...) : AuiBlock()
    data class StatusBannerInfo(val data: StatusBannerInfoData, ...) : AuiBlock()
    data class StatusBannerSuccess(val data: StatusBannerSuccessData, ...) : AuiBlock()
    data class StatusBannerWarning(val data: StatusBannerWarningData, ...) : AuiBlock()
    data class StatusBannerError(val data: StatusBannerErrorData, ...) : AuiBlock()

    // Fallback (plugin lookup + rendering)
    data class Unknown(val type: String, val rawData: JsonElement?, ...) : AuiBlock()
}

data class AuiFeedback(
    val action: String,
    val params: Map<String, String> = emptyMap(),
    // Library-computed — never set by the AI.
    // Joined "Question\nAnswer" pairs separated by blank lines, ready to send back to the AI.
    val formattedEntries: String? = null,
    // Structured Q+A pairs. Use to build a custom summary instead of formattedEntries.
    val entries: List<AuiEntry> = emptyList(),
    val stepsSkipped: Int? = null,
    val stepsTotal: Int? = null,
)

data class AuiEntry(
    val question: String,
    val answer: String,
)

// ── Component Data ───────────────────────────────────────
// Each component type has a simple data class:

data class TextData(val text: String)                        // supports inline Markdown
data class HeadingData(val text: String)
data class CaptionData(val text: String)
data class FileContentData(
    val content: String,
    val filename: String? = null,
    val language: String? = null,
    val title: String? = null,
    val description: String? = null,
)
data class QuickRepliesData(val options: List<QuickReplyOption>)
data class QuickReplyOption(val label: String, val feedback: AuiFeedback? = null)
// ... one per catalog type (see AuiBlockSerializer for the complete mapping)

// ── Input Data Contract ─────────────────────────────────
// User-input data classes implement this interface so the feedback pipeline can
// discover each input's registry key and human-readable label without reflection.

interface AuiInputData {
    val key: String        // identifies the input's value in feedback params
    val label: String?     // used as the entry question in feedback summaries
}

// Implemented by: ChipSelectSingleData, ChipSelectMultiData, RadioListData,
// CheckboxListData, InputTextSingleData, InputSliderData, InputRatingStarsData.

// ── Plugin System ────────────────────────────────────────
// AuiCore owns plugin registration; aui-compose contributes the Composable half.

class AuiPluginRegistry {
    fun register(plugin: AuiPlugin): AuiPluginRegistry
    fun registerAll(vararg plugins: AuiPlugin): AuiPluginRegistry
    fun allPlugins(): List<AuiPlugin>
    fun allActionPlugins(): List<AuiActionPlugin>
    fun actionPlugin(action: String): AuiActionPlugin?
    companion object { val Empty: AuiPluginRegistry }
}

abstract class AuiActionPlugin : AuiPlugin() {
    abstract val action: String
    open val isReadOnly: Boolean = false   // true → stays enabled after a block is spent
    abstract fun handle(feedback: AuiFeedback): Boolean
}
```

### aui-compose — Public API

```kotlin
// ── Main Entry Point ─────────────────────────────────────

@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    collectingFeedbackEnabled: Boolean = true,
    onParseError: ((String) -> Unit)? = null,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null,
    state: AuiRenderState = rememberAuiRenderState(),
)

@Composable
fun AuiRenderer(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    collectingFeedbackEnabled: Boolean = true,
    state: AuiRenderState = rememberAuiRenderState(),
)

// ── Host-rendered stub ───────────────────────────────────
// Opt-in card stub for surfacing EXPANDED or dismissed SURVEY responses in chat.

@Composable
fun AuiResponseCard(
    response: AuiResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    isActive: Boolean = false,
)

// ── Theme ────────────────────────────────────────────────

data class AuiTheme(
    val colors: AuiColors = AuiColors.Default,
    val typography: AuiTypography = AuiTypography.Default,
    val spacing: AuiSpacing = AuiSpacing.Default,
    val shapes: AuiShapes = AuiShapes.Default,
) {
    companion object {
        val Default: AuiTheme
        @Composable fun fromMaterialTheme(): AuiTheme
    }
}

// AuiColors.fromMaterialTheme(), AuiTypography.fromMaterialTheme(),
// AuiShapes.fromMaterialTheme() are also exposed for piecewise bridging.

@Composable
fun AuiThemeProvider(theme: AuiTheme = AuiTheme.Default, content: @Composable () -> Unit)

val LocalAuiTheme: ProvidableCompositionLocal<AuiTheme>

// ── Plugin System (Compose half) ─────────────────────────

abstract class AuiComponentPlugin<T : Any> : AuiPlugin() {
    abstract val componentType: String
    abstract val dataSerializer: KSerializer<T>
    open fun inputMetadata(data: T): InputMetadata? = null

    data class InputMetadata(
        val key: String,
        val label: String? = null,
    )

    @Composable abstract fun Render(
        data: T,
        onFeedback: (() -> Unit)?,
        modifier: Modifier,
    )
}
```

---

## Integration Examples

### Canonical Host Message Shape

The recommended host model is one assistant turn with always-present text plus
optional AUI:

```kotlin
data class AssistantTurn(
    val id: String,
    val text: String,
    val auiResponse: AuiResponse? = null,
)
```

This covers:

- text-only replies
- AUI-only replies
- text + AUI in one turn

### Canonical Host Routing

Keep one `AuiRenderState` per logical assistant turn id and reuse that same
instance anywhere the same response is rendered.

```kotlin
class ChatViewModel : ViewModel() {
    private val renderStates = mutableMapOf<String, AuiRenderState>()

    var activeDetailTurnId by mutableStateOf<String?>(null)
        private set

    fun renderStateFor(turnId: String): AuiRenderState =
        renderStates.getOrPut(turnId) { AuiRenderState() }

    fun onAuiFeedback(feedback: AuiFeedback) {
        sendUserMessage(
            text = feedback.formattedEntries ?: feedback.action,
            metadata = feedback.params,
        )
    }
}
```

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

Routing guidance:

- `inline` renders directly in the chat flow
- `expanded` is host-routed: show `AuiResponseCard` in the timeline and render
  the full `AuiRenderer` in a sheet, dialog, or side pane on demand
- `survey` is also host-routed: the library manages step navigation and the
  final consolidated submit, but the host owns the sheet/dialog/pane lifecycle

If a survey is dismissed and reopened, reusing the same `AuiRenderState`
preserves its progress. If the same response appears in multiple surfaces,
reusing that same state keeps those renders synchronized.

### With Custom Components (Plugin System)

```kotlin
// 1. Define data class and component plugin
@Serializable
data class WeatherData(val location: String, val temp: String, val condition: String)

object WeatherPlugin : AuiComponentPlugin<WeatherData>() {
    override val id = "card_weather"
    override val componentType = "card_weather"
    override val dataSerializer = WeatherData.serializer()
    override val promptSchema = "card_weather(location, temp, condition) — Weather card."

    @Composable
    override fun Render(data: WeatherData, onFeedback: (() -> Unit)?, modifier: Modifier) {
        WeatherCard(location = data.location, temp = data.temp, condition = data.condition)
    }
}

// 2. Register and pass to renderer
val registry = AuiPluginRegistry().register(WeatherPlugin)
AuiRenderer(json = json, pluginRegistry = registry, onFeedback = { ... })
```

---

## Catalog Roadmap

The built-in catalog is deliberately small right now (27 components — see
`AuiCatalogPrompt.ALL_COMPONENT_TYPES`). The types listed below are planned but
**not yet implemented**; the spec at `spec/aui-spec-v1.md` already describes their
JSON shapes. Hosts that need any of them today can ship them as
[`AuiComponentPlugin`](#with-custom-components-plugin-system)s and submit upstream
once the API settles.

Grouped roughly by the order we expect to land them.

### Priority wishlist

Top priority components that would unlock the most value across general AI-chat use cases,
regardless of domain — cross-cutting primitives we'd reach for first if we were
shipping a real assistant today.

1. `code_block` — syntax-highlighted code with copy button. Daily use across every coding / devtool chat.
2. `collapsible` — "show more / less" wrapper. Solves length-vs-depth everywhere: long explanations, optional detail, transcripts.
3. `tabs` — multiple views of the same topic without bloating the response (e.g. "Summary / Pros / Cons").
4. `key_value_list` — structured display primitive for specs, metadata, receipts, any "field: value" layout.
5. `quiz_card` — single-question quiz with feedback/scoring. Killer feature for learning platforms.
6. `callout` (info / warning / tip) — tone and emphasis inside a response without derailing the flow.
7. `video_player` with timestamps — media integration; timestamps let the AI deep-link to moments.
8. `card_carousel` — browsable recommendations (wider cousin of `horizontal_scroll_cards`).

### Text & rich content
- `rich_text` — styled spans (bold/italic/code/underline/strike) on top of `text`
- `section_header` — section title with optional trailing action link
- `loading` — in-flow loading indicator with optional message

### Cards
- `card_basic`, `card_basic_icon`
- `card_image_top`, `card_image_left`
- `card_product_vertical`, `card_product_horizontal`
- `card_profile`, `card_stat`, `card_event`
- `card_order_tracking`, `card_quote`, `card_code`
- `link_preview`

### Lists
- `list_simple`, `list_icon`, `list_avatar`
- `list_numbered`, `list_checklist`
- `horizontal_scroll_cards` — the one explicit nesting point in the spec

### Media
- `image_single`, `image_gallery`, `map_static`

### Buttons & form inputs
- `button_ghost`, `button_danger`
- `button_row_primary_secondary`, `button_row_primary_ghost`
- `input_text_multi`, `input_email`, `input_phone`, `input_number`
- `input_select`, `input_date`, `input_time`
- `form_group` — grouped fields with a single submit button

When one of these lands in the library, remove it from the list above, add its type
string to `AuiCatalogPrompt.ALL_COMPONENT_TYPES`, and register its branch in
`AuiBlockSerializer.selectDeserializer` — the catalog prompt and JSON parser pick
it up automatically from there.

---

## Repository Structure

```
aui/
├── README.md                         # Project overview, quick start
├── LICENSE                           # Apache 2.0
│
├── spec/                             # The AUI specification (format docs)
│   ├── aui-spec-v1.md                # Full format specification
│   ├── schema/                       # JSON Schema for validation
│   └── examples/                     # Example JSON responses
│       ├── poll-inline-yes-no.json
│       ├── poll-inline-rating.json
│       ├── poll-simple.json
│       ├── poll-confirmation.json
│       ├── poll-expanded-survey.json
│       ├── poll-expanded-survey-v2.json
│       ├── poll-survey-flow.json
│       ├── poll-survey-radio-v2.json
│       └── plugin-showcase.json
│
├── aui-core/                         # Pure-Kotlin: models + parser + prompt
│   └── src/main/java/com/bennyjon/aui/core/
│       ├── AuiParser.kt
│       ├── AuiCatalogPrompt.kt       # Generated AI system prompt (+ AuiPromptConfig)
│       ├── model/
│       │   ├── AuiResponse.kt
│       │   ├── AuiDisplay.kt
│       │   ├── AuiBlock.kt           # Sealed class + polymorphic serializer
│       │   ├── AuiFeedback.kt
│       │   ├── AuiEntry.kt
│       │   ├── AuiStep.kt
│       │   └── data/                 # One file per data-class family
│       │       ├── AuiInputData.kt   # Shared interface
│       │       ├── ButtonData.kt
│       │       ├── ChipData.kt
│       │       ├── DisplayData.kt    # Text / Heading / Caption
│       │       ├── InputData.kt      # Text / Slider / RatingStars
│       │       ├── LayoutData.kt     # Divider
│       │       ├── ProgressData.kt   # Stepper / ProgressBar
│       │       ├── QuickRepliesData.kt
│       │       ├── SelectionListData.kt  # Radio / Checkbox
│       │       └── StatusData.kt
│       └── plugin/
│           ├── AuiPlugin.kt
│           ├── AuiActionPlugin.kt    # isReadOnly + handle()
│           └── AuiPluginRegistry.kt
│
├── aui-compose/                      # Compose renderer
│   └── src/main/java/com/bennyjon/aui/compose/
│       ├── AuiRenderer.kt            # Public composable (2 overloads)
│       ├── theme/
│       │   ├── AuiTheme.kt           # + AuiThemeProvider, LocalAuiTheme
│       │   ├── AuiColors.kt          # + fromMaterialTheme()
│       │   ├── AuiContentColors.kt
│       │   ├── AuiTypography.kt      # + fromMaterialTheme()
│       │   ├── AuiSpacing.kt
│       │   └── AuiShapes.kt          # + fromMaterialTheme()
│       ├── display/
│       │   ├── DisplayRouter.kt      # Routes response → BlockRenderer or AuiSurveyContent
│       │   ├── AuiResponseCard.kt    # Host-rendered stub (EXPANDED / SURVEY)
│       │   ├── AuiSurveyContent.kt   # Flat multi-step survey — host owns container
│       │   └── SurveyTestTags.kt
│       ├── components/
│       │   ├── text/                 # AuiText (Markdown), AuiHeading, AuiCaption
│       │   ├── input/                # Buttons, quick replies, chips, inputs, selection rows
│       │   ├── layout/               # AuiDivider, AuiProgressBar, AuiStepperHorizontal
│       │   └── status/               # AuiBadge{Info,Success,Warning,Error}, AuiStatusBanner{Info,Success,Warning,Error}
│       ├── plugin/
│       │   ├── AuiComponentPlugin.kt
│       │   └── AuiPluginRegistryExtensions.kt
│       ├── text/
│       │   └── InlineMarkdown.kt     # AnnotatedString parser for AuiText
│       └── internal/
│           ├── BlockRenderer.kt      # type → composable routing + entries builder
│           └── AuiValueRegistry.kt   # shared input-value state across a BlockRenderer
│
├── demo/                             # Sample app (NOT part of the library)
│   └── src/main/
│       └── java/com/bennyjon/auiandroid/
│           ├── MainActivity.kt
│           ├── DemoApp.kt            # Nav host
│           ├── DemoHomeScreen.kt     # Home cards for live chat / showcase / themes
│           ├── livechat/             # Live chat screen / ViewModel / spent-marking
│           ├── showcase/             # All-blocks showcase
│           ├── settings/             # Settings + generated system prompt viewer
│           ├── data/
│           │   ├── llm/              # Fake / Claude / OpenAI clients + extractor
│           │   └── chat/             # Room-backed repository (provider-agnostic schema)
│           └── plugins/              # OpenUrlPlugin, NavigatePlugin, DemoFunFactPlugin
│
├── docs/
│   ├── architecture.md               # This file
│   ├── livechat.md                   # Demo-only live chat architecture + UX
│   ├── file-content-block.md         # file_content contract
│   └── assets/                       # README gifs
│
└── gradle/libs.versions.toml         # Version catalog
```

---

## Versioning & Compatibility Strategy

### Library Versioning

Follows Semantic Versioning (SemVer):
- `0.x.y` — pre-1.0, breaking changes possible between minor versions
- `1.0.0` — stable public API, breaking changes only in major versions

### Spec Versioning

The AUI JSON spec version is separate from the library version:
- Spec version is in the JSON: `"v": 1`
- Library version tracks which spec versions it supports
- A library at v2.3.0 might support spec v1 and v2

### Forward Compatibility

The library MUST handle unknown component types gracefully:
- Unknown `type` → `AuiBlock.Unknown` → the renderer looks up the plugin registry first, then skips with an `onUnknownBlock` callback if unmatched
- Unknown fields in known types → ignored (JSON parser configured to ignore unknowns)
- Missing / unknown `display` value → parse fails (handled via `onParseError`); hosts decide how to surface
- This allows newer AI models to use newer components without crashing older clients

### Parsing And Fallback Contract

The public contract is intentionally split between strict parsing and tolerant parsing:

- `AuiParser.parse(json)` is strict. It throws when the payload cannot be decoded into a usable `AuiResponse`.
- `AuiParser.parseOrNull(json)` is tolerant. It returns `null` only when the top-level response is unusable, such as invalid JSON or a missing/unknown `display`.

When `parseOrNull` can still understand the top-level response, it salvages what it can:

- unknown block `type` values become `AuiBlock.Unknown`
- malformed known blocks are downgraded to `AuiBlock.Unknown`
- malformed survey steps keep any blocks that can still be decoded
- malformed block `feedback` is dropped rather than crashing the whole response
- unknown JSON fields on known types are ignored

Renderer behavior builds on that contract:

- `AuiRenderer(json = ...)` uses strict parsing internally. A total parse failure causes no UI to render and triggers `onParseError`.
- `AuiRenderer(response = ...)` assumes the host has already decided how to handle parsing and fallback.
- `onUnknownBlock` is render-time only. It fires when an `AuiBlock.Unknown` cannot be rendered by any registered component plugin, or when a matching plugin cannot decode that block's `rawData`.

This gives hosts two valid integration styles:

1. Pre-parse with `parseOrNull`, decide the fallback UI in your own message model, then call `AuiRenderer(response = ...)`.
2. Render from raw JSON and treat `onParseError` / `onUnknownBlock` as observability hooks around an otherwise fire-and-forget render path.

Recommended host stance for LLM-driven payloads:

- keep assistant text independent from AUI so text can still render when AUI fails
- use `onUnknownBlock` for telemetry, not for blocking the rest of the response
- treat unknown future block types as additive forward-compatibility, not as fatal errors

### Backward Compatibility

- New components are additive — never remove existing types
- Existing component data contracts are append-only (new optional fields only)
- Breaking changes to existing components → new type name (e.g., `card_basic_v2`)

---

## Open Source Plan

### Pre-Launch Checklist

- [ ] README with clear value proposition, install instructions, 30-second example
- [ ] LICENSE (Apache 2.0)
- [ ] CONTRIBUTING.md with:
  - How to add a new component
  - How to add a new theme adapter
  - Code style (ktlint config)
  - PR process
  - Issue templates (bug, feature, new component)
- [ ] CODE_OF_CONDUCT.md (Contributor Covenant)
- [ ] CI/CD pipeline:
  - GitHub Actions: build, test, lint on every PR
  - Manual first release to Maven Central from a trusted local machine
  - Optional automated publishing later, after the first release is stable
- [ ] Demo app with screen recordings in README
- [ ] Documentation site (GitHub Pages or Docusaurus)
- [ ] JSON Schema published and versioned
- [ ] At least 20 working components
- [ ] All 3 display levels working
- [ ] Feedback loop working end-to-end
- [ ] 80%+ test coverage on aui-core
- [ ] Snapshot tests for all components in aui-compose

### Launch Strategy

1. **Soft launch** — Push to GitHub, share in 2-3 Android dev communities
2. **Blog post** — "AUI: Interactive native UI for AI chat" 
   - Position: simpler alternative to A2UI for chat-focused use cases
   - Show before/after: plain text chat vs AUI-powered chat
   - Include code samples showing 3-line integration
3. **Demo video** — Screen recording of the demo app in action
4. **Android Weekly / Kotlin Weekly** — Submit for newsletter inclusion
5. **Gather feedback** — Focus on API ergonomics, missing components, theming pain points

### Post-Launch Roadmap

- Community-contributed components (with review process)
- `aui-swiftui` module (iOS renderer)
- `aui-web` module (React or Web Components renderer)
- Pre-built chat screen composable (optional convenience layer)
- AI provider adapters (optional separate artifacts):
  - `aui-adapter-claude` — Anthropic Messages API helper
  - `aui-adapter-openai` — OpenAI Chat Completions helper
  - `aui-adapter-gemini` — Google Gemini helper
  - These are optional, not required — convenience only
- Figma plugin for designing AUI component layouts
- VS Code extension for AUI JSON editing with autocomplete

---

## Design Decisions for Library Quality

### 1. Minimal Dependencies

| Dependency              | Why                                    | Replaceable? |
|-------------------------|----------------------------------------|--------------|
| Kotlinx Serialization   | JSON parsing (core)                    | No           |
| Jetpack Compose          | UI rendering (compose)                 | No           |
| Coil                    | Image loading (compose)                | Yes (adapter)|
| Material 3 Components   | Used internally for some components    | Partially    |

We keep deps minimal so the library doesn't bloat consumer apps.

Coil is the default image loader but can be swapped:
```kotlin
AuiRenderer(
    response = response,
    imageLoader = myCustomImageLoader, // Optional override
    onFeedback = { ... }
)
```

### 2. Block Spacing

Spacing between sibling blocks is the renderer's responsibility, not the AI's.
The root `BlockRenderer` uses `Column(verticalArrangement = Arrangement.spacedBy(theme.blockSpacing))`
to apply a uniform vertical gap between blocks. Host apps customize this value through
`AuiSpacing.blockSpacing` (default `12.dp`).

`section_header` carries additional leading space on top of the base `blockSpacing`
so section boundaries read clearly. This is exposed as `AuiSpacing.sectionHeaderTopSpacing`
(default `8.dp`), applied as extra top padding on the `section_header` composable.

There is no per-block spacing field, no spacer block, and no way for the AI JSON to
influence spacing. The AI never emits spacing blocks.

### 3. No Coroutine Scope Leaks

The library never launches its own coroutines. All async work
(network, AI calls) is the host app's responsibility.
Components that need state (e.g., chip selection) use Compose's
built-in `remember` + state hoisting.

### 4. Compose Preview Support

Every component includes a `@Preview` composable so developers
can see what each component looks like in Android Studio without
running the app:

```kotlin
@Preview
@Composable
private fun CardBasicPreview() {
    AuiTheme {
        AuiCardBasic(
            data = CardBasicData(title = "Sample Card", subtitle = "Preview subtitle"),
            onFeedback = {}
        )
    }
}
```

### 5. ProGuard / R8 Rules Included

The library ships with consumer ProGuard rules so JSON
serialization works correctly in release builds without
consumer-side configuration.

### 6. No Reflection

All type routing uses sealed class `when` expressions.
No reflection, no annotation processing at runtime.
This keeps startup fast and APK size small.

### 7. Accessibility by Default

Every component emits correct semantics:
- Buttons have content descriptions from their labels
- Cards with feedback have "clickable" semantics
- Lists have proper list/item semantics
- Images use `alt` text for content descriptions
- Form inputs have proper label associations

---

## Success Metrics (for open source launch)

### Minimum Bar
- [ ] `aui-core`: 100% of spec v1 component types parsed
- [ ] `aui-compose`: 20+ components rendered (Phase 1+2 catalog)
- [ ] All 3 display levels working
- [ ] Feedback system working
- [ ] Custom theme working
- [ ] Custom component registration working
- [ ] Demo app with provider-swappable live chat integration
- [ ] README, docs, and one blog post

### Stretch Goals
- [ ] 50+ components (full catalog)
- [ ] Snapshot tests for every component
- [ ] Automated publish workflow in GitHub Actions
- [ ] Video demo
- [ ] Interactive component gallery app (like a Compose catalog)
