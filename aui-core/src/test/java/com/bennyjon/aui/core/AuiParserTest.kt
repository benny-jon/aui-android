package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
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

        assertEquals(AuiDisplay.INLINE, response.display)
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

        assertEquals(AuiDisplay.INLINE, response.display)
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
        assertEquals(8, response.blocks.size)

        val chips = response.blocks[2] as AuiBlock.ChipSelectMulti
        assertEquals("features", chips.data.key)
        assertEquals(5, chips.data.options.size)
        assertEquals("chat", chips.data.options[0].value)

        val slider = response.blocks[5] as AuiBlock.InputSlider
        assertEquals("nps", slider.data.key)
        assertEquals(0f, slider.data.min)
        assertEquals(10f, slider.data.max)
        assertEquals(5f, slider.data.value)

        val button = response.blocks[7] as AuiBlock.ButtonPrimary
        assertEquals("Submit Feedback", button.data.label)
        assertEquals("poll_submit", button.feedback?.action)
    }

    // ── Sheet flow ────────────────────────────────────────────────────────────

    @Test
    fun `parse poll-sheet-flow`() {
        val json = loadResource("examples/poll-sheet-flow.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.SHEET, response.display)
        assertEquals("Quick Survey", response.sheetTitle)
        assertEquals(3, response.steps.size)

        val step1 = response.steps[0]
        assertEquals("Experience", step1.label)
        assertEquals("How was your experience?", step1.question)
        assertTrue(step1.skippable)
        val chips = step1.blocks[0] as AuiBlock.ChipSelectSingle
        assertEquals("experience", chips.data.key)
        assertEquals(4, chips.data.options.size)
        val next1 = step1.blocks[1] as AuiBlock.ButtonPrimary
        assertEquals("poll_next_step", next1.feedback?.action)

        val step2 = response.steps[1]
        assertEquals("Features", step2.label)
        val multiChips = step2.blocks[0] as AuiBlock.ChipSelectMulti
        assertEquals("improvements", multiChips.data.key)
        assertEquals(5, multiChips.data.options.size)

        val step3 = response.steps[2]
        assertEquals("Feedback", step3.label)
        assertEquals("Anything else you'd like to tell us?", step3.question)
        val textInput = step3.blocks[0] as AuiBlock.InputTextSingle
        assertEquals("open_feedback", textInput.data.key)
        val submit = step3.blocks[1] as AuiBlock.ButtonPrimary
        assertEquals("poll_complete", submit.feedback?.action)
        assertEquals("onboarding_survey", submit.feedback?.params?.get("poll_id"))
    }

    // ── Confirmation ──────────────────────────────────────────────────────────

    @Test
    fun `parse poll-confirmation`() {
        val json = loadResource("examples/poll-confirmation.json")
        val response = parser.parse(json)

        assertEquals(AuiDisplay.INLINE, response.display)
        assertEquals(3, response.blocks.size)
        assertTrue(response.blocks[0] is AuiBlock.StatusBannerSuccess)
        assertTrue(response.blocks[1] is AuiBlock.Text)
        val badge = response.blocks[2] as AuiBlock.BadgeSuccess
        assertEquals("3 of 3 completed", badge.data.text)
    }

    // ── Unknown type handling ─────────────────────────────────────────────────

    @Test
    fun `unknown type produces Unknown block instead of throwing`() {
        val json = """
            {
              "display": "inline",
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

    // ── Extra JSON fields ignored ─────────────────────────────────────────────

    @Test
    fun `unknown fields on known blocks are ignored`() {
        val json = """
            {
              "display": "inline",
              "blocks": [
                { "type": "text", "data": { "text": "Hi" }, "future_field": "ignored", "id": "blk1" }
              ]
            }
        """.trimIndent()
        val response = parser.parse(json)

        val block = response.blocks[0] as AuiBlock.Text
        assertEquals("Hi", block.data.text)
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
        val json = """{"display":"inline","blocks":[]}"""
        assertNotNull(parser.parseOrNull(json))
    }

    // ── Sheet flow defaults ───────────────────────────────────────────────────

    @Test
    fun `sheetDismissable defaults to true when absent`() {
        val json = """
            {
              "display": "sheet",
              "sheet_title": "My Sheet",
              "steps": []
            }
        """.trimIndent()
        val response = parser.parse(json)
        assertTrue(response.sheetDismissable)
        assertEquals("My Sheet", response.sheetTitle)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadResource(path: String): String {
        return javaClass.classLoader!!.getResourceAsStream(path)!!
            .bufferedReader()
            .readText()
    }
}
