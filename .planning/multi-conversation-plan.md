# Multi-Conversation Support

## Problem
`conversationId` is hardcoded to `"live"` in `LiveChatViewModel`. Every app launch reuses the same conversation. No way to start a new conversation or browse past ones.

## Approach

### 1. New DAO query — list distinct conversations
Add a query to `ChatMessageDao` that returns all distinct `conversationId` values with a preview (first user message text, latest `createdAt` timestamp). `GROUP BY conversationId` returning a lightweight `ConversationSummary` data class (id, preview text, last timestamp, message count).

### 2. Repository — expose conversation list
Add `observeConversations(): Flow<List<ConversationSummary>>` to `ChatRepository` / `DefaultChatRepository`. Purely a DAO read — no LLM involvement.

### 3. Conversation list screen
New `ConversationListScreen` composable:
- List of past conversations (preview text + timestamp).
- "New Chat" FAB/button that mints a UUID and navigates to `LiveChatScreen`.
- Tapping an existing conversation navigates to `LiveChatScreen` with that ID.

### 4. ViewModel changes
`LiveChatViewModel` reads `conversationId` from `SavedStateHandle` (nav argument) instead of hardcoding `"live"`. All existing methods (`send`, `clearConversation`, `onFeedback`) work unchanged — they already use `conversationId`.

### 5. Navigation changes
- New route: `"conversations"` → `ConversationListScreen`.
- Update route to `"live_chat/{conversationId}"` with a nav argument.
- "Live Chat" card on `DemoHomeScreen` navigates to `"conversations"`.
- `ConversationListScreen` handles "New Chat" (mint UUID → `live_chat/{newId}`) and "existing chat" (`live_chat/{existingId}`).

## What stays the same
- `ChatMessageEntity` schema — no migration, `conversationId` column already exists.
- `DefaultChatRepository` send/clear logic — already parameterized by `conversationId`.
- `LiveChatScreen` composable — unchanged, receives everything from ViewModel.

## Files touched
| File | Change |
|---|---|
| `ChatMessageDao` | Add `observeConversations()` query |
| `ChatRepository` | Add `observeConversations()` |
| `DefaultChatRepository` | Implement it (delegate to DAO) |
| `LiveChatViewModel` | Read `conversationId` from `SavedStateHandle` |
| `MainActivity` (nav) | Add `"conversations"` route, parameterize `"live_chat/{id}"` |
| `DemoHomeScreen` | "Live Chat" card → navigates to `"conversations"` |
| **New:** `ConversationListScreen` | List + "New Chat" button |

## Open question
- "Clear" currently deletes all messages, which would make the conversation vanish from the list. That seems fine (effectively "delete conversation"). Could rename button to "Delete" for clarity.
