package com.bennyjon.aui.compose

import com.bennyjon.aui.compose.display.buildSurveyFormattedEntries
import com.bennyjon.aui.compose.internal.buildEntriesFromBlocks
import com.bennyjon.aui.compose.plugin.AuiComponentPlugin
import com.bennyjon.aui.compose.plugin.AuiComponentPlugin.InputMetadata
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectMultiData
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.InputSliderData
import com.bennyjon.aui.core.model.data.TextData
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import kotlinx.serialization.KSerializer
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
            AuiBlock.Divider(),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "rating", options = emptyList()),
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("rating" to "Good"))
        assertEquals(1, result.size)
        assertEquals("Rate your experience", result[0].question)
    }

    @Test
    fun `buildEntriesFromBlocks keeps collecting inputs under the same heading`() {
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "Tell us about yourself")),
            AuiBlock.InputSlider(
                data = InputSliderData(key = "confidence", label = "Confidence", min = 0f, max = 10f),
            ),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(key = "role", options = emptyList()),
            ),
        )
        val registry = mapOf("confidence" to "8", "role" to "Developer")

        val result = buildEntriesFromBlocks(blocks, registry)

        assertEquals(2, result.size)
        assertEquals("Tell us about yourself", result[0].question)
        assertEquals("8", result[0].answer)
        assertEquals("Tell us about yourself", result[1].question)
        assertEquals("Developer", result[1].answer)
    }

    // ── buildSurveyFormattedEntries ────────────────────────────────────────────

    @Test
    fun `buildSurveyFormattedEntries with all steps answered returns full QA pairs`() {
        val entries = listOf(
            AuiEntry(question = "How was it?", answer = "Great"),
            AuiEntry(question = "What to improve?", answer = "Speed, Design"),
        )
        val result = buildSurveyFormattedEntries(entries, skippedCount = 0)
        assertEquals("How was it?\nGreat\n\nWhat to improve?\nSpeed, Design", result)
    }

    @Test
    fun `buildSurveyFormattedEntries with all skipped returns Survey skipped`() {
        val result = buildSurveyFormattedEntries(emptyList(), skippedCount = 3)
        assertEquals("Survey skipped", result)
    }

    @Test
    fun `buildSurveyFormattedEntries with single step skipped returns Survey skipped`() {
        val result = buildSurveyFormattedEntries(emptyList(), skippedCount = 1)
        assertEquals("Survey skipped", result)
    }

    @Test
    fun `buildSurveyFormattedEntries submitted with no answers returns Survey submitted`() {
        val result = buildSurveyFormattedEntries(emptyList(), skippedCount = 0)
        assertEquals("Survey submitted", result)
    }

    @Test
    fun `buildSurveyFormattedEntries with first answered and rest skipped shows partial QA and note`() {
        val entries = listOf(AuiEntry(question = "How was it?", answer = "Good"))
        val result = buildSurveyFormattedEntries(entries, skippedCount = 2)
        assertEquals("How was it?\nGood\n\n(2 questions skipped)", result)
    }

    @Test
    fun `buildSurveyFormattedEntries with one skipped uses singular form`() {
        val entries = listOf(AuiEntry(question = "How was it?", answer = "Good"))
        val result = buildSurveyFormattedEntries(entries, skippedCount = 1)
        assertEquals("How was it?\nGood\n\n(1 question skipped)", result)
    }

    @Test
    fun `buildSurveyFormattedEntries with multiple answered and none skipped returns clean QA`() {
        val entries = listOf(
            AuiEntry(question = "Q1", answer = "A1"),
            AuiEntry(question = "Q2", answer = "A2"),
            AuiEntry(question = "Q3", answer = "A3"),
        )
        val result = buildSurveyFormattedEntries(entries, skippedCount = 0)
        assertEquals("Q1\nA1\n\nQ2\nA2\n\nQ3\nA3", result)
    }

    // ── buildEntriesFromBlocks with plugin inputs ────────────────────────────

    private fun inputPlugin(
        type: String,
        key: String,
        label: String? = null,
    ): AuiComponentPlugin<PluginInputData> = object : AuiComponentPlugin<PluginInputData>() {
        override val id = type
        override val componentType = type
        override val promptSchema = ""
        override val dataSerializer: KSerializer<PluginInputData> = PluginInputData.serializer()

        override fun inputMetadata(data: PluginInputData): InputMetadata =
            InputMetadata(key = data.key, label = data.label)

        @androidx.compose.runtime.Composable
        override fun Render(
            data: PluginInputData,
            onFeedback: (() -> Unit)?,
            modifier: androidx.compose.ui.Modifier,
        ) = Unit
    }

    @kotlinx.serialization.Serializable
    data class PluginInputData(
        val key: String,
        val label: String? = null,
    )

    @Test
    fun `buildEntriesFromBlocks pairs heading with plugin input`() {
        val registry = AuiPluginRegistry()
        registry.register(inputPlugin(type = "date_picker", key = "dob"))

        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "When were you born?")),
            AuiBlock.Unknown(
                type = "date_picker",
                rawData = kotlinx.serialization.json.buildJsonObject {
                    put("key", kotlinx.serialization.json.JsonPrimitive("dob"))
                    put("label", kotlinx.serialization.json.JsonPrimitive("Date of birth"))
                },
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("dob" to "1990-01-15"), registry)

        assertEquals(1, result.size)
        assertEquals("When were you born?", result[0].question)
        assertEquals("1990-01-15", result[0].answer)
    }

    @Test
    fun `buildEntriesFromBlocks keeps plugin inputs under the same heading`() {
        val registry = AuiPluginRegistry()
        registry.register(inputPlugin(type = "date_picker", key = "dob", label = "Date of birth"))
        registry.register(inputPlugin(type = "time_picker", key = "reminder_time", label = "Reminder time"))

        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "Set your reminder")),
            AuiBlock.Unknown(
                type = "date_picker",
                rawData = kotlinx.serialization.json.buildJsonObject {
                    put("key", kotlinx.serialization.json.JsonPrimitive("dob"))
                    put("label", kotlinx.serialization.json.JsonPrimitive("Date of birth"))
                },
            ),
            AuiBlock.Unknown(
                type = "time_picker",
                rawData = kotlinx.serialization.json.buildJsonObject {
                    put("key", kotlinx.serialization.json.JsonPrimitive("reminder_time"))
                    put("label", kotlinx.serialization.json.JsonPrimitive("Reminder time"))
                },
            ),
        )

        val result = buildEntriesFromBlocks(
            blocks = blocks,
            registry = mapOf(
                "dob" to "1990-01-15",
                "reminder_time" to "09:30",
            ),
            pluginRegistry = registry,
        )

        assertEquals(2, result.size)
        assertEquals("Set your reminder", result[0].question)
        assertEquals("1990-01-15", result[0].answer)
        assertEquals("Set your reminder", result[1].question)
        assertEquals("09:30", result[1].answer)
    }

    @Test
    fun `buildEntriesFromBlocks ignores Unknown without plugin`() {
        val blocks = listOf(
            AuiBlock.Heading(data = HeadingData(text = "Pick a date")),
            AuiBlock.Unknown(
                type = "date_picker",
                rawData = kotlinx.serialization.json.buildJsonObject {
                    put("key", kotlinx.serialization.json.JsonPrimitive("dob"))
                },
            ),
        )
        val result = buildEntriesFromBlocks(blocks, mapOf("dob" to "1990-01-15"))
        assertTrue(result.isEmpty())
    }
}
