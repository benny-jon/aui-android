package com.bennyjon.aui.compose

import com.bennyjon.aui.compose.display.buildSheetFormattedEntries
import com.bennyjon.aui.compose.internal.buildEntriesFromBlocks
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectMultiData
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.InputSliderData
import com.bennyjon.aui.core.model.data.TextData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackBuildingTest {

    // ── buildEntriesFromBlocks ────────────────────────────────────────────────

    @Test
    fun `buildEntriesFromBlocks returns empty when no headings`() {
        val blocks = listOf(
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "q1", options = emptyList()),
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("q1" to "Yes"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildEntriesFromBlocks pairs heading with following input`() {
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "How was it?")),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "rating", options = emptyList()),
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("rating" to "Great"))
        assertEquals(1, result.size)
        assertEquals("How was it?", result[0].question)
        assertEquals("Great", result[0].answer)
    }

    @Test
    fun `buildEntriesFromBlocks captures all inputs across split blocks`() {
        // Simulates EXPANDED layout: heading in bubbleBlocks, inputs in contentBlocks.
        // When allBlocksForEntries = response.blocks, both are passed here together.
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "What features do you use?")),
            AuiBlock.ChipSelectMulti(
                data = ChipSelectMultiData(key = "features", options = emptyList()),
            ),
            AuiBlock.Heading(data = HeadingData(text = "How likely to recommend us?")),
            AuiBlock.InputSlider(
                data = InputSliderData(key = "nps", label = "0–10", min = 0f, max = 10f),
            ),
        )
        val registry = mapOf("features" to "Chat, Search", "nps" to "8")
        val result = buildEntriesFromBlocks(blocks, registry)

        assertEquals(2, result.size)
        assertEquals("What features do you use?", result[0].question)
        assertEquals("Chat, Search", result[0].answer)
        assertEquals("How likely to recommend us?", result[1].question)
        assertEquals("8", result[1].answer)
    }

    @Test
    fun `buildEntriesFromBlocks skips inputs with no preceding heading`() {
        val blocks = listOf(
            AuiBlock.Text(data = TextData(text = "Preamble text")),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "q1", options = emptyList()),
            ),
            AuiBlock.Heading(data = HeadingData(text = "Second question")),
            AuiBlock.InputSlider(
                data = InputSliderData(key = "q2", label = "scale", min = 0f, max = 10f),
            ),
        )
        val registry = mapOf("q1" to "Yes", "q2" to "7")
        val result = buildEntriesFromBlocks(blocks, registry)

        // q1 has no preceding heading → skipped; q2 has heading → captured
        assertEquals(1, result.size)
        assertEquals("Second question", result[0].question)
        assertEquals("7", result[0].answer)
    }

    @Test
    fun `buildEntriesFromBlocks omits inputs with blank registry values`() {
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "Pick one")),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "choice", options = emptyList()),
            ),
        )
        val result = buildEntriesFromBlocks(blocks, emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildEntriesFromBlocks ignores non-input blocks between heading and input`() {
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "Rate your experience")),
            AuiBlock.Spacer(),
            AuiBlock.Divider(),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "rating", options = emptyList()),
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("rating" to "Good"))
        assertEquals(1, result.size)
        assertEquals("Rate your experience", result[0].question)
    }

    // ── buildSheetFormattedEntries ────────────────────────────────────────────

    @Test
    fun `buildSheetFormattedEntries with all steps answered returns full QA pairs`() {
        val entries = listOf(
            AuiEntry(question = "How was it?", answer = "Great"),
            AuiEntry(question = "What to improve?", answer = "Speed, Design"),
        )
        val result = buildSheetFormattedEntries(entries, skippedCount = 0)
        assertEquals("How was it?\nGreat\n\nWhat to improve?\nSpeed, Design", result)
    }

    @Test
    fun `buildSheetFormattedEntries with all skipped returns Survey skipped`() {
        val result = buildSheetFormattedEntries(emptyList(), skippedCount = 3)
        assertEquals("Survey skipped", result)
    }

    @Test
    fun `buildSheetFormattedEntries with single step skipped returns Survey skipped`() {
        val result = buildSheetFormattedEntries(emptyList(), skippedCount = 1)
        assertEquals("Survey skipped", result)
    }

    @Test
    fun `buildSheetFormattedEntries submitted with no answers returns Survey submitted`() {
        val result = buildSheetFormattedEntries(emptyList(), skippedCount = 0)
        assertEquals("Survey submitted", result)
    }

    @Test
    fun `buildSheetFormattedEntries with first answered and rest skipped shows partial QA and note`() {
        val entries = listOf(AuiEntry(question = "How was it?", answer = "Good"))
        val result = buildSheetFormattedEntries(entries, skippedCount = 2)
        assertEquals("How was it?\nGood\n\n(2 questions skipped)", result)
    }

    @Test
    fun `buildSheetFormattedEntries with one skipped uses singular form`() {
        val entries = listOf(AuiEntry(question = "How was it?", answer = "Good"))
        val result = buildSheetFormattedEntries(entries, skippedCount = 1)
        assertEquals("How was it?\nGood\n\n(1 question skipped)", result)
    }

    @Test
    fun `buildSheetFormattedEntries with multiple answered and none skipped returns clean QA`() {
        val entries = listOf(
            AuiEntry(question = "Q1", answer = "A1"),
            AuiEntry(question = "Q2", answer = "A2"),
            AuiEntry(question = "Q3", answer = "A3"),
        )
        val result = buildSheetFormattedEntries(entries, skippedCount = 0)
        assertEquals("Q1\nA1\n\nQ2\nA2\n\nQ3\nA3", result)
    }
}
