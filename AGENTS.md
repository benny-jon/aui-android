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
- Last completed: Sanity-checks **S7 — Code hygiene**. Removed production
  renderer logging in favor of the existing `onUnknownBlock` callback contract,
  replaced the last library `!!` assertion in `AuiChart`, narrowed broad
  exception catches in parser/download/plugin-data paths to concrete failure
  types, and made comparison-oriented case folding explicitly `Locale.ROOT`
  while leaving display capitalization locale-aware. Findings recorded in
  `.planning/library-sanity-checks.md`. Verified with `./gradlew :aui-core:test`,
  `./gradlew :aui-compose:testDebugUnitTest`, and
  `./gradlew :aui-compose:compileDebugKotlin`.
- Next recommended task: Sanity-checks **S8 — Compose-specific correctness**:
  audit `LaunchedEffect` / `rememberSaveable` / lifecycle correctness,
  `Context` capture in remembered lambdas, and preview placement in
  `aui-compose`.
- Known blockers: first publish blocked on Sonatype namespace verification +
  GPG key setup (owner-only, see release checklist)
- Known issues: none recorded

## Current Direction

Release-readiness work is focused on tightening the library for adopters:
- canonical host integration example in docs
- error-handling / fallback contract documentation
- release-confidence renderer tests
- publishing mechanics + release checklist

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
- Public docs are synced through 2026-04-25, including `docs/livechat.md`, and
  CI includes separate unit-test and compile-check workflows.
- Maven publishing is scaffolded via `vanniktech-maven-publish` for `aui-core`
  and `aui-compose`. Coordinates: `com.bennyjon:aui-{core,compose}:0.1.0-alpha01`.
  Generated POMs validated locally; first publish gated on owner-side Sonatype
  + GPG setup.

## Next Task

Run **Sanity-checks Session S8 — Compose-specific correctness** from
`.planning/library-sanity-checks.md`:
- audit `LaunchedEffect` keys and `rememberSaveable` coverage for library-owned
  input state
- confirm `Context` is not captured across configuration changes in remembered
  lambdas and that previews stay out of published `main` artifacts
- record findings inline in `.planning/library-sanity-checks.md`

Session 54 (Canonical Host Integration Example) is still queued from
`.planning/first-release-readiness.md` and should run after the sanity-checks
sweep (S1–S10) finishes.

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
