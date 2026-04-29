package com.bennyjon.aui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bennyjon.aui.compose.components.chart.AuiChart
import com.bennyjon.aui.compose.components.input.AuiCheckboxList
import com.bennyjon.aui.compose.components.input.AuiChipSelectMulti
import com.bennyjon.aui.compose.components.input.AuiChipSelectSingle
import com.bennyjon.aui.compose.components.input.AuiInputRatingStars
import com.bennyjon.aui.compose.components.input.AuiInputSlider
import com.bennyjon.aui.compose.components.input.AuiInputTextSingle
import com.bennyjon.aui.compose.components.input.AuiQuickReplies
import com.bennyjon.aui.compose.components.input.AuiRadioList
import com.bennyjon.aui.compose.components.layout.AuiProgressBar
import com.bennyjon.aui.compose.components.layout.AuiStepperHorizontal
import com.bennyjon.aui.compose.components.status.AuiBadgeInfo
import com.bennyjon.aui.compose.components.status.AuiStatusBannerSuccess
import com.bennyjon.aui.compose.components.table.AuiTable
import com.bennyjon.aui.compose.components.text.AuiFileContent
import com.bennyjon.aui.compose.components.text.AuiText
import com.bennyjon.aui.compose.display.AuiResponseCard
import com.bennyjon.aui.compose.display.AuiSurveyContent
import com.bennyjon.aui.compose.theme.AuiThemeProvider
import com.bennyjon.aui.compose.theme.LocalAuiTheme
import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.AuiStep
import com.bennyjon.aui.core.model.data.BadgeInfoData
import com.bennyjon.aui.core.model.data.BadgeSuccessData
import com.bennyjon.aui.core.model.data.BadgeTone
import com.bennyjon.aui.core.model.data.ChartData
import com.bennyjon.aui.core.model.data.ChartPoint
import com.bennyjon.aui.core.model.data.ChartSeries
import com.bennyjon.aui.core.model.data.ChartVariant
import com.bennyjon.aui.core.model.data.CheckboxListData
import com.bennyjon.aui.core.model.data.ChipOption
import com.bennyjon.aui.core.model.data.ChipSelectMultiData
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.FileContentData
import com.bennyjon.aui.core.model.data.HeadingData
import com.bennyjon.aui.core.model.data.InputRatingStarsData
import com.bennyjon.aui.core.model.data.InputSliderData
import com.bennyjon.aui.core.model.data.InputTextSingleData
import com.bennyjon.aui.core.model.data.ProgressBarData
import com.bennyjon.aui.core.model.data.QuickRepliesData
import com.bennyjon.aui.core.model.data.QuickReplyOption
import com.bennyjon.aui.core.model.data.RadioListData
import com.bennyjon.aui.core.model.data.SelectionOption
import com.bennyjon.aui.core.model.data.StatusBannerSuccessData
import com.bennyjon.aui.core.model.data.StepperHorizontalData
import com.bennyjon.aui.core.model.data.StepperStep
import com.bennyjon.aui.core.model.data.TableCell
import com.bennyjon.aui.core.model.data.TableColumn
import com.bennyjon.aui.core.model.data.TableColumnType
import com.bennyjon.aui.core.model.data.TableData
import com.bennyjon.aui.core.model.data.TableNumberFormat
import com.bennyjon.aui.core.model.data.TextData

private fun sampleSurveySteps(): List<AuiStep> = listOf(
    AuiStep(
        question = "How was your experience?",
        blocks = listOf(
            AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(
                    key = "experience",
                    options = listOf(
                        ChipOption(label = "Great", value = "great"),
                        ChipOption(label = "Good", value = "good"),
                    ),
                ),
            ),
        ),
    ),
    AuiStep(
        question = "Any additional comments?",
        blocks = listOf(
            AuiBlock.InputTextSingle(
                data = InputTextSingleData(
                    key = "comments",
                    label = "Comments",
                    placeholder = "Optional",
                ),
            ),
        ),
    ),
)

@Composable
private fun PreviewColumn(content: @Composable ColumnScope.() -> Unit) {
    AuiThemeProvider {
        Column(
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LocalAuiTheme.current.spacing.medium),
            content = content,
        )
    }
}

@Preview(showBackground = true, name = "Renderer")
@Composable
private fun AuiRendererPreview() {
    AuiThemeProvider {
        AuiRenderer(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                blocks = listOf(
                    AuiBlock.StatusBannerSuccess(
                        data = StatusBannerSuccessData(text = "Survey complete!"),
                    ),
                    AuiBlock.Text(
                        data = TextData(text = "Thanks for your feedback. This helps us make the app better for you."),
                    ),
                    AuiBlock.Heading(
                        data = HeadingData(text = "What's next?"),
                    ),
                    AuiBlock.BadgeSuccess(
                        data = BadgeSuccessData(text = "3 of 3 completed"),
                    ),
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "Survey")
@Composable
private fun AuiSurveyContentPreview() {
    AuiThemeProvider {
        AuiSurveyContent(
            steps = sampleSurveySteps(),
            surveyTitle = "Quick Survey",
            onSubmit = {},
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "Response Cards")
@Composable
private fun AuiResponseCardStatesPreview() {
    PreviewColumn {
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                cardTitle = "Headphone picks",
                cardDescription = "Three top noise-cancelling models compared",
            ),
            onClick = {},
        )
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.SURVEY,
                surveyTitle = "Quick feedback",
                steps = sampleSurveySteps(),
            ),
            onClick = {},
        )
        AuiResponseCard(
            response = AuiResponse(
                display = AuiDisplay.EXPANDED,
                cardTitle = "Currently viewed",
            ),
            onClick = {},
            isActive = true,
        )
    }
}

@Preview(showBackground = true, name = "Text States")
@Composable
private fun AuiTextStatesPreview() {
    PreviewColumn {
        AuiText(
            block = AuiBlock.Text(
                data = TextData(
                    text = "Here is **bold**, *italic*, `code`, and a [link](https://example.com).",
                ),
            ),
        )
        AuiText(
            block = AuiBlock.Text(
                data = TextData(
                    text = "This has **unterminated bold that renders literally.",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Inputs Gallery")
@Composable
private fun AuiInputsGalleryPreview() {
    PreviewColumn {
        AuiChipSelectSingle(
            block = AuiBlock.ChipSelectSingle(
                data = ChipSelectSingleData(
                    key = "experience",
                    label = "How was your experience?",
                    options = listOf(
                        ChipOption(label = "Great", value = "great"),
                        ChipOption(label = "Good", value = "good"),
                        ChipOption(label = "Okay", value = "okay"),
                    ),
                    selected = "good",
                ),
            ),
        )
        AuiChipSelectMulti(
            block = AuiBlock.ChipSelectMulti(
                data = ChipSelectMultiData(
                    key = "features",
                    label = "What features do you use most?",
                    options = listOf(
                        ChipOption(label = "Chat", value = "chat"),
                        ChipOption(label = "Search", value = "search"),
                        ChipOption(label = "Tracking", value = "tracking"),
                    ),
                    selected = listOf("chat", "search"),
                ),
            ),
        )
        AuiRadioList(
            block = AuiBlock.RadioList(
                data = RadioListData(
                    key = "satisfaction",
                    label = "How satisfied are you?",
                    options = listOf(
                        SelectionOption(label = "Very satisfied", value = "very_satisfied"),
                        SelectionOption(label = "Neutral", value = "neutral"),
                        SelectionOption(label = "Not satisfied", value = "not_satisfied"),
                    ),
                    selected = "neutral",
                ),
            ),
        )
        AuiCheckboxList(
            block = AuiBlock.CheckboxList(
                data = CheckboxListData(
                    key = "improvements",
                    label = "What needs improvement?",
                    options = listOf(
                        SelectionOption(label = "Speed", value = "speed"),
                        SelectionOption(label = "Design", value = "design"),
                        SelectionOption(label = "Pricing", value = "pricing"),
                    ),
                    selected = listOf("speed", "design"),
                ),
            ),
        )
        AuiInputTextSingle(
            block = AuiBlock.InputTextSingle(
                data = InputTextSingleData(
                    key = "feedback",
                    label = "Your feedback",
                    placeholder = "Optional",
                ),
            ),
        )
        AuiInputSlider(
            block = AuiBlock.InputSlider(
                data = InputSliderData(
                    key = "nps",
                    label = "0 = Not likely, 10 = Very likely",
                    min = 0f,
                    max = 10f,
                    value = 5f,
                    step = 1f,
                ),
            ),
        )
        AuiInputRatingStars(
            block = AuiBlock.InputRatingStars(
                data = InputRatingStarsData(
                    key = "rating",
                    label = "Tap to rate",
                    value = 3,
                ),
            ),
        )
        AuiQuickReplies(
            block = AuiBlock.QuickReplies(
                data = QuickRepliesData(
                    options = listOf(
                        QuickReplyOption(
                            label = "Yes",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "yes")),
                        ),
                        QuickReplyOption(
                            label = "No",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "no")),
                        ),
                        QuickReplyOption(
                            label = "Maybe",
                            feedback = AuiFeedback(action = "poll_answer", params = mapOf("value" to "maybe")),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "Chart Variants")
@Composable
private fun AuiChartVariantsPreview() {
    PreviewColumn {
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Bar,
                title = "Quiz Scores This Week",
                xLabel = "Day",
                yLabel = "Score %",
                series = listOf(
                    ChartSeries(
                        label = "Score",
                        values = listOf(
                            ChartPoint("Mon", 72f),
                            ChartPoint("Tue", 85f),
                            ChartPoint("Wed", 78f),
                            ChartPoint("Fri", 91f),
                        ),
                    ),
                ),
            ),
        )
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Line,
                title = "Daily Active Users",
                xLabel = "Week",
                yLabel = "Users",
                series = listOf(
                    ChartSeries(
                        label = "Android",
                        values = listOf(
                            ChartPoint("W1", 1200f),
                            ChartPoint("W2", 1500f),
                            ChartPoint("W3", 1350f),
                            ChartPoint("W4", 1800f),
                        ),
                    ),
                    ChartSeries(
                        label = "iOS",
                        values = listOf(
                            ChartPoint("W1", 900f),
                            ChartPoint("W2", 1100f),
                            ChartPoint("W3", 1050f),
                            ChartPoint("W4", 1400f),
                        ),
                    ),
                ),
            ),
        )
        AuiChart(
            data = ChartData(
                variant = ChartVariant.Pie,
                title = "Traffic Sources",
                series = listOf(
                    ChartSeries("Organic", listOf(ChartPoint("Organic", 45f))),
                    ChartSeries("Direct", listOf(ChartPoint("Direct", 28f))),
                    ChartSeries("Referral", listOf(ChartPoint("Referral", 17f))),
                    ChartSeries("Social", listOf(ChartPoint("Social", 10f))),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Table")
@Composable
private fun AuiTablePreview() {
    AuiThemeProvider {
        AuiTable(
            data = TableData(
                title = "Weekly Leaderboard",
                columns = listOf(
                    TableColumn(label = "Player", type = TableColumnType.Text),
                    TableColumn(
                        label = "Score",
                        type = TableColumnType.Number,
                        format = TableNumberFormat.Integer,
                    ),
                    TableColumn(label = "Rating", type = TableColumnType.RatingStars),
                    TableColumn(label = "Status", type = TableColumnType.Badge),
                ),
                rows = listOf(
                    listOf(
                        TableCell.Text("Alice"),
                        TableCell.Number(1280.0),
                        TableCell.RatingStars(5f),
                        TableCell.Badge("Leading", BadgeTone.Success),
                    ),
                    listOf(
                        TableCell.Text("Bob"),
                        TableCell.Number(980.0),
                        TableCell.RatingStars(4.5f),
                        TableCell.Badge("Rising", BadgeTone.Info),
                    ),
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}

@Preview(showBackground = true, name = "Status And Layout")
@Composable
private fun AuiStatusAndLayoutPreview() {
    PreviewColumn {
        AuiStatusBannerSuccess(
            block = AuiBlock.StatusBannerSuccess(
                data = StatusBannerSuccessData(text = "Survey complete!"),
            ),
        )
        AuiBadgeInfo(
            block = AuiBlock.BadgeInfo(
                data = BadgeInfoData(text = "New"),
            ),
        )
        AuiProgressBar(
            block = AuiBlock.ProgressBar(
                data = ProgressBarData(
                    label = "Step 2 of 3",
                    progress = 2f,
                    max = 3f,
                ),
            ),
        )
        AuiStepperHorizontal(
            block = AuiBlock.StepperHorizontal(
                data = StepperHorizontalData(
                    steps = listOf(
                        StepperStep("Experience"),
                        StepperStep("Features"),
                        StepperStep("Feedback"),
                    ),
                    current = 1,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "File Content")
@Composable
private fun AuiFileContentPreview() {
    AuiThemeProvider {
        AuiFileContent(
            block = AuiBlock.FileContent(
                data = FileContentData(
                    filename = "README.md",
                    language = "markdown",
                    title = "Project README",
                    description = "Setup and usage guide",
                    content = "# Hello\n\nRun `./gradlew build`.",
                ),
            ),
            modifier = Modifier.padding(LocalAuiTheme.current.spacing.medium),
        )
    }
}
