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

- Current phase: Phase 5, Live Chat Demo
- Goal: add a demo chat flow that talks to real LLM providers end-to-end while
  keeping the library itself a pure renderer
- Current phase plan: `.planning/phase5-live-chat.md`
- Next recommended task: Session 30, polish live chat UX and documentation
- Known blockers: none recorded
- Known issues: none recorded

## Current Direction

Phase 5 work is focused on the `demo` module:
- provider-neutral `LlmClient` integration
- Room-backed chat persistence
- live chat UI and provider switching

Library-level changes are allowed only when they are intrinsic to the renderer
or response model, not chat-product features.

## Recent Progress

- Sessions 1-7: Phase 1 complete
- Sessions 8-10: Phase 2 complete
- Sessions 11-14: Phase 3 complete
- Sessions 15-20: Phase 4 complete
- Session 21: Added generic LLM contracts, structured response extraction, fake
  provider, and tests
- Session 22: Added Room schema, chat repository, DAO/database, and tests
- Session 23: Added live chat ViewModel/UI wiring, spent-marking, and read-only
  response support in the library
- Session 24: Added `ClaudeLlmClient`, provider selection, runtime rebuilding,
  and config-backed API key wiring
- Session 25: Added `AuiPromptConfig`, prompt aggressiveness tuning, custom
  examples, and new prompt tests
- Session 26: Added inline Markdown support for `AuiText`
- Session 27: Reintroduced `INLINE`, clarified `EXPANDED`, added response card
  stub and large-screen host routing guidance
- Session 28: Replaced `sheet` with `survey`, simplified survey authoring, and
  synced spec/architecture/README
- Docs sync on 2026-04-17: updated public docs to match current code
- Status variants on 2026-04-17: expanded status components and color tokens
- Session 29: Added `OpenAiLlmClient`, OpenAI provider selection, and config-backed API key wiring
- Session 30: Refined live chat layout so two-pane only activates when an `EXPANDED`
  response exists, and landscape single-pane keeps chat at split-pane width
- Session 32: Added first-class `file_content` artifacts so markdown/config/source
  files render as single copyable blocks instead of fragmented presentation text
- Session 33: Added download-to-Downloads support to `file_content`, with scoped-storage
  saving on Android 10+ and legacy-permission fallback on older Android
- Session 34: Replaced `file_content` download toasts with an inline themed notice
  under the header, including "Open" and dismiss actions
- Session 31: Tuned live chat context hints so markdown/document requests steer the
  model toward `EXPANDED` document-style AUI instead of defaulting to `INLINE`

## Next Task

Polish the live chat demo and update docs:
- spinner, empty state, error banner with retry
- clear confirmation dialog, snackbar on provider switch
- Phase 5 status/docs sync including `docs/livechat.md`

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
- Historical plans: `.planning/archive/`

## Update Rules

Any coding agent ending a session should update this file if project status has
changed.

Keep updates operational and short:
- keep `Current Status` accurate
- update `Next Task` when the recommendation changes
- append one concise `Recent Progress` line for meaningful completed work
- keep the file brief enough to scan quickly on launch

If a tool-specific file exists, it should point back here instead of becoming a
second source of truth.
