package com.bennyjon.aui.compose.display

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bennyjon.aui.compose.AuiRenderer
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.SelectionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UX source-of-truth tests for [AuiSurveyContent].
 *
 * These tests assert the expected user experience of a 3-step survey — they are deliberately
 * written against the spec, not the current implementation. Each step presents a single
 * single-select question. The library (not the AI) is responsible for injecting Back / Next
 * on intermediate steps and Submit on the final step. On Submit, the library emits a single
 * consolidated [AuiFeedback] containing one entry per answered step. Container-level dismissal
 * is a host concern and is not exercised here.
 */
@RunWith(AndroidJUnit4::class)
class AuiSurveyContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val question1 = "What is your favorite color?"
    private val question2 = "Which device do you prefer?"
    private val question3 = "How did you hear about us?"

    private val option1a = "Red"
    private val option1b = "Blue"
    private val option2a = "Phone"
    private val option2b = "Tablet"
    private val option3a = "Friend"
    private val option3b = "Search"

    private fun threeStepSurvey(): AuiResponse = AuiResponse(
        display = AuiDisplay.SURVEY,
        surveyTitle = "Quick Survey",
        steps = listOf(
            AuiStep(
                question = question1,
                blocks = listOf(
                    AuiBlock.RadioList(
                        data = RadioListData(
                            key = "color",
                            options = listOf(
                                SelectionOption(label = option1a, value = "red"),
                                SelectionOption(label = option1b, value = "blue"),
                            ),
                        ),
                    ),
                ),
            ),
            AuiStep(
                question = question2,
                blocks = listOf(
                    AuiBlock.RadioList(
                        data = RadioListData(
                            key = "device",
                            options = listOf(
                                SelectionOption(label = option2a, value = "phone"),
                                SelectionOption(label = option2b, value = "tablet"),
                            ),
                        ),
                    ),
                ),
            ),
            AuiStep(
                question = question3,
                blocks = listOf(
                    AuiBlock.RadioList(
                        data = RadioListData(
                            key = "source",
                            options = listOf(
                                SelectionOption(label = option3a, value = "friend"),
                                SelectionOption(label = option3b, value = "search"),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun firstStep_showsOnlyNextButton_noBackOrSubmit() {
        composeTestRule.setContent {
            AuiRenderer(response = threeStepSurvey(), onFeedback = {})
        }

        composeTestRule.onNodeWithText(question1).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SurveyTestTags.SUBMIT).assertDoesNotExist()
    }

    @Test
    fun middleStep_showsBackAndNext_noSubmit() {
        composeTestRule.setContent {
            AuiRenderer(response = threeStepSurvey(), onFeedback = {})
        }

        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()

        composeTestRule.onNodeWithText(question2).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.SUBMIT).assertDoesNotExist()
    }

    @Test
    fun lastStep_showsBackAndSubmit_noNext() {
        composeTestRule.setContent {
            AuiRenderer(response = threeStepSurvey(), onFeedback = {})
        }

        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()

        composeTestRule.onNodeWithText(question3).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.SUBMIT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).assertDoesNotExist()
    }

    @Test
    fun back_returnsToPreviousStep_fromEachPosition() {
        composeTestRule.setContent {
            AuiRenderer(response = threeStepSurvey(), onFeedback = {})
        }

        // Advance: 1 → 2 → 3.
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithText(question2).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithText(question3).assertIsDisplayed()

        // Step back: 3 → 2.
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).performClick()
        composeTestRule.onNodeWithText(question2).assertIsDisplayed()
        composeTestRule.onNodeWithText(question3).assertDoesNotExist()

        // Step back: 2 → 1. Back should no longer be present on the first step.
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).performClick()
        composeTestRule.onNodeWithText(question1).assertIsDisplayed()
        composeTestRule.onNodeWithText(question2).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).assertDoesNotExist()
    }

    @Test
    fun submit_emitsConsolidatedFeedbackWithEntriesFromEveryStep() {
        val feedbacks = mutableListOf<AuiFeedback>()
        composeTestRule.setContent {
            AuiRenderer(
                response = threeStepSurvey(),
                onFeedback = { feedbacks.add(it) },
            )
        }

        // Step 1: pick Red, advance.
        composeTestRule.onNodeWithText(option1a).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()

        // Step 2: pick Tablet, advance.
        composeTestRule.onNodeWithText(option2b).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()

        // Step 3: pick Friend, submit.
        composeTestRule.onNodeWithText(option3a).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.SUBMIT).performClick()
        composeTestRule.waitForIdle()

        // Exactly one feedback — the consolidated terminal event. Container dismissal is
        // a host concern now; the library keeps its content mounted until the host unmounts it.
        assertEquals("expected a single terminal feedback on submit", 1, feedbacks.size)
        val feedback = feedbacks.single()

        assertEquals("submit", feedback.action)
        assertEquals(3, feedback.stepsTotal)
        assertEquals(0, feedback.stepsSkipped)

        // Entries preserve step declaration order and use each step's question text.
        assertEquals(3, feedback.entries.size)
        assertEquals(question1, feedback.entries[0].question)
        assertEquals(option1a, feedback.entries[0].answer)
        assertEquals(question2, feedback.entries[1].question)
        assertEquals(option2b, feedback.entries[1].answer)
        assertEquals(question3, feedback.entries[2].question)
        assertEquals(option3a, feedback.entries[2].answer)

        // Params carry the human-readable option labels keyed by each input's `key` — the
        // host forwards these to the AI as chat context, so labels (not internal `value`s)
        // are what land in feedback.
        assertEquals(option1a, feedback.params["color"])
        assertEquals(option2b, feedback.params["device"])
        assertEquals(option3a, feedback.params["source"])

        // formattedEntries is a blank-line-separated Q/A summary ready to send to the AI.
        val expectedFormatted = """
            $question1
            $option1a

            $question2
            $option2b

            $question3
            $option3a
        """.trimIndent()
        assertEquals(expectedFormatted, feedback.formattedEntries)
    }

    @Test
    fun submit_nonTerminalStepFeedbackIsNotEmitted_beforeSubmit() {
        // Advancing between steps must not fire the host `onFeedback` — only the final
        // Submit should produce a terminal event. Any internal Next/Back taps are the
        // library's business.
        val feedbacks = mutableListOf<AuiFeedback>()
        composeTestRule.setContent {
            AuiRenderer(
                response = threeStepSurvey(),
                onFeedback = { feedbacks.add(it) },
            )
        }

        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).performClick()
        composeTestRule.waitForIdle()

        assertEquals("navigation taps must not produce host feedbacks", 0, feedbacks.size)
    }

    @Test
    fun answersPersist_whenNavigatingBack() {
        // Selecting an option on step 1, advancing, then returning must preserve the
        // earlier selection. This also exercises the shared registry across steps.
        val feedbacks = mutableListOf<AuiFeedback>()
        composeTestRule.setContent {
            AuiRenderer(
                response = threeStepSurvey(),
                onFeedback = { feedbacks.add(it) },
            )
        }

        composeTestRule.onNodeWithText(option1a).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.BACK).performClick()

        // Navigate forward through the rest, answering steps 2 and 3.
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithText(option2a).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.NEXT).performClick()
        composeTestRule.onNodeWithText(option3b).performClick()
        composeTestRule.onNodeWithTag(SurveyTestTags.SUBMIT).performClick()
        composeTestRule.waitForIdle()

        val feedback = feedbacks.single()
        assertNotNull(feedback.formattedEntries)
        assertNull(
            "step 1 answer should be preserved after a Back trip — no skipped steps",
            feedback.stepsSkipped?.takeIf { it != 0 },
        )
        assertEquals(3, feedback.entries.size)
        assertEquals(option1a, feedback.entries[0].answer)
        assertEquals(option2a, feedback.entries[1].answer)
        assertEquals(option3b, feedback.entries[2].answer)
    }
}
