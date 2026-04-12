# AUI Phase 4 — Plugin System & Customization

## Goals

1. **Unified plugin system** — One mental model: register plugins. No separate "overrides" concept.
2. **Component plugins** — Add new component types or replace built-in ones. Uses Kotlinx Serialization.
3. **Action plugins** — Register named actions with handlers AND prompt schema, so the AI knows about them.
4. **Plugin registry** — Single source of truth, shared between renderer and prompt generator.
5. **Theme showcase** — Demo app shows multiple themed chat screens to demonstrate the library's flexibility.

---

## Core Architecture

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│   Host App                                               │
│                                                          │
│   ┌────────────────────────────────────────────────┐    │
│   │  AuiPluginRegistry (host owns this)            │    │
│   │                                                │    │
│   │  ┌──────────────────┐  ┌──────────────────┐   │    │
│   │  │ Component Plugins │  │  Action Plugins  │   │    │
│   │  │                   │  │                  │   │    │
│   │  │ ProductReview     │  │ Navigate         │   │    │
│   │  │ MapInteractive    │  │ OpenUrl          │   │    │
│   │  │ MyCustomCard      │  │ ShareText        │   │    │
│   │  │ (overrides        │  │ AddToCart        │   │    │
│   │  │  card_basic)      │  │                  │   │    │
│   │  └──────────────────┘  └──────────────────┘   │    │
│   └────────────────────────────────────────────────┘    │
│                                                          │
│             │                              │             │
│             ▼                              ▼             │
│   ┌──────────────────┐         ┌──────────────────┐    │
│   │   AuiRenderer    │         │ AuiCatalogPrompt │    │
│   │                  │         │                  │    │
│   │  Renders blocks  │         │  Generates AI    │    │
│   │  Routes feedback │         │  system prompt   │    │
│   └──────────────────┘         └──────────────────┘    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

The host app builds **one** `AuiPluginRegistry` and passes it to both
`AuiRenderer` (for rendering) and `AuiCatalogPrompt` (for prompt generation).
Single instance, single source of truth.

---

## Plugin Types

The plugin system is split across two modules based on dependencies:

- **`aui-core`** (pure Kotlin): `AuiPlugin`, `AuiActionPlugin`, `AuiPluginRegistry`, `AuiCatalogPrompt`
- **`aui-compose`** (Compose dependency): `AuiComponentPlugin<T>` (because it has `@Composable Render()`)

This keeps `aui-core` free of Compose dependencies while still letting `AuiCatalogPrompt` (in core) read plugin schemas from the registry. The bridge is simple: every plugin has a `promptSchema: String` on the base interface, so core-side code can read schemas from any plugin — it doesn't need to know whether a plugin renders Compose, SwiftUI, or anything else.

### Base interface (marker) — in `aui-core`

```kotlin
// aui-core: com.bennyjon.aui.core.plugin

/** Marker interface for all plugins. */
sealed interface AuiPlugin {
    /** Unique plugin ID for logging and debugging */
    val id: String

    /** Schema text included in AuiCatalogPrompt output so the AI knows about this plugin */
    val promptSchema: String
}
```

`promptSchema` is lifted up to the marker interface. Every plugin — component or action — must provide one. This is what lets `AuiCatalogPrompt` iterate the registry and pull schemas without knowing what kind of plugin each is.

### AuiActionPlugin — in `aui-core`

Pure Kotlin. Registers a named action with its handler AND its description for the AI prompt.

```kotlin
// aui-core: com.bennyjon.aui.core.plugin

abstract class AuiActionPlugin : AuiPlugin {
    /** The action name (e.g., "navigate", "open_url") */
    abstract val action: String

    /**
     * Handle the action when triggered by user interaction.
     */
    abstract fun handle(feedback: AuiFeedback)
}
```

### AuiComponentPlugin — in `aui-compose`

Lives in `aui-compose` because it has `@Composable Render()`. Extends the `AuiPlugin` marker from core so it can be stored in the same registry alongside action plugins.

```kotlin
// aui-compose: com.bennyjon.aui.compose.plugin

import com.bennyjon.aui.core.plugin.AuiPlugin
import kotlinx.serialization.KSerializer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

abstract class AuiComponentPlugin<T : Any> : AuiPlugin {
    /** The component type this plugin handles (e.g., "card_product_review") */
    abstract val componentType: String

    /**
     * The KSerializer for this component's data.
     * The library uses this to parse the JSON automatically.
     */
    abstract val dataSerializer: KSerializer<T>

    /**
     * Render this component.
     * @param data The parsed data object (via dataSerializer)
     * @param onFeedback Called if the user interacts with the component
     */
    @Composable
    abstract fun Render(
        data: T,
        onFeedback: (() -> Unit)?,
        modifier: Modifier
    )
}
```

---

## Plugin Registry

The registry lives in `aui-core` and stores plugins as the base `AuiPlugin` type. This keeps core free of Compose dependencies while still allowing it to hold component plugins defined in `aui-compose`.

Core-side methods handle action plugins directly (since they're defined in core). Compose-side methods for component plugin lookup are provided via **extension functions** in `aui-compose`, so only code that imports from compose can use them.

### Core side — `aui-core`

```kotlin
// aui-core: com.bennyjon.aui.core.plugin

class AuiPluginRegistry {
    private val plugins = mutableListOf<AuiPlugin>()

    /** Register a plugin (component or action). Last registered wins for duplicates. */
    fun register(plugin: AuiPlugin): AuiPluginRegistry {
        // Remove any existing plugin with the same identity
        // (same action for action plugins, same componentType for component plugins)
        plugins.removeAll { existing -> samePluginSlot(existing, plugin) }
        plugins.add(plugin)
        return this
    }

    /** Register multiple plugins at once. */
    fun registerAll(vararg newPlugins: AuiPlugin): AuiPluginRegistry {
        newPlugins.forEach { register(it) }
        return this
    }

    /** All registered plugins — used by AuiCatalogPrompt to read schemas. */
    fun allPlugins(): List<AuiPlugin> = plugins.toList()

    /** Look up an action plugin by its action name. */
    fun actionPlugin(action: String): AuiActionPlugin? =
        plugins.filterIsInstance<AuiActionPlugin>().find { it.action == action }

    /** All action plugins. */
    fun allActionPlugins(): List<AuiActionPlugin> =
        plugins.filterIsInstance<AuiActionPlugin>()

    private fun samePluginSlot(a: AuiPlugin, b: AuiPlugin): Boolean {
        // Action plugins collide on action name
        if (a is AuiActionPlugin && b is AuiActionPlugin) return a.action == b.action
        // Component plugin collision check happens in aui-compose via the extension function layer.
        // For core-level checks, we compare by plugin id as a fallback.
        return a.id == b.id
    }

    companion object {
        val Empty = AuiPluginRegistry()
    }
}
```

### Compose side — `aui-compose`

Extension functions on `AuiPluginRegistry` that only exist when you import from `aui-compose`:

```kotlin
// aui-compose: com.bennyjon.aui.compose.plugin

import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/** Look up a component plugin by type. Only available from aui-compose. */
fun AuiPluginRegistry.componentPlugin(type: String): AuiComponentPlugin<*>? =
    allPlugins()
        .filterIsInstance<AuiComponentPlugin<*>>()
        .find { it.componentType == type }

/** All component plugins. */
fun AuiPluginRegistry.allComponentPlugins(): List<AuiComponentPlugin<*>> =
    allPlugins().filterIsInstance<AuiComponentPlugin<*>>()
```

### Why this split works

- **`AuiCatalogPrompt` (in core)** iterates `registry.allPlugins()` and reads `promptSchema` from each. It doesn't care whether a plugin is a component plugin or an action plugin — it just builds the AI prompt text. Works without any Compose dependency.

- **`BlockRenderer` (in compose)** calls `registry.componentPlugin(type)` — which is only visible because `BlockRenderer` imports from `aui-compose`. Core-level code can't accidentally try to render.

- **Host apps** call `register(plugin)` on the registry for both action and component plugins. Same API, both work, because both are `AuiPlugin` subtypes.

- **Future Kotlin Multiplatform iOS support** — `aui-core` stays pure, so if you later build a SwiftUI renderer, you'd create `aui-swiftui` with its own `AuiComponentPluginSwiftUI` type that extends the same `AuiPlugin` marker. The core registry, the action plugins, and `AuiCatalogPrompt` all work unchanged.

---

## How Component Resolution Works

When `BlockRenderer` encounters a block:

```
1. Check pluginRegistry.componentPlugin(block.type)
   → if found, use the plugin (this covers both new types AND overrides)
2. Check built-in AuiBlock sealed class cases
   → if found, use the built-in renderer
3. Otherwise → AuiBlock.Unknown, skip + log warning
```

**Overrides are just plugins.** If you register a `MyCardBasicPlugin` with
`componentType = "card_basic"`, it shadows the built-in. There's no separate
"overrides" concept — registration order doesn't matter, registration *intent* does.

---

## How Feedback Routing Works

When user interacts and feedback fires:

```
1. Always call onFeedback(feedback) — for AI conversation, logging, analytics
2. If pluginRegistry has an action plugin for feedback.action:
     pluginRegistry.actionPlugin(feedback.action).handle(feedback)
   else:
     no action handler — that's fine, host app's onFeedback already got it
```

`onFeedback` is for the conversation loop. Action plugins are for side effects.
Both fire — they're not mutually exclusive.

---

## Updated AuiRenderer API

```kotlin
@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
    onFeedback: (AuiFeedback) -> Unit = {},
    onParseError: ((String) -> Unit)? = null,
)
```

Simplest possible call (unchanged from Phase 3):
```kotlin
AuiRenderer(json = auiJson, onFeedback = { sendToAI(it) })
```

With plugins:
```kotlin
AuiRenderer(
    json = auiJson,
    pluginRegistry = myAppRegistry,
    onFeedback = { sendToAI(it) }
)
```

---

## Updated AuiCatalogPrompt API

```kotlin
object AuiCatalogPrompt {
    /**
     * Generate the AI system prompt text.
     * Includes built-in components, plugin components, built-in actions,
     * and plugin actions — all from the same registry.
     */
    fun generate(
        pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty
    ): String
}
```

Generated output sections:

```
## Available Components

### Built-in
text(text)
heading(text)
card_basic(title, subtitle?)
... etc ...

### Plugins
card_product_review(title, rating, review_text, author, date?, helpful_count?)
  — A customer review card with star rating and review text.
map_interactive(latitude, longitude, label?, zoom?)
  — Interactive map with a pin. Tappable to open in maps app.

## Available Actions

### Built-in
(none — all actions come from plugins)

### Plugins
navigate(screen, ...params) — Navigate to a named screen
open_url(url) — Open URL in browser
share_text(text) — Native share sheet
add_to_cart(product_id, qty?) — Add product to shopping cart
open_maps(latitude, longitude) — Open device maps app at coordinates
```

---

## Examples

### Example 1: Component Plugin (new type)

```kotlin
@Serializable
data class ProductReviewData(
    val title: String,
    val rating: Float,
    @SerialName("review_text") val reviewText: String,
    val author: String,
    val date: String? = null,
    @SerialName("helpful_count") val helpfulCount: Int? = null
)

object ProductReviewPlugin : AuiComponentPlugin<ProductReviewData>() {
    override val id = "product_review"
    override val componentType = "card_product_review"
    override val dataSerializer = ProductReviewData.serializer()

    override val promptSchema = """
        card_product_review(title, rating, review_text, author, date?, helpful_count?)
          — A customer review card with star rating and review text.
    """.trimIndent()

    @Composable
    override fun Render(
        data: ProductReviewData,
        onFeedback: (() -> Unit)?,
        modifier: Modifier
    ) {
        ProductReviewCard(
            title = data.title,
            rating = data.rating,
            text = data.reviewText,
            author = data.author,
            date = data.date,
            helpfulCount = data.helpfulCount,
            modifier = modifier,
            onClick = { onFeedback?.invoke() }
        )
    }
}
```

The plugin author defines a `@Serializable` data class. The library handles
all the JSON parsing automatically — no manual field extraction.

### Example 2: Component Plugin (override built-in)

```kotlin
// The host app wants their own card_basic style
@Serializable
data class CardBasicData(
    val title: String,
    val subtitle: String? = null
)

object MyCardBasicPlugin : AuiComponentPlugin<CardBasicData>() {
    override val id = "my_card_basic"
    override val componentType = "card_basic"  // ← shadows the built-in
    override val dataSerializer = CardBasicData.serializer()

    override val promptSchema = ""  // built-in already in prompt; can leave empty

    @Composable
    override fun Render(
        data: CardBasicData,
        onFeedback: (() -> Unit)?,
        modifier: Modifier
    ) {
        // The host app's own card design
        MyBrandCard(
            headline = data.title,
            sublabel = data.subtitle,
            modifier = modifier,
            onTap = { onFeedback?.invoke() }
        )
    }
}
```

When the AI sends `"type": "card_basic"`, this plugin renders it instead
of the built-in. The AI doesn't know — it's still just `card_basic` from
its perspective.

### Example 3: Action Plugin

```kotlin
class NavigateActionPlugin(
    private val navController: NavController
) : AuiActionPlugin() {
    override val id = "navigate"
    override val action = "navigate"

    override val promptSchema = """
        navigate(screen, ...params) — Navigate to a named screen.
          screen: required, the route name
          params: optional, passed as navigation arguments
    """.trimIndent()

    override fun handle(feedback: AuiFeedback) {
        val screen = feedback.params["screen"] as? String ?: return
        navController.navigate(screen)
    }
}

class OpenUrlActionPlugin(
    private val context: Context
) : AuiActionPlugin() {
    override val id = "open_url"
    override val action = "open_url"

    override val promptSchema = "open_url(url) — Open the URL in the browser"

    override fun handle(feedback: AuiFeedback) {
        val url = feedback.params["url"] as? String ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
```

### Example 4: Host App Setup

```kotlin
class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Build the registry once
        val pluginRegistry = AuiPluginRegistry().registerAll(
            // Component plugins
            ProductReviewPlugin,
            MyCardBasicPlugin,  // overrides built-in card_basic
            
            // Action plugins
            NavigateActionPlugin(navController),
            OpenUrlActionPlugin(this),
            ShareTextActionPlugin(this),
            AddToCartActionPlugin(cartRepository)
        )

        // Generate the system prompt from the same registry
        val systemPrompt = AuiCatalogPrompt.generate(pluginRegistry)

        // The ViewModel needs the system prompt for its AI client
        val viewModel: ChatViewModel by viewModels { 
            ChatViewModel.Factory(systemPrompt = systemPrompt)
        }

        setContent {
            // The Composable only needs the registry for rendering + action handling
            ChatScreen(
                viewModel = viewModel,
                pluginRegistry = pluginRegistry
            )
        }
    }
}

class ChatViewModel(
    private val systemPrompt: String  // injected at construction
) : ViewModel() {
    
    private val aiClient = AnthropicClient(
        systemPrompt = systemPrompt,  // sent with every conversation
        // ... other config
    )
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            val response = aiClient.send(text)
            // ... handle response
        }
    }
    
    fun sendFeedback(feedback: AuiFeedback) {
        viewModelScope.launch {
            // The feedback's formattedEntries (or structured entries) 
            // becomes the next user message to the AI
            val response = aiClient.send(feedback.formattedEntries ?: feedback.action)
            // ... handle response
        }
    }
    
    class Factory(private val systemPrompt: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(systemPrompt) as T
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    pluginRegistry: AuiPluginRegistry
) {
    // The Composable only handles rendering and routing user interactions
    val messages by viewModel.messages.collectAsState()
    
    LazyColumn {
        items(messages) { message ->
            when (message) {
                is HostMessage.Ai -> {
                    message.text?.let { AiBubble(it) }
                    message.auiJson?.let { json ->
                        AuiRenderer(
                            json = json,
                            pluginRegistry = pluginRegistry,
                            onFeedback = { feedback ->
                                viewModel.sendFeedback(feedback)
                            }
                        )
                    }
                }
                is HostMessage.User -> UserBubble(message.text)
            }
        }
    }
}
```

**The flow:**
1. Activity builds the registry and generates the system prompt **once**
2. The ViewModel receives the system prompt at construction (via Factory)
3. The ViewModel passes it to its AI client for every conversation
4. The Composable only knows about the registry (for rendering + actions)
5. When feedback fires, the Composable calls `viewModel.sendFeedback()`
6. The ViewModel handles the AI round-trip — the Composable doesn't care

One registry, two different consumers: the **ViewModel** uses it to build the prompt
that goes to the AI, the **Composable** uses it to render plugin components and handle
plugin actions. Same source of truth, no drift.

---

## Theme Showcase (Demo App)

Demo app main screen with 3 buttons that each launch a chat with a different theme:

```
┌──────────────────────────┐
│      AUI Demo            │
│                          │
│  ┌────────────────────┐  │
│  │  Default Theme     │  │
│  │  Material baseline │  │
│  └────────────────────┘  │
│                          │
│  ┌────────────────────┐  │
│  │  Warm Organic      │  │
│  │  Earthy serif tone │  │
│  └────────────────────┘  │
│                          │
└──────────────────────────┘
```

All three screens load the same hardcoded JSON. Same poll, three different
visual styles — proves the theming system works.

### Themes to Build

**Default** — already exists, baseline Material 3.

**Warm Organic** — earthy tones, serif headings:
```kotlin
val warmOrganicTheme = AuiTheme(
    colors = AuiColors(
        primary = Color(0xFF8D6E63),
        onPrimary = Color.White,
        secondary = Color(0xFFA1887F),
        surface = Color(0xFFFFF8E1),
        onSurface = Color(0xFF3E2723),
        background = Color(0xFFFFFDE7),
        onBackground = Color(0xFF3E2723),
        error = Color(0xFFD84315),
        success = Color(0xFF558B2F),
        warning = Color(0xFFF9A825),
        muted = Color(0xFF8D6E63),
        outline = Color(0xFFBCAAA4),
        divider = Color(0xFFD7CCC8)
    ),
    typography = AuiTypography(
        heading = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        // ... rest of type scale
    ),
    spacing = AuiSpacing.Default,
    shapes = AuiShapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(24.dp),
        pill = RoundedCornerShape(50)
    )
)
```

---

## Claude Code Sessions

### Session 15: Plugin Interfaces + Registry

```
Read CLAUDE.md and .planning/phase4-customization.md.

Create the plugin system split across aui-core and aui-compose based on 
dependencies. The phase4 doc has the full rationale.

PART 1 — aui-core (pure Kotlin, no Compose):

1. Create package com.bennyjon.aui.core.plugin

2. Define sealed interface AuiPlugin (marker):
   - id: String
   - promptSchema: String  (lifted to the base so core-side code can read it)

3. Create abstract class AuiActionPlugin : AuiPlugin
   - action: String
   - handle(feedback: AuiFeedback)

4. Create AuiPluginRegistry class in core:
   - Internal list of AuiPlugin
   - register(plugin: AuiPlugin): AuiPluginRegistry  (fluent)
   - registerAll(vararg plugins: AuiPlugin): AuiPluginRegistry  (fluent)
   - allPlugins(): List<AuiPlugin>  (used by AuiCatalogPrompt)
   - actionPlugin(action: String): AuiActionPlugin?
   - allActionPlugins(): List<AuiActionPlugin>
   - Companion: Empty (default empty registry)
   - Register dedup rule: if a new action plugin has same action as an 
     existing one, remove the old one first (last-wins). Same id = same slot.

PART 2 — aui-compose (has Compose dependency):

5. Create package com.bennyjon.aui.compose.plugin

6. Create abstract class AuiComponentPlugin<T : Any> : AuiPlugin
   (imports AuiPlugin from com.bennyjon.aui.core.plugin)
   - componentType: String
   - dataSerializer: KSerializer<T>
   - @Composable abstract fun Render(data: T, onFeedback: (() -> Unit)?, modifier: Modifier)

7. Create extension functions on AuiPluginRegistry in aui-compose:
   - fun AuiPluginRegistry.componentPlugin(type: String): AuiComponentPlugin<*>?
   - fun AuiPluginRegistry.allComponentPlugins(): List<AuiComponentPlugin<*>>
   These use filterIsInstance<AuiComponentPlugin<*>>() on allPlugins().

8. Component plugin dedup: when registering an AuiComponentPlugin<*>, if 
   another one already has the same componentType, it should be replaced.
   Since the core registry doesn't know about componentType, add this 
   check via the extension function path OR override register() at the 
   core level using a generic "slot key" provided by each plugin subtype.
   
   Recommended approach: add an open val `slotKey: String` to AuiPlugin 
   (defaulting to `id`). AuiActionPlugin overrides it to return `action`. 
   AuiComponentPlugin overrides it to return `componentType`. The core 
   registry dedups by slotKey. This keeps dedup logic fully in core while 
   letting each subtype define what "same slot" means.

PART 3 — tests:

9. Write unit tests in aui-core:
   - Register action plugin, retrieve by action name
   - Register two action plugins with same action → second wins
   - registerAll with multiple plugins
   - Empty registry returns null/empty
   - allPlugins() returns all registered (for AuiCatalogPrompt)

10. Write unit tests in aui-compose:
    - Register component plugin, retrieve via componentPlugin(type) extension
    - Register two component plugins with same componentType → second wins
    - Mixed registry (action + component plugins) — both retrievable

Run ./gradlew :aui-core:test and ./gradlew :aui-compose:testDebugUnitTest.
```

### Session 16: Wire Plugins into BlockRenderer + Feedback Routing

```
Read CLAUDE.md and .planning/phase4-customization.md.

Update AuiRenderer and BlockRenderer to use the plugin registry:

1. Add pluginRegistry parameter to AuiRenderer:
   pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty

2. Update BlockRenderer resolution order:
   a. Check pluginRegistry.componentPlugin(block.type)
      → if found, parse data via plugin.dataSerializer, call plugin.Render
   b. Check built-in AuiBlock sealed class cases
   c. Otherwise → skip + log

3. For plugin component data parsing:
   - The block's raw JSON (already a JsonElement) is decoded via 
     Json.decodeFromJsonElement(plugin.dataSerializer, rawData)
   - Use a try/catch — if parsing fails, log and skip the block

4. Update feedback routing in AuiRenderer:
   When feedback fires:
   a. Always call onFeedback(feedback)
   b. If pluginRegistry.actionPlugin(feedback.action) exists:
        actionPlugin.handle(feedback)

5. Write tests:
   - Plugin component renders when type matches
   - Plugin override shadows built-in for same type
   - Built-in renders when no plugin for that type
   - Plugin parses data via dataSerializer correctly
   - Plugin parse error → block skipped, doesn't crash
   - Action plugin handler fires when action matches
   - onFeedback ALWAYS fires, regardless of action plugin presence
   - Unknown action → onFeedback fires, no crash

Run tests.
```

### Session 17: Update AuiCatalogPrompt

```
Read CLAUDE.md and .planning/phase4-customization.md.

Update AuiCatalogPrompt.generate() to take a pluginRegistry:

1. Signature: generate(pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty)

2. Generated output sections:
   - "## Available Components"
     - "### Built-in" → list all built-in component types
     - "### Plugins" → list each plugin's promptSchema (skip if empty string)
   - "## Available Actions"
     - "### Built-in" → currently empty (or remove section if no built-ins)
     - "### Plugins" → list each action plugin's promptSchema
   - Plus the existing format/feedback explanation

3. If pluginRegistry is empty, omit the "Plugins" subsections entirely.

4. Write tests:
   - Empty registry → built-in only
   - Registry with component plugins → includes their schemas
   - Registry with action plugins → includes their schemas
   - Mixed registry → both sections populated
   - Component plugin with empty promptSchema (override case) → not listed

Run tests.
```

### Session 18: Demo App — Theme Showcase

```
Read CLAUDE.md and .planning/phase4-customization.md.

Update the demo app:

1. Create DemoHomeScreen with 3 cards:
   - "Default Theme" — Material baseline
   - "Warm Organic" — earthy + serif headings + extra-rounded corners

2. Define themes in demo/src/main/kotlin/com/bennyjon/aui/demo/theme/Themes.kt
   (NOT in the library — these are demo-specific to show host app capability)

3. Each card navigates to ChatScreen with that theme.
   All 3 ChatScreens load the SAME hardcoded JSON poll responses.

4. Use NavController for navigation between home and chat screens.

5. Add a back button on each chat screen to return to home.

Build, run on emulator. Verify all 3 themes look distinct and polished
when rendering the same poll content.
```

### Session 19: Demo App — Plugins Showcase

```
Read CLAUDE.md and .planning/phase4-customization.md.

Add plugin demonstrations to the demo app:

1. Create three demo plugins in demo/src/main/kotlin/com/bennyjon/aui/demo/plugins/:

   a. DemoFunFactPlugin (component plugin):
      - componentType = "demo_fun_fact"
      - @Serializable data class FunFactData(title, fact, source?)
      - Renders a colorful card with the fact
   
   b. ToastNavigatePlugin (action plugin):
      - action = "navigate"
      - On handle: show a Toast with the screen name (since demo has no real nav)
   
   c. OpenUrlPlugin (action plugin):
      - action = "open_url"
      - On handle: launches Intent.ACTION_VIEW with the URL

2. Build a DemoPluginRegistry that registers all three plugins.

3. Pass the registry to AuiRenderer in the chat screens.

4. Add a sample JSON file (spec/examples/poll-with-plugins.json) that uses:
   - A demo_fun_fact block
   - A button with action="open_url" and url param
   - A button with action="navigate" and screen param

5. Add a 4th button on DemoHomeScreen: "Plugin Showcase" that opens a 
   ChatScreen which loads this plugin sample.

6. Log AuiCatalogPrompt.generate(pluginRegistry) output to Logcat
   when the screen opens, so developers can see what AI prompt is built.

Build, run, verify:
- demo_fun_fact renders correctly
- open_url button launches browser
- navigate button shows toast
- Logcat shows the prompt with plugin schemas
```

### Session 20: Review + Documentation

```
Read CLAUDE.md. Review Phase 4 as a library consumer.

1. Plugin API ergonomics:
   - Can I add a new component in under 30 lines? (data class + plugin object)
   - Can I override a built-in in under 30 lines?
   - Can I register an action in under 15 lines?

2. AuiPluginRegistry usage:
   - Is it clear that the same registry must be passed to renderer AND prompt?
   - Does building the registry feel natural?

3. Resolution behavior:
   - What happens if I register two plugins with the same component type?
     (second one should win — last registered wins)
   - What happens if my plugin's data class doesn't match the JSON?
     (should log + skip, not crash)

4. AuiCatalogPrompt output:
   - Is the output well-formatted?
   - Does it clearly distinguish built-in vs plugin?
   - Could an AI follow the schema correctly?

5. KDoc on all new public API:
   - AuiPlugin, AuiComponentPlugin, AuiActionPlugin
   - AuiPluginRegistry and all its methods
   - Updated AuiRenderer signature

6. Update README with a "Customization" section showing:
   - How to add a custom component (with code example)
   - How to override a built-in component
   - How to add a custom action
   - How to build the registry once and pass it everywhere
```

---

## Phase 4 Deliverables

- [ ] AuiPlugin (sealed interface), AuiComponentPlugin, AuiActionPlugin
- [ ] AuiPluginRegistry with register, registerAll, lookup methods
- [ ] BlockRenderer uses pluginRegistry.componentPlugin() before built-ins
- [ ] Plugin component data parsed via Kotlinx Serialization (no manual extraction)
- [ ] Plugin override pattern works (register plugin with built-in's componentType)
- [ ] AuiRenderer feedback routing: onFeedback always + actionPlugin.handle if registered
- [ ] AuiCatalogPrompt.generate(pluginRegistry) includes plugin schemas
- [ ] Demo app: DemoHomeScreen with 3 theme buttons + 1 plugin showcase button
- [ ] Demo app: Default, Warm Organic themes all working
- [ ] Demo app: 3 working plugins (FunFact component, ToastNavigate action, OpenUrl action)
- [ ] All new public API has KDoc
- [ ] README updated with Customization section

---

## Updated CLAUDE.md Sections (after implementation)

Add to Key Design Decisions:
```
- Plugin system: AuiComponentPlugin (rendering) + AuiActionPlugin (side effects), unified by AuiPluginRegistry.
- Component overrides are just plugins that share a built-in's componentType. No separate override concept.
- Plugin component data uses Kotlinx Serialization — plugins declare a @Serializable data class + KSerializer.
- Component resolution: pluginRegistry → built-in → skip unknown.
- Feedback routing: onFeedback always fires (AI/logging) + actionPlugin.handle fires for registered actions.
- AuiCatalogPrompt.generate(pluginRegistry) auto-includes plugin schemas so AI prompts stay in sync.
```

Move to Completed Phases:
```
### Phase 4 ✅ — Plugin System & Customization
AuiComponentPlugin and AuiActionPlugin with AuiPluginRegistry. 
BlockRenderer uses plugins before built-ins. 
AuiCatalogPrompt includes plugin schemas. 
Demo app: 3 themes (Default/Warm Organic) + plugin showcase 
with FunFact component and OpenUrl/Navigate actions.
```
