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
- Library is a pure renderer with callback. It reports interactions; the host app handles them. No chat history, conversation state, or message models.

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

### Phase 4 ✅ — Plugin System & Customization
AuiComponentPlugin<T> for custom/override components (Kotlinx Serialization dataSerializer). AuiActionPlugin for named actions with chain-of-responsibility routing. AuiPluginRegistry as single source of truth for renderer + prompt. BlockRenderer resolution: plugin → built-in → skip. AuiCatalogPrompt.generate(pluginRegistry) auto-includes plugin schemas. Demo app: theme showcase (3 themes) + plugin showcase (FunFact component, Navigate/OpenUrl actions). README Customization section with examples. All public API KDocced.

### Phase 3 ✅ — Clean Library Boundary
Library is a pure renderer with callback. No chat management. AuiRenderer handles sheets internally (inert on re-render). AuiFeedback has stepsSkipped/stepsTotal typed fields. AuiCatalogPrompt generates AI system prompt. Demo uses own message model. All public API has KDoc. README with quick-start guide. Architecture doc renamed from typo.

### Phase 2 ✅ — Polls Polish
Fixed: expanded poll multi-input capture (shared registry + allBlocksForEntries), sheet skip-all fallback text. Added: radio_list and checkbox_list with SelectionRow shared composable.

### Phase 1 ✅ — Polls & Feedback Collection
17 components, 3 display levels, sheet multi-step with formattedEntries, demo app.

## Current Phase

**Phase 5: Live Chat Demo** — see `.planning/phase5-live-chat.md` for sessions 21-26 and the deliverables checklist. **Claude Code: read that phase plan at the start of every Phase 5 session.**

Add a "Live Chat" entry point to the demo that talks to a real LLM end-to-end. Generic LlmClient interface (Fake / Claude / OpenAI), Room-backed ChatRepository with provider-agnostic schema, all in the demo module. Library stays a pure renderer.

## Session Convention

Every Claude Code session reads **two files**: this `CLAUDE.md` and the phase plan named in **Current Phase** above. Do NOT read `execution-guide.md` (human-facing index) or `.planning/archive/` (historical reference).

## Known Issues
- None

## Session Log
- Sessions 1-7: Phase 1 complete.
- Sessions 8-10: Phase 2 complete.
- Sessions 11-14: Phase 3 complete.
- Sessions 15-20: Phase 4 complete. Plugin system (component + action plugins, registry, BlockRenderer resolution, catalog prompt integration). Demo: theme showcase + plugin showcase. README customization docs. Spacing refactor: removed spacer component, renderer uses Arrangement.spacedBy.
- Session 21: LlmClient interface + LlmMessage + LlmRawResult in demo/data/llm/. AuiResponseExtractor parses structured JSON envelope { text, aui }. FakeLlmClient cycles 4 scripted responses (text-only, inline poll, sheet survey, confirmation). Added kotlinx-coroutines-test. 15 new tests.
- Session 22: Room schema + ChatRepository. ChatMessageEntity (UUID PK, rawContent column, no discriminator), ChatMessageDao, ChatDatabase in demo/data/chat/db/. ChatMessage flat domain model + ChatRepository interface (zero aui-core imports). DefaultChatRepository orchestrates DAO + LlmClient with entity-to-domain mapper. Added Room, KSP, Robolectric deps. Upgraded Kotlin to 2.3.20. 7 new tests.
- Session 23: Library: AuiActionPlugin.isReadOnly property + AuiResponse.isReadOnly(pluginRegistry) extension in aui-core. Demo: DemoServiceLocator (object DI), LiveChatViewModel (StateFlow + markSpentInteractives + send/onFeedback/clear), LiveChatScreen (bubbles, AuiRenderer with spent alpha, error banner, sending spinner), AuiFeedbackExt + SpentMarker utilities. 5th "Live Chat" card on DemoHomeScreen with dedicated nav route. OpenUrlPlugin + ToastNavigatePlugin marked isReadOnly=true. DemoHomeScreen now scrollable. 8 new isReadOnly tests.
- Session 24: ClaudeLlmClient against real Anthropic Messages API (Ktor, POST /v1/messages). ANTHROPIC_API_KEY from config.properties → BuildConfig. LlmProvider enum (FAKE, CLAUDE) + LlmClientFactory. DemoServiceLocator.setProvider() rebuilds client + repository at runtime. Provider dropdown in LiveChatScreen TopAppBar (Claude disabled if no key). LiveChatViewModel uses flatMapLatest for repository switching.
- Session 25: AuiPromptConfig (Aggressiveness enum + customExamples) added to AuiCatalogPrompt.generate(). Three framing variants (Conservative/Balanced/Eager). "When to reach for which component" cheat sheet. Two new built-in examples (expanded link buttons, per-option quick_replies). Collector-vs-trigger feedback clarification. 12 new tests. README updated.
- Session 26: AuiText inline Markdown (bold/italic/code/links) via AnnotatedString parser. Added `code` TextStyle to AuiTypography. 17 new tests.
- Next: Session 27 — OpenAiLlmClient. See `.planning/phase5-live-chat.md`.
