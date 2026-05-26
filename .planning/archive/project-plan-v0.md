# AUI — Project Plan

## Overview

Build a working AUI (Attributed UI) system: a JSON format that lets AI assistants
respond with interactive native UI components inside a chat conversation.

**Platform:** Android (Jetpack Compose) first
**AI Backend:** Anthropic Claude API (Phase 2)
**First Milestone:** Working renderer with hardcoded JSON

---

## Phase 1: Foundation (Weeks 1-2)
**Goal:** Render hardcoded AUI JSON as native Compose UI inside a chat screen.

### 1A: JSON Schema & Parsing (Week 1, Days 1-3)

- [ ] Finalize AUI JSON schema (based on spec v0.5)
- [ ] Define Kotlin data classes for:
  - `AuiResponse` (top-level: display, blocks, sheet_title?, sheet_dismissable?)
  - `AuiBlock` (sealed class with type, data, id?, feedback?)
  - `AuiFeedback` (action, params, label)
  - Data classes per component type (TextData, CardBasicData, etc.)
- [ ] Build JSON parser (Kotlinx Serialization or Moshi)
  - Polymorphic deserialization on `type` field
  - Graceful handling of unknown types (skip, log warning)
  - Graceful handling of missing optional fields
- [ ] Unit tests: parse sample JSON → verify data classes
- [ ] Create 5-6 hardcoded JSON samples:
  - Simple text-only inline response
  - Inline with badges and quick replies
  - Expanded with product cards in horizontal scroll
  - Expanded with list + quick replies
  - Sheet with form
  - Sheet with chips + confirmation button

### 1B: Core Renderer (Week 1, Days 3-5)

- [ ] Create `AuiRenderer` composable:
  - Takes `AuiResponse` → emits Compose UI
  - Routes each block to its component composable by type
  - Handles unknown types (skip silently)
- [ ] Implement Phase 1 components (~12 components):

  **Text:**
  - `text` — Text with optional markdown (bold, italic, code, links)
  - `heading` — Bold section heading
  - `caption` — Small muted text

  **Cards:**
  - `card_basic` — Title + subtitle
  - `card_basic_icon` — Icon + title + subtitle
  - `card_image_left` — Image left, text right

  **Lists:**
  - `list_simple` — Plain text list
  - `list_icon` — List with leading icons

  **Input:**
  - `button_primary` — Filled CTA button
  - `button_secondary` — Outlined button
  - `quick_replies` — Row of suggestion chips

  **Utility:**
  - `divider` — Horizontal line
  - `spacer` — Vertical space

### 1C: Theme Registry (Week 1, Day 5)

- [ ] Define `AuiTheme` object:
  - Color roles: primary, secondary, surface, background, error, success, etc.
  - Typography roles: heading, body, caption, label, etc.
  - Spacing scale: xs, s, m, l, xl
  - Shape scale: small, medium, large corner radii
- [ ] Wire all Phase 1 components to use AuiTheme (no hardcoded values)
- [ ] Wrap in Compose `CompositionLocalProvider` for easy overrides
- [ ] Test: swap theme → all components update

### 1D: Chat Integration (Week 2, Days 1-3)

- [ ] Build basic chat screen:
  - LazyColumn with message bubbles
  - User messages (right-aligned, simple text)
  - AI messages (left-aligned, rendered via AuiRenderer)
  - Text input bar at bottom (for later use)
- [ ] Implement display levels:
  - `inline` — render blocks inside the AI bubble composable
  - `expanded` — text blocks in bubble, remaining blocks full-width below
  - `sheet` — text blocks in bubble, remaining blocks in BottomSheet
- [ ] Load hardcoded JSON samples as AI "messages" to verify rendering
- [ ] Screenshot test each display level

### 1E: Feedback Loop (Week 2, Days 3-5)

- [ ] Implement feedback handling:
  - Components with `feedback` become clickable
  - On tap: create a `FeedbackEvent` (action, params, label)
  - Display `label` as a new user message in the chat
  - Store the `FeedbackEvent` in conversation history
- [ ] Handle per-item feedback in lists and horizontal scroll
- [ ] Handle `quick_replies` feedback (each option has its own)
- [ ] Handle sheet dismiss event (optional `sheet_dismissed` feedback)
- [ ] Handle `{{value}}` and `{{label}}` placeholder substitution in feedback labels
- [ ] Test the loop:
  - Show expanded response with tappable cards
  - Tap card → user message appears → next hardcoded response loads
  - Show sheet → interact → sheet closes → confirmation appears

### Phase 1 Deliverable
A chat screen that loads hardcoded AUI JSON and renders fully interactive
native Compose UI. User can tap components, feedback appears as messages,
and the conversation progresses through hardcoded responses.

---

## Phase 2: AI Integration (Weeks 3-4)
**Goal:** Replace hardcoded JSON with live Claude API responses.

### 2A: Claude API Client (Week 3, Days 1-2)

- [ ] Set up Anthropic SDK / HTTP client (Ktor or OkHttp)
- [ ] Build conversation manager:
  - Maintains message history (user text + AI AUI responses + feedback events)
  - Serializes history into Claude messages format
  - Handles structured output (JSON mode or system prompt instruction)
- [ ] System prompt with AUI catalog manifest (from spec v0.5)
- [ ] Parse Claude response → AuiResponse
- [ ] Error handling: malformed JSON → show error_state component or text fallback

### 2B: Streaming (Week 3, Days 3-5)

- [ ] Stream Claude response via SSE
- [ ] Progressive rendering strategy:
  - Option A: Wait for complete JSON, then render all at once
  - Option B: Stream text blocks immediately, render components after JSON is complete
  - Start with Option A (simpler), iterate to Option B
- [ ] Show `loading` component while waiting for response
- [ ] Handle timeout / network errors gracefully

### 2C: Full Conversation Loop (Week 4, Days 1-3)

- [ ] Wire it all together:
  - User types message → sent to Claude with history
  - Claude responds with AUI JSON → rendered in chat
  - User taps component → feedback sent to Claude as next message
  - Claude responds to feedback → rendered in chat
  - Repeat
- [ ] Test scenarios:
  - Open-ended question → text-only inline response
  - "Show me restaurants" → expanded cards
  - Tap restaurant → sheet with booking form
  - Submit form → inline confirmation
- [ ] Handle mixed responses (some turns text-only, some with components)

### 2D: Polish & Edge Cases (Week 4, Days 3-5)

- [ ] Handle Claude not returning valid AUI JSON (fallback to text)
- [ ] Handle Claude using unknown component types (skip gracefully)
- [ ] Handle large responses (many blocks — scroll performance)
- [ ] Handle rapid feedback taps (debounce)
- [ ] Handle sheet + inline in conversation history scroll position
- [ ] Add basic error recovery (retry button)
- [ ] Performance profiling (JSON parsing time, render time)

### Phase 2 Deliverable
A fully working chat app where Claude responds with interactive AUI
components. The AI dynamically chooses inline/expanded/sheet presentation,
and user interactions flow back as conversation context.

---

## Phase 3: Catalog Expansion (Weeks 5-6)
**Goal:** Build out the full Phase 1+2 component catalog from the spec.

### 3A: Content Components

- [ ] `card_image_top` — Card with image on top
- [ ] `card_product_vertical` — Product card (image, title, price, rating)
- [ ] `card_product_horizontal` — Horizontal product card
- [ ] `card_profile` — Avatar + name + bio
- [ ] `card_stat` — Big number + label + trend
- [ ] `card_event` — Event with date/time/location
- [ ] `card_order_tracking` — Order status display
- [ ] `card_quote` — Quote/testimonial
- [ ] `card_code` — Syntax-highlighted code block
- [ ] `image_single` — Standalone image
- [ ] `image_gallery` — Swipeable images with page indicator
- [ ] `badge_info`, `badge_success`, `badge_warning`, `badge_error`
- [ ] `link_preview` — URL unfurling card

### 3B: List & Layout Components

- [ ] `list_avatar` — List with leading images
- [ ] `list_numbered` — Ordered list
- [ ] `list_checklist` — Checkable list (read-only)
- [ ] `horizontal_scroll_cards` — Scrollable card row
- [ ] `section_header` — Title + optional trailing action
- [ ] `status_banner_info/success/warning/error`
- [ ] `stepper_horizontal` — Step progress bar
- [ ] `progress_bar` — Progress indicator

### 3C: Input Components

- [ ] `chip_select_single` — Single-select chips
- [ ] `chip_select_multi` — Multi-select chips
- [ ] `input_text_single` — Text input with submit
- [ ] `input_text_multi` — Multiline text input
- [ ] `input_email` — Email keyboard input
- [ ] `input_select` — Dropdown picker
- [ ] `input_date` — Date picker
- [ ] `input_slider` — Range slider
- [ ] `input_rating_stars` — Star rating
- [ ] `form_group` — Grouped inputs with submit
- [ ] `button_ghost`, `button_danger`
- [ ] `button_row_primary_secondary`, `button_row_primary_ghost`

### 3D: Utility

- [ ] `loading` — Loading indicator
- [ ] `map_static` — Static map image
- [ ] `rich_text` — Text with inline spans

### Phase 3 Deliverable
Full component catalog (~50 components) all rendering natively in Compose.
The AI can use any component from the catalog to build rich responses.

---

## Phase 4: Production Hardening (Weeks 7-8)
**Goal:** Make it robust enough for real use.

- [ ] Accessibility: content descriptions, focus order, screen reader support
- [ ] Animation: entry animations for blocks, sheet transitions
- [ ] Caching: image caching (Coil), response caching
- [ ] Offline: graceful degradation when network is unavailable
- [ ] Component expiration: disable interactive components after use
- [ ] Dark mode: theme toggle with full component support
- [ ] Performance: benchmark rendering 50+ blocks, optimize LazyColumn
- [ ] Testing: snapshot tests for every component, integration tests for feedback loop
- [ ] Documentation: KDoc for all public APIs, sample app, README

---

## Phase 5: Open Source & Beyond (Week 9+)
**Goal:** Share it with the world.

- [ ] Extract renderer into standalone library (Maven artifact)
- [ ] Publish JSON schema as a standalone spec document
- [ ] Create sample app demonstrating all components + AI integration
- [ ] Write blog post: "Why we built AUI" — positioning vs A2UI
- [ ] Publish to GitHub with Apache 2.0 license
- [ ] Start iOS (SwiftUI) renderer
- [ ] Explore contributing AUI learnings to A2UI project

---

## Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| UI Framework       | Jetpack Compose                     |
| Language           | Kotlin                              |
| JSON Parsing       | Kotlinx Serialization               |
| Image Loading      | Coil (Compose)                      |
| Networking         | Ktor Client (for Claude API)        |
| AI                 | Anthropic Claude API (Messages)     |
| Dependency Injection | Hilt or Koin (optional for v1)   |
| Build              | Gradle (Kotlin DSL)                 |
| Min SDK            | 26 (Android 8.0)                    |
| Target SDK         | 35                                  |

---

## Project Structure (proposed)

```
aui-android/
├── app/                          # Demo/sample chat app
│   ├── ui/
│   │   ├── ChatScreen.kt         # Main chat screen
│   │   ├── ChatViewModel.kt      # Conversation state management
│   │   └── theme/
│   │       └── AppTheme.kt       # App-specific theme overrides
│   ├── data/
│   │   ├── SampleResponses.kt    # Hardcoded JSON for Phase 1
│   │   └── ClaudeApiClient.kt    # Claude integration (Phase 2)
│   └── MainActivity.kt
│
├── aui-core/                     # Library: parsing + data models
│   ├── model/
│   │   ├── AuiResponse.kt        # Top-level response
│   │   ├── AuiBlock.kt           # Sealed class for all block types
│   │   ├── AuiFeedback.kt        # Feedback data
│   │   ├── components/           # Data classes per component type
│   │   │   ├── TextData.kt
│   │   │   ├── CardBasicData.kt
│   │   │   ├── ListSimpleData.kt
│   │   │   └── ...
│   │   └── AuiDisplay.kt         # Enum: inline, expanded, sheet
│   ├── parser/
│   │   └── AuiParser.kt          # JSON → AuiResponse
│   └── schema/
│       └── aui-schema.json       # JSON Schema for validation
│
├── aui-compose/                  # Library: Compose renderer
│   ├── AuiRenderer.kt            # Main entry point composable
│   ├── theme/
│   │   ├── AuiTheme.kt           # Theme definition
│   │   └── AuiDefaults.kt        # Default theme values
│   ├── components/               # One file per component
│   │   ├── AuiText.kt
│   │   ├── AuiHeading.kt
│   │   ├── AuiCardBasic.kt
│   │   ├── AuiCardBasicIcon.kt
│   │   ├── AuiListSimple.kt
│   │   ├── AuiListIcon.kt
│   │   ├── AuiButtonPrimary.kt
│   │   ├── AuiButtonSecondary.kt
│   │   ├── AuiQuickReplies.kt
│   │   ├── AuiDivider.kt
│   │   ├── AuiSpacer.kt
│   │   └── ...
│   ├── display/                  # Display level handling
│   │   ├── InlineDisplay.kt      # Render inside bubble
│   │   ├── ExpandedDisplay.kt    # Full-width in feed
│   │   └── SheetDisplay.kt       # Bottom sheet
│   └── feedback/
│       └── FeedbackHandler.kt    # Tap handling + event creation
│
└── build.gradle.kts
```

---

## Key Design Decisions

### 1. Sealed class for blocks (not reflection)
Each component type is a case in a Kotlin sealed class. This gives us:
- Compile-time exhaustive `when` matching
- No reflection overhead
- Clear error if a new type is added but not handled

### 2. Theme via CompositionLocal
`AuiTheme` is provided via `CompositionLocalProvider`, just like MaterialTheme.
This means:
- Any app can override the theme by wrapping AuiRenderer
- Components never hardcode colors/fonts/spacing
- Dark mode is just a different theme instance

### 3. Feedback as a callback
`AuiRenderer` takes a callback: `onFeedback: (AuiFeedback) -> Unit`
The host app decides what to do with it (add to chat, send to API, etc.)
The renderer doesn't know about conversations — it just reports taps.

### 4. Parser is separate from renderer
`aui-core` has no Compose dependency. It's pure Kotlin.
This means:
- It can be shared with iOS via KMP later
- It can be used server-side for validation
- It can be tested without Android instrumentation

### 5. Unknown types are skipped, not crashed
If the JSON contains `"type": "card_weather"` but the renderer doesn't
have that component yet, it silently skips it and logs a warning.
This is critical for forward compatibility — the AI might use newer
components that an older client doesn't have yet.

---

## Success Criteria (Phase 1)

- [ ] Chat screen renders 6 different hardcoded AUI responses correctly
- [ ] All 3 display levels (inline, expanded, sheet) work
- [ ] Tapping a component with feedback creates a user message
- [ ] Theme swap changes all component appearances
- [ ] Unknown component types are skipped gracefully
- [ ] No hardcoded colors, fonts, or dimensions in any component
