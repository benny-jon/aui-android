# AUI — Attributed UI Spec (Draft v0.5)

> A catalog-driven format for AI assistants to respond with **interactive UI**  
> inside a chat conversation — at three levels of prominence.  
> Rendered natively via Jetpack Compose (Android) and SwiftUI (iOS).

---

## The Idea

Today, AI chat responses are plain text — maybe with some markdown. AUI lets an AI respond with **rich, interactive components**: cards, forms, lists, buttons, media — that users can see, tap, and interact with.

The AI chooses **how prominently** to present its response based on what it's showing:

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  EXPANDED                              SHEET                 │
│  ────────                              ─────                 │
│                                                              │
│  Full-width in the chat feed.          Bottom sheet overlay. │
│  Quick answers, confirmations,         Focused, actionable.  │
│  rich content.                                               │
│                                                              │
│  ┌──────────────┐       ┌──────────────────┐                 │
│  │ AI bubble    │       │                  │   ┌───────────┐ │
│  │              │       │ ┌──────────────┐ │   │ ░░░░░░░░░ │ │
│  │ Here's your  │       │ │  Full-width  │ │   │           │ │
│  │ tracking:    │       │ │  card with   │ │   │ Book Your │ │
│  │              │       │ │  image and   │ │   │ Table     │ │
│  │ #4812 shipped│       │ │  details     │ │   │           │ │
│  │              │       │ └──────────────┘ │   │ [form]    │ │
│  │ [Track] [Help│       │                  │   │ [chips]   │ │
│  └──────────────┘       │ ┌──┐ ┌──┐ ┌──┐  │   │           │ │
│                         │ │  │ │  │ │  │  │   │ [Confirm] │ │
│  Good for:              │ └──┘ └──┘ └──┘  │   │           │ │
│  · Quick info           └──────────────────┘   └───────────┘ │
│  · Simple replies                                            │
│  · Short status         Good for:           Good for:        │
│                         · Product results   · Booking flows  │
│                         · Rich cards        · Forms          │
│                         · Image galleries   · Confirmations  │
│                         · Scrollable rows   · Multi-step     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**User interactions flow back into the conversation.** Regardless of presentation level, when a user taps a button, selects a chip, or submits a form, the result becomes the next user message — closing the loop.

---

## Design Principles

1. **Catalog, not language.** The AI picks from pre-built components. It never designs UI.
2. **One type = one component.** No variants. `button_primary` and `button_secondary` are separate types.
3. **Two presentation levels.** The AI picks `expanded` or `sheet` per response based on content.
4. **Interactions close the loop.** Every user interaction produces a feedback event that feeds back into the conversation.
5. **Text is always an option.** The AI can respond with plain text, components, or both. Components are additive.
6. **Progressive enrichment.** Start with a small catalog. Add components over time. The format never changes.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   AI MODEL                                                   │
│                                                              │
│   Receives:  conversation history + catalog manifest         │
│   Produces:  array of content blocks + presentation level    │
│                                                              │
└──────────────────┬───────────────────────────────────────────┘
                   │  AUI JSON
                   ▼
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│   CHAT CLIENT (native per platform)                          │
│                                                              │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│   │    Theme      │  │  Component   │  │   Feedback       │  │
│   │   Registry    │  │   Catalog    │  │   Handler        │  │
│   └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  Presentation Router                                 │   │
│   │                                                      │   │
│   │  "expanded" → render full-width in chat feed         │   │
│   │  "sheet"    → render in bottom sheet overlay         │   │
│   └──────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Message Format

### AI Response

```json
{
  "display": "expanded | sheet",
  "blocks": [ ... ]
}
```

That's the entire top-level structure. Two fields.

| Field     | Required | Description                                                           |
|-----------|----------|-----------------------------------------------------------------------|
| `display` | yes      | Presentation level: `expanded` or `sheet`                             |
| `blocks`  | yes      | Array of content blocks (text + catalog components)                   |

### Presentation Levels

#### `expanded`

Rendered **full-width in the chat feed**. Leading text/heading/caption blocks appear as the AI's chat bubble, and the remaining content blocks render full-width below — similar to how link previews or media attachments appear in messaging apps.

Best for: quick answers, confirmations, rich cards, media, lists, product cards, image galleries, scrollable carousels, data displays.

```json
{
  "display": "expanded",
  "blocks": [
    { "type": "text", "data": { "text": "Here are some options for you:" } },
    {
      "type": "horizontal_scroll_cards",
      "data": {
        "items": [
          { "type": "card_product_vertical", "data": { ... }, "feedback": { ... } },
          { "type": "card_product_vertical", "data": { ... }, "feedback": { ... } }
        ]
      }
    },
    { "type": "quick_replies", "data": { "options": [{ "label": "Filter" }, { "label": "Sort" }] } }
  ]
}
```

#### `sheet`

Rendered as a **bottom sheet overlay** on top of the chat. The chat is still visible behind it (dimmed). The sheet demands attention — the user should interact with it or dismiss it before continuing.

Best for: booking flows, multi-field forms, confirmations with consequences, multi-step processes, detailed views.

Sheet responses use a `steps` array instead of `blocks`. Each step is rendered inside the same persistent bottom sheet — the sheet stays open as the user advances. The library automatically renders a stepper indicator when there is more than one step.

```json
{
  "display": "sheet",
  "sheet_title": "Quick Survey",
  "steps": [
    {
      "label": "Experience",
      "question": "How was your experience?",
      "skippable": true,
      "blocks": [
        {
          "type": "chip_select_single",
          "data": {
            "key": "experience",
            "options": [
              { "label": "😊 Great", "value": "great" },
              { "label": "🙂 Good", "value": "good" },
              { "label": "😐 Okay", "value": "okay" },
              { "label": "😞 Poor", "value": "poor" }
            ]
          }
        },
        {
          "type": "button_primary",
          "data": { "label": "Next" },
          "feedback": { "action": "poll_next_step", "params": { "poll_id": "survey" } }
        }
      ]
    },
    {
      "label": "Feedback",
      "question": "Anything else you'd like to tell us?",
      "skippable": true,
      "blocks": [
        {
          "type": "input_text_single",
          "data": { "key": "open_feedback", "label": "Your feedback", "placeholder": "Optional" }
        },
        {
          "type": "button_primary",
          "data": { "label": "Submit" },
          "feedback": { "action": "poll_complete", "params": { "poll_id": "survey" } }
        }
      ]
    }
  ]
}
```

Sheet-specific fields:

| Field               | Required | Description                                                                 |
|---------------------|----------|-----------------------------------------------------------------------------|
| `sheet_title`       | no       | Title shown in the sheet header area                                        |
| `sheet_dismissable` | no       | Can the user swipe down to dismiss? Default: `true`                         |
| `steps`             | yes      | Ordered list of steps. Each step has its own `blocks`, `label`, `question`, and `skippable` |

Step fields:

| Field       | Required | Description                                                                                    |
|-------------|----------|------------------------------------------------------------------------------------------------|
| `blocks`    | yes      | Blocks to render for this step. Include a `button_primary` as the advance/submit trigger.      |
| `label`     | no       | Short label shown in the stepper indicator (e.g. `"Experience"`). Defaults to the step number. |
| `question`  | no       | Full question text recorded in the `formattedEntries` summary when the user answers.           |
| `skippable` | no       | When `true`, the library renders a "Skip" button. Skipped steps are excluded from entries.     |

The library emits a single consolidated `AuiFeedback` when the last step is submitted or skipped. `feedback.formattedEntries` contains all recorded Q&A pairs. `feedback.params` contains the merged params from all steps, plus two additional keys:

| Key              | Type   | Description                                                  |
|------------------|--------|--------------------------------------------------------------|
| `steps_total`    | string | Total number of steps in the sheet (e.g. `"3"`)              |
| `steps_skipped`  | string | Number of steps the user skipped (e.g. `"1"`)                |

When a sheet is dismissed without interaction, the client sends a feedback event: `{ "action": "sheet_dismissed" }`. The `steps_total` key is included but `steps_skipped` is not (skip buttons were not used).

When a sheet interaction triggers feedback, the sheet closes automatically and the feedback appears as the next user message.

---

### Mixed Presentation

In many cases, the AI wants to say something inline AND show rich content outside the bubble. This is handled naturally because `text` blocks at the start of an `expanded` or `sheet` response are rendered as the AI's chat bubble, and the remaining blocks appear in the expanded/sheet area:

```json
{
  "display": "expanded",
  "blocks": [
    { "type": "text", "data": { "text": "I found 3 Italian restaurants near you:" } },
    { "type": "card_image_left", "data": { ... }, "feedback": { ... } },
    { "type": "card_image_left", "data": { ... }, "feedback": { ... } },
    { "type": "card_image_left", "data": { ... }, "feedback": { ... } }
  ]
}
```

**Rendering behavior:**
- The `text` block → rendered as a normal AI chat bubble
- The `card_image_left` blocks → rendered full-width below the bubble in the feed

This means the AI doesn't need to think about "what goes in the bubble vs outside." It just lists blocks in order, and the client's presentation router splits them based on the `display` mode:

| Display    | `text` blocks                  | Other blocks                         |
|------------|--------------------------------|--------------------------------------|
| `expanded` | In the bubble                  | Full-width below the bubble          |
| `sheet`    | Uses `steps` array (not `blocks`) — each step rendered in the persistent bottom sheet |

---

## Content Blocks

### Block Structure

Every block has:

| Field       | Required | Description                                          |
|-------------|----------|------------------------------------------------------|
| `type`      | yes      | Component type from catalog (or `"text"`)            |
| `data`      | yes      | Component-specific data fields                       |
| `id`        | no       | Unique ID for tracking/accessibility                 |
| `feedback`  | no       | What happens when the user interacts (see §Feedback) |

---

## Feedback System

User interactions with components become conversation input.

### How it works:

1. A component has a `feedback` object
2. User taps/selects/submits
3. The client generates a feedback event
4. The event is sent as the next user message in the conversation
5. The AI receives it and responds
6. If the component was in a sheet, the sheet closes automatically

### Feedback Object

```json
{
  "feedback": {
    "action": "machine_readable_action_name",
    "params": { "key": "value" }
  }
}
```

- `action` — what happened (for the AI to understand)
- `params` — structured data about the interaction

The AI does **not** set a display label. The library computes one automatically (`formattedEntries`) from the heading→input pairs it finds in the rendered blocks. For a multi-step sheet, this is built from each step's `question` and the user's answer, joined by blank lines:

```
How was your experience?
😊 Great

What would you like to see improved?
Speed, Design
```

Consumers can use `feedback.formattedEntries` directly as the chat bubble text, or build a custom format from `feedback.entries` (a list of `{ question, answer }` pairs).

### Feedback in Conversation History

```json
[
  { "role": "user", "content": "Book me a table tonight" },
  { "role": "assistant", "content": { "display": "expanded", "blocks": [ ... ] } },
  { "role": "user", "content": { "feedback": { "action": "select_restaurant", "params": { "id": "nonnas" } } } },
  { "role": "assistant", "content": { "display": "sheet", "steps": [ ... ] } },
  { "role": "user", "content": { "feedback": { "action": "confirm_booking", "params": { "restaurant": "nonnas", "time": "19:30", "party_size": "2" } } } },
  { "role": "assistant", "content": { "display": "expanded", "blocks": [ ... ] } }
]
```

Note how the AI escalated from `expanded` (showing options) → `sheet` (booking form) → `expanded` (confirmation). The presentation level changes per response based on what's appropriate.

### Items with Individual Feedback

List items and cards inside `horizontal_scroll_cards` each carry their own feedback:

```json
{
  "type": "list_icon",
  "data": {
    "items": [
      {
        "icon": "flight",
        "title": "Flight AA 1234",
        "subtitle": "Departs 3:45 PM",
        "feedback": {
          "action": "view_flight",
          "params": { "flight": "AA1234" }
        }
      },
      {
        "icon": "flight",
        "title": "Flight UA 5678",
        "subtitle": "Departs 5:20 PM",
        "feedback": {
          "action": "view_flight",
          "params": { "flight": "UA5678" }
        }
      }
    ]
  }
}
```

---

## Component Catalog

Organized by what the AI is trying to accomplish. Every component works at any display level — the presentation router handles placement.

### Layout & Spacing

Sibling blocks inside a `blocks` array are automatically spaced vertically by the renderer according to the host app's AUI theme. AI authors do not control spacing and should not attempt to — there is no spacing field, no spacer block, and no way to request tighter or looser layout. Blocks that should feel visually grouped (like a button and a related chip) should be composed using group/container components instead.

### DISPLAYING INFORMATION

#### `text`
Plain text. Supports basic markdown (bold, italic, code, links).
```
data: { text: string }
```

#### `heading`
Bold section heading.
```
data: { text: string }
```

#### `caption`
Small muted text for metadata, timestamps, footnotes.
```
data: { text: string }
```

#### `rich_text`
Text with inline styled spans.
```
data: {
  text: string,
  spans: [{ start: int, len: int, style: "bold"|"italic"|"code"|"underline"|"strike" }]
}
```

#### `card_basic`
Simple card with title and optional subtitle.
```
data: { title: string, subtitle?: string }
```

#### `card_basic_icon`
Card with leading icon, title, subtitle.
```
data: { icon: string, title: string, subtitle?: string }
```

#### `card_image_top`
Card with image on top, title below.
```
data: { image: string, title: string, subtitle?: string, caption?: string }
```

#### `card_image_left`
Card with image on left, text on right.
```
data: { image: string, title: string, subtitle?: string }
```

#### `card_product_vertical`
Product — image top, name + price below.
```
data: { image: string, title: string, price: string, rating?: number, review_count?: number }
```

#### `card_product_horizontal`
Product — image left, details right.
```
data: { image: string, title: string, price: string, rating?: number, review_count?: number }
```

#### `card_profile`
Person card with avatar, name, bio.
```
data: { avatar: string, name: string, subtitle?: string, bio?: string }
```

#### `card_stat`
Big number with label and optional trend.
```
data: { value: string, label: string, trend?: string, trend_up?: boolean }
```

#### `card_event`
Event with date, time, location.
```
data: { title: string, date: string, time?: string, location?: string, image?: string }
```

#### `card_order_tracking`
Order status display.
```
data: { order_id: string, status: string, eta?: string, tracking_url?: string, items_summary?: string }
```

#### `card_quote`
Highlighted quote or testimonial.
```
data: { text: string, author?: string, source?: string }
```

#### `card_code`
Code snippet with syntax highlighting.
```
data: { code: string, language?: string, title?: string }
```

#### `image_single`
Standalone image.
```
data: { src: string, alt?: string }
```

#### `image_gallery`
Swipeable image set with page indicators.
```
data: { images: [{ src: string, alt?: string }] }
```

#### `badge_info`
Info status pill.
```
data: { text: string }
```

#### `badge_success`
Success status pill.
```
data: { text: string }
```

#### `badge_warning`
Warning status pill.
```
data: { text: string }
```

#### `badge_error`
Error status pill.
```
data: { text: string }
```

#### `link_preview`
URL preview card (like link unfurling).
```
data: { url: string, title: string, description?: string, image?: string }
```

#### `map_static`
Static map image with pin.
```
data: { latitude: number, longitude: number, label?: string, zoom?: number }
```

---

### DISPLAYING LISTS

#### `list_simple`
Plain text list.
```
data: { items: [{ title: string, subtitle?: string, feedback?: {} }] }
```

#### `list_icon`
List with leading icons.
```
data: { items: [{ icon: string, title: string, subtitle?: string, feedback?: {} }] }
```

#### `list_avatar`
List with leading images/avatars.
```
data: { items: [{ avatar: string, title: string, subtitle?: string, feedback?: {} }] }
```

#### `list_numbered`
Ordered/numbered list.
```
data: { items: [{ text: string }] }
```

#### `list_checklist`
Read-only checklist (for displaying status).
```
data: { items: [{ text: string, checked: boolean }] }
```

#### `horizontal_scroll_cards`
Horizontally scrollable row of cards.
```
data: { items: [{ type: string, data: {}, feedback?: {} }] }
```
Items must be card-type components. This is the only nesting point.

---

### PROGRESS & STATUS

#### `stepper_horizontal`
Step progress bar.
```
data: { steps: [{ label: string }], current: number }
```

#### `progress_bar`
Progress bar with label.
```
data: { label: string, progress: number, max?: number }
```

#### `status_banner_info`
Info-level banner.
```
data: { text: string }
```

#### `status_banner_success`
Success banner.
```
data: { text: string }
```

#### `status_banner_warning`
Warning banner.
```
data: { text: string }
```

#### `status_banner_error`
Error banner.
```
data: { text: string }
```

---

### COLLECTING USER INPUT

#### `button_primary`
Main CTA button.
```
data: { label: string, icon?: string }
```

#### `button_secondary`
Secondary action button.
```
data: { label: string, icon?: string }
```

#### `button_ghost`
Minimal text button.
```
data: { label: string, icon?: string }
```

#### `button_danger`
Destructive action button.
```
data: { label: string, icon?: string }
```

#### `button_row_primary_secondary`
Two buttons: primary + secondary side by side.
```
data: { primary_label: string, secondary_label: string }
Feedback: primary_feedback: {}, secondary_feedback: {}
```

#### `button_row_primary_ghost`
Two buttons: primary + ghost side by side.
```
data: { primary_label: string, ghost_label: string }
Feedback: primary_feedback: {}, ghost_feedback: {}
```

#### `quick_replies`
Row of tappable suggestion chips.
```
data: { options: [{ label: string, feedback?: {} }] }
If feedback is omitted on an option, the label is sent as plain text user message.
```

#### `chip_select_single`
Single-select chip group.
```
data: { key: string, label?: string, options: [{ label: string, value: string }], selected?: string }
```

#### `chip_select_multi`
Multi-select chip group.
```
data: { key: string, label?: string, options: [{ label: string, value: string }], selected?: [string] }
```

#### `radio_list`
Vertical single-select list with radio buttons. Each option has a label and optional description.
```
data: { key: string, label?: string, options: [{ label: string, description?: string, value: string }], selected?: string }
```

#### `checkbox_list`
Vertical multi-select list with checkboxes. Each option has a label and optional description.
```
data: { key: string, label?: string, options: [{ label: string, description?: string, value: string }], selected?: [string] }
```

#### `input_text_single`
Single text input with submit.
```
data: { key: string, label: string, placeholder?: string, submit_label?: string }
```

#### `input_text_multi`
Multi-line text input with submit.
```
data: { key: string, label: string, placeholder?: string, rows?: number, submit_label?: string }
```

#### `input_email`
Email input with email keyboard.
```
data: { key: string, label: string, placeholder?: string, required?: boolean }
```

#### `input_phone`
Phone input with phone keyboard.
```
data: { key: string, label: string, placeholder?: string, required?: boolean }
```

#### `input_number`
Numeric input.
```
data: { key: string, label: string, placeholder?: string, min?: number, max?: number }
```

#### `input_select`
Dropdown/picker.
```
data: { key: string, label: string, options: [{ label: string, value: string }], selected?: string }
```

#### `input_date`
Date picker.
```
data: { key: string, label: string, value?: string, min_date?: string, max_date?: string }
```

#### `input_time`
Time picker.
```
data: { key: string, label: string, value?: string }
```

#### `input_slider`
Range slider.
```
data: { key: string, label: string, min: number, max: number, value?: number, step?: number }
```

#### `input_rating_stars`
Star rating input (1-5).
```
data: { key: string, label?: string, value?: number }
```

#### `form_group`
Multiple inputs grouped together with a single submit button.
```
data: {
  fields: [
    { type: "<input_type>", key: string, label: string, ...type-specific fields }
  ],
  submit_label: string
}
All field values are collected and sent as params in the feedback.
```

---

### UTILITY

#### `divider`
Visual separator line.
```
data: {}
```

#### `loading`
Loading indicator.
```
data: { message?: string }
```

#### `section_header`
Section title, optionally with a trailing action link.
```
data: { title: string, action_label?: string }
Optional: feedback (for the trailing action)
```

---

## Complete Examples

### Example 1: Quick Status (expanded)

**User:** "Did my payment go through?"

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Yes, your payment was processed successfully!" }
    },
    {
      "type": "badge_success",
      "data": { "text": "Payment confirmed" }
    },
    {
      "type": "text",
      "data": { "text": "$249.99 charged to Visa ending in 4821.\nConfirmation #TXN-88421." }
    },
    {
      "type": "quick_replies",
      "data": {
        "options": [
          { "label": "View receipt" },
          { "label": "Track order" }
        ]
      }
    }
  ]
}
```

### Example 2: Product Recommendations (expanded)

**User:** "Show me running shoes under $150"

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Here are the top-rated running shoes in your budget:" }
    },
    {
      "type": "horizontal_scroll_cards",
      "data": {
        "items": [
          {
            "type": "card_product_vertical",
            "data": {
              "image": "https://cdn.shop.co/nike_pegasus.jpg",
              "title": "Nike Pegasus 41",
              "price": "$129.99",
              "rating": 4.5
            },
            "feedback": {
              "action": "view_product",
              "params": { "id": "nike_pegasus_41" }
            }
          },
          {
            "type": "card_product_vertical",
            "data": {
              "image": "https://cdn.shop.co/brooks_ghost.jpg",
              "title": "Brooks Ghost 16",
              "price": "$139.99",
              "rating": 4.7
            },
            "feedback": {
              "action": "view_product",
              "params": { "id": "brooks_ghost_16" }
            }
          },
          {
            "type": "card_product_vertical",
            "data": {
              "image": "https://cdn.shop.co/asics_nimbus.jpg",
              "title": "ASICS Gel-Nimbus 26",
              "price": "$149.99",
              "rating": 4.3
            },
            "feedback": {
              "action": "view_product",
              "params": { "id": "asics_nimbus_26" }
            }
          }
        ]
      }
    },
    {
      "type": "quick_replies",
      "data": {
        "options": [
          { "label": "Filter by brand" },
          { "label": "Show trail running" },
          { "label": "Sort by rating" }
        ]
      }
    }
  ]
}
```

### Example 3: Browsing Restaurants (expanded → sheet)

**User:** "Find me Italian restaurants nearby"

**AI Response 1 — expanded (showing options):**

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "I found 3 Italian restaurants with great reviews near you:" }
    },
    {
      "type": "card_image_left",
      "data": {
        "image": "https://img.resto.co/lucias.jpg",
        "title": "Lucia's Trattoria",
        "subtitle": "★ 4.6 · $$$ · 0.3 mi"
      },
      "feedback": {
        "action": "select_restaurant",
        "params": { "id": "lucias" }
      }
    },
    {
      "type": "card_image_left",
      "data": {
        "image": "https://img.resto.co/nonnas.jpg",
        "title": "Nonna's Kitchen",
        "subtitle": "★ 4.8 · $$ · 1.1 mi"
      },
      "feedback": {
        "action": "select_restaurant",
        "params": { "id": "nonnas" }
      }
    },
    {
      "type": "card_image_left",
      "data": {
        "image": "https://img.resto.co/vespa.jpg",
        "title": "Vespa Italian Bistro",
        "subtitle": "★ 4.4 · $$ · 0.8 mi"
      },
      "feedback": {
        "action": "select_restaurant",
        "params": { "id": "vespa" }
      }
    }
  ]
}
```

**User taps "Nonna's Kitchen" → feedback sent**

**AI Response 2 — sheet (booking flow):**

```json
{
  "display": "sheet",
  "sheet_title": "Book a Table",
  "steps": [
    {
      "label": "Time",
      "question": "What time works for you?",
      "blocks": [
        {
          "type": "chip_select_single",
          "data": {
            "key": "time",
            "label": "Tonight's availability",
            "options": [
              { "label": "6:00 PM", "value": "18:00" },
              { "label": "7:30 PM", "value": "19:30" },
              { "label": "8:45 PM", "value": "20:45" }
            ]
          }
        },
        {
          "type": "button_primary",
          "data": { "label": "Next" },
          "feedback": { "action": "booking_step", "params": { "restaurant": "nonnas" } }
        }
      ]
    },
    {
      "label": "Party size",
      "question": "How many guests?",
      "blocks": [
        {
          "type": "chip_select_single",
          "data": {
            "key": "party_size",
            "label": "Party size",
            "options": [
              { "label": "1", "value": "1" },
              { "label": "2", "value": "2" },
              { "label": "3-4", "value": "4" },
              { "label": "5+", "value": "5" }
            ]
          }
        },
        {
          "type": "button_primary",
          "data": { "label": "Confirm Reservation" },
          "feedback": { "action": "confirm_booking", "params": { "restaurant": "nonnas" } }
        }
      ]
    }
  ]
}
```

**User selects time + party size + taps Confirm → sheet closes → feedback sent**

**AI Response 3 — expanded (confirmation):**

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "status_banner_success",
      "data": { "text": "Reservation confirmed!" }
    },
    {
      "type": "card_event",
      "data": {
        "title": "Nonna's Kitchen",
        "date": "Tonight",
        "time": "7:30 PM",
        "location": "123 Main St, Downtown"
      }
    },
    {
      "type": "text",
      "data": { "text": "Party of 2. Enjoy your dinner!" }
    },
    {
      "type": "quick_replies",
      "data": {
        "options": [
          { "label": "Get directions" },
          { "label": "View menu" },
          { "label": "Cancel reservation" }
        ]
      }
    }
  ]
}
```

### Example 4: Filing an Issue (sheet with form)

**User:** "I need to report a problem with my delivery"

```json
{
  "display": "sheet",
  "sheet_title": "Report an Issue",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Sorry to hear that. Please describe the issue and we'll get it resolved." }
    },
    {
      "type": "form_group",
      "data": {
        "fields": [
          {
            "type": "input_select",
            "key": "issue_type",
            "label": "What happened?",
            "options": [
              { "label": "Package damaged", "value": "damaged" },
              { "label": "Wrong item received", "value": "wrong_item" },
              { "label": "Package not received", "value": "not_received" },
              { "label": "Item missing from order", "value": "missing_item" }
            ]
          },
          {
            "type": "input_text_multi",
            "key": "description",
            "label": "Additional details (optional)",
            "placeholder": "Describe what happened..."
          }
        ],
        "submit_label": "Submit Report"
      },
      "feedback": {
        "action": "submitted_issue_report",
        "params": { "order_id": "4812" }
      }
    }
  ]
}
```

### Example 5: Simple Conversational (expanded)

**User:** "What's the weather like?"

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Here's the weather for San Francisco:" }
    },
    {
      "type": "card_basic_icon",
      "data": {
        "icon": "partly_cloudy",
        "title": "68°F — Partly Cloudy",
        "subtitle": "High 72° · Low 58° · 5% chance of rain"
      }
    }
  ]
}
```

---

## AI System Prompt Contract

```
You are a helpful assistant. You respond with rich UI when it enhances
the experience, and plain text when it doesn't.

Response format (expanded):
{
  "display": "expanded" | "expanded",
  "blocks": [ ... ]
}

Response format (sheet — multi-step):
{
  "display": "sheet",
  "sheet_title": "...",
  "steps": [
    { "label": "Step name", "question": "Full question text?", "skippable": true, "blocks": [ ... ] }
  ]
}

DISPLAY LEVELS:
  expanded — full-width in chat feed. Quick answers, confirmations, rich cards, media, lists.
  sheet    — bottom sheet overlay. Multi-step surveys, forms, focused input.

Choose the LEAST prominent level that serves the content well.
Use "sheet" only when user input is needed or focused attention is required.

BLOCK FORMAT:
  { "type": "<component>", "data": { ... }, "feedback": { ... } }

FEEDBACK: (on interactive components)
  { "action": "name", "params": { ... } }
  Do NOT set a "label" field. The library computes the display summary automatically
  from the rendered inputs and the step "question" fields.

AVAILABLE COMPONENTS:

Display:
  text(text) · heading(text) · caption(text) · rich_text(text, spans[])
  card_basic(title, subtitle?)
  card_basic_icon(icon, title, subtitle?)
  card_image_top(image, title, subtitle?, caption?)
  card_image_left(image, title, subtitle?)
  card_product_vertical(image, title, price, rating?, review_count?)
  card_product_horizontal(image, title, price, rating?, review_count?)
  card_profile(avatar, name, subtitle?, bio?)
  card_stat(value, label, trend?, trend_up?)
  card_event(title, date, time?, location?, image?)
  card_order_tracking(order_id, status, eta?, tracking_url?, items_summary?)
  card_quote(text, author?, source?)
  card_code(code, language?, title?)
  image_single(src, alt?)
  image_gallery(images[]{src, alt?})
  badge_info(text) | badge_success(text) | badge_warning(text) | badge_error(text)
  link_preview(url, title, description?, image?)
  map_static(latitude, longitude, label?, zoom?)

Lists:
  list_simple(items[]{title, subtitle?})
  list_icon(items[]{icon, title, subtitle?})
  list_avatar(items[]{avatar, title, subtitle?})
  list_numbered(items[]{text})
  list_checklist(items[]{text, checked})
  horizontal_scroll_cards(items[]{type, data, feedback?})

Progress:
  stepper_horizontal(steps[]{label}, current)
  progress_bar(label, progress, max?)
  status_banner_info(text) | status_banner_success(text) | status_banner_warning(text) | status_banner_error(text)

Input:
  button_primary(label, icon?) | button_secondary(label, icon?) | button_ghost(label, icon?) | button_danger(label, icon?)
  button_row_primary_secondary(primary_label, secondary_label)
  button_row_primary_ghost(primary_label, ghost_label)
  quick_replies(options[]{label})
  chip_select_single(key, options[]{label, value}, label?, selected?)
  chip_select_multi(key, options[]{label, value}, label?, selected?)
  radio_list(key, options[]{label, description?, value}, label?, selected?)
  checkbox_list(key, options[]{label, description?, value}, label?, selected?)
  input_text_single(key, label, placeholder?, submit_label?)
  input_text_multi(key, label, placeholder?, rows?, submit_label?)
  input_email(key, label, placeholder?)
  input_phone(key, label, placeholder?)
  input_number(key, label, placeholder?, min?, max?)
  input_select(key, label, options[]{label, value}, selected?)
  input_date(key, label, value?, min_date?, max_date?)
  input_time(key, label, value?)
  input_slider(key, label, min, max, value?, step?)
  input_rating_stars(key, label?, value?)
  form_group(fields[], submit_label)

Utility:
  divider() · loading(message?) · section_header(title, action_label?)

Sheet-only fields (top-level):
  sheet_title: string — title in the sheet header
  sheet_dismissable: boolean — can user swipe to dismiss (default true)
  steps[]: array of steps (use instead of blocks for sheet)
    step.label: string — short label for the stepper indicator
    step.question: string — question recorded in the feedback summary
    step.skippable: boolean — show a Skip button (default false)
    step.blocks[]: blocks for this step

GUIDELINES:
  - Start with text for context, then use components
  - Use quick_replies at the end to suggest next steps
  - Keep it concise: 3-8 blocks for expanded, 3-10 for sheet
  - Use text-only when components add no value
  - Every interactive component MUST have a feedback object
  - Prefer expanded. Use sheet only for focused multi-step input.
```

---

## Token Efficiency

| Response Type                          | Display  | Tokens   | Blocks |
|----------------------------------------|----------|----------|--------|
| Simple text answer                     | expanded | ~30-50   | 1-2    |
| Status + quick replies                 | expanded | ~100-150 | 3-4    |
| Product recommendations (carousel)     | expanded | ~250-350 | 3-4    |
| Restaurant listings (3 cards)          | expanded | ~250-350 | 4-5    |
| Booking flow (form)                    | sheet    | ~200-300 | 5-7    |
| Issue report (form)                    | sheet    | ~150-250 | 3-4    |
| Full multi-step (expanded → sheet)     | mixed    | ~400-600 | 8-12   |

---

## Catalog Growth Phases

### Phase 1 — Chat MVP (~20 components)
text, heading, caption, card_basic, card_basic_icon, card_image_left,
list_simple, list_icon, button_primary, button_secondary, quick_replies,
chip_select_single, input_text_single, form_group, divider,
badge_success, badge_error, status_banner_success, loading

### Phase 2 — Rich Content (+15 components)
card_product_vertical, card_product_horizontal, card_profile, card_event,
card_order_tracking, card_stat, card_image_top, image_single, image_gallery,
horizontal_scroll_cards, stepper_horizontal, progress_bar,
list_avatar, list_checklist, link_preview

### Phase 3 — Advanced Input (+12 components)
chip_select_multi, input_text_multi, input_email, input_phone, input_number,
input_select, input_date, input_time, input_slider, input_rating_stars,
button_ghost, button_danger

### Phase 4+ — Domain-Specific
card_code, card_quote, map_static, rich_text, section_header,
badge_info, badge_warning, status_banner_info, status_banner_warning,
list_numbered, button_row_primary_secondary, button_row_primary_ghost,
+ whatever your domain needs

---

## Open Questions

- [ ] **Streaming**: Can blocks stream in one at a time? (text first, then cards appear)
- [ ] **Sheet from expanded**: Can a card in expanded view open a sheet? (e.g., tap product → sheet with details)
- [ ] **Sheet stacking**: Can a sheet trigger another sheet, or only one at a time?
- [ ] **Block updates**: Can the AI update a previously sent message's blocks? (e.g., mark a step complete)
- [ ] **Component expiration**: Should interactive components disable after use?
- [ ] **Client-side actions**: Some feedback (copy, open URL, call phone) could be handled locally without round-tripping to the AI.
- [ ] **Accessibility**: Auto-derived from component semantics, or explicit a11y labels?
