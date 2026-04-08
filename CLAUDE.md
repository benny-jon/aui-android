# AUI — Attributed UI for Android

## What This Is
An open-source Kotlin library for rendering AI-driven interactive UI in Jetpack Compose.
AI assistants respond with JSON describing pre-built native components instead of plain text.
The library parses the JSON and renders native Compose UI.

## Architecture
Two library modules + one demo app:

- `aui-core` — Pure Kotlin. JSON parsing, data models, validation. NO Android or Compose dependencies.
- `aui-compose` — Jetpack Compose renderer. Components, theming, display routing, feedback handling. Depends on aui-core.
- `demo` — Sample chat app with Claude API integration. NOT part of the library.

## Tech Stack
- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- JSON: Kotlinx Serialization (polymorphic on `type` field)
- Images: Coil for Compose
- Networking (demo only): Ktor Client
- Min SDK: 26, Target SDK: 35

## Key Design Decisions
- Component types form a sealed hierarchy. Use exhaustive `when` — never reflection.
- Unknown JSON types are preserved, never crash. Unknown fields are silently ignored.
- Theme via composition locals. Components never hardcode colors, fonts, spacing, or dimensions.
- No variants. Each visual variant is a separate component type.
- The library never launches coroutines. All async work is the host app's responsibility.
- Feedback is a callback. The renderer reports interactions; the host app handles them.
- The library auto-generates feedback display labels from question→answer pairs. The AI does not set a label.
- Library is a pure renderer with callback. It does not manage chat history, conversation state, or message models. Host apps own all of that.
- AuiPlugin is a regular interface (not sealed) because AuiComponentPlugin extends it from a different module (aui-compose). Dedup uses AuiPlugin.slotKey — action plugins key on action name, component plugins key on componentType.

## Package Structure
- `com.bennyjon.aui.core` — Parser, models, validation, AuiCatalogPrompt
- `com.bennyjon.aui.core.model` — AuiResponse, AuiBlock, AuiFeedback
- `com.bennyjon.aui.core.model.data` — Component data classes (TextData, CardBasicData, etc.)
- `com.bennyjon.aui.core.plugin` — AuiPlugin, AuiActionPlugin, AuiPluginRegistry
- `com.bennyjon.aui.compose` — AuiRenderer, AuiComponentRegistry
- `com.bennyjon.aui.compose.theme` — AuiTheme, AuiColors, AuiTypography, AuiSpacing, AuiShapes
- `com.bennyjon.aui.compose.display` — DisplayRouter, InlineDisplay, ExpandedDisplay, SheetDisplay
- `com.bennyjon.aui.compose.components.*` — One file per component (text/, cards/, lists/, input/, status/, media/, layout/)
- `com.bennyjon.aui.compose.plugin` — AuiComponentPlugin, AuiPluginRegistry extension functions
- `com.bennyjon.aui.compose.internal` — BlockRenderer, FeedbackModifier, PlaceholderResolver

## Coding Conventions
- All public API classes and functions must have KDoc comments.
- Internal implementation classes are marked `internal`.
- Data classes for component data named `{ComponentName}Data` (e.g., `CardBasicData`).
- Composable components named `Aui{ComponentName}` (e.g., `AuiCardBasic`).
- Every composable component must include a `@Preview` function.
- Use Material 3 as a base, but always go through AuiTheme — never reference `MaterialTheme` directly in components.
- Prefer `Modifier` parameter as second parameter in all composables.
- No wildcard imports.

## Commands
- Build: `./gradlew build`
- Test core: `./gradlew :aui-core:test`
- Test compose: `./gradlew :aui-compose:testDebugUnitTest`
- Run demo: `./gradlew :demo:installDebug`

## Important References
- Full AUI spec: `spec/aui-spec-v1.md`
- Library architecture doc: `docs/architecture.md` (was `architecute.md`, renamed in Session 14)
- JSON examples: `spec/examples/`

## Keeping This File Up To Date

**Claude: you MUST update this file at the end of every session.** Follow these rules:

1. **Session Log** — Keep it minimal. One line per phase (e.g., "Sessions 1-7: Phase 1 complete"), plus a "Next:" line pointing to what comes next. Details about what was built go in Completed Phases and Key Design Decisions, NOT the session log.
2. **Completed Phases** — When done, move from Current to Completed with ✅.
3. **Current Phase** — Keep accurate. Check off completed goals.
4. **Key Design Decisions** — Add new patterns/conventions discovered during the session.
5. **Known Issues** — Note broken/incomplete items. Remove when fixed.

Rules: Never delete existing sections unless factually wrong. Keep under ~120 lines. Trim old session log entries (keep last 10). When compacting (`/compact`), preserve this file fully.

---

## Completed Phases

### Phase 3 ✅ — Clean Library Boundary
Library is a pure renderer with callback. No chat management. AuiRenderer handles sheets internally (inert on re-render). AuiFeedback has stepsSkipped/stepsTotal typed fields. AuiCatalogPrompt generates AI system prompt. Demo uses own message model. All public API has KDoc. README with quick-start guide. Architecture doc renamed from typo.

### Phase 2 ✅ — Polls Polish
Fixed: expanded poll multi-input capture (shared registry + allBlocksForEntries), sheet skip-all fallback text. Added: radio_list and checkbox_list with SelectionRow shared composable.

### Phase 1 ✅ — Polls & Feedback Collection
17 components, 3 display levels, sheet multi-step with formattedEntries, demo app.

## Current Phase
Phase 4: Plugin System & Customization — Turn AUI into an extensible library.

Goals:
1. AuiComponentPlugin<T> for rendering (new types + overrides of built-ins). Uses Kotlinx Serialization — plugin declares a `dataSerializer: KSerializer<T>`.
2. AuiActionPlugin for named actions with handler + promptSchema (so the AI knows about them).
3. AuiPluginRegistry — single source of truth. Host app builds once, passes to both ViewModel (for prompt generation) and Composable (for rendering + action handling).
4. BlockRenderer resolution order: pluginRegistry → built-ins → skip unknown. Overrides are just plugins sharing a built-in's componentType.
5. Feedback routing: onFeedback always fires (AI/logging) + actionPlugin.handle fires for registered actions.
6. AuiCatalogPrompt.generate(pluginRegistry) includes plugin schemas automatically.
7. Demo app: DemoHomeScreen with 3 theme buttons (Default, Dark Neon, Warm Organic) + Plugin Showcase button with FunFact component + Navigate/OpenUrl actions.

**Module split:** AuiPlugin (marker), AuiActionPlugin, AuiPluginRegistry, and AuiCatalogPrompt live in `aui-core` (pure Kotlin). AuiComponentPlugin<T> lives in `aui-compose` because it has `@Composable Render()`. Component plugin lookup on the registry uses extension functions in compose. This keeps core free of Compose dependencies so AuiCatalogPrompt can read plugin schemas. Shared base via AuiPlugin.slotKey (open val) for dedup.

Sessions: 15 (plugin interfaces + registry), 16 (BlockRenderer wiring + feedback routing), 17 (AuiCatalogPrompt update), 18 (theme showcase), 19 (plugin showcase), 20 (review + docs)
Detailed plan: `.planning/phase4-customization.md`

## Known Issues
- None

## Session Log
- Sessions 1-7: Phase 1 complete.
- Sessions 8-10: Phase 2 complete.
- Sessions 11-14: Phase 3 complete.
- Cleanup (2026-04-07): Redistributed Key Design Decisions to KDoc and spec.
- Session 15: Plugin interfaces + registry. AuiPlugin (interface, not sealed — cross-module), AuiActionPlugin, AuiPluginRegistry in aui-core. AuiComponentPlugin<T> + extension functions in aui-compose. 21 new tests.
- Next: Session 16 (wire plugins into BlockRenderer + feedback routing).
