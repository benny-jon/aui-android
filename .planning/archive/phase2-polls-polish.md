# AUI Phase 2 — Polls Polish

## Goals

1. **Fix expanded poll feedback** — all input values must be captured in the response, not just the last one
2. **Fix skip-all feedback** — skipping every step in a sheet must produce a human-readable message, never a raw action ID
3. **Add `radio_list` and `checkbox_list` components** — proper survey-style single/multi selection with descriptions

---

## Bug Fix 1: Expanded Poll Missing Input Values

### The Problem

In an expanded poll with multiple questions (e.g., chip_select_multi + slider), only the last input's value appears in the feedback bubble. Selections from earlier questions are silently lost.

**Current behavior:**
```
User selects: Recommendations, Account settings (chip_select_multi)
User slides to: 8 (input_slider)  
User taps: Submit Feedback

Feedback bubble shows:
  "How likely are you to recommend us?
   5"

→ First question's answer is MISSING
```

**Expected behavior:**
```
Feedback bubble shows:
  "What features do you use most?
   Recommendations, Account settings

   How likely are you to recommend us?
   8"

→ ALL input values captured
```

### The Fix

The state collector in expanded/inline mode must gather values from ALL input components in the blocks array, not just the one closest to the submit button.

**Implementation requirements:**

1. **Every input component** (`chip_select_single`, `chip_select_multi`, `input_slider`, `input_rating_stars`, `input_text_single`, `quick_replies`, and the new `radio_list`, `checkbox_list`) must report its current value to the parent state collector whenever it changes.

2. **The state collector** maintains a `Map<String, InputEntry>` where:
   ```kotlin
   data class InputEntry(
       val key: String,           // the input's "key" field
       val question: String?,     // the nearest preceding heading/text block
       val displayValue: String   // human-readable selected value(s)
   )
   ```

3. **Question association:** For each input component, walk backward through the blocks to find the nearest `heading` or `text` block — that becomes the `question` for this input. This pairs inputs with their questions automatically without the AI needing to specify it.

4. **When the submit button is tapped**, the state collector builds `formattedEntries` from ALL collected `InputEntry` values:
   ```
   What features do you use most?
   Recommendations, Account settings

   How likely are you to recommend us?
   8
   ```

5. **The `feedback.params`** on the submit button gets ALL input values merged in:
   ```json
   {
     "action": "poll_submit",
     "params": {
       "poll_id": "feature_survey",
       "features": ["recs", "settings"],
       "nps": "8"
     }
   }
   ```

6. **Unanswered questions** (input exists but user made no selection) should either:
   - Be omitted from `formattedEntries` entirely (clean look), OR
   - Show as "Skipped" or "No answer"
   
   Recommend: **omit unanswered** from the display text, but still include the key with a null/empty value in `feedback.params` so the AI knows the question was presented but not answered.

### Test Cases

| Scenario | Expected formattedEntries |
|----------|--------------------------|
| Answer both questions | Both Q&A pairs shown |
| Answer only first question | Only first Q&A shown |
| Answer only second question | Only second Q&A shown |
| Answer neither, tap submit | "Feedback submitted" (fallback text) |
| Single question poll (inline) | One Q&A pair |

---

## Bug Fix 2: Sheet Skip-All Shows Raw Action ID

### The Problem

When a user skips every step in a multi-step sheet, the feedback bubble shows the raw `feedback.action` string (e.g., `poll_complete`) instead of a meaningful message.

**Current behavior:**
```
User opens sheet survey
User taps Skip on step 1
User taps Skip on step 2  
User taps Skip on step 3 (or Submit with nothing filled)

Feedback bubble shows:
  "poll_complete"

→ Raw action ID, not human-readable
```

**Expected behavior:**
```
Feedback bubble shows:
  "Survey skipped"

→ Or if some steps answered and some skipped:
  "How was your experience?
   Good
   
   (2 questions skipped)"
```

### The Fix

The sheet's consolidated feedback builder needs fallback handling:

1. **If `formattedEntries` is empty** (no steps were answered), use a fallback display text:
   - If all steps were skipped: **"Survey skipped"**
   - If no inputs had values but submit was tapped: **"Survey submitted"**
   
2. **If `formattedEntries` is partially filled** (some answered, some skipped):
   - Show the answered Q&A pairs normally
   - Append a note at the end: **"(N questions skipped)"** where N is the count of skipped steps

3. **The fallback text should be configurable** at the `AuiRenderer` level:
   ```kotlin
   AuiRenderer(
       response = response,
       theme = theme,
       onFeedback = { ... },
       emptyFeedbackText = "Survey skipped",  // default
       partialSkipFormat = { count -> "($count questions skipped)" }  // default
   )
   ```
   
   But have sensible defaults so most consumers don't need to configure this.

4. **`feedback.params` should still include all step data** even for skipped steps — so the AI knows the full picture:
   ```json
   {
     "action": "poll_complete",
     "params": {
       "poll_id": "survey",
       "experience": null,
       "improvements": null,
       "open_feedback": null,
       "steps_skipped": 3,
       "steps_total": 3
     }
   }
   ```

### Test Cases

| Scenario | Feedback bubble text | params |
|----------|---------------------|--------|
| All steps answered | Full Q&A pairs | All values filled |
| All steps skipped | "Survey skipped" | All null + steps_skipped=3 |
| Steps 1 answered, 2-3 skipped | Step 1 Q&A + "(2 questions skipped)" | Step 1 filled, 2-3 null |
| Steps 1-2 answered, 3 skipped | Steps 1-2 Q&A + "(1 question skipped)" | Steps 1-2 filled, 3 null |
| Single step, skipped | "Survey skipped" | value null |
| Single step, submitted empty | "Survey submitted" | value empty |

---

## New Component: `radio_list`

Vertical list of options with radio buttons. Single selection.
Each option has a label (required) and description (optional).

### Behavior
- Tapping an option selects it and deselects the previous
- Selected row: filled radio circle, subtle primary tint background
- Unselected row: empty radio circle, transparent background
- The entire row is tappable (not just the radio circle)
- Thin divider between rows

### Data Contract
```
data: {
  key: string,
  label?: string,
  options: [
    {
      label: string,
      description?: string,
      value: string
    }
  ],
  selected?: string
}
```

### Visual
```
How satisfied are you?
┌──────────────────────────────────────┐
│  ◉  Very satisfied                   │  ← selected (tinted bg)
│     I had a great experience overall │
├──────────────────────────────────────┤
│  ○  Somewhat satisfied               │
│     It was okay but could be better  │
├──────────────────────────────────────┤
│  ○  Not satisfied                    │
│     I had issues that need addressed │
└──────────────────────────────────────┘
```

### JSON Example
```json
{
  "type": "radio_list",
  "data": {
    "key": "satisfaction",
    "label": "How satisfied are you?",
    "options": [
      {
        "label": "Very satisfied",
        "description": "I had a great experience overall",
        "value": "very_satisfied"
      },
      {
        "label": "Somewhat satisfied",
        "description": "It was okay but could be better",
        "value": "somewhat_satisfied"
      },
      {
        "label": "Neutral",
        "value": "neutral"
      },
      {
        "label": "Not satisfied",
        "description": "I had issues that need to be addressed",
        "value": "not_satisfied"
      }
    ]
  }
}
```

### Display Value for formattedEntries
The selected option's `label` text. Example: `"Very satisfied"`

---

## New Component: `checkbox_list`

Vertical list of options with checkboxes. Multi selection.
Each option has a label (required) and description (optional).

### Behavior
- Tapping an option toggles it on/off independently
- Checked row: filled checkbox, subtle primary tint background
- Unchecked row: empty checkbox, transparent background
- The entire row is tappable
- Multiple can be selected simultaneously
- Thin divider between rows

### Data Contract
```
data: {
  key: string,
  label?: string,
  options: [
    {
      label: string,
      description?: string,
      value: string
    }
  ],
  selected?: [string]
}
```

### Visual
```
What areas need improvement?
┌──────────────────────────────────────┐
│  ☑  Speed and performance            │  ← checked (tinted bg)
│     App feels slow in some areas     │
├──────────────────────────────────────┤
│  ☑  Design and usability             │  ← checked (tinted bg)
│     Some screens are hard to navigate│
├──────────────────────────────────────┤
│  ☐  Feature set                      │
│     Missing features I need          │
├──────────────────────────────────────┤
│  ☐  Pricing                          │
│     Current pricing doesn't feel fair│
└──────────────────────────────────────┘
```

### JSON Example
```json
{
  "type": "checkbox_list",
  "data": {
    "key": "improvements",
    "label": "What areas need improvement?",
    "options": [
      {
        "label": "Speed and performance",
        "description": "App feels slow in some areas",
        "value": "speed"
      },
      {
        "label": "Design and usability",
        "description": "Some screens are hard to navigate",
        "value": "design"
      },
      {
        "label": "Feature set",
        "description": "Missing features I need",
        "value": "features"
      },
      {
        "label": "Pricing",
        "description": "Current pricing doesn't feel fair",
        "value": "pricing"
      }
    ]
  }
}
```

### Display Value for formattedEntries
Comma-separated labels of all checked options. Example: `"Speed and performance, Design and usability"`

---

## Compose Implementation Notes

### Shared SelectionRow

Both components share a row layout — extract an internal composable:

```kotlin
@Composable
internal fun SelectionRow(
    selected: Boolean,
    label: String,
    description: String?,
    indicator: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Renders:
- Full row clickable with ripple
- Selected: subtle `AuiTheme.colors.primary.copy(alpha = 0.08f)` background
- Indicator (RadioButton or Checkbox) on the left
- Label in `AuiTheme.typography.body`
- Description (if present) in `AuiTheme.typography.caption` with `AuiTheme.colors.muted`

### State Reporting

Both components must plug into the same state collection system as chip_select and other inputs. They report via a callback:

```kotlin
// For radio_list
onValueChanged: (key: String, value: String?, displayValue: String?) -> Unit

// For checkbox_list  
onValueChanged: (key: String, values: List<String>, displayValue: String?) -> Unit
```

Where `displayValue` is the human-readable text for formattedEntries (the label, not the value).

---

## Updated Sample JSON Files

### spec/examples/poll-expanded-survey-v2.json

Uses radio_list + checkbox_list for a richer survey:

```json
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "We'd love your feedback! Just a few quick questions:" }
    },
    {
      "type": "radio_list",
      "data": {
        "key": "satisfaction",
        "label": "How satisfied are you with our service?",
        "options": [
          {
            "label": "Very satisfied",
            "description": "Everything works great, I'm happy",
            "value": "very_satisfied"
          },
          {
            "label": "Somewhat satisfied",
            "description": "It's good but there's room for improvement",
            "value": "somewhat_satisfied"
          },
          {
            "label": "Neutral",
            "value": "neutral"
          },
          {
            "label": "Not satisfied",
            "description": "I've had significant issues",
            "value": "not_satisfied"
          }
        ]
      }
    },
    { "type": "divider", "data": {} },
    {
      "type": "checkbox_list",
      "data": {
        "key": "used_features",
        "label": "Which features have you used? (select all that apply)",
        "options": [
          {
            "label": "Chat assistant",
            "description": "Ask questions and get help",
            "value": "chat"
          },
          {
            "label": "Product search",
            "description": "Find and compare products",
            "value": "search"
          },
          {
            "label": "Order tracking",
            "description": "Track deliveries and returns",
            "value": "tracking"
          },
          {
            "label": "Recommendations",
            "description": "Personalized suggestions",
            "value": "recs"
          }
        ]
      }
    },
    { "type": "spacer", "data": {} },
    {
      "type": "button_primary",
      "data": { "label": "Submit Feedback" },
      "feedback": {
        "action": "poll_submit",
        "params": { "poll_id": "feature_survey_v2" }
      }
    }
  ]
}
```

**Expected feedback when both answered:**
```
How satisfied are you with our service?
Somewhat satisfied

Which features have you used?
Chat assistant, Order tracking
```

**Expected feedback when only first answered:**
```
How satisfied are you with our service?
Somewhat satisfied
```

**Expected feedback when neither answered:**
```
Feedback submitted
```

### spec/examples/poll-sheet-radio-v2.json

Multi-step sheet using radio_list:

```json
{
  "display": "sheet",
  "sheet_title": "Quick Survey",
  "steps": [
    {
      "label": "Experience",
      "question": "How was your overall experience?",
      "skippable": true,
      "blocks": [
        {
          "type": "radio_list",
          "data": {
            "key": "experience",
            "options": [
              {
                "label": "Excellent",
                "description": "Exceeded my expectations in every way",
                "value": "excellent"
              },
              {
                "label": "Good",
                "description": "Met my expectations, worked well",
                "value": "good"
              },
              {
                "label": "Average",
                "description": "Nothing special, gets the job done",
                "value": "average"
              },
              {
                "label": "Below average",
                "description": "Had some frustrating moments",
                "value": "below_average"
              },
              {
                "label": "Poor",
                "description": "Significantly below what I expected",
                "value": "poor"
              }
            ]
          }
        }
      ]
    },
    {
      "label": "Improvements",
      "question": "What should we focus on improving?",
      "skippable": true,
      "blocks": [
        {
          "type": "checkbox_list",
          "data": {
            "key": "improvements",
            "options": [
              {
                "label": "Response speed",
                "description": "Faster answers and loading times",
                "value": "speed"
              },
              {
                "label": "Answer accuracy",
                "description": "More precise and reliable responses",
                "value": "accuracy"
              },
              {
                "label": "Visual design",
                "description": "Cleaner, more modern interface",
                "value": "design"
              },
              {
                "label": "More features",
                "description": "Additional capabilities and tools",
                "value": "features"
              }
            ]
          }
        }
      ]
    },
    {
      "label": "Comments",
      "question": "Anything else you'd like to tell us?",
      "skippable": true,
      "blocks": [
        {
          "type": "input_text_single",
          "data": {
            "key": "comments",
            "label": "Your feedback",
            "placeholder": "Optional — type anything here..."
          }
        }
      ]
    }
  ]
}
```

**Expected feedback when all answered:**
```
How was your overall experience?
Good

What should we focus on improving?
Response speed, Visual design

Anything else you'd like to tell us?
Love the app, keep it up!
```

**Expected feedback when all skipped:**
```
Survey skipped
```

**Expected feedback when step 1 answered, 2-3 skipped:**
```
How was your overall experience?
Good

(2 questions skipped)
```

---

## Claude Code Session Prompts

### Session A: Fix the feedback bugs

```
Read CLAUDE.md and .planning/phase2-polls-polish.md.

There are two bugs to fix:

BUG 1 — Expanded polls don't capture all input values.
In an expanded poll with multiple inputs (e.g., chip_select_multi + slider), 
only the last input's value appears in the feedback. The state collector 
needs to gather values from ALL input components and associate each with 
its nearest preceding heading/text block as the question.

BUG 2 — Skipping all steps in a sheet shows the raw action ID.
When all steps are skipped, formattedEntries is empty and the feedback 
bubble falls through to showing the action string. We need fallback text:
- All skipped → "Survey skipped"  
- Some answered, some skipped → answered Q&A + "(N questions skipped)"
- Submitted with no input → "Survey submitted"

Also add steps_skipped and steps_total to feedback.params for sheets.

Fix both bugs. Write tests covering the scenarios listed in the phase2 doc.
Run tests.
```

### Session B: Add radio_list and checkbox_list

```
Read CLAUDE.md and .planning/phase2-polls-polish.md.

Add two new components: radio_list and checkbox_list.
The phase2 doc has the full spec, data contracts, JSON examples, 
and Compose implementation notes.

1. Add data models to aui-core:
   - RadioListData, RadioOption
   - CheckboxListData, CheckboxOption  
   - AuiBlock.RadioList and AuiBlock.CheckboxList sealed class cases
   - Register in polymorphic serializer

2. Create sample JSON files:
   - spec/examples/poll-expanded-survey-v2.json
   - spec/examples/poll-sheet-radio-v2.json
   
   Write parser tests. Run ./gradlew :aui-core:test.

3. Create internal SelectionRow composable that both components share.
   Renders: indicator + label + optional description, full row tappable,
   selected state background tint using AuiTheme.colors.primary at 8% alpha.

4. Create AuiRadioList with @Preview.
   Must report value changes to the state collector so formattedEntries 
   captures the selected label.

5. Create AuiCheckboxList with @Preview.
   Must report value changes. Display value = comma-separated labels.

6. Register both in BlockRenderer.

7. Update demo to use v2 sample JSONs.

Build, run, verify both components appear correctly and their values 
show up properly in the feedback bubble.
```

---

## Spec Update Needed

After implementation, update `spec/aui-spec-v1.md` to add:

Under **COLLECTING USER INPUT**, add:

```
#### `radio_list`
Vertical single-select list with radio buttons. Options have label + optional description.
data: { key: string, label?: string, options: [{ label: string, description?: string, value: string }], selected?: string }

#### `checkbox_list`
Vertical multi-select list with checkboxes. Options have label + optional description.
data: { key: string, label?: string, options: [{ label: string, description?: string, value: string }], selected?: [string] }
```

Under the **AI System Prompt Contract**, add to the Input section:
```
  radio_list(key, options[]{label, description?, value}, label?, selected?)
  checkbox_list(key, options[]{label, description?, value}, label?, selected?)
```

---

## Complete Input Component Set After Phase 2

| Need                         | Component              | Status |
|------------------------------|------------------------|--------|
| Quick yes/no                 | quick_replies          | ✅ Done |
| Pick one (compact)           | chip_select_single     | ✅ Done |
| Pick many (compact)          | chip_select_multi      | ✅ Done |
| Pick one (with description)  | radio_list             | 🆕 New |
| Pick many (with description) | checkbox_list          | 🆕 New |
| Rate 1-5 stars               | input_rating_stars     | ✅ Done |
| Scale / range                | input_slider           | ✅ Done |
| Open-ended text              | input_text_single      | ✅ Done |
| Progress through steps       | stepper_horizontal     | ✅ Done |
| Submit action                | button_primary         | ✅ Done |

After this, the poll/survey system is complete. Every standard question 
type is covered, feedback is reliable, and skip flows are handled gracefully.
