# AUI Phase 1 — Polls & Feedback Collection

## First Milestone
The AI can send a poll (single question, multi-question, or multi-step)
as AUI JSON. The user sees native Compose UI, selects answers, and
feedback flows back into the conversation.

---

## Component Set (Polls-Focused)

Only build what polls need. Nothing more.

### Display
| Type                  | Purpose in Polls                          |
|-----------------------|-------------------------------------------|
| `text`                | Question text, descriptions, explanations |
| `heading`             | Poll title, section headers               |
| `caption`             | Helper text, "Step 2 of 3", metadata      |

### Input
| Type                  | Purpose in Polls                          |
|-----------------------|-------------------------------------------|
| `chip_select_single`  | Single-choice question (pick one)         |
| `chip_select_multi`   | Multi-choice question (pick many)         |
| `button_primary`      | Submit / Next step                        |
| `button_secondary`    | Back / Skip                               |
| `quick_replies`       | Lightweight yes/no or simple options      |
| `input_rating_stars`  | Star rating feedback (1-5)                |
| `input_text_single`   | Open-ended short answer                   |
| `input_slider`        | Scale questions (1-10, how likely, etc.)  |

### Layout
| Type                  | Purpose in Polls                          |
|-----------------------|-------------------------------------------|
| `divider`             | Separate questions                        |
| `spacer`              | Visual breathing room                     |
| `stepper_horizontal`  | Multi-step progress (Step 1 of 3)         |
| `progress_bar`        | Completion progress                       |

### Status
| Type                  | Purpose in Polls                          |
|-----------------------|-------------------------------------------|
| `badge_success`       | "Submitted" confirmation pill             |
| `status_banner_success` | "Thanks for your feedback!" banner      |

**Total: ~17 components** — tight, focused, shippable.

---

## Example Scenarios

### Scenario 1: Simple Single Question (inline)

AI asks a quick poll right in the chat:

```json
{
  "display": "inline",
  "blocks": [
    {
      "type": "heading",
      "data": { "text": "Quick question" }
    },
    {
      "type": "text",
      "data": { "text": "How would you rate your experience today?" }
    },
    {
      "type": "input_rating_stars",
      "data": { "key": "rating", "label": "Tap to rate" },
      "feedback": {
        "action": "poll_answer",
        "params": { "poll_id": "exp_rating" },
        "label": "Rated {{value}} stars"
      }
    }
  ]
}
```

### Scenario 2: Multi-Choice Survey (expanded)

AI presents a longer poll that breaks out of the bubble:

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Help us improve! A few quick questions:" }
    },
    {
      "type": "heading",
      "data": { "text": "What features do you use most?" }
    },
    {
      "type": "chip_select_multi",
      "data": {
        "key": "features",
        "options": [
          { "label": "Chat", "value": "chat" },
          { "label": "Search", "value": "search" },
          { "label": "Recommendations", "value": "recs" },
          { "label": "Order tracking", "value": "tracking" },
          { "label": "Account settings", "value": "settings" }
        ]
      }
    },
    { "type": "divider", "data": {} },
    {
      "type": "heading",
      "data": { "text": "How likely are you to recommend us?" }
    },
    {
      "type": "input_slider",
      "data": {
        "key": "nps",
        "label": "0 = Not likely, 10 = Very likely",
        "min": 0,
        "max": 10,
        "value": 5,
        "step": 1
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "button_primary",
      "data": { "label": "Submit Feedback" },
      "feedback": {
        "action": "poll_submit",
        "params": { "poll_id": "feature_survey" },
        "label": "Submitted feedback"
      }
    }
  ]
}
```

### Scenario 3: Multi-Step Poll (sheet)

AI opens a focused bottom sheet for a step-by-step survey:

**Step 1:**
```json
{
  "display": "sheet",
  "sheet_title": "Quick Survey",
  "blocks": [
    {
      "type": "stepper_horizontal",
      "data": {
        "steps": [
          { "label": "Experience" },
          { "label": "Features" },
          { "label": "Feedback" }
        ],
        "current": 0
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "heading",
      "data": { "text": "How was your experience?" }
    },
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
    { "type": "spacer", "data": {} },
    {
      "type": "button_primary",
      "data": { "label": "Next" },
      "feedback": {
        "action": "poll_next_step",
        "params": { "poll_id": "onboarding_survey", "step": "1" },
        "label": "Experience: {{experience}}"
      }
    }
  ]
}
```

**Step 2:** (AI responds with step 2 after receiving feedback)
```json
{
  "display": "sheet",
  "sheet_title": "Quick Survey",
  "blocks": [
    {
      "type": "stepper_horizontal",
      "data": {
        "steps": [
          { "label": "Experience" },
          { "label": "Features" },
          { "label": "Feedback" }
        ],
        "current": 1
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "heading",
      "data": { "text": "What would you like to see improved?" }
    },
    {
      "type": "chip_select_multi",
      "data": {
        "key": "improvements",
        "options": [
          { "label": "Speed", "value": "speed" },
          { "label": "Design", "value": "design" },
          { "label": "Features", "value": "features" },
          { "label": "Accuracy", "value": "accuracy" },
          { "label": "Pricing", "value": "pricing" }
        ]
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "button_primary",
      "data": { "label": "Next" },
      "feedback": {
        "action": "poll_next_step",
        "params": { "poll_id": "onboarding_survey", "step": "2" },
        "label": "Wants improvement in: {{improvements}}"
      }
    }
  ]
}
```

**Step 3:**
```json
{
  "display": "sheet",
  "sheet_title": "Quick Survey",
  "blocks": [
    {
      "type": "stepper_horizontal",
      "data": {
        "steps": [
          { "label": "Experience" },
          { "label": "Features" },
          { "label": "Feedback" }
        ],
        "current": 2
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "heading",
      "data": { "text": "Anything else you'd like to tell us?" }
    },
    {
      "type": "input_text_single",
      "data": {
        "key": "open_feedback",
        "label": "Your feedback",
        "placeholder": "Optional — type anything here..."
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "button_primary",
      "data": { "label": "Submit" },
      "feedback": {
        "action": "poll_complete",
        "params": { "poll_id": "onboarding_survey" },
        "label": "Completed the survey"
      }
    }
  ]
}
```

**Confirmation (after submit — back to inline):**
```json
{
  "display": "inline",
  "blocks": [
    {
      "type": "status_banner_success",
      "data": { "text": "Survey complete!" }
    },
    {
      "type": "text",
      "data": { "text": "Thanks for your feedback. This helps us make the app better for you." }
    },
    {
      "type": "badge_success",
      "data": { "text": "3 of 3 completed" }
    }
  ]
}
```

### Scenario 4: Quick Yes/No (inline)

Simplest possible poll — uses quick_replies:

```json
{
  "display": "inline",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "Was this response helpful?" }
    },
    {
      "type": "quick_replies",
      "data": {
        "options": [
          {
            "label": "👍 Yes",
            "feedback": {
              "action": "poll_answer",
              "params": { "poll_id": "helpful", "value": "yes" },
              "label": "Yes, this was helpful"
            }
          },
          {
            "label": "👎 No",
            "feedback": {
              "action": "poll_answer",
              "params": { "poll_id": "helpful", "value": "no" },
              "label": "No, this wasn't helpful"
            }
          }
        ]
      }
    }
  ]
}
```

---

## Claude Code Sessions (Updated)

### Session 1: Gradle Dependencies + Data Models

Since the repo structure already exists, start with:

```
Read CLAUDE.md, spec/aui-spec-v1.md, and docs/architecture.md.

The Gradle multi-module project structure already exists. 
Add the necessary dependencies to each module's build.gradle.kts:

- aui-core: Kotlinx Serialization (JSON)
- aui-compose: depends on aui-core, Jetpack Compose (BOM), Compose Material 3, Coil Compose
- demo: depends on aui-compose, Ktor Client (Android + Content Negotiation + JSON), Kotlinx Serialization

Also set up:
- gradle/libs.versions.toml with a version catalog
- Kotlinx Serialization Gradle plugin in the root build.gradle.kts
- Make sure everything compiles with ./gradlew build

Don't create any source files yet, just get the build working.
```

Then in the same session:

```
Now create the data models in aui-core for our polls-focused Phase 1.

We need these component types:
- text, heading, caption (display)
- chip_select_single, chip_select_multi (selection input)
- button_primary, button_secondary (actions)
- quick_replies (lightweight options)
- input_rating_stars, input_text_single, input_slider (other input)
- divider, spacer (layout)
- stepper_horizontal, progress_bar (progress)
- badge_success, status_banner_success (status)

Create:
1. AuiResponse (display, blocks, sheetTitle?, sheetDismissable?)
2. AuiDisplay enum (INLINE, EXPANDED, SHEET)
3. AuiFeedback (action, params map, label?)
4. AuiBlock sealed class with a case for each component type above
5. Data classes for each component (in com.bennyjon.aui.core.model.data)
6. AuiParser using Kotlinx Serialization with polymorphic deserialization on "type"
7. Unknown type handling → AuiBlock.Unknown

Use @SerialName annotations for snake_case JSON field mapping.
Configure the Json instance with ignoreUnknownKeys = true and isLenient = true.

Write unit tests that parse the sample JSON files in spec/examples/.
Run ./gradlew :aui-core:test and make everything pass.
```

### Session 2: Sample JSON Files

```
Read CLAUDE.md.

Create these sample JSON files in spec/examples/ for our polls use case:

1. poll-inline-rating.json — inline star rating question
2. poll-inline-yes-no.json — inline quick_replies yes/no
3. poll-expanded-survey.json — expanded multi-question with chips + slider
4. poll-sheet-step1.json — sheet with stepper + single-choice chips
5. poll-sheet-step2.json — sheet with stepper + multi-choice chips
6. poll-sheet-step3.json — sheet with stepper + text input
7. poll-confirmation.json — inline success banner + badge

I've included example JSON in .planning/phase1-polls.md for reference. 
Follow those patterns exactly.

After creating the files, add parser tests that load each one. 
Run the tests.
```

### Session 3: Theme + Text/Layout Components

```
Read CLAUDE.md.

Create the AUI theme system in aui-compose:
- AuiTheme data class with colors, typography, spacing, shapes
- AuiColors, AuiTypography, AuiSpacing, AuiShapes data classes
- AuiTheme.Default companion with Material-like defaults
- AuiTheme.fromMaterialTheme() composable adapter
- Provide via CompositionLocal

Then create these components (each in its own file with @Preview):
- AuiText (text)
- AuiHeading (heading)
- AuiCaption (caption)
- AuiDivider (divider)
- AuiSpacer (spacer)

Every component uses AuiTheme only — no hardcoded values.
Each gets a @Preview composable.
```

### Session 4: Input Components (the core of polls)

```
Read CLAUDE.md. Continue building aui-compose components.

These are the critical components for polls. Take care with each one.

Create:
- AuiChipSelectSingle (chip_select_single):
  - Horizontal flow of chips (use FlowRow)
  - Single selection: tapping one deselects the previous
  - Selected chip gets primary color fill
  - Stores selected value in local state
  
- AuiChipSelectMulti (chip_select_multi):
  - Same layout as single, but allows multiple selection
  - Toggle on/off per chip
  - Stores set of selected values

- AuiQuickReplies (quick_replies):
  - Horizontal scrollable row of outlined chips
  - Each option triggers its own feedback immediately on tap
  - After tap, the tapped option gets highlighted briefly

- AuiInputRatingStars (input_rating_stars):
  - Row of 5 star icons
  - Tap to select rating (1-5)
  - Filled stars up to selection, outlined after

- AuiInputTextSingle (input_text_single):
  - Text field with label and placeholder
  - Optional submit button/icon

- AuiInputSlider (input_slider):
  - Material Slider with min/max/step
  - Show current value label

Each component must accept Modifier and use AuiTheme.
Each gets a @Preview.
```

### Session 5: Buttons + Progress + Status

```
Read CLAUDE.md.

Create:
- AuiButtonPrimary (button_primary) — filled button, primary color
- AuiButtonSecondary (button_secondary) — outlined button
- AuiStepperHorizontal (stepper_horizontal) — step indicators with labels,
  completed/current/upcoming states visually distinct
- AuiProgressBar (progress_bar) — linear progress with label
- AuiBadgeSuccess (badge_success) — small success-colored pill
- AuiStatusBannerSuccess (status_banner_success) — full-width success banner

Then create the BlockRenderer (internal) that maps AuiBlock sealed class
cases to composables using a `when` expression.

Then create the public AuiRenderer composable that:
- Accepts AuiResponse + AuiTheme + onFeedback
- Wraps content in theme provider
- Passes blocks to BlockRenderer

Test by creating a @Preview that parses one of the sample JSONs 
and passes it to AuiRenderer.
```

### Session 6: Display Levels

```
Read CLAUDE.md. This is the critical session for display routing.

Create the display system:

DisplayRouter composable that takes AuiResponse and routes:
- INLINE → all blocks in a Column (the caller wraps in a bubble)
- EXPANDED → split blocks: leading text/heading/caption blocks go in a 
  "bubble" section (returned separately), remaining blocks go full-width
- SHEET → split blocks same way: text goes in bubble,
  remaining go in Material 3 ModalBottomSheet

The split logic: scan blocks from start. While type is text/heading/caption,
accumulate in "bubble" list. First non-text block and everything after → 
"content" list.

For sheets:
- Use sheet_title from AuiResponse in the sheet header
- sheet_dismissable controls drag-to-dismiss
- When sheet closes via submission, it auto-dismisses

Update AuiRenderer to use DisplayRouter.

Test with the poll sample JSONs — inline should render in a column,
expanded should split text from chips, sheet should open a bottom sheet.
```

### Session 7: Demo Chat Screen + Full Loop

```
Read CLAUDE.md. Build the demo app.

Create a chat screen that demonstrates the full poll flow:

- ChatMessage sealed class: UserText, UserFeedback, AiResponse
- ChatViewModel:
  - Holds list of ChatMessage
  - Pre-loads hardcoded poll JSON responses in a sequence
  - onFeedback: adds UserFeedback message, loads next response in sequence
  - The sequence: 
    1. Start with poll-expanded-survey.json
    2. On submit → poll-sheet-step1.json
    3. On next → poll-sheet-step2.json
    4. On next → poll-sheet-step3.json
    5. On submit → poll-confirmation.json
    
- ChatScreen composable:
  - LazyColumn with user bubbles (right) and AI responses (left)
  - AI responses rendered via AuiRenderer with proper display levels
  - Text input bar at bottom
  
- MainActivity launches ChatScreen

The key test: run the app, see the expanded survey, interact with chips 
and slider, submit, see the sheet open for step 1, go through all 3 steps,
see the confirmation inline. Every interaction should create a user
feedback message in the chat.

Make it compile and run.
```

---

## What You'll Have After These 7 Sessions

A working Android app that:
1. Shows an AI-generated poll as native Compose UI
2. Lets users select options, rate with stars, type text, slide scales
3. Supports multi-step flows with a progress stepper in a bottom sheet
4. Every interaction feeds back as a chat message
5. All 3 display levels working (inline, expanded, sheet)
6. Fully themed — swap colors and everything updates
7. Clean library architecture ready for open source
