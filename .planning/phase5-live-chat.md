# Phase 5: Live Chat Demo

**Goal:** Add a "Live Chat" entry point to the demo app that talks to a real LLM
end-to-end. The library stays a pure renderer — everything chat-related in this
phase lives in the `demo` module.

A generic `LlmClient` interface lets us swap Fake / Claude / OpenAI with no
changes to the repository, ViewModel, or UI. Conversation state is persisted in
Room with a provider-agnostic, self-describing schema.

**One small library addition:** `AuiActionPlugin.isReadOnly` property and
`AuiResponse.isReadOnly(pluginRegistry)` function in aui-core — needed so the
host can distinguish spent interactive responses from always-clickable read-only
ones. This is intrinsic to the response model, not chat-specific.

---

## Architecture

All new code under `demo/src/main/kotlin/com/bennyjon/aui/demo/`:

```
livechat/
  LiveChatScreen.kt              # Compose UI
  LiveChatViewModel.kt           # StateFlow, send(), onFeedback()
  LiveChatViewModelFactory.kt    # Wires DemoServiceLocator
  AuiFeedbackExt.kt              # AuiFeedback.toUserMessageText()
  SpentMarker.kt                 # markSpentInteractives()
data/
  llm/
    LlmClient.kt                 # interface — provider-neutral
    LlmMessage.kt                # role + content
    LlmResponse.kt               # flat data class, all nullable fields
    AuiResponseExtractor.kt      # internal: structured envelope → LlmResponse
    FakeLlmClient.kt
    ClaudeLlmClient.kt
    OpenAiLlmClient.kt
    LlmProvider.kt               # enum: FAKE, CLAUDE, OPENAI
    LlmClientFactory.kt
  chat/
    ChatMessage.kt               # flat data class with optional fields
    ChatRepository.kt            # interface — AUI-free
    DefaultChatRepository.kt     # Room + LlmClient orchestration
    db/
      ChatDatabase.kt
      ChatMessageDao.kt
      ChatMessageEntity.kt
DemoServiceLocator.kt            # object-based DI (no Hilt)
```

Plus two small additions in the library:
- `aui-core`: `AuiActionPlugin.isReadOnly` property, `AuiResponse.isReadOnly()`

### LLM response envelope

The system prompt instructs the LLM to always respond with a structured JSON
envelope containing an always-present `text` field and an optional `aui` field:

```json
{
  "text": "Here's a quick poll for you!",
  "aui": { "layout": "inline", "blocks": [ ... ] }
}
```

For OpenAI, the `aui` payload can alternatively arrive via tool/function calls.

`AuiResponseExtractor` deserializes this envelope — no fence-stripping heuristics
needed. `text` is always present; `aui` is optional. This means a single LLM
reply can carry both conversational text and an AUI response.

### LLM client contracts

```kotlin
interface LlmClient {
    suspend fun complete(systemPrompt: String, history: List<LlmMessage>): LlmResponse
}

data class LlmMessage(val role: Role, val content: String) {
    enum class Role { USER, ASSISTANT }
}

/**
 * Raw result from an LlmClient completion call.
 *
 * Carries the unprocessed content string exactly as received from the provider.
 * Parsing into text, AUI JSON, etc. is deferred to the repository layer when
 * loading from the database, so the original response is always preserved for
 * replay and debugging.
 */
data class LlmRawResult(
    val rawContent: String? = null,
    val errorMessage: String? = null,
    val cause: Throwable? = null,
)
```

### Room schema — raw content storage

```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,        // UUID
    val conversationId: String,        // UUID
    val role: String,                  // "user" | "assistant"
    val rawContent: String? = null,    // unprocessed LLM response (user text for user rows)
    val errorMessage: String? = null,  // assistant error
    val createdAt: Long,
)
```

Stores the raw LLM response as-is. Parsing into text/AUI happens at load time
via `AuiResponseExtractor` in `toDomain()`. UUIDs minted by
`DefaultChatRepository` — single mint site.

### Domain model

```kotlin
/**
 * Flat chat message. Optional fields describe what the message carries.
 * A single assistant message can have both [text] and [auiResponse].
 */
data class ChatMessage(
    val id: String,
    val createdAt: Long,
    val role: Role,
    val text: String? = null,
    val auiResponse: AuiResponse? = null,
    val rawAuiJson: String? = null,
    val errorMessage: String? = null,
    val isAuiSpent: Boolean = false,
) {
    enum class Role { USER, ASSISTANT }
}
```

Single flat class — mirrors the entity shape but adds `auiResponse` (parsed)
and `isAuiSpent` (derived). Entity and domain model remain separate classes:
entity owns `conversationId` and Room annotations, domain model owns parsed
AUI and spent state. The mapper between them is trivial.

`isAuiSpent` is derived by the ViewModel via the AUI library's
`AuiResponse.isReadOnly(pluginRegistry)` — never stored in DB.

### Repository — AUI-free

```kotlin
interface ChatRepository {
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun sendUserMessage(conversationId: String, text: String)
    suspend fun clearConversation(conversationId: String)
}
```

Three methods. Zero imports from `aui-core`. No `recordFeedback` — feedback
is converted to text in the ViewModel before reaching the repo.

`sendUserMessage` flow:
1. Insert user row (UUID minted here, `rawContent` = user text).
2. Read all rows; map to `List<LlmMessage>`. Assistant rows run through
   `AuiResponseExtractor` to rebuild content for history.
3. Call `llmClient.complete(systemPrompt, history)` → `LlmRawResult`.
4. Insert assistant row: `rawContent` = raw LLM response, `errorMessage` if failed.
5. Flow re-emits.

### ViewModel — the AUI ↔ chat boundary

```kotlin
class LiveChatViewModel(...) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> =
        repo.observeMessages(conversationId)
            .map { it.markSpentInteractives(pluginRegistry) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun send(text: String) {
        viewModelScope.launch { repo.sendUserMessage(conversationId, text) }
    }

    fun onFeedback(feedback: AuiFeedback) {
        send(feedback.toUserMessageText())
    }
}
```

### Feedback text conversion

```kotlin
// livechat/AuiFeedbackExt.kt
internal fun AuiFeedback.toUserMessageText(): String =
    formattedEntries.ifEmpty { label }
```

### Spent-marking — read-only aware

```kotlin
// livechat/SpentMarker.kt
internal fun List<ChatMessage>.markSpentInteractives(
    pluginRegistry: AuiPluginRegistry,
): List<ChatMessage> {
    val lastAuiIndex = indexOfLast { it.auiResponse != null }
    return mapIndexed { index, msg ->
        if (msg.auiResponse != null
            && index < lastAuiIndex
            && !msg.auiResponse.isReadOnly(pluginRegistry)
        ) msg.copy(isAuiSpent = true) else msg
    }
}
```

Rule: all-but-last message with an `auiResponse` is marked `isAuiSpent = true`,
unless the AUI library reports the response as read-only via
`AuiResponse.isReadOnly(pluginRegistry)`. DB is never mutated — `isAuiSpent`
is derived at render time.

### Library addition: `isReadOnly`

```kotlin
// AuiActionPlugin (aui-core) — one new property with default
interface AuiActionPlugin {
    val action: String
    val isReadOnly: Boolean get() = false  // default: assume interactive
    fun handle(...): Boolean
    val promptSchema: String
}

// AuiResponse (aui-core) — one new function
fun AuiResponse.isReadOnly(pluginRegistry: AuiPluginRegistry): Boolean =
    blocks.all { block ->
        val feedback = block.feedback ?: return@all true
        when (feedback.action) {
            "submit" -> false
            else -> pluginRegistry.findActionPlugin(feedback.action)?.isReadOnly ?: false
        }
    }
```

Demo plugins updated: `OpenUrlPlugin.isReadOnly = true`,
`ToastNavigatePlugin.isReadOnly = true`.

---

## Session 21: LlmClient interface + FakeLlmClient

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

Create the LLM client abstraction in
demo/src/main/kotlin/com/bennyjon/aui/demo/data/llm/:

1. LlmClient.kt — interface: suspend fun complete(systemPrompt, history): LlmRawResult
2. LlmMessage.kt — data class with Role enum (USER, ASSISTANT)
3. LlmRawResult.kt — flat data class: rawContent?, errorMessage?, cause?.
   rawContent carries the unprocessed LLM response string.
4. LlmResponse.kt — parsed result from AuiResponseExtractor (text, auiJson,
   auiResponse, errorMessage, cause). Used internally at load time, not by clients.
5. AuiResponseExtractor.kt — internal object:
   - fromRawResponse(rawText): deserializes the structured JSON envelope
     { "text": "...", "aui": { ... } }. Parses the aui field (if present)
     via AuiParser.parse. Returns LlmResponse with text always set,
     auiJson+auiResponse set when aui field is present.
   - error(message, cause?): returns LlmResponse with errorMessage set
6. FakeLlmClient.kt — cycles a scripted sequence, returns LlmRawResult
   with rawContent = scripted JSON strings. Wraps around at end.

Tests:
- AuiResponseExtractorTest: envelope with text only, envelope with text+aui,
  malformed JSON → error, error()
- FakeLlmClientTest: returns expected sequence

Run :demo:testDebugUnitTest. No Room or Compose deps.
```

---

## Session 22: Room schema + ChatRepository

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

Add Room to demo:

1. demo/build.gradle.kts: Room runtime, Room KTX, Room compiler (KSP).
   Update libs.versions.toml.

2. Create demo/data/chat/db/:
   - ChatMessageEntity: @PrimaryKey id (String/UUID), conversationId,
     role ("user"|"assistant"), rawContent?, errorMessage?, createdAt.
     Stores unprocessed LLM response. Parsing deferred to load time.
   - ChatMessageDao: observeMessages(Flow), insert, clearConversation
   - ChatDatabase (version=2, exportSchema=false, destructive migration)

3. Create demo/data/chat/:
   - ChatMessage flat data class: id, createdAt, role (enum USER/ASSISTANT),
     text?, auiResponse?, rawAuiJson?, rawContent?, errorMessage?,
     isAuiSpent (default false). text/auiResponse/rawAuiJson derived from
     rawContent at load time via AuiResponseExtractor.
   - ChatRepository interface: observeMessages, sendUserMessage,
     clearConversation. Three methods. ZERO aui-core imports.
   - DefaultChatRepository(llmClient, dao, systemPrompt, ioDispatcher):
     * Mints UUIDs via UUID.randomUUID().toString()
     * sendUserMessage: insert user (rawContent=text) → build history
       (extract content from rawContent for assistant rows) → complete
       → store rawContent from LlmRawResult → insert
     * Entity-to-domain mapper: runs AuiResponseExtractor on rawContent
     * clearConversation delegates to dao

4. Test (Robolectric or in-memory Room):
   - FakeLlmClient + repo: sendUserMessage("hi") → two messages observed
   - clearConversation → empty list

Run :demo:testDebugUnitTest.
```

---

## Session 23: LiveChatScreen + ViewModel + isReadOnly + 5th home card

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

TWO parts: small library addition, then demo UI.

PART A — Library (aui-core):

1. AuiActionPlugin: add val isReadOnly: Boolean get() = false
   KDoc: "True for pass-through actions (open_url, navigate) that don't
   collect input."

2. AuiResponse.isReadOnly(pluginRegistry: AuiPluginRegistry): Boolean
   Every block's feedback action is absent, or is a plugin with isReadOnly=true.
   "submit" → always false. Unknown actions → false.

3. Update demo plugins: OpenUrlPlugin.isReadOnly = true,
   ToastNavigatePlugin.isReadOnly = true.

4. Unit tests in aui-core: no-feedback → true, submit → false,
   only open_url → true, mixed → false.

Run :aui-core:test.

PART B — Demo UI:

5. DemoServiceLocator (object): db, dao, currentLlmClient (FakeLlmClient),
   chatRepository, systemPrompt (AuiCatalogPrompt.generate(pluginRegistry)),
   pluginRegistry (DemoPluginRegistry.create()).

6. livechat/AuiFeedbackExt.kt:
     fun AuiFeedback.toUserMessageText() = formattedEntries.ifEmpty { label }

7. livechat/SpentMarker.kt: markSpentInteractives(pluginRegistry).
   Checks msg.auiResponse != null (not a type check). All-but-last AUI
   marked isAuiSpent = true UNLESS auiResponse.isReadOnly(registry).

8. LiveChatViewModel(repo, systemPrompt, conversationId, pluginRegistry):
   messages StateFlow with markSpentInteractives transform, send(text),
   onFeedback(feedback) = send(feedback.toUserMessageText()), isSending.

9. LiveChatViewModelFactory wires DemoServiceLocator.

10. LiveChatScreen: TopAppBar (back + Clear), LazyColumn renders based on
    ChatMessage fields:
    - role=USER → right-aligned bubble showing text
    - role=ASSISTANT with text only → left-aligned bubble
    - role=ASSISTANT with text + auiResponse → left bubble for text,
      then AuiRenderer (or grayed out if isAuiSpent)
    - errorMessage non-null → red error banner
    Spinner on isSending, text input bar at bottom.

11. 5th DemoHomeScreen card "Live Chat" → LiveChatScreen, conversationId="live"

Build, install. Verify: type → response with text+AUI → tap chips → submit →
previous AUI grayed out → open_url buttons stay clickable → kill/reopen
persists → clear works.
```

---

## Session 24: ClaudeLlmClient

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

1. Add Ktor deps to demo (client-android, content-negotiation, kotlinx-json).

2. ANTHROPIC_API_KEY from config.properties → BuildConfig. Empty = don't fail build.

3. ClaudeLlmClient(apiKey, httpClient, model = "claude-sonnet-4-5"):
   POST /v1/messages, headers (x-api-key, anthropic-version: 2023-06-01),
   body { model, max_tokens: 4096, system: systemPrompt, messages }.
   System prompt instructs the model to respond with a JSON envelope:
   { "text": "...", "aui": { ... } } where aui is optional.
   Extract content[0].text → AuiResponseExtractor.fromRawResponse.
   Wrap failures → AuiResponseExtractor.error.

4. LlmProvider enum (FAKE, CLAUDE), LlmClientFactory.create(provider).
   DemoServiceLocator.setProvider clears conversation on switch.

5. Provider dropdown on LiveChatScreen TopAppBar. Claude disabled if no key.

6. Manual test: Claude → "Show me a poll" → text bubble + AUI renders →
   submit → previous AUI grayed out.
```

---

## Session 25: AuiCatalogPrompt — Tuning Knobs & Content Improvements

```
Read CLAUDE.md and .planning/session-aui-catalog-prompt-tuning.md.
```

---

## Session 26: Inline Markdown support for `AuiText`

```
Read CLAUDE.md and .planning/session-aui-text-markdown.md.
```

---

## Session 29: OpenAiLlmClient

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

ACCEPTANCE: diff outside demo/data/llm/ is at most one line. If bigger,
the abstraction leaks — refactor first.

1. OPENAI_API_KEY from config.properties → BuildConfig.

2. OpenAiLlmClient(apiKey, httpClient, model = "gpt-4o"):
   POST /v1/chat/completions, Authorization: Bearer,
   body { model, messages: [{role:"system", content:systemPrompt}, ...] }.
   System prompt uses the same JSON envelope format. Alternatively, AUI
   payload can arrive via tool/function calls — extract accordingly.
   Result → AuiResponseExtractor.fromRawResponse.

3. Add OPENAI to enum + factory + dropdown.

4. Manual test. Note GPT-4o AUI JSON reliability in CLAUDE.md.
```

---

## Session 30: Polish + Documentation

```
Read CLAUDE.md and .planning/phase5-live-chat.md.

Polish: spinner, empty state, error banner with retry, Clear confirmation
dialog, snackbar on provider switch.

Update CLAUDE.md: Phase 5 → Completed ✅, session log 21-26, design decisions.

Create docs/livechat.md: architecture overview, LlmResponse contract,
JSON envelope format, "add a new provider" recipe pointing at OpenAiLlmClient.

DO NOT touch: spec/aui-spec-v1.md, docs/architecture.md, other .planning/ files.
```

---

## Phase 5 Deliverables

- [ ] LlmClient interface + flat LlmResponse data class
- [ ] AuiResponseExtractor (structured envelope parsing, error handling)
- [ ] FakeLlmClient + unit tests
- [ ] Room DB: ChatMessageEntity (UUID PK, rawContent column, errorMessage, no discriminator)
- [ ] DefaultChatRepository: stores raw content, parses at load time via AuiResponseExtractor, single UUID mint, three methods
- [ ] ChatMessage flat data class with optional fields (text, auiResponse, rawAuiJson, rawContent, errorMessage, isAuiSpent)
- [ ] LiveChatScreen renders all field combinations; spent AUI grayed out
- [ ] 5th DemoHomeScreen card "Live Chat"
- [ ] ViewModel: send(), onFeedback() via toUserMessageText(), markSpentInteractives
- [ ] AuiActionPlugin.isReadOnly + AuiResponse.isReadOnly(registry) in aui-core
- [ ] ClaudeLlmClient against real Anthropic API
- [ ] OpenAiLlmClient against real OpenAI API
- [ ] Provider dropdown with clear-on-switch
- [ ] API keys from config.properties → BuildConfig (gitignored)
- [ ] docs/livechat.md
- [ ] CLAUDE.md updated, Phase 5 complete

---

## Design Notes

**Flat ChatMessage:** single data class with optional fields, mirroring the
entity shape. A single assistant message can carry both `text` and `auiResponse`.
Entity and domain model remain separate classes — entity owns Room annotations
and `conversationId`, domain model owns parsed `auiResponse` and derived
`isAuiSpent`. Mapping between them is trivial.

**Structured LLM envelope:** the system prompt instructs the model to respond
with `{ "text": "...", "aui": { ... } }`. `text` is always present on success,
`aui` is optional. `AuiResponseExtractor` deserializes this envelope directly —
no fence-stripping or heuristic parsing needed.

**isAuiSpent determination:** the ViewModel asks the AUI library whether each
`auiResponse` contains only read-only components via
`AuiResponse.isReadOnly(pluginRegistry)`. All-but-last AUI messages are marked
`isAuiSpent = true` unless the response is read-only.

**Raw content storage:** LlmClient returns `LlmRawResult` with the
unprocessed response string. DB stores `rawContent` as-is. Parsing into
text/AUI happens at load time via `AuiResponseExtractor` in `toDomain()`.
This preserves originals for replay/debugging and allows parsing logic
updates without data migration.

**Deferred parsing per provider:** `AuiResponseExtractor` auto-detects
Claude API format vs structured envelope. Future providers (OpenAI) add
new detection paths without changing the storage schema.

**AUI-free repo:** feedback → text conversion in ViewModel via
`formattedEntries.ifEmpty { label }`. Repo sees only strings. Typed messages and
feedback are indistinguishable at persistence, which is correct.

**Append-only DB, derived spent state:** never mutate rows. `isAuiSpent` is
computed by ViewModel from conversation shape and the library's `isReadOnly` check.

**isReadOnly in the library:** plugins declare `isReadOnly = true` for
pass-through actions. `AuiResponse.isReadOnly(registry)` aggregates across all
blocks. Hosts make rendering decisions without reimplementing component
classification.

**No streaming:** AUI needs full JSON. Defer.

**Object-based DI:** demo stays light. One ViewModel, one repo, one DB.

**Provider dropdown > separate cards:** proves the abstraction visually.
