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
 * Each time [onFeedback] is called, a [ChatMessage.UserFeedback] is appended and the next
 * response in the sequence is loaded. [onUserText] appends a plain [ChatMessage.UserText].
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
        val label = feedback.label?.takeIf { it.isNotBlank() } ?: feedback.action
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
            SHEET_STEP1_JSON,
            SHEET_STEP2_JSON,
            SHEET_STEP3_JSON,
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
        "label": "Features: {{features}} — NPS: {{nps}}/10"
      }
    }
  ]
}
""".trimIndent()

private val SHEET_STEP1_JSON = """
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
""".trimIndent()

private val SHEET_STEP2_JSON = """
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
""".trimIndent()

private val SHEET_STEP3_JSON = """
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
        "label": "{{open_feedback}}"
      }
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
