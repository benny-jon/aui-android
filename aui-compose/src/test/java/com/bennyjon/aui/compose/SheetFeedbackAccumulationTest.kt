package com.bennyjon.aui.compose

import com.bennyjon.aui.compose.display.buildSheetFormattedEntries
import com.bennyjon.aui.compose.display.getStepAnswer
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiEntry
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.CheckboxListData
import com.bennyjon.aui.core.model.data.InputTextSingleData
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.SelectionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies that a 3-step sheet survey (radio_list → checkbox_list → input_text_single)
 * correctly accumulates all answers into the final consolidated feedback.
 *
 * These tests exercise the same accumulation logic that [SheetFlowDisplay] runs on each step
 * advance: merging registry values into params via the button, extracting the answer with
 * [getStepAnswer], building [AuiEntry] pairs, and producing [buildSheetFormattedEntries].
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

    // ── getStepAnswer ─────────────────────────────────────────────────────────

    @Test
    fun `getStepAnswer extracts radio_list selection from params`() {
        val params = mapOf("experience" to "Good", "poll_id" to "test_survey")
        val answer = getStepAnswer(step1, params)
        assertEquals("Good", answer)
    }

    @Test
    fun `getStepAnswer extracts checkbox_list selections from params`() {
        val params = mapOf("improvements" to "Response speed, Visual design", "poll_id" to "test_survey")
        val answer = getStepAnswer(step2, params)
        assertEquals("Response speed, Visual design", answer)
    }

    @Test
    fun `getStepAnswer extracts text input from params`() {
        val params = mapOf("comments" to "Love the app!", "poll_id" to "test_survey")
        val answer = getStepAnswer(step3, params)
        assertEquals("Love the app!", answer)
    }

    @Test
    fun `getStepAnswer returns null when input key is absent from params`() {
        val answer = getStepAnswer(step1, mapOf("poll_id" to "test_survey"))
        assertEquals(null, answer)
    }

    @Test
    fun `getStepAnswer returns null when input value is blank`() {
        val answer = getStepAnswer(step1, mapOf("experience" to "  "))
        assertEquals(null, answer)
    }

    // ── Full 3-step accumulation ──────────────────────────────────────────────

    @Test
    fun `all three steps answered produces complete feedback with all QA pairs`() {
        // Simulate what AuiButtonPrimary emits: registry merged into params.
        // The registry holds display labels; the button appends the step's own params.
        val step1Params = mapOf("experience" to "Good", "poll_id" to "test_survey")
        val step2Params = mapOf("improvements" to "Response speed, Visual design", "poll_id" to "test_survey")
        val step3Params = mapOf("comments" to "Love the app!", "poll_id" to "test_survey")

        // Replicate SheetFlowDisplay.advance() for each step
        val accumulatedParams = mutableMapOf<String, String>()
        val accumulatedEntries = mutableListOf<AuiEntry>()

        listOf(step1Params, step2Params, step3Params).forEachIndexed { i, params ->
            val step = steps[i]
            accumulatedParams.putAll(params)
            val answer = getStepAnswer(step, params)
            val question = step.question
            if (question != null && answer != null) {
                accumulatedEntries.add(AuiEntry(question = question, answer = answer))
            }
        }

        // All 3 entries captured
        assertEquals(3, accumulatedEntries.size)

        assertEquals("How was your overall experience?", accumulatedEntries[0].question)
        assertEquals("Good", accumulatedEntries[0].answer)

        assertEquals("What should we focus on improving?", accumulatedEntries[1].question)
        assertEquals("Response speed, Visual design", accumulatedEntries[1].answer)

        assertEquals("Anything else you'd like to tell us?", accumulatedEntries[2].question)
        assertEquals("Love the app!", accumulatedEntries[2].answer)

        // params contain all values
        assertEquals("Good", accumulatedParams["experience"])
        assertEquals("Response speed, Visual design", accumulatedParams["improvements"])
        assertEquals("Love the app!", accumulatedParams["comments"])

        // formattedEntries joins all QA pairs
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

        val accumulatedEntries = mutableListOf<AuiEntry>()
        val answer = getStepAnswer(step1, step1Params)
        assertNotNull(answer)
        accumulatedEntries.add(AuiEntry(question = checkNotNull(step1.question), answer = checkNotNull(answer)))

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

        val accumulatedEntries = mutableListOf<AuiEntry>()

        // Step 1 skipped — no entry
        // Step 2 answered
        val answer = getStepAnswer(step2, step2Params)
        assertNotNull(answer)
        accumulatedEntries.add(AuiEntry(question = checkNotNull(step2.question), answer = checkNotNull(answer)))
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
}
