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
- The library auto-generates the feedback display label (`formattedEntries`) from question→answer pairs. The AI does NOT set a label.
- Each step has `question` (recorded in feedback summary), `label` (shown in stepper), and `skippable` (shows Skip button).
- For expanded/inline, the library pairs each input with the nearest preceding heading/text block as its question.

## Package Structure
- `com.bennyjon.aui.core` — Parser, models, validation
- `com.bennyjon.aui.core.model` — AuiResponse, AuiBlock, AuiFeedback
- `com.bennyjon.aui.core.model.data` — Component data classes (TextData, CardBasicData, etc.)
- `com.bennyjon.aui.compose` — AuiRenderer, AuiComponentRegistry
- `com.bennyjon.aui.compose.theme` — AuiTheme, AuiColors, AuiTypography, AuiSpacing, AuiShapes
- `com.bennyjon.aui.compose.display` — DisplayRouter, InlineDisplay, ExpandedDisplay, SheetDisplay
- `com.bennyjon.aui.compose.components.*` — One file per component, organized in subpackages (text/, cards/, lists/, input/, status/, media/, layout/)
- `com.bennyjon.aui.compose.internal` — BlockRenderer, FeedbackModifier, PlaceholderResolver

## Coding Conventions
- All public API classes and functions must have KDoc comments.
- Internal implementation classes are marked `internal`.
- Data classes for component data are in `com.bennyjon.aui.core.model.data` and named `{ComponentName}Data` (e.g., `CardBasicData`).
- Composable components are in `com.bennyjon.aui.compose.components` and named `Aui{ComponentName}` (e.g., `AuiCardBasic`).
- Every composable component must include a `@Preview` function.
- Use Material 3 as a base, but always go through AuiTheme — never reference `MaterialTheme` directly in components.
- Prefer `Modifier` parameter as second parameter in all composables.
- No wildcard imports.

## Commands
- Build: `./gradlew build`
- Test core: `./gradlew :aui-core:test`
- Test compose: `./gradlew :aui-compose:testDebugUnitTest`
- Lint: `./gradlew detekt` (when configured)
- Run demo: `./gradlew :demo:installDebug`

## Important References
- Full AUI spec: `spec/aui-spec-v1.md`
- Library architecture doc: `docs/architecture.md`
- JSON examples: `spec/examples/`
- JSON schema: `spec/schema/aui-response.schema.json`

## Keeping This File Up To Date

**Claude: you MUST update this file at the end of every session.** This is how you communicate with your future self across sessions. Follow these rules:

### What to update after each session:

1. **Session Log** — Add a one-line entry to the Session Log section at the bottom of this file. Format: `- Session N (YYYY-MM-DD): [what was done]`. Be specific about what was built, fixed, or changed — not vague ("worked on components").

2. **Completed Phases** — When a phase is fully done, move it from "Current Phase" to "Completed Phases" with a ✅ and a summary of what was delivered.

3. **Current Phase** — Update to reflect what's actually in progress. If goals were completed, check them off. If new goals emerged, add them.

4. **Key Design Decisions** — If a new architectural decision was made during the session (e.g., a new pattern, a convention change, a gotcha discovered), add it here. This is the most important section for future sessions.

5. **Known Issues** — If something is broken or incomplete at the end of a session, note it. Remove items once fixed.

### Rules:
- NEVER delete or rewrite existing sections unless they are factually wrong.
- NEVER make this file longer than ~120 lines. If it's getting long, trim old session log entries (keep last 10) and consolidate completed phases into shorter summaries.
- ALWAYS keep "Current Phase" accurate — this is the first thing you read next session.
- When compacting context (`/compact`), preserve the full content of this file.

---

## Completed Phases

### Phase 1 ✅ — Polls & Feedback Collection
Implemented: text, heading, caption, chip_select_single, chip_select_multi, quick_replies,
input_rating_stars, input_text_single, input_slider, button_primary, button_secondary,
divider, spacer, stepper_horizontal, progress_bar, badge_success, status_banner_success.
All 3 display levels working (inline, expanded, sheet with multi-step).
Sheet steps system with auto-generated formattedEntries.

## Current Phase
Phase 2: Polls Polish — Fix feedback bugs + add radio_list and checkbox_list.

Three goals:
1. Fix: expanded polls must capture ALL input values (not just the last one)
2. Fix: skipping all steps in a sheet must show "Survey skipped" (not raw action ID)
3. Add: radio_list (single-select with descriptions) and checkbox_list (multi-select with descriptions)

Detailed plan: `.planning/phase2-polls-polish.md`

## Known Issues
- Expanded polls with multiple inputs only capture the last input's value in feedback (fixing in Phase 2, Session 8)
- Skipping all steps in a sheet shows raw action ID instead of "Survey skipped" (fixing in Phase 2, Session 9)

## Session Log
- Sessions 1-7: Phase 1 complete. Parser, 17 components, 3 display levels, sheet multi-step, formattedEntries, demo app.
