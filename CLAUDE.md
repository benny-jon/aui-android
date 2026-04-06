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
- Every AUI component type is a case in a Kotlin sealed class (`AuiBlock`). Use exhaustive `when` — never reflection.
- Unknown JSON `type` values → `AuiBlock.Unknown`. Never crash. Skip in rendering, log a warning.
- Unknown JSON fields in known types → ignored (Kotlinx Serialization `ignoreUnknownKeys = true`).
- Theme via `CompositionLocalProvider`. Components NEVER hardcode colors, fonts, spacing, or dimensions.
- No variants. Each visual variant is a separate component type (e.g., `button_primary` and `button_secondary` are separate sealed class cases).
- The library never launches coroutines. All async work is the host app's responsibility.
- Feedback is a callback: `onFeedback: (AuiFeedback) -> Unit`. The renderer reports taps. The host app handles them.
- Sheet display uses `steps` array (not `blocks`). The sheet stays open across steps — no close/reopen.
- The library auto-generates feedback display label (`formattedEntries`) from question→answer pairs. The AI does NOT set a label.
- Each step has `question` (recorded in feedback summary), `label` (shown in stepper), and `skippable` (shows Skip button).
- For expanded/inline, the library pairs each input with the nearest preceding heading/text block as its question.
- EXPANDED display uses a shared registry (`registryOverride`) between bubble and content BlockRenderers, and both pass `allBlocksForEntries = response.blocks` so heading→input pairing works across the split.
- `BlockRenderer` accepts optional `registryOverride: MutableState<Map<String,String>>?` and `allBlocksForEntries: List<AuiBlock>?` parameters. Default behavior (null) is unchanged.
- Sheet tracks `skippedCount` separately. `buildSheetFormattedEntries()` (internal, testable) produces display string with fallbacks: "Survey skipped" / "Survey submitted" / partial Q&A + "(N questions skipped)".
- Sheet `feedback.params` always includes `steps_total` and `steps_skipped` (string keys). `AuiFeedback.stepsTotal` and `AuiFeedback.stepsSkipped` are also set as typed `Int?` fields.
- Sheet dismiss (`onDismissRequest`) calls `onFeedback(action="sheet_dismissed", stepsTotal=N)`. `stepsSkipped` is null on dismiss (skip buttons weren't used).
- `SheetFlowDisplay` uses `rememberSaveable` for the `showSheet` flag so the sheet stays closed if the composable leaves and re-enters the composition (scroll away + back), provided the host uses stable `LazyColumn` keys.
- `AuiRenderer` has two overloads: `(json: String, ..., onParseError, onUnknownBlock)` and `(response: AuiResponse, ..., onUnknownBlock)`. The JSON overload parses internally and calls the response overload.
- `onUnknownBlock: ((AuiBlock.Unknown) -> Unit)?` is threaded from `AuiRenderer` → `DisplayRouter` → `BlockRenderer` / `SheetFlowDisplay`.
- `radio_list` and `checkbox_list` share `SelectionOption` (label, description?, value) and internal `SelectionRow` composable. Bordered `Column` clipped to `theme.shapes.card` with `HorizontalDivider` between rows, primary-tint (8% alpha) background on selected rows.
- `AuiCatalogPrompt.generate()` returns the AI system prompt text. `ALL_COMPONENT_TYPES` list ensures compile-time sync — tests fail if a new `AuiBlock` type is added without updating the catalog. Optional `availableActions` parameter restricts which feedback actions the AI should use.
- **Library is a pure renderer with callback. It does NOT manage chat history, conversation state, or message models. Host apps own all of that.**

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
- Library architecture doc: `docs/architecture.md`
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

### Phase 2 ✅ — Polls Polish
Fixed: expanded poll multi-input capture (shared registry + allBlocksForEntries), sheet skip-all fallback text. Added: radio_list and checkbox_list with SelectionRow shared composable.

### Phase 1 ✅ — Polls & Feedback Collection
17 components, 3 display levels, sheet multi-step with formattedEntries, demo app.

## Current Phase
Phase 3: Clean Library Boundary — Library is a pure renderer with callback. No chat management.

Goals:
1. ✅ Delete AuiChatManager/AuiChatMessage if they exist — not the library's job (didn't exist)
2. ✅ AuiRenderer handles sheets internally (open, step, close, callback). Inert on re-render.
3. ✅ AuiFeedback gets `stepsSkipped: Int?` and `stepsTotal: Int?` typed fields
4. ✅ AuiCatalogPrompt generates AI system prompt schema from the component catalog
5. Demo app uses its own message model, shows sheet consumption pattern (set auiJson=null)

Sessions: 11 (clean API), 12 (CatalogPrompt), 13 (demo rewrite), 14 (review + docs)
Detailed plan: `.planning/phase3-host-integration.md`

## Known Issues
- Demo app still uses AuiResponse directly (Session 13 will rewrite to use raw JSON + own message model)

## Session Log
- Sessions 1-7: Phase 1 complete. Parser, 17 components, 3 display levels, sheet multi-step, formattedEntries, demo app.
- Session 8 (2026-04-05): Fixed expanded polls missing inputs (shared registry + allBlocksForEntries) and sheet skip-all (buildSheetFormattedEntries fallback). 13 new unit tests.
- Session 10 (2026-04-05): Added radio_list and checkbox_list. SelectionRow composable, parser tests, demo updated to v2 JSON. Full build clean.
- Session 11 (2026-04-05): Clean API. Added JSON string overload to AuiRenderer (onParseError, onUnknownBlock). Threaded onUnknownBlock through DisplayRouter/BlockRenderer/SheetFlowDisplay. Added stepsSkipped/stepsTotal typed fields to AuiFeedback. Sheet dismiss now calls onFeedback(action="sheet_dismissed"). SheetFlowDisplay uses rememberSaveable for inert-on-re-entry behavior. 3 new tests.
- Session 12 (2026-04-05): Created AuiCatalogPrompt. Object with generate(availableActions?) returning AI system prompt text. Covers response format, display levels, all 19 component types with data fields, feedback format, sheet fields, and guidelines. 15 new tests verifying component coverage, structural sections, and availableActions parameter.
