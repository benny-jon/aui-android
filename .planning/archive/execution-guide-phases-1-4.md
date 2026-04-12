# Execution Guide — Archived Session Prompts (Phases 1-4)

This file preserves the original session-by-session prompt text for
Phases 1 through 4, as it appeared in `execution-guide.md` before the
guide was trimmed to an index. Claude Code does not need to read this
file — it exists for human reference into how earlier phases were structured.

For the workflow conventions and current phase pointer, see
`execution-guide.md` and `CLAUDE.md`.

---

## Session 1: Gradle Setup + aui-core Models

**Goal:** Working Gradle multi-module build + all data model classes + parser.

Open Claude Code and say:

```
Set up the Gradle multi-module project for AUI. Read CLAUDE.md, 
spec/aui-spec-v1.md, and docs/architecture.md first to understand 
the architecture.

Create:
1. Root build.gradle.kts with version catalog
2. settings.gradle.kts with all three modules
3. aui-core/build.gradle.kts (pure Kotlin, kotlinx-serialization)
4. aui-compose/build.gradle.kts (depends on aui-core, Compose, Coil)
5. demo/build.gradle.kts (Android app, depends on aui-compose, Ktor)
6. gradle/libs.versions.toml

Make sure it compiles.
```

Then in the SAME session (context is still warm):

```
Now create all the data models in aui-core for the Phase 1 components.
Read the spec for the exact component types and their data fields.

Create:
- AuiResponse, AuiDisplay, AuiBlock (sealed class), AuiFeedback
- Data classes for: text, heading, caption, card_basic, card_basic_icon,
  card_image_left, list_simple, list_icon, button_primary, button_secondary,
  quick_replies, divider, spacer
- AuiParser using Kotlinx Serialization with polymorphic deserialization on the "type" field
- Handle unknown types as AuiBlock.Unknown

Then write unit tests for the parser using the JSON examples in spec/examples/.
Run the tests and make sure they pass.
```

**After this session:** Review the code. Make sure the sealed class structure
looks right. Commit what's good, note what needs changes.

---

## Session 2: Sample JSON Files

**Goal:** Create the hardcoded JSON samples that will drive Phase 1 testing.

```
Read the AUI spec and create 6 sample JSON files in spec/examples/:

1. inline-simple.json — text + quick_replies (payment confirmation)
2. inline-badges.json — text + badge_success + quick_replies (order status)
3. expanded-products.json — text + horizontal_scroll of product cards + quick_replies
4. expanded-restaurants.json — text + 3 card_image_left cards (restaurant selection)
5. sheet-booking.json — sheet with card_image_top + chip_select + button_primary
6. sheet-form.json — sheet with form_group (issue report)

Each file must be valid against our AuiBlock data models.
After creating them, write a parser test that loads and parses each file.
Run the tests.
```

---

## Session 3: Compose Components (Text + Layout)

**Goal:** Build the first composable components and the theme system.

```
Read CLAUDE.md and the aui-compose package structure.

Create the AUI theme system:
- AuiTheme, AuiColors, AuiTypography, AuiSpacing, AuiShapes
- AuiTheme.Default with Material-like defaults  
- AuiTheme.fromMaterialTheme() adapter
- Provide via CompositionLocalProvider

Then create these components (one file each, with @Preview):
- AuiText (text)
- AuiHeading (heading)
- AuiCaption (caption)
- AuiDivider (divider)
- AuiSpacer (spacer)

Each component must:
- Accept its data class + Modifier + onFeedback callback
- Use only AuiTheme for colors/fonts/spacing (never hardcode)
- Include a @Preview composable
- Be in the correct subpackage under com.bennyjon.aui.compose.components
```

---

## Session 4: Compose Components (Cards + Lists)

**Goal:** Build the card and list components.

```
Read CLAUDE.md. Continue building aui-compose components.

Create these components (one file each, with @Preview):
- AuiCardBasic (card_basic) — elevated surface, title + subtitle
- AuiCardBasicIcon (card_basic_icon) — icon + title + subtitle
- AuiCardImageLeft (card_image_left) — image on left, text on right, use Coil

For image loading, use Coil's AsyncImage composable.

Then create:
- AuiListSimple (list_simple) — vertical list of title + subtitle items
- AuiListIcon (list_icon) — vertical list with leading icons

Lists items with individual feedback should each be clickable.
```

---

## Session 5: Compose Components (Input) + BlockRenderer

**Goal:** Build input components + the routing layer that maps type → composable.

```
Read CLAUDE.md.

Create these input components:
- AuiButtonPrimary (button_primary) — filled button
- AuiButtonSecondary (button_secondary) — outlined button
- AuiQuickReplies (quick_replies) — horizontal row of chips

Then create the BlockRenderer (internal):
- Takes an AuiBlock + onFeedback callback
- Uses `when` on the sealed class to route to the correct composable
- AuiBlock.Unknown → skip (emit nothing, log warning)

Then create AuiRenderer (public API):
- Takes AuiResponse + AuiTheme + onFeedback
- Wraps content in AuiTheme provider
- Passes blocks to BlockRenderer

Test by calling AuiRenderer with a parsed sample JSON in a @Preview.
```

---

## Session 6: Display Levels

**Goal:** Implement inline, expanded, and sheet display routing.

```
Read CLAUDE.md. This is the critical session.

Create the display system in com.bennyjon.aui.compose.display:

DisplayRouter: Takes an AuiResponse and routes to the right display:
- INLINE → InlineDisplay: all blocks rendered in a Column (caller wraps in bubble)
- EXPANDED → ExpandedDisplay: leading text blocks go in a "bubble" section,
  remaining blocks render full-width below
- SHEET → SheetDisplay: leading text blocks go in a "bubble" section,
  remaining blocks go in a BottomSheet (Material 3 ModalBottomSheet)

Update AuiRenderer to use DisplayRouter instead of rendering all blocks in a Column.

The split logic for expanded/sheet: scan blocks from the start. While type == text/heading/caption,
those go in the bubble section. First non-text block and everything after → expanded/sheet section.

For the sheet:
- Use sheet_title from AuiResponse if present
- Use sheet_dismissable to control drag-to-dismiss
- On dismiss, call onFeedback with a "sheet_dismissed" action if appropriate

Test with the sample JSONs.
```

---

## Session 7: Chat Screen + Feedback Loop

**Goal:** Wire everything into a working chat screen in the demo app.

```
Read CLAUDE.md. Build the demo app chat screen.

Create:
- ChatMessage sealed class (UserText, UserFeedback, AiResponse)
- ChatViewModel with:
  - List of ChatMessage as state
  - Pre-loaded hardcoded AUI responses (load from spec/examples/)
  - onFeedback handler that:
    1. Adds a UserFeedback message with the label
    2. Loads the next hardcoded response
- ChatScreen composable:
  - LazyColumn of messages
  - User messages right-aligned in colored bubbles
  - AI messages left-aligned, rendered via AuiRenderer
  - Text input bar at bottom (functional but just adds UserText for now)
- MainActivity that launches ChatScreen

The key flow to test:
1. App opens with a pre-loaded AI response (expanded, restaurant cards)
2. User taps a card → feedback label appears as user message
3. Next AI response loads (sheet, booking form)
4. User interacts with sheet → sheet closes, confirmation appears (inline)

Make it compile and run on an emulator.
```

---

## Tips for Each Session

### Start fresh
`/clear` at the start of each session. Each session has one focused goal.
Don't let context from the previous session bleed in.

### Let Claude Code read the docs
Always start with "Read CLAUDE.md" or "Read CLAUDE.md and spec/aui-spec-v1.md."
This grounds the session in your architecture.

### Verify often
After Claude Code writes code, say "Build and run the tests" or 
"Make sure this compiles." Don't let it move on with broken code.

### Commit after each session
Review what Claude Code produced, clean up if needed, commit.
Each session = one atomic commit.

### Use /compact wisely
If a session gets long (50%+ context), say:
"/compact focus on the current task, preserve the list of files modified
and any failing tests"

### Review with a fresh session
After Sessions 5-7, start a fresh session:
```
Review the aui-compose module as a senior Android engineer. 
Look for: inconsistent patterns between components, theme values 
being hardcoded, missing @Preview functions, public API that should 
be internal. Read CLAUDE.md first for context.
```

### Don't over-prompt
Bad: Writing a 500-word prompt describing every detail.
Good: "Create AuiCardBasic following the same pattern as AuiCardBasicIcon."
Claude Code can read the existing code and match patterns.

---

## After Phase 1 (Sessions 1-7)

You'll have:
- A working Gradle multi-module project
- aui-core with parser + data models + tests
- aui-compose with ~17 components + theme + 3 display levels
- Sheet multi-step with auto-generated formattedEntries
- A demo app with a hardcoded conversation that renders AUI

---

## Phase 2: Polls Polish (Sessions 8-10)

Phase 2 is split into 3 sessions: bugs first, then new components, then review.
**Fix bugs BEFORE adding components** — the state collection fix changes how all
inputs work, so new components should build on top of the fixed system.

Detailed spec: `.planning/phase2-polls-polish.md`

### Session 8: Fix Expanded Poll Feedback

```
Read CLAUDE.md and .planning/phase2-polls-polish.md.

FIX: Expanded polls with multiple inputs only capture the last input's 
value in the feedback. For example, a poll with chip_select_multi + slider 
only shows the slider answer — the chip selections are lost.

The state collector needs to:
1. Gather values from ALL input components in the blocks array
2. For each input, walk backward through blocks to find the nearest 
   heading or text block — use that as the question
3. Build formattedEntries from all collected question-answer pairs
4. Merge all input values into feedback.params

Also handle unanswered inputs: omit from formattedEntries display text
but include the key with null in feedback.params.

If ALL inputs are unanswered when submit is tapped, show "Feedback submitted" 
as the fallback display text.

Write tests covering:
- Both questions answered → both Q&A pairs in formattedEntries
- Only first answered → only first Q&A shown
- Only second answered → only second Q&A shown  
- Neither answered → "Feedback submitted" fallback
- Single question poll → one Q&A pair

Run tests.
```

### Session 9: Fix Sheet Skip-All Feedback

```
Read CLAUDE.md and .planning/phase2-polls-polish.md.

FIX: When all steps are skipped in a multi-step sheet, the feedback bubble 
shows the raw action ID (e.g., "poll_complete") instead of a meaningful message.

The sheet's consolidated feedback builder needs:
1. If formattedEntries is empty and all steps skipped → display "Survey skipped"
2. If formattedEntries is empty but submit was tapped → display "Survey submitted"  
3. If some steps answered and some skipped → show answered Q&A pairs + 
   append "(N questions skipped)" at the end
4. Add steps_skipped and steps_total to feedback.params

Write tests covering:
- All steps answered → full Q&A pairs, steps_skipped=0
- All steps skipped → "Survey skipped", steps_skipped=3
- Steps 1 answered, 2-3 skipped → Step 1 Q&A + "(2 questions skipped)"
- Single step skipped → "Survey skipped"
- Single step submitted empty → "Survey submitted"

Run tests.
```

### Session 10: Add radio_list and checkbox_list

```
Read CLAUDE.md and .planning/phase2-polls-polish.md.

Add two new components: radio_list and checkbox_list.
The phase2 doc has full spec, data contracts, JSON examples, 
and Compose implementation notes.

1. Add data models to aui-core:
   - RadioListData with RadioOption (label, description?, value)
   - CheckboxListData with CheckboxOption (label, description?, value)
   - AuiBlock.RadioList and AuiBlock.CheckboxList sealed class cases
   - Register in polymorphic serializer

2. Create sample JSON files:
   - spec/examples/poll-expanded-survey-v2.json
   - spec/examples/poll-sheet-radio-v2.json
   Write parser tests. Run ./gradlew :aui-core:test.

3. Create internal SelectionRow composable in 
   com.bennyjon.aui.compose.components.input
   Renders: indicator + label + optional description, full row tappable,
   selected row gets primary color at 8% alpha background.

4. Create AuiRadioList with @Preview.
   Single selection. Reports value changes to state collector.
   Display value for formattedEntries = selected option's label text.

5. Create AuiCheckboxList with @Preview.
   Multi selection. Reports value changes to state collector.
   Display value = comma-separated labels of checked options.

6. Register both in BlockRenderer.

7. Update demo to use v2 sample JSONs. Verify both components appear
   and their values show in feedback bubble correctly.

8. Update spec/aui-spec-v1.md — add radio_list and checkbox_list 
   to the Component Catalog and AI System Prompt Contract sections.

Build, run, test.
```

### After Phase 2: Review Session

Start a fresh session:

```
Read CLAUDE.md. Review the aui-compose module as a senior Android engineer.

Focus on:
1. State collection: does every input component (chip_select_single, 
   chip_select_multi, radio_list, checkbox_list, input_slider, 
   input_rating_stars, input_text_single) report values consistently?
2. formattedEntries: test expanded polls with all combinations of 
   answered/unanswered inputs. Test sheets with all combinations 
   of answered/skipped steps.
3. Look for inconsistent patterns between SelectionRow and how 
   chips handle selection state.
4. Check that all new components use AuiTheme exclusively.
5. Verify @Preview exists for radio_list and checkbox_list.
```

---

## Phase 2 Deliverables

After Sessions 8-10:
- [ ] Expanded polls capture ALL input values in feedback
- [ ] Sheet skip-all shows "Survey skipped" (never raw action ID)
- [ ] Partial skip shows answered Q&A + "(N questions skipped)"
- [ ] radio_list renders with radio circles + label + description
- [ ] checkbox_list renders with checkboxes + label + description
- [ ] Both new components plug into state collection correctly
- [ ] All formattedEntries edge cases covered by tests
- [ ] Spec updated with new components
- [ ] Demo app shows the new components in action

---

Then for Phase 3, start a new sequence of sessions.

---

## Phase 3: Clean Library Boundary (Sessions 11-14)

Phase 3 redesigns how host apps consume the library. The key insight:
the library is a **renderer with a callback**, NOT a chat manager.
It doesn't own messages, conversation state, or chat architecture.

Detailed spec: `.planning/phase3-host-integration.md`

### Session 11: Clean Up Public API

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

The library should be a pure renderer with a callback. It should NOT 
manage chat history or conversation state.

1. Delete AuiChatManager if it exists — not the library's responsibility.
2. Delete AuiChatMessage if it exists — host apps define their own.
3. Delete AuiSheetState as a public class — sheet state is internal.

4. Ensure AuiRenderer handles sheets internally:
   - When display = "sheet", AuiRenderer opens a ModalBottomSheet
   - Handles step navigation inside the sheet
   - On submit: calls onFeedback with consolidated AuiFeedback, closes sheet
   - On dismiss: calls onFeedback with action="sheet_dismissed", closes sheet
   - AuiRenderer emits no visible content for sheet responses 
     (the sheet is the content)
   - After the sheet closes, the composable is inert if called again 
     (so if host app forgets to remove the JSON, it doesn't re-open)

5. Update AuiFeedback to include:
   - formattedEntries: String? (plain text summary)
   - entries: List<FeedbackEntry>? (structured Q&A pairs)
   - stepsSkipped: Int? (null for non-sheet)
   - stepsTotal: Int? (null for non-sheet)

6. Verify AuiRenderer public API is exactly:
   - AuiRenderer(json, modifier, theme, onFeedback, onParseError, onUnknownBlock)
   - AuiRenderer(response, modifier, theme, onFeedback, onUnknownBlock)
   Nothing else is public.

Write tests. Run them.
```

### Session 12: AuiCatalogPrompt

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

Create AuiCatalogPrompt in com.bennyjon.aui.compose:

1. Object with a generate() function that returns the full AI system 
   prompt text describing the AUI format and available components.
   
2. The generated text should include:
   - Response format (display + blocks / steps)
   - All component types with their data fields
   - Feedback format explanation
   - Guidelines for the AI
   
3. Optional parameter: availableActions list. If provided, includes 
   a section listing valid action IDs.

4. The output should match what's in the AI System Prompt Contract 
   section of spec/aui-spec-v1.md — generate it from code so it 
   stays in sync with the actual component catalog.

5. Write a test that verifies generate() includes all registered 
   component types.

Run tests.
```

### Session 13: Rewrite Demo App

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

Rewrite the demo app to show a clean integration pattern:

1. Define DemoMessage sealed class IN THE DEMO APP (not the library):
   - Ai(id, text?, auiJson?)
   - User(text)

2. DemoViewModel:
   - Manages List<DemoMessage> in StateFlow
   - Loads hardcoded JSON responses
   - onAuiFeedback: marks sheet AUI as consumed (sets auiJson=null), 
     adds User message with formattedEntries, loads next response
   - onUserText: adds User message

3. ChatScreen:
   - LazyColumn renders DemoMessage.Ai and DemoMessage.User
   - For Ai messages: show text bubble + AuiRenderer(auiJson)
   - AuiRenderer gets onFeedback wired to viewModel.onAuiFeedback
   - Input bar at bottom

4. Verify flows:
   - Expanded poll: renders, submit works, feedback shows as user message
   - Sheet survey: opens, step through, submit, sheet gone, feedback shows
   - Sheet dismiss: sheet gone, "Survey dismissed" shows
   - Scroll back: old expanded visible, old sheet consumed (no re-open)

5. Delete any old ChatMessage, AuiChatManager references.

Build, run, test on emulator.
```

### Session 14: Review + Documentation

```
Read CLAUDE.md. Review as a developer seeing this library for the first time.

1. Can I integrate AUI in 3 lines? (dependency, AuiRenderer, onFeedback)
2. Is anything from the library leaking into the host app's architecture?
3. Does AuiRenderer work if I forget to handle sheet consumption? 
   (It should be safe — not crash, not re-open)
4. Is AuiCatalogPrompt output clear enough for an AI to follow?
5. Is the demo app a good example without being prescriptive?
6. Add KDoc to all public API (AuiRenderer, AuiTheme, AuiFeedback, AuiCatalogPrompt)
7. Update README.md with a quick-start integration guide
```

---

## Phase 3 Deliverables

After Sessions 11-14:
- [ ] Library has NO chat/conversation management — pure renderer + callback
- [ ] AuiRenderer handles sheets internally (open, navigate, close)
- [ ] AuiFeedback includes formattedEntries + structured entries
- [ ] Sheet rendered twice is safely inert (no re-open)
- [ ] AuiCatalogPrompt generates AI system prompt text
- [ ] Demo app uses its own message model (not library types)
- [ ] Demo shows sheet consumption pattern (set auiJson=null after feedback)
- [ ] All public API has KDoc
- [ ] README has integration quick-start

---

## Phase 4: Plugin System & Customization (Sessions 15-20)

Phase 4 turns AUI into a real extensible library. Host apps can add new
component types, override built-ins, register actions with prompt schemas,
and showcase the library's theming power.

Key insight: **overrides and new components are the same thing** — they're
both plugins. Register a plugin with a built-in's `componentType` and it
shadows the built-in. One mental model, one plugin system.

Detailed spec: `.planning/phase4-customization.md`

### Session 15: Plugin Interfaces + Registry

```
Read CLAUDE.md and .planning/phase4-customization.md.

Create the plugin system split across aui-core and aui-compose based on 
dependencies. The phase4 doc has the full rationale.

PART 1 — aui-core (pure Kotlin, no Compose):

1. Create package com.bennyjon.aui.core.plugin

2. Define sealed interface AuiPlugin (marker):
   - id: String
   - promptSchema: String  (lifted to base so core can read it)
   - open val slotKey: String get() = id  (for dedup; subtypes override)

3. Create abstract class AuiActionPlugin : AuiPlugin
   - action: String
   - override val slotKey get() = action
   - handle(feedback: AuiFeedback)

4. Create AuiPluginRegistry class in core:
   - Internal list of AuiPlugin
   - register(plugin): dedups by slotKey (last-wins), fluent
   - registerAll(vararg plugins): fluent
   - allPlugins(): List<AuiPlugin>  (for AuiCatalogPrompt)
   - actionPlugin(action): AuiActionPlugin?
   - allActionPlugins(): List<AuiActionPlugin>
   - Companion: Empty

PART 2 — aui-compose (Compose dependency):

5. Create package com.bennyjon.aui.compose.plugin

6. Create abstract class AuiComponentPlugin<T : Any> : AuiPlugin
   (imports AuiPlugin from com.bennyjon.aui.core.plugin)
   - componentType: String
   - override val slotKey get() = componentType
   - dataSerializer: KSerializer<T>
   - @Composable abstract fun Render(data, onFeedback, modifier)

7. Extension functions on AuiPluginRegistry in compose:
   - fun componentPlugin(type): AuiComponentPlugin<*>?
   - fun allComponentPlugins(): List<AuiComponentPlugin<*>>
   Both use filterIsInstance<AuiComponentPlugin<*>>() on allPlugins().

PART 3 — tests:

8. aui-core tests:
   - Register action plugin, retrieve by action
   - Two action plugins with same action → last wins
   - registerAll, Empty registry, allPlugins() returns everything

9. aui-compose tests:
   - Register component plugin, retrieve via extension
   - Two component plugins with same componentType → last wins
   - Mixed registry (action + component) — both types retrievable

Run ./gradlew :aui-core:test and ./gradlew :aui-compose:testDebugUnitTest.

Rationale: aui-core must stay free of Compose dependencies so AuiCatalogPrompt
(in core) can read plugin schemas without pulling in Compose. Component plugins
live in aui-compose because @Composable can only exist there. The shared 
AuiPlugin marker + slotKey lets the core registry dedup and store both types.
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
   - Use Json.decodeFromJsonElement(plugin.dataSerializer, rawData)
   - Try/catch: if parsing fails, log and skip the block (don't crash)

4. Update feedback routing in AuiRenderer:
   When feedback fires:
   a. Always call onFeedback(feedback) — for AI conversation, logging
   b. If pluginRegistry.actionPlugin(feedback.action) exists:
        actionPlugin.handle(feedback) — for side effects

5. Write tests:
   - Plugin component renders when type matches
   - Plugin override shadows built-in for same type
   - Built-in renders when no plugin for that type
   - Plugin parses data via dataSerializer correctly
   - Plugin parse error → block skipped, no crash
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
     - "### Built-in" → all built-in component types
     - "### Plugins" → each plugin's promptSchema (skip empty strings)
   - "## Available Actions"
     - "### Plugins" → each action plugin's promptSchema
   - Plus existing format/feedback explanation

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

Update the demo app to demonstrate theming power:

1. Create DemoHomeScreen with 3 cards:
   - "Default Theme" — Material baseline
   - "Warm Organic" — earthy + serif headings + extra-rounded corners

2. Define themes in demo/src/main/kotlin/com/bennyjon/aui/demo/theme/Themes.kt
   (NOT in the library — these are demo-specific)

3. Each card navigates to ChatScreen with that theme.
   All 3 ChatScreens load the SAME hardcoded JSON poll responses.

4. Use NavController for navigation between home and chat screens.

5. Add a back button on each chat screen to return home.

Build, run on emulator. Verify all 3 themes look distinct and polished
when rendering the same poll content.
```

### Session 19: Demo App — Plugins Showcase

```
Read CLAUDE.md and .planning/phase4-customization.md.

Add plugin demonstrations to the demo app:

1. Create three demo plugins in 
   demo/src/main/kotlin/com/bennyjon/aui/demo/plugins/:

   a. DemoFunFactPlugin (AuiComponentPlugin):
      - componentType = "demo_fun_fact"
      - @Serializable data class FunFactData(title, fact, source?)
      - Renders a colorful card

   b. ToastNavigatePlugin (AuiActionPlugin):
      - action = "navigate"
      - handle: shows a Toast with the screen name

   c. OpenUrlPlugin (AuiActionPlugin):
      - action = "open_url"
      - handle: launches Intent.ACTION_VIEW with the URL

2. Build a DemoPluginRegistry that registers all three.

3. Wire the registry: Activity builds it, generates the prompt via 
   AuiCatalogPrompt.generate(registry), injects prompt into ViewModel
   via Factory, passes registry to the Composable for rendering.

4. Add a sample JSON (spec/examples/poll-with-plugins.json) using:
   - A demo_fun_fact block
   - A button with action="open_url" and url param
   - A button with action="navigate" and screen param

5. Add a 4th button on DemoHomeScreen: "Plugin Showcase" opening a 
   ChatScreen that loads this plugin sample.

6. Log AuiCatalogPrompt.generate(registry) output to Logcat on screen 
   open so developers can see what AI prompt is built.

Build, run, verify:
- demo_fun_fact renders correctly
- open_url button launches browser
- navigate button shows toast
- Logcat shows prompt with plugin schemas
```

### Session 20: Review + Documentation

```
Read CLAUDE.md. Review Phase 4 as a library consumer.

1. Plugin API ergonomics:
   - Can I add a new component in under 30 lines? (data class + plugin object)
   - Can I override a built-in in under 30 lines?
   - Can I register an action in under 15 lines?

2. AuiPluginRegistry usage:
   - Is it clear the same registry goes to both ViewModel (for prompt) 
     and Composable (for rendering)?
   - Does building the registry feel natural?

3. Resolution behavior:
   - Two plugins with same componentType → last registered wins
   - Plugin data class mismatch → logged + skipped, no crash

4. AuiCatalogPrompt output:
   - Well-formatted? Distinguishes built-in vs plugin clearly?
   - Could an AI follow the schema?

5. KDoc on all new public API:
   - AuiPlugin, AuiComponentPlugin, AuiActionPlugin
   - AuiPluginRegistry and all methods
   - Updated AuiRenderer signature

6. Update README with a "Customization" section showing:
   - How to add a custom component (with code example)
   - How to override a built-in
   - How to add a custom action
   - How to build the registry once and use it in both ViewModel and Composable
```

---

## Phase 4 Deliverables

After Sessions 15-20:
- [ ] AuiPlugin (sealed), AuiComponentPlugin, AuiActionPlugin defined
- [ ] AuiPluginRegistry with register, registerAll, lookup methods
- [ ] BlockRenderer uses pluginRegistry.componentPlugin() before built-ins
- [ ] Plugin component data parsed via Kotlinx Serialization (not manual)
- [ ] Plugin override pattern works (register with built-in's componentType)
- [ ] AuiRenderer feedback: onFeedback always + actionPlugin.handle if registered
- [ ] AuiCatalogPrompt.generate(pluginRegistry) includes plugin schemas
- [ ] Demo: DemoHomeScreen with 3 theme buttons + 1 plugin showcase button
- [ ] Demo: Default, Warm Organic themes working
- [ ] Demo: 3 working plugins (FunFact component, Navigate/OpenUrl actions)
- [ ] All new public API has KDoc
- [ ] README updated with Customization section

---

## Maintaining CLAUDE.md

Claude Code updates CLAUDE.md automatically at the end of every session
(see the "Keeping This File Up To Date" section in CLAUDE.md).

After each phase, also manually verify:
- "Completed Phases" is accurate
- "Current Phase" points to the right next step
- "Known Issues" has no stale entries
- Session Log entries are specific, not vague
