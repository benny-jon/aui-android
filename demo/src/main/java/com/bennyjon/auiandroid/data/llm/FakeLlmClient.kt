package com.bennyjon.auiandroid.data.llm

/**
 * An [LlmClient] that cycles through a scripted sequence of responses.
 *
 * Used for demo and testing purposes. Each call to [complete] returns the next
 * response in the sequence, wrapping around to the start after the last one.
 */
class FakeLlmClient : LlmClient {

    private var index = 0

    override suspend fun complete(
        systemPrompt: String,
        history: List<LlmMessage>,
    ): LlmRawResult {
        val rawJson = SCRIPTED_RESPONSES[index % SCRIPTED_RESPONSES.size]
        index++
        return LlmRawResult(rawContent = rawJson)
    }

    internal companion object {

        val SCRIPTED_RESPONSES = listOf(
            // 1) Text-only greeting
            """
            {
              "text": "Hello! I'm a fake assistant running locally. No API calls needed. Try sending another message to see an interactive poll!"
            }
            """.trimIndent(),

            // 2) Text + inline quick replies (yes/no)
            """
            {
              "text": "Great question! Was this response helpful?",
              "aui": {
                "display": "inline",
                "blocks": [
                  {
                    "type": "quick_replies",
                    "data": {
                      "options": [
                        {
                          "label": "Yes",
                          "feedback": {
                            "action": "submit",
                            "params": { "poll_id": "helpful", "value": "yes" }
                          }
                        },
                        {
                          "label": "No",
                          "feedback": {
                            "action": "submit",
                            "params": { "poll_id": "helpful", "value": "no" }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
            """.trimIndent(),

            // 3) Text + sheet survey (multi-step)
            """
            {
              "text": "I'd love to learn more about you! Here's a quick survey:",
              "aui": {
                "display": "sheet",
                "sheet_title": "Quick Survey",
                "steps": [
                  {
                    "label": "Role",
                    "question": "What best describes your role?",
                    "blocks": [
                      {
                        "type": "radio_list",
                        "data": {
                          "key": "role",
                          "options": [
                            { "label": "Developer", "value": "dev" },
                            { "label": "Designer", "value": "design" },
                            { "label": "Product manager", "value": "pm" },
                            { "label": "Other", "value": "other" }
                          ]
                        }
                      },
                      {
                        "type": "button_primary",
                        "data": { "label": "Next" },
                        "feedback": { "action": "submit", "params": {} }
                      }
                    ]
                  },
                  {
                    "label": "Source",
                    "question": "How did you hear about us?",
                    "blocks": [
                      {
                        "type": "radio_list",
                        "data": {
                          "key": "source",
                          "options": [
                            { "label": "Search engine", "value": "search" },
                            { "label": "Social media", "value": "social" },
                            { "label": "Friend or colleague", "value": "referral" },
                            { "label": "Other", "value": "other" }
                          ]
                        }
                      },
                      {
                        "type": "button_primary",
                        "data": { "label": "Submit" },
                        "feedback": { "action": "submit", "params": {} }
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),

            // 4) Text + expanded product recommendations (with card_title/card_description so
            //    hosts can show a stub on narrow windows or a side pane on wide windows).
            """
            {
              "text": "Here are some headphones I'd recommend looking at:",
              "aui": {
                "display": "expanded",
                "card_title": "Headphone picks",
                "card_description": "Three top noise-cancelling models compared",
                "blocks": [
                  { "type": "heading", "data": { "text": "Sony WH-1000XM5" } },
                  { "type": "text", "data": { "text": "Top-tier noise cancellation. 30-hour battery. Around ${'$'}348." } },
                  {
                    "type": "button_primary",
                    "data": { "label": "View on Amazon" },
                    "feedback": { "action": "open_url", "params": { "url": "https://example.com/sony" } }
                  },
                  { "type": "divider" },
                  { "type": "heading", "data": { "text": "Bose QuietComfort Ultra" } },
                  { "type": "text", "data": { "text": "Excellent comfort for long wear. Around ${'$'}379." } },
                  {
                    "type": "button_secondary",
                    "data": { "label": "View on Amazon" },
                    "feedback": { "action": "open_url", "params": { "url": "https://example.com/bose" } }
                  },
                  { "type": "divider" },
                  { "type": "heading", "data": { "text": "Apple AirPods Pro 2" } },
                  { "type": "text", "data": { "text": "Best for the Apple ecosystem. Around ${'$'}249." } },
                  {
                    "type": "button_secondary",
                    "data": { "label": "View on Amazon" },
                    "feedback": { "action": "open_url", "params": { "url": "https://example.com/airpods" } }
                  }
                ]
              }
            }
            """.trimIndent(),

            // 5) Text + inline confirmation (compact status)
            """
            {
              "text": "Thanks for completing the survey!",
              "aui": {
                "display": "inline",
                "blocks": [
                  {
                    "type": "status_banner_success",
                    "data": { "text": "Survey complete!" }
                  },
                  {
                    "type": "badge_success",
                    "data": { "text": "3 of 3 completed" }
                  }
                ]
              }
            }
            """.trimIndent(),
        )
    }
}
