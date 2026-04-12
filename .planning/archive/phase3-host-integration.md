# AUI Phase 3 — Clean Library Boundary

## The Principle

The library is a **renderer with a callback**. It does not manage chat history,
conversation state, networking, or message models. Those are the host app's domain.

The integration surface is two touchpoints:
1. **Host app → Library:** Pass AUI JSON to render
2. **Library → Host app:** Callback with structured feedback when user interacts

```kotlin
AuiRenderer(
    json = auiJsonExtractedFromAiResponse,
    theme = AuiTheme.fromMaterialTheme(),
    onFeedback = { feedback ->
        // feedback.action: String
        // feedback.params: Map<String, Any?>
        // feedback.formattedEntries: String? (human-readable summary)
        // feedback.entries: List<FeedbackEntry>? (structured Q&A pairs)
        
        // Host app decides what to do:
        // - Save to Room
        // - Send to AI backend
        // - Show as a user message
        // - Navigate somewhere
        // - Whatever
    }
)
```

That's the entire public API for rendering.

---

## What Changes From Phase 2

### Delete
- `AuiChatManager` — not the library's job
- `AuiChatMessage` — not the library's job
- `AuiSheetState` as a public class — sheet state is internal to the renderer

### Keep
- `AuiRenderer` — the main composable (takes JSON or AuiResponse)
- `AuiTheme` — theming system
- `AuiFeedback` — the callback data object
- All components — unchanged
- Sheet rendering — but handled internally by AuiRenderer, not exposed

### Add/Clarify
- `AuiCatalogPrompt` — a helper that generates the schema text for the AI system prompt
- Clear documentation on how host apps integrate
- The demo app shows a realistic integration pattern without forcing it

---

## AuiRenderer — The Complete Public API

```kotlin
/**
 * Renders an AUI response as native Compose UI.
 *
 * For inline/expanded: renders components directly. Place this in your
 * chat message list wherever AI responses go.
 *
 * For sheet: automatically opens a bottom sheet overlay.
 * The sheet handles its own step navigation and closes on submit/dismiss.
 *
 * @param json Raw AUI JSON string from the AI response
 * @param theme Your app's theme. Use AuiTheme.fromMaterialTheme() for auto-mapping.
 * @param onFeedback Called when the user submits, taps, or interacts with any component.
 *   For sheets: called once on submit/dismiss with all collected values.
 *   For inline/expanded: called when the user taps a component with feedback.
 * @param onParseError Called if the JSON is malformed. Optional.
 * @param onUnknownBlock Called for component types not in the catalog. Optional.
 */
@Composable
fun AuiRenderer(
    json: String,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
    onParseError: ((String) -> Unit)? = null,
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null
)

/** Pre-parsed variant */
@Composable
fun AuiRenderer(
    response: AuiResponse,
    modifier: Modifier = Modifier,
    theme: AuiTheme = AuiTheme.Default,
    onFeedback: (AuiFeedback) -> Unit = {},
    onUnknownBlock: ((AuiBlock.Unknown) -> Unit)? = null
)
```

### Sheet Behavior (internal to AuiRenderer)

When `AuiRenderer` receives a response with `display: "sheet"`:
- It renders nothing in the inline layout (the composable emits no visible content in the chat)
- It opens a `ModalBottomSheet` as an overlay
- The sheet handles step navigation internally
- On submit: calls `onFeedback` with the consolidated feedback, then closes the sheet
- On dismiss: calls `onFeedback` with `action = "sheet_dismissed"`, then closes the sheet
- The sheet is gone. It cannot be retaken.

**Important:** The host app should remove or replace the AUI response from its
state after `onFeedback` is called for a sheet. Otherwise, if the user scrolls
back, `AuiRenderer` would try to open the sheet again.

Simple pattern for host apps:

```kotlin
// In your message model
data class AiMessage(
    val text: String?,
    val auiJson: String?,       // AUI response, null after sheet submission
    val auiConsumed: Boolean = false  // flag to prevent re-rendering sheets
)
```

Or even simpler — when `onFeedback` fires for a sheet, just set `auiJson = null`
on that message. The text part stays, the AUI part is gone.

---

## AuiFeedback — What the Host App Receives

```kotlin
data class AuiFeedback(
    /** Machine-readable action name (e.g., "poll_submit", "sheet_dismissed") */
    val action: String,
    
    /** Structured key-value data from the interaction */
    val params: Map<String, Any?>,
    
    /** 
     * Human-readable summary of the interaction.
     * For surveys, this is the question-answer pairs formatted for display.
     * For button taps, this is typically null (use the button label instead).
     * 
     * Example:
     *   "How was your experience?\nGood\n\nWhat should we improve?\nSpeed, Design"
     */
    val formattedEntries: String? = null,
    
    /**
     * Structured question-answer pairs. 
     * Host app can use these to build its own display format.
     */
    val entries: List<FeedbackEntry>? = null,
    
    /**
     * For sheets: number of steps skipped / total steps.
     * null for non-sheet interactions.
     */
    val stepsSkipped: Int? = null,
    val stepsTotal: Int? = null
)

data class FeedbackEntry(
    val question: String,
    val answer: String
)
```

The host app gets both `formattedEntries` (ready-to-display string) and
`entries` (structured data). It can use either:

```kotlin
// Option A: Use the pre-formatted string directly
userMessageText = feedback.formattedEntries ?: "Submitted"

// Option B: Format it yourself (for localization, custom styling, etc.)
userMessageText = feedback.entries?.joinToString("\n\n") { entry ->
    "${getString(R.string.question_prefix)} ${entry.question}\n${getString(R.string.answer_prefix)} ${entry.answer}"
} ?: getString(R.string.submitted)
```

This solves the localization issue — `formattedEntries` is language-agnostic
(no Q:/A: prefixes), but if the host app wants prefixes in their language,
they can build it from `entries`.

---

## AuiCatalogPrompt — AI Schema Helper

A convenience utility that generates the text to include in the AI's system prompt.
This tells the AI what components are available and how to format responses.

```kotlin
object AuiCatalogPrompt {
    /**
     * Returns the full catalog prompt text that should be included 
     * in the AI's system prompt.
     *
     * @param availableActions Optional list of action IDs the host app supports.
     *   If provided, the prompt tells the AI which actions are valid.
     */
    fun generate(
        availableActions: List<String>? = null
    ): String
}
```

Usage in host app:

```kotlin
val systemPrompt = """
    You are a helpful assistant for our shopping app.
    
    ${AuiCatalogPrompt.generate(
        availableActions = listOf("navigate", "add_to_cart", "open_url")
    )}
    
    Additional app-specific instructions here...
"""
```

This way the host app doesn't have to maintain the AUI schema manually.
When the library adds new components, the prompt updates automatically
on the next library version bump.

---

## Integration Guide — How Host Apps Use AUI

### Step 1: Add dependency

```kotlin
implementation("com.bennyjon.aui:aui-compose:0.1.0")
```

### Step 2: Include AUI schema in your AI system prompt

```kotlin
val systemPrompt = buildString {
    append("You are a helpful assistant.\n\n")
    append(AuiCatalogPrompt.generate())
}
```

### Step 3: Extract AUI JSON from AI response

The AI response may contain both text and AUI JSON. The host app decides
how to structure this. Common patterns:

**Pattern A: AI returns JSON with a text field**
```json
{
  "text": "Help us improve!",
  "aui": { "display": "sheet", "steps": [...] }
}
```

**Pattern B: AI uses tool calling / structured output**
The AI returns AUI as a tool result or structured output block.

**Pattern C: AI returns pure AUI JSON**
The entire response is AUI JSON. No separate text.

The library doesn't care which pattern you use. It just needs the
AUI JSON string or parsed `AuiResponse`.

### Step 4: Render in your chat UI

```kotlin
// In your message composable, wherever AI responses go:
message.auiJson?.let { json ->
    AuiRenderer(
        json = json,
        theme = AuiTheme.fromMaterialTheme(),
        onFeedback = { feedback ->
            // Handle it however your app needs
            viewModel.onAuiFeedback(message.id, feedback)
        }
    )
}
```

### Step 5: Handle feedback

```kotlin
// In your ViewModel or wherever
fun onAuiFeedback(messageId: String, feedback: AuiFeedback) {
    // 1. If it was a sheet, mark the AUI as consumed so it doesn't re-render
    if (feedback.stepsTotal != null) {
        markAuiConsumed(messageId)
    }
    
    // 2. Add user's answer to your chat history however you do it
    val userMessage = UserMessage(
        text = feedback.formattedEntries ?: feedback.action,
        metadata = feedback.params
    )
    chatRepository.addMessage(userMessage)
    
    // 3. Send to AI backend if needed
    aiClient.sendFeedback(feedback)
}
```

### That's it. No special chat manager, no custom message types, no lifecycle management beyond "mark sheet as consumed."

---

## Demo App (Updated)

The demo app shows ONE way to integrate. It's deliberately simple —
it uses in-memory state, not Room. But it demonstrates the pattern
that works with any architecture.

```kotlin
class DemoViewModel : ViewModel() {
    // Host app's own message model — nothing from AUI
    private val _messages = MutableStateFlow<List<DemoMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    
    private val sampleResponses = listOf(/* hardcoded JSON strings */)
    private var responseIndex = 0
    
    init {
        // Load first AI response
        addAiResponse(text = "Welcome! Let's get your feedback.", auiJson = sampleResponses[0])
    }
    
    private fun addAiResponse(text: String? = null, auiJson: String? = null) {
        _messages.update { it + DemoMessage.Ai(
            id = UUID.randomUUID().toString(),
            text = text,
            auiJson = auiJson
        )}
    }
    
    fun onAuiFeedback(messageId: String, feedback: AuiFeedback) {
        // Mark sheet AUI as consumed
        if (feedback.stepsTotal != null) {
            _messages.update { messages ->
                messages.map { msg ->
                    if (msg is DemoMessage.Ai && msg.id == messageId) {
                        msg.copy(auiJson = null)
                    } else msg
                }
            }
        }
        
        // Add user feedback as a message
        _messages.update { it + DemoMessage.User(
            text = feedback.formattedEntries ?: "Submitted"
        )}
        
        // Load next AI response
        responseIndex++
        if (responseIndex < sampleResponses.size) {
            addAiResponse(auiJson = sampleResponses[responseIndex])
        }
    }
    
    fun onUserText(text: String) {
        _messages.update { it + DemoMessage.User(text = text) }
    }
}

// Host app's own message model
sealed class DemoMessage {
    data class Ai(val id: String, val text: String?, val auiJson: String?) : DemoMessage()
    data class User(val text: String) : DemoMessage()
}
```

Note: `DemoMessage` is in the demo app, NOT in the library. Every host app
defines its own message type.

---

## Claude Code Sessions

### Session 11: Clean Up Public API

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

The library should be a pure renderer with a callback. It should NOT 
manage chat history or conversation state.

1. Delete AuiChatManager if it exists — not the library's responsibility.
2. Delete AuiChatMessage if it exists — host apps define their own.
3. Delete AuiSheetState as a public class — sheet state is internal.

4. Ensure AuiRenderer handles sheets internally:
   - When display = "sheet", AuiRenderer opens a ModalBottomSheet
   - Handles step navigation inside the sheet
   - On submit: calls onFeedback with consolidated AuiFeedback, closes sheet
   - On dismiss: calls onFeedback with action="sheet_dismissed", closes sheet
   - AuiRenderer emits no visible content for sheet responses 
     (the sheet is the content)
   - After the sheet closes, the composable is inert if called again 
     (so if host app forgets to remove the JSON, it doesn't re-open)

5. Update AuiFeedback to include:
   - formattedEntries: String? (plain text summary)
   - entries: List<FeedbackEntry>? (structured Q&A pairs)
   - stepsSkipped: Int? (null for non-sheet)
   - stepsTotal: Int? (null for non-sheet)

6. Verify AuiRenderer public API is exactly:
   - AuiRenderer(json, modifier, theme, onFeedback, onParseError, onUnknownBlock)
   - AuiRenderer(response, modifier, theme, onFeedback, onUnknownBlock)
   Nothing else is public.

Write tests. Run them.
```

### Session 12: AuiCatalogPrompt

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

Create AuiCatalogPrompt in com.bennyjon.aui.compose:

1. Object with a generate() function that returns the full AI system 
   prompt text describing the AUI format and available components.
   
2. The generated text should include:
   - Response format (display + blocks / steps)
   - All component types with their data fields
   - Feedback format explanation
   - Guidelines for the AI
   
3. Optional parameter: availableActions list. If provided, includes 
   a section listing valid action IDs.

4. The output should match what's in the AI System Prompt Contract 
   section of spec/aui-spec-v1.md — generate it from code so it 
   stays in sync with the actual component catalog.

5. Write a test that verifies generate() includes all registered 
   component types.

Run tests.
```

### Session 13: Rewrite Demo App

```
Read CLAUDE.md and .planning/phase3-host-integration.md.

Rewrite the demo app to show a clean integration pattern:

1. Define DemoMessage sealed class IN THE DEMO APP (not the library):
   - Ai(id, text?, auiJson?)
   - User(text)

2. DemoViewModel:
   - Manages List<DemoMessage> in StateFlow
   - Loads hardcoded JSON responses
   - onAuiFeedback: marks sheet AUI as consumed (sets auiJson=null), 
     adds User message with formattedEntries, loads next response
   - onUserText: adds User message

3. ChatScreen:
   - LazyColumn renders DemoMessage.Ai and DemoMessage.User
   - For Ai messages: show text bubble + AuiRenderer(auiJson)
   - AuiRenderer gets onFeedback wired to viewModel.onAuiFeedback
   - Input bar at bottom

4. Verify flows:
   - Expanded poll: renders, submit works, feedback shows as user message
   - Sheet survey: opens, step through, submit, sheet gone, feedback shows
   - Sheet dismiss: sheet gone, "Survey dismissed" shows
   - Scroll back: old expanded visible, old sheet consumed (no re-open)

5. Delete any old ChatMessage, AuiChatManager references.

Build, run, test on emulator.
```

### Session 14: Review + Documentation

```
Read CLAUDE.md. Review as a developer seeing this library for the first time.

1. Can I integrate AUI in 3 lines? (dependency, AuiRenderer, onFeedback)
2. Is anything from the library leaking into the host app's architecture?
3. Does AuiRenderer work if I forget to handle sheet consumption? 
   (It should be safe — not crash, not re-open)
4. Is AuiCatalogPrompt output clear enough for an AI to follow?
5. Is the demo app a good example without being prescriptive?
6. Add KDoc to all public API (AuiRenderer, AuiTheme, AuiFeedback, AuiCatalogPrompt)
7. Update README.md with a quick-start integration guide
```

---

## Phase 3 Deliverables

- [ ] Library has NO chat/conversation management — pure renderer + callback
- [ ] AuiRenderer handles sheets internally (open, navigate, close)
- [ ] AuiFeedback includes formattedEntries + structured entries
- [ ] AuiCatalogPrompt generates AI system prompt text
- [ ] Demo app uses its own message model (not library types)
- [ ] Demo shows sheet consumption pattern (set auiJson=null after feedback)
- [ ] All public API has KDoc
- [ ] README has integration quick-start
