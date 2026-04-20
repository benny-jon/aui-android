package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.data.FileContentData
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuiParserTest {

    private lateinit var parser: AuiParser

    @Before
    fun setUp() {
        parser = AuiParser()
    }

    // ── Inline rating poll ────────────────────────────────────────────────────

    @Test
    fun `parse poll-inline-rating`() {
        val json = loadResource("examples/poll-inline-rating.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(3, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.Heading)
        assertTrue(response.blocks[1] is AuiBlock.Text)
        val stars = response.blocks[2] as AuiBlock.InputRatingStars
        assertEquals("rating", stars.data.key)
        assertEquals("Tap to rate", stars.data.label)
        assertNotNull(stars.feedback)
        assertEquals("poll_answer", stars.feedback!!.action)
        assertEquals("exp_rating", stars.feedback!!.params["poll_id"])
    }

    // ── Inline yes/no poll ────────────────────────────────────────────────────

    @Test
    fun `parse poll-inline-yes-no`() {
        val json = loadResource("examples/poll-inline-yes-no.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(2, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.Text)
        val replies = response.blocks[1] as AuiBlock.QuickReplies
        assertEquals(2, replies.data.options.size)
        assertEquals("👍 Yes", replies.data.options[0].label)
        assertEquals("poll_answer", replies.data.options[0].feedback?.action)
        assertEquals("yes", replies.data.options[0].feedback?.params?.get("value"))
    }

    // ── Expanded survey ───────────────────────────────────────────────────────

    @Test
    fun `parse poll-expanded-survey`() {
        val json = loadResource("examples/poll-expanded-survey.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(7, response.blocks.size)

        val chips = response.blocks[2] as AuiBlock.ChipSelectMulti
        assertEquals("features", chips.data.key)
        assertEquals(5, chips.data.options.size)
        assertEquals("chat", chips.data.options[0].value)

        val slider = response.blocks[5] as AuiBlock.InputSlider
        assertEquals("nps", slider.data.key)
        assertEquals(0f, slider.data.min)
        assertEquals(10f, slider.data.max)
        assertEquals(5f, slider.data.value)

        val button = response.blocks[6] as AuiBlock.ButtonPrimary
        assertEquals("Submit Feedback", button.data.label)
        assertEquals("poll_submit", button.feedback?.action)
    }

    // ── Survey flow ───────────────────────────────────────────────────────────

    @Test
    fun `parse poll-survey-flow`() {
        val json = loadResource("examples/poll-survey-flow.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.SURVEY, response.display)
        assertEquals("Quick Survey", response.surveyTitle)
        assertEquals(3, response.steps.size)

        val step1 = response.steps[0]
        assertEquals("How was your experience?", step1.question)
        assertEquals(1, step1.blocks.size)
        val chips = step1.blocks[0] as AuiBlock.ChipSelectSingle
        assertEquals("experience", chips.data.key)
        assertEquals(4, chips.data.options.size)

        val step2 = response.steps[1]
        assertEquals("What would you like to see improved?", step2.question)
        val multiChips = step2.blocks[0] as AuiBlock.ChipSelectMulti
        assertEquals("improvements", multiChips.data.key)
        assertEquals(5, multiChips.data.options.size)

        val step3 = response.steps[2]
        assertEquals("Anything else you'd like to tell us?", step3.question)
        assertEquals(1, step3.blocks.size)
        val textInput = step3.blocks[0] as AuiBlock.InputTextSingle
        assertEquals("open_feedback", textInput.data.key)
    }

    // ── Confirmation ──────────────────────────────────────────────────────────

    @Test
    fun `parse poll-confirmation`() {
        val json = loadResource("examples/poll-confirmation.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(3, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.StatusBannerSuccess)
        assertTrue(response.blocks[1] is AuiBlock.Text)
        val badge = response.blocks[2] as AuiBlock.BadgeSuccess
        assertEquals("3 of 3 completed", badge.data.text)
    }

    // ── Expanded survey v2 (radio_list + checkbox_list) ──────────────────────

    @Test
    fun `parse poll-expanded-survey-v2`() {
        val json = loadResource("examples/poll-expanded-survey-v2.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(7, response.blocks.size)

        val radioList = response.blocks[2] as AuiBlock.RadioList
        assertEquals("satisfaction", radioList.data.key)
        assertEquals(4, radioList.data.options.size)
        assertEquals("Very satisfied", radioList.data.options[0].label)
        assertEquals("Everything works great, I'm happy", radioList.data.options[0].description)
        assertEquals("very_satisfied", radioList.data.options[0].value)
        assertNull(radioList.data.options[2].description)
        assertNull(radioList.data.selected)

        val checkboxList = response.blocks[5] as AuiBlock.CheckboxList
        assertEquals("used_features", checkboxList.data.key)
        assertEquals(4, checkboxList.data.options.size)
        assertEquals("Chat assistant", checkboxList.data.options[0].label)
        assertEquals("chat", checkboxList.data.options[0].value)
        assertEquals(emptyList<String>(), checkboxList.data.selected)

        val button = response.blocks[6] as AuiBlock.ButtonPrimary
        assertEquals("Submit Feedback", button.data.label)
        assertEquals("poll_submit", button.feedback?.action)
        assertEquals("feature_survey_v2", button.feedback?.params?.get("poll_id"))
    }

    // ── Survey flow v2 (radio_list + checkbox_list in steps) ─────────────────

    @Test
    fun `parse poll-survey-radio-v2`() {
        val json = loadResource("examples/poll-survey-radio-v2.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.SURVEY, response.display)
        assertEquals("Quick Survey", response.surveyTitle)
        assertEquals(3, response.steps.size)

        val step1 = response.steps[0]
        assertEquals("How was your overall experience?", step1.question)
        val radioList = step1.blocks[0] as AuiBlock.RadioList
        assertEquals("experience", radioList.data.key)
        assertEquals(5, radioList.data.options.size)
        assertEquals("Excellent", radioList.data.options[0].label)
        assertEquals("excellent", radioList.data.options[0].value)

        val step2 = response.steps[1]
        assertEquals("What should we focus on improving?", step2.question)
        val checkboxList = step2.blocks[0] as AuiBlock.CheckboxList
        assertEquals("improvements", checkboxList.data.key)
        assertEquals(4, checkboxList.data.options.size)
        assertEquals("Response speed", checkboxList.data.options[0].label)

        val step3 = response.steps[2]
        assertEquals("Anything else you'd like to tell us?", step3.question)
        assertEquals(1, step3.blocks.size)
        assertTrue(step3.blocks[0] is AuiBlock.InputTextSingle)
    }

    // ── Unknown type handling ─────────────────────────────────────────────────

    @Test
    fun `unknown type produces Unknown block instead of throwing`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                { "type": "text", "data": { "text": "Hello" } },
                { "type": "future_component_v9", "data": { "foo": "bar" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        assertEquals(2, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.Text)
        val unknown = response.blocks[1] as AuiBlock.Unknown
        assertEquals("future_component_v9", unknown.type)
    }

    @Test
    fun `unknown block preserves rawData for plugin parsing`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                { "type": "custom_widget", "data": { "title": "Hello", "count": 42 } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        val unknown = response.blocks[0] as AuiBlock.Unknown
        assertEquals("custom_widget", unknown.type)
        assertNotNull(unknown.rawData)
        assertEquals("Hello", unknown.rawData!!.jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals("42", unknown.rawData!!.jsonObject["count"]?.jsonPrimitive?.content)
    }

    @Test
    fun `unknown block without data field has null rawData`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                { "type": "no_data_block" }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        val unknown = response.blocks[0] as AuiBlock.Unknown
        assertEquals("no_data_block", unknown.type)
        assertNull(unknown.rawData)
    }

    @Test
    fun `unknown block preserves feedback alongside rawData`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                {
                  "type": "plugin_button",
                  "data": { "label": "Click me" },
                  "feedback": { "action": "open_url", "params": { "url": "https://example.com" } }
                }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        val unknown = response.blocks[0] as AuiBlock.Unknown
        assertEquals("plugin_button", unknown.type)
        assertNotNull(unknown.rawData)
        assertEquals("Click me", unknown.rawData!!.jsonObject["label"]?.jsonPrimitive?.content)
        assertNotNull(unknown.feedback)
        assertEquals("open_url", unknown.feedback!!.action)
        assertEquals("https://example.com", unknown.feedback!!.params["url"])
    }

    // ── Extra JSON fields ignored ─────────────────────────────────────────────

    @Test
    fun `unknown fields on known blocks are ignored`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                { "type": "text", "data": { "text": "Hi" }, "future_field": "ignored", "id": "blk1" }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        val block = response.blocks[0] as AuiBlock.Text
        assertEquals("Hi", block.data.text)
    }

    @Test
    fun `parse normalizes nested data feedback into top level block feedback`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                {
                  "type": "button_primary",
                  "data": {
                    "label": "Submit Quiz",
                    "feedback": { "action": "submit", "params": { "source": "quiz" } }
                  }
                }
              ]
            }
        """.trimIndent()

        val response = parser.parse(json)

        assertEquals(AuiDisplay.INLINE, response.display)
        val button = response.blocks.single() as AuiBlock.ButtonPrimary
        assertEquals("Submit Quiz", button.data.label)
        assertNotNull(button.feedback)
        assertEquals("submit", button.feedback!!.action)
        assertEquals("quiz", button.feedback!!.params["source"])
    }

    @Test
    fun `parse normalizes nested data feedback inside survey step blocks`() {
        val json = """
            {
              "display": "survey",
              "survey_title": "Quiz",
              "steps": [
                {
                  "question": "Ready?",
                  "blocks": [
                    {
                      "type": "button_primary",
                      "data": {
                        "label": "Submit",
                        "feedback": { "action": "submit", "params": { "step": "1" } }
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = parser.parse(json)

        assertEquals(AuiDisplay.SURVEY, response.display)
        val button = response.steps.single().blocks.single() as AuiBlock.ButtonPrimary
        assertEquals("Submit", button.data.label)
        assertNotNull(button.feedback)
        assertEquals("submit", button.feedback!!.action)
        assertEquals("1", button.feedback!!.params["step"])
    }

    // ── parseOrNull ───────────────────────────────────────────────────────────

    @Test
    fun `parseOrNull returns null for invalid JSON`() {
        assertNull(parser.parseOrNull("not json at all"))
        assertNull(parser.parseOrNull("{}"))
        assertNull(parser.parseOrNull(""))
    }

    @Test
    fun `parseOrNull returns response for valid JSON`() {
        val json = """{"display":"expanded","blocks":[]}"""
        assertNotNull(parser.parseOrNull(json))
    }

    @Test
    fun `parseOrNull preserves quick replies nested option feedback`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                {
                  "type": "quick_replies",
                  "data": {
                    "options": [
                      {
                        "label": "Yes",
                        "feedback": { "action": "submit", "params": { "value": "yes" } }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val response = parser.parseOrNull(json)

        assertNotNull(response)
        val replies = response!!.blocks.single() as AuiBlock.QuickReplies
        assertEquals("submit", replies.data.options.single().feedback?.action)
        assertEquals("yes", replies.data.options.single().feedback?.params?.get("value"))
    }

    @Test
    fun `parseOrNull salvages malformed known blocks as Unknown`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                { "type": "heading", "data": { "text": "Attendance" } },
                {
                  "type": "chart",
                  "data": {
                    "variant": "line",
                    "series": [
                      {
                        "label": "Tickets Sold",
                        "values": [
                          { "x": 1990, "y": 1293 }
                        ]
                      },
                      { "x": 2023, "y": 1050 }
                    ]
                  }
                },
                { "type": "text", "data": { "text": "Recovered after 2020." } }
              ]
            }
        """.trimIndent()

        val response = parser.parseOrNull(json)

        assertNotNull(response)
        assertEquals(AuiDisplay.INLINE, response!!.display)
        assertEquals(3, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.Heading)
        assertTrue(response.blocks[1] is AuiBlock.Unknown)
        assertEquals("chart", (response.blocks[1] as AuiBlock.Unknown).type)
        assertTrue(response.blocks[2] is AuiBlock.Text)
    }

    @Test
    fun `parseOrNull salvages malformed survey steps and blocks`() {
        val json = """
            {
              "display": "survey",
              "survey_title": "Quick check-in",
              "steps": [
                {
                  "question": "How did it go?",
                  "blocks": [
                    {
                      "type": "radio_list",
                      "data": {
                        "key": "rating",
                        "options": [
                          { "label": "Great", "value": "great" }
                        ]
                      }
                    }
                  ]
                },
                {
                  "question": "Show me a trend",
                  "blocks": [
                    {
                      "type": "chart",
                      "data": {
                        "variant": "line",
                        "series": [
                          {
                            "label": "Sales",
                            "values": [
                              { "x": 1, "y": 100 }
                            ]
                          },
                          { "x": 2, "y": 150 }
                        ]
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val response = parser.parseOrNull(json)

        assertNotNull(response)
        assertEquals(AuiDisplay.SURVEY, response!!.display)
        assertEquals(2, response.steps.size)
        assertTrue(response.steps[0].blocks[0] is AuiBlock.RadioList)
        assertTrue(response.steps[1].blocks[0] is AuiBlock.Unknown)
        assertEquals("chart", (response.steps[1].blocks[0] as AuiBlock.Unknown).type)
    }

    // ── Inline display + card stub fields ─────────────────────────────────────

    @Test
    fun `parse inline display roundtrips to AuiDisplay INLINE`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                { "type": "text", "data": { "text": "Quick reply" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals(AuiDisplay.INLINE, response.display)
        assertEquals(1, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.Text)
    }

    @Test
    fun `parse expanded with card_title and card_description populates fields`() {
        val json = """
            {
              "display": "expanded",
              "card_title": "Headphone picks",
              "card_description": "Three top noise-cancelling models",
              "blocks": [
                { "type": "heading", "data": { "text": "Sony WH-1000XM5" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals("Headphone picks", response.cardTitle)
        assertEquals("Three top noise-cancelling models", response.cardDescription)
    }

    @Test
    fun `parse file_content block populates exact artifact fields`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                {
                  "type": "file_content",
                  "data": {
                    "filename": "README.md",
                    "language": "markdown",
                    "title": "Project README",
                    "description": "Setup guide",
                    "content": "# Hello\n\nRun ./gradlew build"
                  }
                }
              ]
            }
        """.trimIndent()

        val response = parser.parse(json)

        assertEquals(AuiDisplay.EXPANDED, response.display)
        assertEquals(1, response.blocks.size)
        val block = response.blocks[0] as AuiBlock.FileContent
        assertEquals(
            FileContentData(
                filename = "README.md",
                language = "markdown",
                title = "Project README",
                description = "Setup guide",
                content = "# Hello\n\nRun ./gradlew build",
            ),
            block.data,
        )
    }

    @Test
    fun `parse expanded without card fields leaves them null`() {
        val json = """
            {
              "display": "expanded",
              "blocks": [
                { "type": "text", "data": { "text": "Hello" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertNull(response.cardTitle)
        assertNull(response.cardDescription)
    }

    // ── Survey parsing ────────────────────────────────────────────────────────

    @Test
    fun `parse survey with survey_title populates surveyTitle`() {
        val json = """
            {
              "display": "survey",
              "survey_title": "My Survey",
              "steps": [
                {
                  "question": "Rate us?",
                  "blocks": [
                    { "type": "input_rating_stars", "data": { "key": "rating" } }
                  ]
                }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals(AuiDisplay.SURVEY, response.display)
        assertEquals("My Survey", response.surveyTitle)
        assertEquals(1, response.steps.size)
        assertEquals("Rate us?", response.steps[0].question)
    }

    @Test
    fun `parse survey step without question leaves it null`() {
        val json = """
            {
              "display": "survey",
              "survey_title": "Quick",
              "steps": [
                {
                  "blocks": [
                    { "type": "input_rating_stars", "data": { "key": "rating" } }
                  ]
                }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertNull(response.steps[0].question)
    }

    @Test
    fun `parse survey step preserves optional label and defaults to null when absent`() {
        val json = """
            {
              "display": "survey",
              "survey_title": "Labeled",
              "steps": [
                {
                  "label": "Rating",
                  "question": "How was it?",
                  "blocks": [
                    { "type": "input_rating_stars", "data": { "key": "rating" } }
                  ]
                },
                {
                  "question": "Anything else?",
                  "blocks": [
                    { "type": "input_text_single", "data": { "key": "comment", "label": "Comment" } }
                  ]
                }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals("Rating", response.steps[0].label)
        assertNull(response.steps[1].label)
    }

    // ── Status variants (info/warning/error) ─────────────────────────────────

    @Test
    fun `parse all four badge severity variants`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                { "type": "badge_info",    "data": { "text": "New" } },
                { "type": "badge_success", "data": { "text": "Verified" } },
                { "type": "badge_warning", "data": { "text": "Low stock" } },
                { "type": "badge_error",   "data": { "text": "Offline" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals(4, response.blocks.size)
        assertEquals("New",        (response.blocks[0] as AuiBlock.BadgeInfo).data.text)
        assertEquals("Verified",   (response.blocks[1] as AuiBlock.BadgeSuccess).data.text)
        assertEquals("Low stock",  (response.blocks[2] as AuiBlock.BadgeWarning).data.text)
        assertEquals("Offline",    (response.blocks[3] as AuiBlock.BadgeError).data.text)
    }

    @Test
    fun `parse all four status banner severity variants`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                { "type": "status_banner_info",    "data": { "text": "FYI" } },
                { "type": "status_banner_success", "data": { "text": "Done!" } },
                { "type": "status_banner_warning", "data": { "text": "Careful" } },
                { "type": "status_banner_error",   "data": { "text": "Failed" } }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertEquals(4, response.blocks.size)
        assertEquals("FYI",     (response.blocks[0] as AuiBlock.StatusBannerInfo).data.text)
        assertEquals("Done!",   (response.blocks[1] as AuiBlock.StatusBannerSuccess).data.text)
        assertEquals("Careful", (response.blocks[2] as AuiBlock.StatusBannerWarning).data.text)
        assertEquals("Failed",  (response.blocks[3] as AuiBlock.StatusBannerError).data.text)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadResource(path: String): String {
        return javaClass.classLoader!!.getResourceAsStream(path)!!
            .bufferedReader()
            .readText()
    }
}
