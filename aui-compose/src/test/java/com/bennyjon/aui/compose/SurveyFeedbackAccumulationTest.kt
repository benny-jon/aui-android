package com.bennyjon.aui.compose

import com.bennyjon.aui.compose.display.buildSurveyFormattedEntries
import com.bennyjon.aui.compose.display.getAllStepEntries
import com.bennyjon.aui.compose.plugin.AuiComponentPlugin
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.CheckboxListData
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.InputRatingStarsData
import com.bennyjon.aui.core.model.data.InputSliderData
import com.bennyjon.aui.core.model.data.InputTextSingleData
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.SelectionOption
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that a 3-step survey (radio_list → checkbox_list → input_text_single)
 * correctly accumulates all answers into the final consolidated feedback.
 *
 * These tests exercise the same accumulation logic that [SurveyFlowDisplay] runs on
 * submit: scanning the shared registry against each step's inputs with
 * [getAllStepEntries] and producing [buildSurveyFormattedEntries].
 */
class SurveyFeedbackAccumulationTest {

    // ── Test data ─────────────────────────────────────────────────────────────

    private val step1 = AuiStep(
        question = "How was your overall experience?",
        blocks = listOf(
            AuiBlock.RadioList(
                data = RadioListData(
                    key = "experience",
                    options = listOf(
                        SelectionOption(label = "Excellent", description = "Exceeded expectations", value = "excellent"),
                        SelectionOption(label = "Good", description = "Met expectations", value = "good"),
                        SelectionOption(label = "Average", value = "average"),
                        SelectionOption(label = "Poor", description = "Below expectations", value = "poor"),
                    ),
                ),
            ),
        ),
    )

    private val step2 = AuiStep(
        question = "What should we focus on improving?",
        blocks = listOf(
            AuiBlock.CheckboxList(
                data = CheckboxListData(
                    key = "improvements",
                    options = listOf(
                        SelectionOption(label = "Response speed", description = "Faster answers", value = "speed"),
                        SelectionOption(label = "Answer accuracy", description = "More precise responses", value = "accuracy"),
                        SelectionOption(label = "Visual design", description = "Cleaner interface", value = "design"),
                        SelectionOption(label = "More features", description = "Additional tools", value = "features"),
                    ),
                ),
            ),
        ),
    )

    private val step3 = AuiStep(
        question = "Anything else you'd like to tell us?",
        blocks = listOf(
            AuiBlock.InputTextSingle(
                data = InputTextSingleData(
                    key = "comments",
                    label = "Your feedback",
                    placeholder = "Optional — type anything here...",
                ),
            ),
        ),
    )

    private val steps = listOf(step1, step2, step3)

    // ── Full 3-step accumulation ──────────────────────────────────────────────

    @Test
    fun `all three steps answered produces complete feedback with all QA pairs`() {
        val registry = mapOf(
            "experience" to "Good",
            "improvements" to "Response speed, Visual design",
            "comments" to "Love the app!",
        )

        val accumulatedEntries = mutableListOf<AuiEntry>()
        for (step in steps) {
            accumulatedEntries.addAll(getAllStepEntries(step, registry))
        }

        assertEquals(3, accumulatedEntries.size)

        // Single-input steps use step.question
        assertEquals("How was your overall experience?", accumulatedEntries[0].question)
        assertEquals("Good", accumulatedEntries[0].answer)

        assertEquals("What should we focus on improving?", accumulatedEntries[1].question)
        assertEquals("Response speed, Visual design", accumulatedEntries[1].answer)

        assertEquals("Anything else you'd like to tell us?", accumulatedEntries[2].question)
        assertEquals("Love the app!", accumulatedEntries[2].answer)

        val formattedEntries = buildSurveyFormattedEntries(accumulatedEntries, skippedCount = 0)
        assertEquals(
            "How was your overall experience?\nGood\n\n" +
                "What should we focus on improving?\nResponse speed, Visual design\n\n" +
                "Anything else you'd like to tell us?\nLove the app!",
            formattedEntries,
        )
    }

    @Test
    fun `step 1 answered steps 2 and 3 skipped produces partial feedback`() {
        val registry = mapOf("experience" to "Excellent")

        val accumulatedEntries = mutableListOf<AuiEntry>()
        var skipped = 0
        for (step in steps) {
            val stepEntries = getAllStepEntries(step, registry)
            if (stepEntries.isEmpty()) skipped++
            accumulatedEntries.addAll(stepEntries)
        }

        assertEquals(1, accumulatedEntries.size)
        assertEquals(2, skipped)

        val formattedEntries = buildSurveyFormattedEntries(accumulatedEntries, skippedCount = skipped)
        assertEquals(
            "How was your overall experience?\nExcellent\n\n(2 questions skipped)",
            formattedEntries,
        )
    }

    @Test
    fun `all steps skipped produces Survey skipped`() {
        val formattedEntries = buildSurveyFormattedEntries(emptyList(), skippedCount = 3)
        assertEquals("Survey skipped", formattedEntries)
    }

    @Test
    fun `empty entries with zero skipped produces Survey submitted`() {
        val formattedEntries = buildSurveyFormattedEntries(emptyList(), skippedCount = 0)
        assertEquals("Survey submitted", formattedEntries)
    }

    @Test
    fun `single skipped uses singular form`() {
        val entries = listOf(AuiEntry(question = "Q1", answer = "A1"))
        val formattedEntries = buildSurveyFormattedEntries(entries, skippedCount = 1)
        assertEquals("Q1\nA1\n\n(1 question skipped)", formattedEntries)
    }

    // ── stepsSkipped / stepsTotal typed fields ────────────────────────────────

    @Test
    fun `finalized feedback includes stepsTotal equal to step count`() {
        val entries = listOf(
            AuiEntry(question = "How was your overall experience?", answer = "Good"),
        )
        val feedback = AuiFeedback(
            action = "submit",
            entries = entries,
            formattedEntries = buildSurveyFormattedEntries(entries, skippedCount = 2),
            stepsSkipped = 2,
            stepsTotal = 3,
        )
        assertEquals(3, feedback.stepsTotal)
        assertEquals(2, feedback.stepsSkipped)
    }

    @Test
    fun `all steps answered produces stepsSkipped of zero`() {
        val feedback = AuiFeedback(
            action = "submit",
            stepsSkipped = 0,
            stepsTotal = 3,
        )
        assertEquals(0, feedback.stepsSkipped)
        assertEquals(3, feedback.stepsTotal)
    }

    @Test
    fun `non-survey feedback has null stepsSkipped and stepsTotal`() {
        val feedback = AuiFeedback(action = "button_tap")
        assertEquals(null, feedback.stepsSkipped)
        assertEquals(null, feedback.stepsTotal)
    }

    // ── getAllStepEntries — multi-input ───────────────────────────────────────

    private val multiInputStep = AuiStep(
        question = "Tell us about yourself.",
        blocks = listOf(
            AuiBlock.InputTextSingle(
                data = InputTextSingleData(key = "display_name", label = "Display name"),
            ),
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(
                    key = "role",
                    label = "Your role",
                    options = listOf(
                        ChipOption(label = "Developer", value = "dev"),
                        ChipOption(label = "Designer", value = "design"),
                    ),
                ),
            ),
        ),
    )

    private val threeInputStep = AuiStep(
        question = "How would you rate AUI?",
        blocks = listOf(
            AuiBlock.InputRatingStars(
                data = InputRatingStarsData(key = "rating", label = "Overall rating"),
            ),
            AuiBlock.InputSlider(
                data = InputSliderData(key = "recommend", label = "Likely to recommend?", min = 0f, max = 10f),
            ),
            AuiBlock.InputTextSingle(
                data = InputTextSingleData(key = "comments", label = "Comments"),
            ),
        ),
    )

    @Test
    fun `getAllStepEntries captures all inputs from a multi-input step`() {
        val params = mapOf("display_name" to "Benny", "role" to "Developer")
        val entries = getAllStepEntries(multiInputStep, params)

        assertEquals(2, entries.size)
        assertEquals("Display name", entries[0].question)
        assertEquals("Benny", entries[0].answer)
        assertEquals("Your role", entries[1].question)
        assertEquals("Developer", entries[1].answer)
    }

    @Test
    fun `getAllStepEntries uses step question for single-input step`() {
        val entries = getAllStepEntries(step1, mapOf("experience" to "Excellent"))

        assertEquals(1, entries.size)
        assertEquals("How was your overall experience?", entries[0].question)
        assertEquals("Excellent", entries[0].answer)
    }

    @Test
    fun `getAllStepEntries skips inputs with blank answers`() {
        val params = mapOf("display_name" to "Benny", "role" to "")
        val entries = getAllStepEntries(multiInputStep, params)

        // Only display_name answered, so it falls back to single-input behavior
        assertEquals(1, entries.size)
        assertEquals("Tell us about yourself.", entries[0].question)
        assertEquals("Benny", entries[0].answer)
    }

    @Test
    fun `getAllStepEntries returns empty when no inputs answered`() {
        val entries = getAllStepEntries(multiInputStep, emptyMap())
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getAllStepEntries captures three inputs with individual labels`() {
        val params = mapOf("rating" to "4 stars", "recommend" to "8", "comments" to "Great!")
        val entries = getAllStepEntries(threeInputStep, params)

        assertEquals(3, entries.size)
        assertEquals("Overall rating", entries[0].question)
        assertEquals("Likely to recommend?", entries[1].question)
        assertEquals("Comments", entries[2].question)
    }

    @Test
    fun `multi-input step accumulation produces all entries in final feedback`() {
        val registry = mapOf(
            "display_name" to "Benny",
            "role" to "Developer",
            "rating" to "4 stars",
            "recommend" to "8",
            "comments" to "Great!",
        )

        val accumulatedEntries = mutableListOf<AuiEntry>()
        accumulatedEntries.addAll(getAllStepEntries(multiInputStep, registry))
        accumulatedEntries.addAll(getAllStepEntries(threeInputStep, registry))

        assertEquals(5, accumulatedEntries.size)
        val formatted = buildSurveyFormattedEntries(accumulatedEntries, skippedCount = 0)
        assertTrue(formatted.contains("Display name\nBenny"))
        assertTrue(formatted.contains("Your role\nDeveloper"))
        assertTrue(formatted.contains("Overall rating\n4 stars"))
        assertTrue(formatted.contains("Likely to recommend?\n8"))
        assertTrue(formatted.contains("Comments\nGreat!"))
    }

    // ── Plugin input support in getAllStepEntries ────────────────────────────

    private fun inputPlugin(
        type: String,
        key: String,
        label: String? = null,
    ): AuiComponentPlugin<String> = object : AuiComponentPlugin<String>() {
        override val id = type
        override val componentType = type
        override val promptSchema = ""
        override val dataSerializer: KSerializer<String> = String.serializer()
        override val inputKey: String = key
        override val inputLabel: String? = label

        @androidx.compose.runtime.Composable
        override fun Render(
            data: String,
            onFeedback: (() -> Unit)?,
            modifier: androidx.compose.ui.Modifier,
        ) = Unit
    }

    @Test
    fun `getAllStepEntries captures plugin input when pluginRegistry provided`() {
        val registry = AuiPluginRegistry()
        registry.register(inputPlugin(type = "date_picker", key = "dob", label = "Date of birth"))

        val step = AuiStep(
            blocks = listOf(
                AuiBlock.Unknown(type = "date_picker"),
            ),
        )
        val params = mapOf("dob" to "1990-01-15")
        val entries = getAllStepEntries(step, params, registry)

        assertEquals(1, entries.size)
        assertEquals("Date of birth", entries[0].question)
        assertEquals("1990-01-15", entries[0].answer)
    }

    @Test
    fun `getAllStepEntries uses inputKey as question when plugin has no inputLabel`() {
        val registry = AuiPluginRegistry()
        registry.register(inputPlugin(type = "color_picker", key = "fav_color"))

        val step = AuiStep(
            blocks = listOf(
                AuiBlock.Unknown(type = "color_picker"),
            ),
        )
        val params = mapOf("fav_color" to "Blue")
        val entries = getAllStepEntries(step, params, registry)

        assertEquals(1, entries.size)
        assertEquals("fav_color", entries[0].question)
        assertEquals("Blue", entries[0].answer)
    }

    @Test
    fun `getAllStepEntries ignores Unknown blocks without plugin`() {
        val step = AuiStep(
            blocks = listOf(
                AuiBlock.Unknown(type = "mystery_input"),
            ),
        )
        val entries = getAllStepEntries(step, mapOf("mystery" to "value"))
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getAllStepEntries mixes built-in and plugin inputs`() {
        val registry = AuiPluginRegistry()
        registry.register(inputPlugin(type = "date_picker", key = "dob", label = "Date of birth"))

        val step = AuiStep(
            blocks = listOf(
                AuiBlock.InputTextSingle(
                    data = InputTextSingleData(key = "name", label = "Your name"),
                ),
                AuiBlock.Unknown(type = "date_picker"),
            ),
        )
        val params = mapOf("name" to "Benny", "dob" to "1990-01-15")
        val entries = getAllStepEntries(step, params, registry)

        assertEquals(2, entries.size)
        assertEquals("Your name", entries[0].question)
        assertEquals("Benny", entries[0].answer)
        assertEquals("Date of birth", entries[1].question)
        assertEquals("1990-01-15", entries[1].answer)
    }
}
