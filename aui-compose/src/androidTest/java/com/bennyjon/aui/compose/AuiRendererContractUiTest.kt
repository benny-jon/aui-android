package com.bennyjon.aui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
import com.bennyjon.aui.core.model.data.ChartData
import com.bennyjon.aui.core.model.data.ChartPoint
import com.bennyjon.aui.core.model.data.ChartSeries
import com.bennyjon.aui.core.model.data.ChartVariant
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.InputTextSingleData
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuiRendererContractUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandedSubmit_buildsEntriesAcrossBubbleAndContentBlocks() {
        val feedbacks = mutableListOf<AuiFeedback>()

        composeTestRule.setContent {
            AuiRenderer(
                response = AuiResponse(
                    display = AuiDisplay.EXPANDED,
                    blocks = listOf(
                        AuiBlock.Heading(data = HeadingData(text = "Tell us about yourself")),
                        AuiBlock.InputTextSingle(
                            data = InputTextSingleData(
                                key = "name",
                                label = "Name",
                            ),
                        ),
                        AuiBlock.ChipSelectSingle(
                            data = ChipSelectSingleData(
                                key = "role",
                                label = "Role",
                                options = listOf(
                                    ChipOption(label = "Developer", value = "dev"),
                                    ChipOption(label = "Designer", value = "design"),
                                ),
                            ),
                        ),
                        AuiBlock.ButtonPrimary(
                            data = ButtonPrimaryData(label = "Continue"),
                            feedback = AuiFeedback(
                                action = "submit_profile",
                                params = mapOf("source" to "release-test"),
                            ),
                        ),
                    ),
                ),
                onFeedback = { feedbacks.add(it) },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Benny")
        composeTestRule.onNodeWithText("Developer").performClick()
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.waitForIdle()

        val feedback = feedbacks.single()
        assertEquals("submit_profile", feedback.action)
        assertEquals("Benny", feedback.params["name"])
        assertEquals("Developer", feedback.params["role"])
        assertEquals("release-test", feedback.params["source"])
        assertEquals(2, feedback.entries.size)
        assertEquals("Tell us about yourself", feedback.entries[0].question)
        assertEquals("Benny", feedback.entries[0].answer)
        assertEquals("Tell us about yourself", feedback.entries[1].question)
        assertEquals("Developer", feedback.entries[1].answer)
        assertEquals(
            "Tell us about yourself\nBenny\n\nTell us about yourself\nDeveloper",
            feedback.formattedEntries,
        )
    }

    @Test
    fun collectingFeedbackDisabled_suppressesInteractiveActions_butAllowsReadOnlyPluginActions() {
        val hostFeedbacks = mutableListOf<AuiFeedback>()
        val readOnlyPluginFeedbacks = mutableListOf<AuiFeedback>()
        val pluginRegistry = AuiPluginRegistry().register(
            object : AuiActionPlugin() {
                override val id = "open-url"
                override val action = "open_url"
                override val promptSchema = "open_url(url)"
                override val isReadOnly = true

                override fun handle(feedback: AuiFeedback): Boolean {
                    readOnlyPluginFeedbacks.add(feedback)
                    return true
                }
            },
        )

        composeTestRule.setContent {
            AuiRenderer(
                response = AuiResponse(
                    display = AuiDisplay.INLINE,
                    blocks = listOf(
                        AuiBlock.ButtonPrimary(
                            data = ButtonPrimaryData(label = "Submit"),
                            feedback = AuiFeedback(action = "submit"),
                        ),
                        AuiBlock.ButtonSecondary(
                            data = ButtonSecondaryData(label = "Open docs"),
                            feedback = AuiFeedback(
                                action = "open_url",
                                params = mapOf("url" to "https://example.com/docs"),
                            ),
                        ),
                    ),
                ),
                pluginRegistry = pluginRegistry,
                collectingFeedbackEnabled = false,
                onFeedback = { hostFeedbacks.add(it) },
            )
        }

        composeTestRule.onNodeWithText("Submit").performClick()
        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        assertTrue("interactive feedback should be suppressed", hostFeedbacks.isEmpty())
        assertEquals(1, readOnlyPluginFeedbacks.size)
        assertEquals("open_url", readOnlyPluginFeedbacks.single().action)
        assertEquals("https://example.com/docs", readOnlyPluginFeedbacks.single().params["url"])
    }

    @Test
    fun unknownBlocks_areReported_withoutBlockingKnownBlocks() {
        val unknownBlocks = mutableListOf<AuiBlock.Unknown>()

        composeTestRule.setContent {
            AuiRenderer(
                json = """
                    {
                      "display": "inline",
                      "blocks": [
                        { "type": "text", "data": { "text": "Known content" } },
                        { "type": "mystery_widget", "data": { "value": 1 } }
                      ]
                    }
                """.trimIndent(),
                onUnknownBlock = { unknownBlocks.add(it) },
            )
        }

        composeTestRule.onNodeWithText("Known content").assertIsDisplayed()
        composeTestRule.waitForIdle()

        assertEquals(1, unknownBlocks.size)
        assertEquals("mystery_widget", unknownBlocks.single().type)
    }

    @Test
    fun malformedJson_reportsParseError_andDoesNotCrash() {
        val parseErrors = mutableListOf<String>()

        composeTestRule.setContent {
            AuiRenderer(
                json = "{",
                onParseError = { parseErrors.add(it) },
            )
        }

        composeTestRule.waitForIdle()

        assertEquals(1, parseErrors.size)
        assertTrue(parseErrors.single().isNotBlank())
    }

    @Test
    fun chartBlock_exposesAccessibleSummary() {
        val expectedDescription =
            "Bar chart: Quarterly revenue. X axis Quarter, Y axis Revenue. Series North: Q1 10, Q2 12"

        composeTestRule.setContent {
            AuiRenderer(
                response = AuiResponse(
                    display = AuiDisplay.INLINE,
                    blocks = listOf(
                        AuiBlock.Chart(
                            data = ChartData(
                                variant = ChartVariant.Bar,
                                title = "Quarterly revenue",
                                xLabel = "Quarter",
                                yLabel = "Revenue",
                                series = listOf(
                                    ChartSeries(
                                        label = "North",
                                        values = listOf(
                                            ChartPoint(x = "Q1", y = 10f),
                                            ChartPoint(x = "Q2", y = 12f),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("Quarterly revenue").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(expectedDescription).assertExists()
    }
}
