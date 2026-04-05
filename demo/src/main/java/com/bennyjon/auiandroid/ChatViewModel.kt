package com.bennyjon.auiandroid

import androidx.lifecycle.ViewModel
import com.bennyjon.aui.core.AuiParser
import com.bennyjon.aui.core.model.AuiFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the demo chat screen.
 *
 * Holds a list of [ChatMessage]s and steps through a pre-loaded sequence of poll JSON responses.
 * [onFeedback] receives a consolidated [AuiFeedback] from the library — for multi-step flows
 * [AuiFeedback.entries] contains all Q+A pairs ready to format for the chat bubble.
 * [onUserText] appends a plain [ChatMessage.UserText].
 */
class ChatViewModel : ViewModel() {

    private val parser = AuiParser()
    private var sequenceIndex = 0

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        loadNextResponse()
    }

    fun onFeedback(feedback: AuiFeedback) {
        val label = feedback.formattedEntries?.takeIf { it.isNotBlank() } ?: feedback.action
        append(ChatMessage.UserFeedback(label = label))
        loadNextResponse()
    }

    fun onUserText(text: String) {
        if (text.isBlank()) return
        append(ChatMessage.UserText(text = text))
    }

    private fun loadNextResponse() {
        if (sequenceIndex >= RESPONSE_SEQUENCE.size) return
        val json = RESPONSE_SEQUENCE[sequenceIndex++]
        val response = parser.parseOrNull(json) ?: return
        append(ChatMessage.AiResponse(response = response))
    }

    private fun append(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private companion object {
        val RESPONSE_SEQUENCE = listOf(
            EXPANDED_SURVEY_JSON,
            SHEET_JSON,
            CONFIRMATION_JSON,
        )
    }
}

// ── JSON sequence ─────────────────────────────────────────────────────────────

private val EXPANDED_SURVEY_JSON = """
{
  "display": "expanded",
  "blocks": [
    {
      "type": "text",
      "data": { "text": "We'd love your feedback! Just a few quick questions:" }
    },
    {
      "type": "heading",
      "data": { "text": "How satisfied are you with our service?" }
    },
    {
      "type": "radio_list",
      "data": {
        "key": "satisfaction",
        "options": [
          { "label": "Very satisfied", "description": "Everything works great, I'm happy", "value": "very_satisfied" },
          { "label": "Somewhat satisfied", "description": "It's good but there's room for improvement", "value": "somewhat_satisfied" },
          { "label": "Neutral", "value": "neutral" },
          { "label": "Not satisfied", "description": "I've had significant issues", "value": "not_satisfied" }
        ]
      }
    },
    { "type": "divider", "data": {} },
    {
      "type": "heading",
      "data": { "text": "Which features have you used? (select all that apply)" }
    },
    {
      "type": "checkbox_list",
      "data": {
        "key": "used_features",
        "options": [
          { "label": "Chat assistant", "description": "Ask questions and get help", "value": "chat" },
          { "label": "Product search", "description": "Find and compare products", "value": "search" },
          { "label": "Order tracking", "description": "Track deliveries and returns", "value": "tracking" },
          { "label": "Recommendations", "description": "Personalized suggestions", "value": "recs" }
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
""".trimIndent()

private val SHEET_JSON = """
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
              { "label": "Excellent", "description": "Exceeded my expectations in every way", "value": "excellent" },
              { "label": "Good", "description": "Met my expectations, worked well", "value": "good" },
              { "label": "Average", "description": "Nothing special, gets the job done", "value": "average" },
              { "label": "Below average", "description": "Had some frustrating moments", "value": "below_average" },
              { "label": "Poor", "description": "Significantly below what I expected", "value": "poor" }
            ]
          }
        },
        {
          "type": "button_primary",
          "data": { "label": "Next" },
          "feedback": {
            "action": "poll_next_step",
            "params": { "poll_id": "radio_survey" }
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
              { "label": "Response speed", "description": "Faster answers and loading times", "value": "speed" },
              { "label": "Answer accuracy", "description": "More precise and reliable responses", "value": "accuracy" },
              { "label": "Visual design", "description": "Cleaner, more modern interface", "value": "design" },
              { "label": "More features", "description": "Additional capabilities and tools", "value": "features" }
            ]
          }
        },
        {
          "type": "button_primary",
          "data": { "label": "Next" },
          "feedback": {
            "action": "poll_next_step",
            "params": { "poll_id": "radio_survey" }
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
        },
        {
          "type": "button_primary",
          "data": { "label": "Submit" },
          "feedback": {
            "action": "poll_complete",
            "params": { "poll_id": "radio_survey" }
          }
        }
      ]
    }
  ]
}
""".trimIndent()

private val CONFIRMATION_JSON = """
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
""".trimIndent()
