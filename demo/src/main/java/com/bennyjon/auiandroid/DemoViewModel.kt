package com.bennyjon.auiandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bennyjon.aui.core.model.AuiFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Drives the demo chat screen.
 *
 * Manages a list of [DemoMessage]s and steps through a pre-loaded sequence of raw AUI JSON
 * responses. The ViewModel never parses JSON itself — it passes raw strings to
 * [com.bennyjon.aui.compose.AuiRenderer], which handles parsing internally.
 *
 * Key integration patterns demonstrated:
 * - **Sheet consumption:** When [onAuiFeedback] is called for a sheet response
 *   (`stepsTotal != null`), the message's `auiJson` is set to `null` so the sheet
 *   cannot re-open if the user scrolls back to it.
 * - **Feedback as user message:** The library's [AuiFeedback.formattedEntries] is displayed
 *   as a user chat bubble, showing the Q&A summary without any custom formatting.
 *
 * @param responseSequence The ordered list of AUI JSON strings to display. Each feedback
 *   interaction advances to the next entry. Defaults to the built-in theme showcase sequence.
 */
class DemoViewModel(
    private val responseSequence: List<String> = DEFAULT_SEQUENCE,
) : ViewModel() {

    private var sequenceIndex = 0

    private val _messages = MutableStateFlow<List<DemoMessage>>(emptyList())
    val messages: StateFlow<List<DemoMessage>> = _messages.asStateFlow()

    init {
        loadNextResponse()
    }

    /**
     * Called when the user interacts with an AUI component that has feedback configured.
     *
     * @param messageId The [DemoMessage.Ai.id] of the message that produced this feedback.
     * @param feedback The structured feedback from the AUI library.
     */
    fun onAuiFeedback(messageId: String, feedback: AuiFeedback) {
        // For sheet responses: mark the AUI as consumed so it doesn't re-open on scroll-back.
        if (feedback.stepsTotal != null) {
            _messages.update { messages ->
                messages.map { msg ->
                    if (msg is DemoMessage.Ai && msg.id == messageId) {
                        msg.copy(auiJson = null)
                    } else {
                        msg
                    }
                }
            }
        }

        // Add user feedback as a chat bubble.
        val label = feedback.formattedEntries?.takeIf { it.isNotBlank() } ?: feedback.action
        _messages.update { it + DemoMessage.User(text = label) }

        // Load the next AI response in the demo sequence.
        loadNextResponse()
    }

    /** Appends a plain text message from the user. */
    fun onUserText(text: String) {
        if (text.isBlank()) return
        _messages.update { it + DemoMessage.User(text = text) }
    }

    private fun loadNextResponse() {
        if (sequenceIndex >= responseSequence.size) return
        val json = responseSequence[sequenceIndex++]
        _messages.update { it + DemoMessage.Ai(auiJson = json) }
    }

    companion object {
        /** Default sequence used by the theme showcase screens. */
        val DEFAULT_SEQUENCE = listOf(
            EXPANDED_ACTION_JSON,
            SHEET_JSON,
            CONFIRMATION_JSON,
        )

        /** Sequence used by the plugin showcase screen. */
        val PLUGIN_SEQUENCE = listOf(
            PLUGIN_INTRO_JSON,
            PLUGIN_ACTIONS_JSON,
            PLUGIN_COMBINED_JSON,
        )
    }
}

/**
 * Factory for creating [DemoViewModel] with a custom response sequence.
 *
 * Used by the plugin showcase screen to load plugin-specific JSON instead
 * of the default theme showcase sequence.
 */
class DemoViewModelFactory(
    private val responseSequence: List<String>,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DemoViewModel(responseSequence) as T
}

// ── JSON sequence ─────────────────────────────────────────────────────────────

private val EXPANDED_ACTION_JSON = """
{
  "display": "expanded",
  "blocks": [
    {
      "type": "heading",
      "data": { "text": "Recommended for you" }
    },
    {
      "type": "text",
      "data": { "text": "Based on your browsing history, here are some features you might like:" }
    },
    {
      "type": "chip_select_single",
      "data": {
        "key": "interest",
        "label": "What interests you most?",
        "options": [
          { "label": "Smart Alerts", "value": "alerts" },
          { "label": "Price Tracking", "value": "tracking" },
          { "label": "Wish Lists", "value": "wishlists" },
          { "label": "Deal Finder", "value": "deals" }
        ]
      }
    },
    {
      "type": "button_primary",
      "data": { "label": "Tell me more" },
      "feedback": {
        "action": "explore_feature",
        "params": { "source": "recommendation_card" }
      }
    },
    {
      "type": "button_secondary",
      "data": { "label": "Not now" },
      "feedback": {
        "action": "dismiss_recommendation",
        "params": { "source": "recommendation_card" }
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

// ── Plugin showcase JSON sequence ────────────────────────────────────────────

private val PLUGIN_INTRO_JSON = """
{
  "display": "inline",
  "blocks": [
    {
      "type": "heading",
      "data": { "text": "Plugin Components" }
    },
    {
      "type": "text",
      "data": { "text": "This card is rendered by a custom component plugin — the library doesn't know about \"demo_fun_fact\" out of the box." }
    },
    {
      "type": "demo_fun_fact",
      "data": {
        "title": "Did you know?",
        "fact": "Kotlin was named after Kotlin Island near St. Petersburg, Russia. The JetBrains team chose the name because it was short, memorable, and — like Java — named after an island.",
        "source": "JetBrains Blog"
      }
    },
    {
      "type": "button_primary",
      "data": { "label": "Tell me more" },
      "feedback": {
        "action": "more_facts",
        "params": { "topic": "kotlin" }
      }
    }
  ]
}
""".trimIndent()

private val PLUGIN_ACTIONS_JSON = """
{
  "display": "inline",
  "blocks": [
    {
      "type": "heading",
      "data": { "text": "Plugin Actions" }
    },
    {
      "type": "text",
      "data": { "text": "These buttons trigger action plugins. \"Open URL\" launches the browser. \"Navigate\" shows a Toast (since the demo has no deep-link routes)." }
    },
    {
      "type": "button_primary",
      "data": { "label": "Open Kotlin Website" },
      "feedback": {
        "action": "open_url",
        "params": { "url": "https://kotlinlang.org" }
      }
    },
    {
      "type": "button_secondary",
      "data": { "label": "Navigate to Settings" },
      "feedback": {
        "action": "navigate",
        "params": { "screen": "settings" }
      }
    }
  ]
}
""".trimIndent()

private val PLUGIN_COMBINED_JSON = """
{
  "display": "inline",
  "blocks": [
    {
      "type": "demo_fun_fact",
      "data": {
        "title": "Compose Fun Fact",
        "fact": "Jetpack Compose was first announced at Google I/O 2019 and reached stable 1.0 in July 2021. It replaced the 10-year-old View system as the recommended way to build Android UI."
      }
    },
    {
      "type": "button_primary",
      "data": { "label": "Open Compose Docs" },
      "feedback": {
        "action": "open_url",
        "params": { "url": "https://developer.android.com/compose" }
      }
    },
    {
      "type": "button_secondary",
      "data": { "label": "Go to Profile" },
      "feedback": {
        "action": "navigate",
        "params": { "screen": "profile" }
      }
    }
  ]
}
""".trimIndent()
