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

## Current Phase
Phase 1: Polls & Feedback Collection — The AI sends polls/surveys as AUI JSON. Users interact with native Compose UI. Feedback flows back.

Focus components (polls-specific):
- Display: text, heading, caption
- Input: chip_select_single, chip_select_multi, quick_replies, input_rating_stars, input_text_single, input_slider, button_primary, button_secondary
- Layout: divider, spacer, stepper_horizontal, progress_bar
- Status: badge_success, status_banner_success

Detailed plan and sample JSON: `.planning/phase1-polls.md`
