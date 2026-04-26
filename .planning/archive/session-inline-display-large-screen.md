# Session Plan — INLINE display + large-screen detail pane

**Status:** Not started. Execute any time after current Phase 5 sessions complete.

## Goal

Re-introduce `AuiDisplay.INLINE` with new semantics so the host can route
responses between the chat feed and a detail pane on large screens, while
keeping the library a pure renderer. The AI picks the intent (chat flow vs
focused content); the host picks the placement based on window size.

- **INLINE** — belongs in the chat flow. Always renders inline in the
  chat list, regardless of screen size.
- **EXPANDED** — focused / detail content. Always surfaced through a
  "detail surface" the user opens deliberately. The host switches
  affordance at a **single width breakpoint** (default **600 dp**,
  matching `WIDTH_DP_MEDIUM_LOWER_BOUND` from `androidx.window`):
  - Width **< 600 dp** → tappable **card stub** in the chat
    (title + short description) that opens a **bottom sheet** containing
    the full render when tapped.
  - Width **≥ 600 dp** → tappable **card stub** in the chat + full
    render in a persistent right-side **detail pane**.
- **SHEET** — unchanged. Still used for AI-authored multi-step flows.

The library renders INLINE and EXPANDED identically (same `else` branch
in `DisplayRouter`). All stub/card/sheet/pane routing is the host app's
responsibility.

## Non-goals

- No change to sheet behavior on any window size.
- No change to `AuiRenderer`'s public API.
- No foldable / table-top posture handling. A single width breakpoint
  is enough; we explicitly skip posture / hinge-aware layouts.
- No multi-tier adaptive layouts (Compact / Medium / Expanded / Large /
  ExtraLarge). One breakpoint, one layout swap.

---

## Library changes (aui-core + aui-compose)

1. **`aui-core/.../model/AuiDisplay.kt`** — add `INLINE` with serial name
   `"inline"`. Place it first in the enum (most common intent). Update KDoc
   to describe the semantic distinction (chat flow vs focused content), not
   the rendering (which is host-dependent).

2. **`aui-core/.../model/AuiResponse.kt`** — add two optional top-level
   fields used when `display == EXPANDED` to populate the host's stub card:
   ```kotlin
   @SerialName("card_title") val cardTitle: String? = null,
   @SerialName("card_description") val cardDescription: String? = null,
   ```
   Hosts fall back to the first `heading`/`text` block when these are
   null, so existing content stays renderable.

3. **`aui-compose/.../display/DisplayRouter.kt`** — no logic change. The
   current `else` branch already catches everything non-SHEET, so INLINE
   and EXPANDED render identically. Update KDoc to state this explicitly
   and note that host apps may route EXPANDED to a separate surface
   (sheet in narrow layouts, detail pane in wide layouts).

4. **`aui-core/.../AuiCatalogPrompt.kt`**:
   - Update `DISPLAY_LEVELS`, `SCHEMA_FORMAT`, `COMPONENT_CHEAT_SHEET`
     copy to describe all three levels.
   - Teach the AI the rule of thumb:
     - INLINE → quick replies, short confirmations, small polls, "keep
       the conversation moving" content. Renders directly in the chat.
     - EXPANDED → rich cards, long lists, comparisons, multi-block
       content a user may want to study. Surfaced through a tappable
       card stub; the full content opens in a sheet (small screens) or
       a detail pane (large screens). **The AI should add a short
       `card_title` and `card_description` so the stub is meaningful.**
   - Schema addition: top-level optional `card_title` and
     `card_description` on `AuiResponse` when `display == "expanded"`.
     Falls back to the first `heading`/`text` block if omitted.
   - Extend `ALL_COMPONENT_TYPES` / display-level test list if present.

5. **Tests**:
   - `AuiParserTest` — round-trip `"inline"`, and `card_title` /
     `card_description` fields on an EXPANDED response.
   - `AuiCatalogPromptTest` — assert the three levels are documented.
   - `AuiResponseIsReadOnlyTest` — no change expected; verify.

## Prompt: per-turn device context

The system prompt is currently built once via Hilt
(`AppModule.provideSystemPrompt`). Device size must be refreshed per turn
(rotation, resize), so it can't live in the singleton prompt.

**Approach A (preferred)** — add a `contextHints` parameter to
`ChatRepository.sendUserMessage`. `DefaultChatRepository` concatenates the
hints onto the static catalog prompt when calling the `LlmClient`. Smaller
blast radius; keeps the static prompt memoized.

```kotlin
// ChatRepository.kt
suspend fun sendUserMessage(
    conversationId: String,
    text: String,
    contextHints: String = "",
)
```

`LiveChatViewModel.send` builds `contextHints` from the current window
width, e.g.:

```
DEVICE: width=840dp, height=1080dp, layout=two_pane.
Prefer "inline" for chat-flow messages; use "expanded" when the
response is focused content the user may want to linger on.
```

`layout` is a derived value: `two_pane` when width ≥ 600 dp, otherwise
`single_column`. This gives the LLM a plain-language hint without
leaking Android terminology.

**Approach B (rejected for now)** — replace the `@SystemPrompt` string with
a `SystemPromptBuilder` interface. Larger change, unnecessary today.

## Demo host changes

1. **`LiveChatScreen`** — read current window width (via
   `BoxWithConstraints`, or `currentWindowAdaptiveInfo().windowSizeClass
   .isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)` from
   `androidx.window.core.layout`). Define one constant:
   ```kotlin
   private val TwoPaneBreakpointDp = 600.dp
   ```
   Two layouts:
   - **Width < 600 dp**: single-column chat list (today's layout).
     INLINE renders inline; EXPANDED renders as a tappable card stub that
     opens a bottom sheet on tap.
   - **Width ≥ 600 dp**: `Row { ChatList(weight=1f); DetailPane(weight=1f) }`.
     INLINE renders inline; EXPANDED renders as a tappable card stub in
     chat **and** as the full render in the right pane.

2. **Card stub composable** — new `ExpandedResponseCard` in
   `demo/livechat/`:
   - Pulls `cardTitle` / `cardDescription` from the response, falling
     back to the first `heading`/`text` block.
   - Grays out when `isAuiSpent` is true (matches existing spent
     styling), but stays tappable so the user can review what they
     already answered.
   - Click handler is supplied by the parent (opens a sheet in
     single-column mode, swaps the active detail message in two-pane
     mode).

3. **Single-column EXPANDED sheet** — hoist a
   `var activeExpandedMessage: ChatMessage? by remember` in
   `LiveChatScreen` plus a `ModalBottomSheet` that shows `AuiRenderer`
   bound to that message's response. Tapping the stub sets it; dismissal
   clears it. This is a **display affordance**, distinct from AI-authored
   `AuiDisplay.SHEET` multi-step flows. Important nuances:
   - The sheet is stateless wrt feedback. When the user interacts and
     feedback fires, dismiss the sheet immediately (the message flips to
     spent via `markSpentInteractives`, and re-tapping the now-grayed
     stub re-opens the spent read-only render).
   - Do **not** reuse `SheetFlowDisplay` — that's for multi-step flows.
     A plain `ModalBottomSheet` + `AuiRenderer` is enough.

4. **Detail pane state (width ≥ 600 dp only)** — add to `LiveChatViewModel`:
   ```kotlin
   val activeDetailMessageId: StateFlow<String?>
   fun openDetail(messageId: String)
   fun closeDetail()
   ```
   Defaults to the newest assistant message whose `auiResponse.display ==
   EXPANDED`. Tapping a stub in chat swaps it. A new EXPANDED response
   arriving auto-promotes to active (sticky-latest behavior).

5. **Spent behavior**:
   - Single-column: sheet dismisses on feedback; stub turns gray; re-tappable.
   - Two-pane: detail pane content grays out in place; user can keep the
     pane on the spent message or tap another stub. No auto-advance.

6. **Device context wiring**: `LiveChatScreen` passes the current
   width / height dp to the ViewModel on every composition; the VM
   derives `layout = if (widthDp >= 600) "two_pane" else "single_column"`.
   `send()` reads the latest on dispatch and forwards to the repository.

7. **Fake content** — mark a few responses in `FakeLlmClient.kt` and
   `demo/src/main/assets/all-blocks-showcase.json` with `"inline"` so the
   new mode is exercised without a real LLM. Add at least one EXPANDED
   fake response that includes `card_title` / `card_description` to
   verify stub rendering + sheet open on compact.

## Files likely to touch

- `aui-core/src/main/java/com/bennyjon/aui/core/model/AuiDisplay.kt`
- `aui-core/src/main/java/com/bennyjon/aui/core/model/AuiResponse.kt`
  (add `cardTitle` / `cardDescription`)
- `aui-core/src/main/java/com/bennyjon/aui/core/AuiCatalogPrompt.kt`
- `aui-compose/src/main/java/com/bennyjon/aui/compose/display/DisplayRouter.kt`
  (docs only)
- `aui-core/src/test/java/com/bennyjon/aui/core/AuiCatalogPromptTest.kt`
- `aui-core/src/test/java/com/bennyjon/aui/core/AuiParserTest.kt`
- `demo/src/main/java/com/bennyjon/auiandroid/data/chat/ChatRepository.kt`
- `demo/src/main/java/com/bennyjon/auiandroid/data/chat/DefaultChatRepository.kt`
- `demo/src/main/java/com/bennyjon/auiandroid/livechat/LiveChatScreen.kt`
- `demo/src/main/java/com/bennyjon/auiandroid/livechat/LiveChatViewModel.kt`
- `demo/src/main/java/com/bennyjon/auiandroid/livechat/ExpandedResponseCard.kt`
  (new — tappable stub composable)
- `demo/src/main/java/com/bennyjon/auiandroid/data/llm/FakeLlmClient.kt`
- `demo/src/main/assets/all-blocks-showcase.json`
- `spec/aui-spec-v1.md` (add INLINE description + card fields)
- `docs/architecture.md` (one-line mention of host-side routing)

## Open questions to resolve before coding

1. **Stub in chat for EXPANDED in two-pane mode**: should it always
   show, or only when the message is not the currently-active detail?
   (Default suggestion: always show, but highlight the active one.)
2. **Empty detail pane (two-pane mode)**: nothing, placeholder text, or
   sticky last EXPANDED? (Default suggestion: sticky last EXPANDED —
   closest to what the user was looking at.)
3. **Explicit close button** in the detail pane header? Needed only if
   "empty detail pane" is ever a real state.
4. **Sheet dismissal timing (single-column)**: dismiss immediately when
   a non-submit feedback fires (e.g. picking a chip in a multi-step
   form) or wait until a terminal action like `submit`? (Default
   suggestion: dismiss only on submit/terminal feedback to avoid losing
   in-progress input; mirror the semantics of `SheetFlowDisplay`.)
5. **AI-authored SHEET in two-pane mode**: keep bottom sheet as-is, or
   route sheets to the detail pane? (Default: keep unchanged, revisit
   later.)
6. **Breakpoint value**: 600 dp (the `WIDTH_DP_MEDIUM_LOWER_BOUND` used
   by Google's list-detail samples) vs 840 dp (the old "Expanded"
   threshold — stricter, avoids crowded panes on 7" tablets). (Default
   suggestion: start at 600 dp; revisit if panes feel cramped.)
7. **Fallback when `card_title` is missing**: is using the first
   `heading`/`text` block good enough, or should we require
   `card_title`? (Default: soft fallback so older content and
   non-compliant LLMs still render.)
8. **Existing persisted rows** (pre-INLINE) — verify Room round-trips
   them; they'll deserialize as `EXPANDED` or `SHEET`, so no migration is
   needed. Add a test if useful.

## Execution order (suggested)

1. Library: enum + `AuiResponse.cardTitle` / `cardDescription` +
   prompt copy + parser tests.
2. Library: DisplayRouter KDoc.
3. Demo: extend `ChatRepository` with `contextHints`; thread through
   `DefaultChatRepository` and `LiveChatViewModel.send`.
4. Demo: `ExpandedResponseCard` composable + card-stub-opens-
   bottom-sheet path (applied everywhere first; skip breakpoint logic
   for this step). Verify feedback flow and spent rendering end-to-end.
5. Demo: add the 600 dp breakpoint check + two-pane scaffold. At / above
   the breakpoint, render full EXPANDED in the right pane in addition
   to the stub (and suppress the bottom sheet).
6. Demo: wire device context into the prompt (width / height dp +
   derived `layout` string).
7. Demo: mark sample FakeLlm responses with `inline` and add EXPANDED
   fakes with `card_title` / `card_description`.
8. Manual test matrix: phone portrait (~360 dp), phone landscape
   (~640–800 dp — crosses the breakpoint, good regression coverage),
   tablet portrait (~800 dp) and landscape (~1200 dp), rotation
   mid-conversation while a sheet or pane is open, switching provider
   while in two-pane mode.

## Acceptance criteria

- AUI JSON with `"display": "inline"` parses and renders inline.
- AUI JSON with `"display": "expanded"` + `card_title` /
  `card_description` parses and renders as a tappable card stub in chat.
- On a phone emulator (e.g. Pixel 5, ~411 dp wide): INLINE renders
  inline; tapping an EXPANDED stub opens a bottom sheet with the full
  render; feedback inside the sheet fires correctly and the stub
  becomes spent (grayed, still tappable, read-only).
- On a tablet emulator (e.g. Pixel Tablet, ~1280 dp wide): INLINE
  renders inline; the EXPANDED stub shows in chat AND the full render
  shows in the right detail pane. Tapping a different stub swaps the
  pane contents.
- Resizing / rotating across the 600 dp boundary mid-conversation
  switches layouts cleanly without losing state: going narrow collapses
  the detail pane (stubs remain, tapping opens a sheet); going wide
  restores the pane on the sticky-latest EXPANDED response.
- The system prompt on each send includes the current width / height
  in dp and a derived `layout` hint, plus an instruction on when to
  pick INLINE vs EXPANDED.
- Unit tests cover parser round-trip for `"inline"` and for
  `card_title` / `card_description`, plus prompt copy.
