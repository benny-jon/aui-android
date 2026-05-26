# AUI Android Agent Handoff

This is the canonical launch-time status file for coding agents working in this
repository. Use it as the source of truth for the repo's current state and what
to do next.

## Project Summary

AUI Android is an open-source Kotlin library for rendering AI-driven
interactive UI in Jetpack Compose.

The repo has three main modules:
- `aui-core`: pure Kotlin models, parsing, validation, prompt generation
- `aui-compose`: Compose renderer, themes, display routing, built-in components
- `demo`: sample app and experimental integrations; current live work happens
  here, not in the library modules unless explicitly noted

## Read Order

Read these files in order at the start of a work session:
1. `AGENTS.md`
2. `.planning/phase5-live-chat.md`
3. `README.md` for public-facing usage and examples
4. `docs/architecture.md` for design details
5. `spec/aui-spec-v1.md` when changing wire format, prompt rules, or components

Do not treat `.planning/archive/` as current execution guidance.

## Current Status

- Current phase: First Release Readiness (Sessions 53–58) +
  library-sanity-checks pre-tag audit (S1–S10)
- Goal: prepare AUI Android for its first external release as a usable,
  documented, test-backed library
- Current phase plan: `.planning/first-release-readiness.md`
- Sanity-checks audit plan: `.planning/library-sanity-checks.md`
- Release mechanics tracker: `.planning/release-checklist.md`
- Last completed: Session 56 — Release-Confidence Renderer Tests. Added a
  focused `AuiRendererContractUiTest` suite covering expanded multi-input
  feedback aggregation across split blocks, `collectingFeedbackEnabled` vs
  read-only action-plugin behavior, unknown-block reporting, parse-error
  callback behavior, and chart accessibility summaries. Also fixed stale
  Compose test imports in `AuiSurveyContentUiTest` so androidTest sources
  compile again.
- Last completed: Session 57 — Publishing Mechanics + Release Checklist final
  alignment. Release docs now explicitly match the checked-in Maven Central
  scaffold (`vanniktech-maven-publish`, root `GROUP` / `VERSION_NAME` /
  POM metadata, module `POM_ARTIFACT_ID` values), the first-release path is
  documented as manual owner-run publishing, and the connected
  `:aui-compose:connectedDebugAndroidTest` failure remains documented as a
  local AVD/Espresso caveat rather than a release gate.
- Last completed: First publish — `0.1.0-alpha01` was published to Maven
  Central and tag `v0.1.0-alpha01` was pushed. Local repo follow-up updated
  the README to treat Maven Central as the primary install path, added the
  first `CHANGELOG.md` entry, and marked release-checklist publish milestones.
- Last completed: Post-release follow-up — `0.1.0-alpha01` was smoke-tested
  from a fresh consumer app resolved from Maven Central, and a GitHub release
  entry was created for tag `v0.1.0-alpha01`.
- Next recommended task: none queued.
- Known blockers: none for the first publish or immediate post-release
  follow-up.
- Known issues: local `:aui-compose:connectedDebugAndroidTest` currently fails
  on the configured AVD before assertions run with Espresso /
  `InputManager.getInstance` reflection failure; unit tests and androidTest
  source compilation are green.
- Follow-up queued: none.

## Current Direction

Release-readiness work is focused on tightening the library for adopters:
- error-handling / fallback contract documentation
- release-confidence renderer tests
- publishing mechanics + release checklist
- keeping the newly canonical host integration docs aligned with the real API

Phase 5 (live chat demo) is feature-complete for first-release purposes;
further demo polish is fair game only when it sharpens the host integration
story.

Library-level changes are allowed only when they are intrinsic to the renderer
or response model, not chat-product features.

## Current Capabilities

- Demo live chat supports provider-neutral `LlmClient` integration with real
  OpenAI and Claude backends, provider switching, and Room-backed persistence.
- The renderer supports current Phase 5 response needs including Markdown text,
  tolerant unknown-block parsing, `file_content`, `chart`, and `table` blocks.
- The demo includes key UX polish already landed: multiline composer, centered
  empty state, retryable error banners, responsive split-pane behavior, and a
  Settings screen for prompt/debug inspection.
- Public docs are synced through 2026-05-04, including a canonical host
  integration example plus explicit error-handling / compatibility guidance in
  `README.md` and `docs/architecture.md`. Maven Central is now the primary
  install path for `0.1.0-alpha01`, and CI includes separate unit-test and
  compile-check workflows.
- Renderer contract coverage now includes survey UX flows plus focused
  non-survey checks for expanded feedback aggregation, disabled interactive
  states vs read-only plugin actions, parse/unknown fallback callbacks, and
  chart accessibility output.
- AUI Android is now published on Maven Central as
  `com.bennyjon:aui-core:0.1.0-alpha01` and
  `com.bennyjon:aui-compose:0.1.0-alpha01`.

## Next Task

No queued release follow-up. Keep the connected-androidTest AVD/Espresso
failure documented as a non-gating environment caveat unless reproduced as a
repo issue.

## Constraints

- The library is a pure renderer with callback. Do not add chat history,
  networking, or conversation state to library modules.
- `aui-core` must not depend on Android or Compose.
- Theme and component behavior should continue to route through AUI abstractions
  rather than direct Material theming in components.
- Unknown JSON types should remain non-fatal and preserved where applicable.
- Prefer additive, well-scoped changes; keep public API docs current.

## Canonical References

- Public usage and integration: `README.md`
- Architecture: `docs/architecture.md`
- Wire format and component spec: `spec/aui-spec-v1.md`
- Active execution plan: `.planning/phase5-live-chat.md`
- Release-readiness plan: `.planning/first-release-readiness.md`
- Historical plans: `.planning/archive/`

If the user asks to prepare the library for a first release, release readiness,
publishing, or launch polish, load `.planning/first-release-readiness.md` in
addition to the current phase plan.

## Update Rules

Any coding agent ending a session should update this file if project status has
changed.

Keep updates operational and short:
- keep `Current Status` accurate
- update `Next Task` when the recommendation changes
- refresh `Current Capabilities` only when launch-relevant behavior changes
- keep the file brief enough to scan quickly on launch

If a tool-specific file exists, it should point back here instead of becoming a
second source of truth.
