package com.bennyjon.aui.compose.display

/**
 * Stable `testTag` identifiers applied to the library-injected survey navigation controls.
 *
 * Exposed as public constants so UI tests (the library's own instrumented tests or a
 * hosting app's) can reliably locate the Back / Next / Submit buttons regardless of label,
 * locale, or theme.
 */
object SurveyTestTags {
    /** Applied to the Back button, present on every step after the first. */
    const val BACK = "aui_survey_back"

    /** Applied to the Next button, present on every step except the last. */
    const val NEXT = "aui_survey_next"

    /** Applied to the Submit button, present only on the last step. */
    const val SUBMIT = "aui_survey_submit"
}
