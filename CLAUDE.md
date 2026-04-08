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

## Package Structure
- `com.bennyjon.aui.core` — Parser, models, validation, AuiCatalogPrompt
- `com.bennyjon.aui.core.model` — AuiResponse, AuiBlock, AuiFeedback
- `com.bennyjon.aui.core.model.data` — Component data classes (TextData, CardBasicData, etc.)
- `com.bennyjon.aui.compose` — AuiRenderer, AuiComponentRegistry
- `com.bennyjon.aui.compose.theme` — AuiTheme, AuiColors, AuiTypography, AuiSpacing, AuiShapes
- `com.bennyjon.aui.compose.display` — DisplayRouter, InlineDisplay, ExpandedDisplay, SheetDisplay
- `com.bennyjon.aui.compose.components.*` — One file per component (text/, cards/, lists/, input/, status/, media/, layout/)
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

1. **Session Log** — Add: `- Session N (YYYY-MM-DD): [what was done]`. Be specific.
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
Phase 3 is complete. All goals delivered. Ready for next phase.

Detailed plan: `.planning/phase3-host-integration.md`

## Known Issues
- None

## Session Log
- Sessions 1-7: Phase 1 complete. Parser, 17 components, 3 display levels, sheet multi-step, formattedEntries, demo app.
- Session 8 (2026-04-05): Fixed expanded polls missing inputs (shared registry + allBlocksForEntries) and sheet skip-all (buildSheetFormattedEntries fallback). 13 new unit tests.
- Session 10 (2026-04-05): Added radio_list and checkbox_list. SelectionRow composable, parser tests, demo updated to v2 JSON. Full build clean.
- Session 11 (2026-04-05): Clean API. Added JSON string overload to AuiRenderer (onParseError, onUnknownBlock). Threaded onUnknownBlock through DisplayRouter/BlockRenderer/SheetFlowDisplay. Added stepsSkipped/stepsTotal typed fields to AuiFeedback. Sheet dismiss now calls onFeedback(action="sheet_dismissed"). SheetFlowDisplay uses rememberSaveable for inert-on-re-entry behavior. 3 new tests.
- Session 12 (2026-04-05): Created AuiCatalogPrompt. Object with generate(availableActions?) returning AI system prompt text. Covers response format, display levels, all 19 component types with data fields, feedback format, sheet fields, and guidelines. 15 new tests verifying component coverage, structural sections, and availableActions parameter.
- Session 13 (2026-04-05): Rewrote demo app. Replaced ChatMessage/ChatViewModel with DemoMessage/DemoViewModel. Demo now uses its own message model (not library types), passes raw JSON to AuiRenderer, and demonstrates sheet consumption pattern (set auiJson=null after feedback). Stable LazyColumn keys via DemoMessage.Ai.id.
- Session 14 (2026-04-05): Review + docs. Audited all public API — 100% KDoc coverage confirmed. Verified sheet safety (rememberSaveable prevents re-open). Created README.md with quick-start integration guide. Renamed docs/architecute.md → docs/architecture.md. Phase 3 complete.
- Cleanup (2026-04-07): Redistributed Key Design Decisions to KDoc and spec. Implementation details moved to SheetFlowDisplay KDoc; format details moved to aui-spec-v1.md. CLAUDE.md trimmed from 22 to 8 principles.
