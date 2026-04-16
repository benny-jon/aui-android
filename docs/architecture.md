# AUI Android — Library Architecture

> An open-source library for rendering AI-driven interactive UI in native Jetpack Compose.  
> Drop-in. Provider-agnostic. Themeable. Production-ready.

---

## What This Library Is

AUI Android is a Compose library that takes a JSON response and renders rich,
interactive native UI — cards, lists, forms, buttons, chips — inside your app.
It's designed for AI chat interfaces but works anywhere you need server-driven UI.

```kotlin
// That's it. This is the integration.
AuiRenderer(
    json = """{ "display": "expanded", "blocks": [...] }""",
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
│  │  │  ├── Theme system (AuiTheme)                    │     │   │
│  │  │  ├── Display router (expanded/sheet)             │     │   │
│  │  │  ├── Block spacing (Arrangement.spacedBy)       │     │   │
│  │  │  ├── Component catalog (50+ composables)        │     │   │
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
│  Chat screen + Claude API integration            │
│  Shows how to use the library end-to-end         │
│  NOT a dependency — just a reference              │
└──────────────────────────────────────────────────┘
```

---

## Published Artifacts

```kotlin
// build.gradle.kts (consumer's app)
dependencies {
    // Option A: Full package (most apps want this)
    implementation("com.bennyjon.aui:aui-compose:0.1.0")
    
    // Option B: Core only (for custom renderers, server-side validation, KMP)
    implementation("com.bennyjon.aui:aui-core:0.1.0")
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

object AuiParser {
    /** Parse a JSON string into an AuiResponse. Returns null on failure. */
    fun parse(json: String): AuiResponse?
    
    /** Parse with detailed error reporting. */
    fun parseResult(json: String): AuiParseResult<AuiResponse>
}

sealed class AuiParseResult<T> {
    data class Success<T>(val value: T) : AuiParseResult<T>()
    data class Error<T>(val message: String, val cause: Throwable?) : AuiParseResult<T>()
}

// ── Data Models ──────────────────────────────────────────

data class AuiResponse(
    val display: AuiDisplay,
    val blocks: List<AuiBlock>,
    val sheetTitle: String? = null,
    val sheetDismissable: Boolean = true
)

enum class AuiDisplay { EXPANDED, SHEET }

sealed class AuiBlock {
    abstract val id: String?
    abstract val feedback: AuiFeedback?
    
    data class Text(val data: TextData, ...) : AuiBlock()
    data class Heading(val data: HeadingData, ...) : AuiBlock()
    data class CardBasic(val data: CardBasicData, ...) : AuiBlock()
    data class QuickReplies(val data: QuickRepliesData, ...) : AuiBlock()
    data class Unknown(val type: String, val rawJson: String, ...) : AuiBlock()
    // ... all component types
}

data class AuiFeedback(
    val action: String,
    val params: Map<String, String> = emptyMap(),
    // Library-computed — never set by the AI.
    // Joined "Question\nAnswer" pairs separated by blank lines, ready to send back to the AI.
    val formattedEntries: String? = null,
    // Structured Q+A pairs. Use to build a custom summary instead of formattedEntries.
    val entries: List<AuiEntry> = emptyList(),
)

data class AuiEntry(
    val question: String,
    val answer: String,
)

// ── Component Data ───────────────────────────────────────
// Each component type has a simple data class:

data class TextData(val text: String)
data class HeadingData(val text: String)
data class CardBasicData(val title: String, val subtitle: String? = null)
data class QuickRepliesData(val options: List<QuickReplyOption>)
data class QuickReplyOption(val label: String, val feedback: AuiFeedback? = null)
// ... etc for every component

// ── Input Data Contract ─────────────────────────────────
// All user-input data classes implement this interface,
// giving the feedback pipeline a uniform way to discover
// the input's registry key and human-readable label.

interface AuiInputData {
    val key: String        // identifies the input's value in feedback params
    val label: String?     // used as the entry question in feedback summaries
}

// Implemented by: ChipSelectSingleData, ChipSelectMultiData,
// InputTextSingleData, InputTextMultiData, InputEmailData,
// InputPhoneData, InputNumberData, InputSelectData, InputDateData,
// InputTimeData, InputSliderData, InputRatingStarsData
```

### aui-compose — Public API

```kotlin
// ── Main Entry Point ─────────────────────────────────────

@Composable
fun AuiRenderer(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null
)

/** Convenience: parse + render in one call */
@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
    onParseError: ((String) -> Unit)? = null,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null
)

// ── Theme ────────────────────────────────────────────────

data class AuiTheme(
    val colors: AuiColors,
    val typography: AuiTypography,
    val spacing: AuiSpacing,
    val shapes: AuiShapes
) {
    companion object {
        val Default: AuiTheme  // Material-like defaults
        
        /** Create an AuiTheme from your existing MaterialTheme */
        @Composable
        fun fromMaterialTheme(): AuiTheme
    }
}

data class AuiColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val muted: Color,
    val outline: Color,
    val divider: Color
)

data class AuiTypography(
    val heading: TextStyle,
    val headingSmall: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val code: TextStyle
)

data class AuiSpacing(
    val xs: Dp,    // default 4.dp
    val s: Dp,     // default 8.dp
    val m: Dp,     // default 16.dp
    val l: Dp,     // default 24.dp
    val xl: Dp,    // default 32.dp
    val xxl: Dp,   // default 48.dp
    val blockSpacing: Dp,           // default 12.dp — vertical gap between sibling blocks
    val sectionHeaderTopSpacing: Dp // default 8.dp — extra top padding above section_header
)

data class AuiShapes(
    val small: Shape,     // default RoundedCornerShape(8.dp)
    val medium: Shape,    // default RoundedCornerShape(12.dp)
    val large: Shape,     // default RoundedCornerShape(16.dp)
    val pill: Shape       // default RoundedCornerShape(50)
)

// ── Component Registry (for extensibility) ───────────────

object AuiComponentRegistry {
    /** Register a custom component renderer for a type */
    fun register(type: String, renderer: @Composable (AuiBlock, AuiFeedback?) -> Unit)
    
    /** Unregister a custom component */
    fun unregister(type: String)
}
```

---

## Integration Examples

### Minimal Integration (3 lines)

```kotlin
@Composable
fun AiMessageBubble(jsonResponse: String) {
    AuiRenderer(
        json = jsonResponse,
        onFeedback = { feedback -> 
            Log.d("AUI", "User tapped: ${feedback.action}")
        }
    )
}
```

### With Custom Theme (matching your app's design)

```kotlin
@Composable
fun AiMessageBubble(jsonResponse: String) {
    val myTheme = AuiTheme(
        colors = AuiColors(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            surface = Color(0xFFFFFBFE),
            // ... your brand colors
        ),
        typography = AuiTypography(
            heading = TextStyle(
                fontFamily = YourBrandFont,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            // ... your type scale
        ),
        spacing = AuiSpacing.Default,
        shapes = AuiShapes.Default
    )
    
    AuiRenderer(
        json = jsonResponse,
        theme = myTheme,
        onFeedback = { sendToChatViewModel(it) }
    )
}
```

### Adapting from MaterialTheme (zero config)

```kotlin
@Composable
fun AiMessageBubble(jsonResponse: String) {
    AuiRenderer(
        json = jsonResponse,
        theme = AuiTheme.fromMaterialTheme(), // auto-maps your Material colors/fonts
        onFeedback = { sendToChatViewModel(it) }
    )
}
```

### Full Chat Integration

```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    
    LazyColumn {
        items(messages) { message ->
            when (message) {
                is ChatMessage.UserText -> UserBubble(message.text)
                is ChatMessage.UserFeedback -> UserBubble(message.feedback.formattedEntries ?: message.feedback.action)
                is ChatMessage.AiResponse -> {
                    AuiRenderer(
                        response = message.auiResponse,
                        theme = AuiTheme.fromMaterialTheme(),
                        onFeedback = { feedback ->
                            viewModel.onFeedback(feedback)
                        }
                    )
                }
            }
        }
    }
}
```

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

## Repository Structure

```
aui/
├── README.md                         # Project overview, quick start
├── LICENSE                           # Apache 2.0
├── CONTRIBUTING.md                   # Contribution guidelines
├── CHANGELOG.md                      # Version history
├── CODE_OF_CONDUCT.md
│
├── spec/                             # The AUI specification (format docs)
│   ├── README.md                     # Spec overview
│   ├── aui-spec-v1.md               # Full format specification
│   ├── schema/
│   │   └── aui-response.schema.json # JSON Schema for validation
│   └── examples/                     # Example JSON files
│       ├── inline-simple.json
│       ├── inline-badges.json
│       ├── expanded-products.json
│       ├── expanded-restaurants.json
│       ├── sheet-booking.json
│       ├── sheet-form.json
│       └── full-conversation.json    # Multi-turn example
│
├── aui-core/                         # Kotlin library: models + parser
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/bennyjon/aui/core/
│       │   ├── AuiParser.kt
│       │   ├── AuiParseResult.kt
│       │   ├── model/
│       │   │   ├── AuiResponse.kt
│       │   │   ├── AuiDisplay.kt
│       │   │   ├── AuiBlock.kt       # Sealed class
│       │   │   ├── AuiFeedback.kt
│       │   │   └── data/             # Component data classes
│       │   │       ├── TextData.kt
│       │   │       ├── HeadingData.kt
│       │   │       ├── CardBasicData.kt
│       │   │       ├── CardBasicIconData.kt
│       │   │       ├── CardImageLeftData.kt
│       │   │       ├── ListSimpleData.kt
│       │   │       ├── ListIconData.kt
│       │   │       ├── ButtonData.kt
│       │   │       ├── QuickRepliesData.kt
│       │   │       ├── AuiInputData.kt        # Shared interface for input data classes
│       │   │       ├── ChipSelectData.kt
│       │   │       ├── FormGroupData.kt
│       │   │       └── ... (one per component type)
│       │   └── validation/
│       │       └── AuiValidator.kt    # Optional schema validation
│       └── test/kotlin/com/bennyjon/aui/core/
│           ├── AuiParserTest.kt
│           ├── AuiParserEdgeCasesTest.kt
│           └── model/
│               └── AuiBlockSerializationTest.kt
│
├── aui-compose/                      # Compose renderer library
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/bennyjon/aui/compose/
│       │   ├── AuiRenderer.kt        # Main public composable
│       │   ├── AuiComponentRegistry.kt
│       │   │
│       │   ├── theme/
│       │   │   ├── AuiTheme.kt       # Theme data class + defaults
│       │   │   ├── AuiColors.kt
│       │   │   ├── AuiTypography.kt
│       │   │   ├── AuiSpacing.kt
│       │   │   ├── AuiShapes.kt
│       │   │   └── MaterialThemeAdapter.kt  # fromMaterialTheme()
│       │   │
│       │   ├── display/
│       │   │   ├── DisplayRouter.kt   # Routes to expanded/sheet
│       │   │   ├── InlineDisplay.kt
│       │   │   ├── ExpandedDisplay.kt
│       │   │   └── SheetDisplay.kt
│       │   │
│       │   ├── components/            # One file per component
│       │   │   ├── text/
│       │   │   │   ├── AuiText.kt
│       │   │   │   ├── AuiHeading.kt
│       │   │   │   ├── AuiCaption.kt
│       │   │   │   └── AuiRichText.kt
│       │   │   ├── cards/
│       │   │   │   ├── AuiCardBasic.kt
│       │   │   │   ├── AuiCardBasicIcon.kt
│       │   │   │   ├── AuiCardImageTop.kt
│       │   │   │   ├── AuiCardImageLeft.kt
│       │   │   │   ├── AuiCardProductVertical.kt
│       │   │   │   ├── AuiCardProductHorizontal.kt
│       │   │   │   ├── AuiCardProfile.kt
│       │   │   │   ├── AuiCardStat.kt
│       │   │   │   ├── AuiCardEvent.kt
│       │   │   │   ├── AuiCardOrderTracking.kt
│       │   │   │   ├── AuiCardQuote.kt
│       │   │   │   └── AuiCardCode.kt
│       │   │   ├── lists/
│       │   │   │   ├── AuiListSimple.kt
│       │   │   │   ├── AuiListIcon.kt
│       │   │   │   ├── AuiListAvatar.kt
│       │   │   │   ├── AuiListNumbered.kt
│       │   │   │   ├── AuiListChecklist.kt
│       │   │   │   └── AuiHorizontalScrollCards.kt
│       │   │   ├── input/
│       │   │   │   ├── AuiButtonPrimary.kt
│       │   │   │   ├── AuiButtonSecondary.kt
│       │   │   │   ├── AuiButtonGhost.kt
│       │   │   │   ├── AuiButtonDanger.kt
│       │   │   │   ├── AuiButtonRowPrimarySecondary.kt
│       │   │   │   ├── AuiButtonRowPrimaryGhost.kt
│       │   │   │   ├── AuiQuickReplies.kt
│       │   │   │   ├── AuiChipSelectSingle.kt
│       │   │   │   ├── AuiChipSelectMulti.kt
│       │   │   │   ├── AuiInputTextSingle.kt
│       │   │   │   ├── AuiInputTextMulti.kt
│       │   │   │   ├── AuiInputEmail.kt
│       │   │   │   ├── AuiInputPhone.kt
│       │   │   │   ├── AuiInputNumber.kt
│       │   │   │   ├── AuiInputSelect.kt
│       │   │   │   ├── AuiInputDate.kt
│       │   │   │   ├── AuiInputTime.kt
│       │   │   │   ├── AuiInputSlider.kt
│       │   │   │   ├── AuiInputRatingStars.kt
│       │   │   │   └── AuiFormGroup.kt
│       │   │   ├── status/
│       │   │   │   ├── AuiBadgeInfo.kt
│       │   │   │   ├── AuiBadgeSuccess.kt
│       │   │   │   ├── AuiBadgeWarning.kt
│       │   │   │   ├── AuiBadgeError.kt
│       │   │   │   ├── AuiStatusBannerInfo.kt
│       │   │   │   ├── AuiStatusBannerSuccess.kt
│       │   │   │   ├── AuiStatusBannerWarning.kt
│       │   │   │   ├── AuiStatusBannerError.kt
│       │   │   │   ├── AuiStepperHorizontal.kt
│       │   │   │   └── AuiProgressBar.kt
│       │   │   ├── media/
│       │   │   │   ├── AuiImageSingle.kt
│       │   │   │   ├── AuiImageGallery.kt
│       │   │   │   └── AuiMapStatic.kt
│       │   │   └── layout/
│       │   │       ├── AuiDivider.kt
│       │   │       ├── AuiSectionHeader.kt
│       │   │       └── AuiLoading.kt
│       │   │
│       │   └── internal/              # Not public API
│       │       ├── BlockRenderer.kt   # type → composable routing; builds AuiEntry list from heading→input pairs
│       │       ├── AuiValueRegistry.kt # shared input value state across a BlockRenderer
│       │       └── FeedbackModifier.kt # clickable + ripple for feedback
│       │
│       └── test/kotlin/com/bennyjon/aui/compose/
│           ├── AuiRendererTest.kt
│           ├── display/
│           │   ├── InlineDisplayTest.kt
│           │   ├── ExpandedDisplayTest.kt
│           │   └── SheetDisplayTest.kt
│           └── components/
│               └── ... (snapshot tests per component)
│
├── demo/                             # Sample app (NOT part of library)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/bennyjon/aui/demo/
│       │   ├── MainActivity.kt
│       │   ├── ChatScreen.kt
│       │   ├── ChatViewModel.kt
│       │   ├── ChatMessage.kt
│       │   ├── ai/
│       │   │   ├── AiChatClient.kt         # Interface
│       │   │   ├── ClaudeChatClient.kt      # Claude implementation
│       │   │   ├── HardcodedChatClient.kt   # For testing
│       │   │   └── SystemPrompt.kt          # AUI catalog manifest
│       │   └── samples/
│       │       └── SampleResponses.kt       # Hardcoded JSON samples
│       └── res/
│           └── ...
│
├── docs/                             # Documentation site content
│   ├── getting-started.md
│   ├── theming.md
│   ├── custom-components.md
│   ├── display-levels.md
│   ├── feedback-system.md
│   ├── ai-integration-guide.md       # How to wire up any AI provider
│   └── migration-from-a2ui.md        # For people coming from A2UI
│
└── gradle/
    └── libs.versions.toml            # Version catalog
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
- Unknown `type` → `AuiBlock.Unknown` → skipped in rendering (or custom handler)
- Unknown fields in known types → ignored (JSON parser configured to ignore unknowns)
- Unknown `display` value → falls back to `expanded`
- This allows newer AI models to use newer components without crashing older clients

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
  - Snapshot publishing to Maven Central (on main merge)
  - Release publishing (on tag)
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
- [ ] Demo app with Claude integration
- [ ] README, docs, and one blog post

### Stretch Goals
- [ ] 50+ components (full catalog)
- [ ] Snapshot tests for every component
- [ ] GitHub Actions CI/CD
- [ ] Maven Central publishing
- [ ] Video demo
- [ ] Interactive component gallery app (like a Compose catalog)
