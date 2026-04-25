# Live Chat Demo

`demo/` includes a reference chat experience that talks to real LLM providers
end-to-end while keeping the AUI library itself a pure renderer.

This document is about the demo app only. None of this chat state, persistence,
or networking belongs in `aui-core` or `aui-compose`.

## What The Demo Covers

- Provider-swappable chat via `Fake`, `Claude`, and `OpenAI`
- Room-backed message persistence
- AUI rendering inside a real chat surface
- Host-owned routing for `inline`, `expanded`, and `survey` responses
- Settings for provider-independent debug tooling and prompt inspection

## Architecture

The live chat implementation lives under
`demo/src/main/java/com/bennyjon/auiandroid/`.

Key areas:

- `livechat/`
  `LiveChatScreen.kt` renders the chat UI, detail pane/sheet routing, empty
  state, spinner, retry affordance, and composer.
- `livechat/`
  `LiveChatViewModel.kt` owns sending, retry, provider switching, selected
  detail state, and window-size hints used by the system prompt.
- `data/chat/`
  `DefaultChatRepository.kt` persists messages in Room and orchestrates LLM
  calls without pushing chat concerns into the library modules.
- `data/llm/`
  `LlmClient.kt` defines the provider-neutral contract. `FakeLlmClient`,
  `ClaudeLlmClient`, and `OpenAiLlmClient` implement it.
- `settings/`
  `SettingsScreen.kt` and `SystemPromptScreen.kt` expose debug-log toggles and a
  generated prompt viewer with copy/download actions.

## Provider Model

The demo uses a provider-neutral `LlmClient` so the UI, repository, and stored
message schema do not care which backend is active.

Available providers today:

- `Fake` for local scripted responses
- `Claude` via Anthropic Messages API
- `OpenAI` via Chat Completions

The selected provider is persisted with DataStore. Providers that need API keys
are disabled in the UI when their keys are not configured.

## Message Persistence

Messages are stored in Room as raw user text or raw assistant payloads. The demo
parses assistant AUI at load time instead of storing an already-interpreted chat
model.

That keeps the stored record provider-agnostic and preserves the original model
output for replay/debugging.

## Display Routing

The demo deliberately treats display levels as host concerns:

- `inline`
  Renders directly in the chat timeline.
- `expanded`
  Renders as an `AuiResponseCard` stub in chat. On narrow layouts it opens in a
  bottom sheet; on wide layouts it opens in a side detail pane.
- `survey`
  Always opens in a modal sheet because it is an input flow. The card remains in
  chat so the user can reopen it after dismissing the sheet.

Older interactive AUI responses are marked spent in chat, while read-only
actions such as `open_url` can stay enabled.

## Settings And Debugging

The demo includes a Settings screen with:

- `Chat Debug Logs`
  Toggles verbose repository/extractor logging for live chat troubleshooting.
- `System Prompt`
  Shows the generated `AuiCatalogPrompt`, with copy and Downloads export.

This makes it easier to inspect exactly what schema and examples the active demo
is sending to a provider.

## What The Demo Is Not

- Not part of the library API surface
- Not a required architecture for AUI consumers
- Not a signal that AUI manages chat history or networking for you

Use it as a reference implementation for host-side responsibilities around a
pure renderer.
