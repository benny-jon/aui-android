package com.bennyjon.aui.compose

import com.bennyjon.aui.compose.display.buildSheetFormattedEntries
import com.bennyjon.aui.compose.display.getAllStepEntries
import com.bennyjon.aui.compose.display.isTerminalSheetAction
import com.bennyjon.aui.compose.plugin.AuiComponentPlugin
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that a 3-step sheet survey (radio_list → checkbox_list → input_text_single)
 * correctly accumulates all answers into the final consolidated feedback.
 *
 * These tests exercise the same accumulation logic that [SheetFlowDisplay] runs on each step
 * advance: merging registry values into params via the button, extracting entries with
 * [getAllStepEntries], and producing [buildSheetFormattedEntries].
 */
class SheetFeedbackAccumulationTest {

    // ── Test data ─────────────────────────────────────────────────────────────

    private val step1 = AuiStep(
        label = "Experience",
        question = "How was your overall experience?",
        skippable = true,
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
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Next"),
                feedback = AuiFeedback(action = "poll_next_step", params = mapOf("poll_id" to "test_survey")),
            ),
        ),
    )

    private val step2 = AuiStep(
        label = "Improvements",
        question = "What should we focus on improving?",
        skippable = true,
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
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Next"),
                feedback = AuiFeedback(action = "poll_next_step", params = mapOf("poll_id" to "test_survey")),
            ),
        ),
    )

    private val step3 = AuiStep(
        label = "Comments",
        question = "Anything else you'd like to tell us?",
        skippable = true,
        blocks = listOf(
            AuiBlock.InputTextSingle(
                data = InputTextSingleData(
                    key = "comments",
                    label = "Your feedback",
                    placeholder = "Optional — type anything here...",
                ),
            ),
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Submit"),
                feedback = AuiFeedback(action = "poll_complete", params = mapOf("poll_id" to "test_survey")),
            ),
        ),
    )

    private val steps = listOf(step1, step2, step3)

    // ── Full 3-step accumulation ──────────────────────────────────────────────

    @Test
    fun `all three steps answered produces complete feedback with all QA pairs`() {
        val step1Params = mapOf("experience" to "Good", "poll_id" to "test_survey")
        val step2Params = mapOf("improvements" to "Response speed, Visual design", "poll_id" to "test_survey")
        val step3Params = mapOf("comments" to "Love the app!", "poll_id" to "test_survey")

        val accumulatedParams = mutableMapOf<String, String>()
        val accumulatedEntries = mutableListOf<AuiEntry>()

        listOf(step1Params, step2Params, step3Params).forEachIndexed { i, params ->
            val step = steps[i]
            accumulatedParams.putAll(params)
            accumulatedEntries.addAll(getAllStepEntries(step, params))
        }

        assertEquals(3, accumulatedEntries.size)

        // Single-input steps use step.question
        assertEquals("How was your overall experience?", accumulatedEntries[0].question)
        assertEquals("Good", accumulatedEntries[0].answer)

        assertEquals("What should we focus on improving?", accumulatedEntries[1].question)
        assertEquals("Response speed, Visual design", accumulatedEntries[1].answer)

        assertEquals("Anything else you'd like to tell us?", accumulatedEntries[2].question)
        assertEquals("Love the app!", accumulatedEntries[2].answer)

        assertEquals("Good", accumulatedParams["experience"])
        assertEquals("Response speed, Visual design", accumulatedParams["improvements"])
        assertEquals("Love the app!", accumulatedParams["comments"])

        val formattedEntries = buildSheetFormattedEntries(accumulatedEntries, skippedCount = 0)
        assertEquals(
            "How was your overall experience?\nGood\n\n" +
                "What should we focus on improving?\nResponse speed, Visual design\n\n" +
                "Anything else you'd like to tell us?\nLove the app!",
            formattedEntries,
        )
    }

    @Test
    fun `step 1 answered steps 2 and 3 skipped produces partial feedback`() {
        val step1Params = mapOf("experience" to "Excellent", "poll_id" to "test_survey")

        val accumulatedEntries = getAllStepEntries(step1, step1Params).toMutableList()
        assertEquals(1, accumulatedEntries.size)

        val formattedEntries = buildSheetFormattedEntries(accumulatedEntries, skippedCount = 2)
        assertEquals(
            "How was your overall experience?\nExcellent\n\n(2 questions skipped)",
            formattedEntries,
        )
    }

    @Test
    fun `all steps skipped produces Survey skipped`() {
        val formattedEntries = buildSheetFormattedEntries(emptyList(), skippedCount = 3)
        assertEquals("Survey skipped", formattedEntries)
    }

    @Test
    fun `step 2 only answered produces single QA entry`() {
        val step2Params = mapOf("improvements" to "Answer accuracy, More features", "poll_id" to "test_survey")

        // Step 1 skipped — no entry
        // Step 2 answered
        val accumulatedEntries = getAllStepEntries(step2, step2Params).toMutableList()
        // Step 3 skipped — no entry

        assertEquals(1, accumulatedEntries.size)
        assertEquals("What should we focus on improving?", accumulatedEntries[0].question)
        assertEquals("Answer accuracy, More features", accumulatedEntries[0].answer)

        val formattedEntries = buildSheetFormattedEntries(accumulatedEntries, skippedCount = 2)
        assertEquals(
            "What should we focus on improving?\nAnswer accuracy, More features\n\n(2 questions skipped)",
            formattedEntries,
        )
    }

    // ── stepsSkipped / stepsTotal typed fields ────────────────────────────────

    @Test
    fun `finalized feedback includes stepsTotal equal to step count`() {
        val entries = listOf(
            AuiEntry(question = "How was your overall experience?", answer = "Good"),
        )
        val feedback = AuiFeedback(
            action = "poll_complete",
            entries = entries,
            formattedEntries = buildSheetFormattedEntries(entries, skippedCount = 2),
            stepsSkipped = 2,
            stepsTotal = 3,
        )
        assertEquals(3, feedback.stepsTotal)
        assertEquals(2, feedback.stepsSkipped)
    }

    @Test
    fun `all steps answered produces stepsSkipped of zero`() {
        val feedback = AuiFeedback(
            action = "poll_complete",
            stepsSkipped = 0,
            stepsTotal = 3,
        )
        assertEquals(0, feedback.stepsSkipped)
        assertEquals(3, feedback.stepsTotal)
    }

    @Test
    fun `non-sheet feedback has null stepsSkipped and stepsTotal`() {
        val feedback = AuiFeedback(action = "button_tap")
        assertEquals(null, feedback.stepsSkipped)
        assertEquals(null, feedback.stepsTotal)
    }

    // ── getAllStepEntries — multi-input ───────────────────────────────────────

    private val multiInputStep = AuiStep(
        label = "Profile",
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
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Next"),
                feedback = AuiFeedback(action = "submit"),
            ),
        ),
    )

    private val threeInputStep = AuiStep(
        label = "Rating",
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
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Submit"),
                feedback = AuiFeedback(action = "submit"),
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
        val profileParams = mapOf("display_name" to "Benny", "role" to "Developer")
        val ratingParams = mapOf("rating" to "4 stars", "recommend" to "8", "comments" to "Great!")

        val accumulatedEntries = mutableListOf<AuiEntry>()
        accumulatedEntries.addAll(getAllStepEntries(multiInputStep, profileParams))
        accumulatedEntries.addAll(getAllStepEntries(threeInputStep, ratingParams))

        assertEquals(5, accumulatedEntries.size)
        val formatted = buildSheetFormattedEntries(accumulatedEntries, skippedCount = 0)
        assertTrue(formatted.contains("Display name\nBenny"))
        assertTrue(formatted.contains("Your role\nDeveloper"))
        assertTrue(formatted.contains("Overall rating\n4 stars"))
        assertTrue(formatted.contains("Likely to recommend?\n8"))
        assertTrue(formatted.contains("Comments\nGreat!"))
    }

    // ── isTerminalSheetAction ────────────────────────────────────────────────

    @Test
    fun `submit is terminal`() {
        assertTrue(isTerminalSheetAction("submit", step1))
    }

    @Test
    fun `poll_complete is terminal`() {
        assertTrue(isTerminalSheetAction("poll_complete", step1))
    }

    @Test
    fun `poll_submit is terminal`() {
        assertTrue(isTerminalSheetAction("poll_submit", step1))
    }

    @Test
    fun `open_url is not terminal on step with one button`() {
        // step1 has one ButtonPrimary with action "poll_next_step", so open_url doesn't match
        assertFalse(isTerminalSheetAction("open_url", step1))
    }

    @Test
    fun `single button fallback makes its own action terminal`() {
        // step1 has exactly one ButtonPrimary with action "poll_next_step"
        assertTrue(isTerminalSheetAction("poll_next_step", step1))
    }

    @Test
    fun `open_url is not terminal on step with multiple buttons`() {
        val stepWithMultipleButtons = AuiStep(
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Open Link"),
                    feedback = AuiFeedback(action = "open_url"),
                ),
            ),
        )
        assertFalse(isTerminalSheetAction("open_url", stepWithMultipleButtons))
    }

    @Test
    fun `navigate is not terminal`() {
        assertFalse(isTerminalSheetAction("navigate", step1))
    }

    @Test
    fun `secondary button action is not terminal`() {
        val stepWithSecondary = AuiStep(
            blocks = listOf(
                AuiBlock.ButtonSecondary(
                    data = ButtonSecondaryData(label = "Read docs"),
                    feedback = AuiFeedback(action = "open_url"),
                ),
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Next"),
                    feedback = AuiFeedback(action = "submit"),
                ),
            ),
        )
        // open_url is not in TERMINAL_SHEET_ACTIONS and doesn't match the single ButtonPrimary
        assertFalse(isTerminalSheetAction("open_url", stepWithSecondary))
        // submit IS terminal
        assertTrue(isTerminalSheetAction("submit", stepWithSecondary))
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
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
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
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
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
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
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
            label = "Profile",
            blocks = listOf(
                AuiBlock.InputTextSingle(
                    data = InputTextSingleData(key = "name", label = "Your name"),
                ),
                AuiBlock.Unknown(type = "date_picker"),
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
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
