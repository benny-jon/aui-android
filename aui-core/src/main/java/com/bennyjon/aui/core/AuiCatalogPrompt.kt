package com.bennyjon.aui.core

/**
 * Generates the AI system prompt text describing the AUI response format and available components.
 *
 * Include the output of [generate] in your AI assistant's system prompt so it knows how to
 * produce valid AUI JSON responses. The generated text describes the response structure, all
 * supported component types with their data fields, feedback format, and usage guidelines.
 *
 * Example:
 * ```kotlin
 * val systemPrompt = buildString {
 *     append("You are a helpful assistant for our shopping app.\n\n")
 *     append(AuiCatalogPrompt.generate(
 *         availableActions = listOf("navigate", "add_to_cart", "open_url")
 *     ))
 *     append("\n\nAdditional app-specific instructions here...")
 * }
 * ```
 *
 * When the library adds new components, the prompt updates automatically on the next
 * library version bump — no manual schema maintenance required.
 */
object AuiCatalogPrompt {

    /**
     * Returns the full catalog prompt text to include in the AI's system prompt.
     *
     * @param availableActions Optional list of action IDs the host app supports.
     *   If provided, the prompt tells the AI which actions are valid for feedback.
     * @return A multi-line string describing the AUI format, components, and guidelines.
     */
    fun generate(availableActions: List<String>? = null): String = buildString {
        appendLine(RESPONSE_FORMAT)
        appendLine()
        appendLine(DISPLAY_LEVELS)
        appendLine()
        appendLine(BLOCK_FORMAT)
        appendLine()
        appendLine(FEEDBACK_FORMAT)
        appendLine()
        appendLine(COMPONENTS)
        appendLine()
        appendLine(SHEET_FIELDS)
        appendLine()
        if (availableActions != null) {
            appendLine("AVAILABLE ACTIONS:")
            appendLine("  The host app supports these action identifiers in feedback:")
            availableActions.forEach { action ->
                appendLine("  - $action")
            }
            appendLine("  Use only these action values in feedback objects.")
            appendLine()
        }
        append(GUIDELINES)
    }

    // Keeping each section as a named constant makes the output easy to test against
    // and keeps the generate() function readable.

    internal const val RESPONSE_FORMAT = """Response format (inline/expanded):
{
  "display": "inline" | "expanded",
  "blocks": [ ... ]
}

Response format (sheet — multi-step):
{
  "display": "sheet",
  "sheet_title": "...",
  "steps": [
    { "label": "Step name", "question": "Full question text?", "skippable": true, "blocks": [ ... ] }
  ]
}"""

    internal const val DISPLAY_LEVELS = """DISPLAY LEVELS:
  inline   — inside chat bubble. Quick answers, confirmations, simple status.
  expanded — full-width in chat feed. Rich cards, carousels, media, lists.
  sheet    — bottom sheet overlay. Multi-step surveys, forms, focused input.

Choose the LEAST prominent level that serves the content well.
Use "sheet" only when multi-step user input is needed or focused attention is required."""

    internal const val BLOCK_FORMAT = """BLOCK FORMAT:
  { "type": "<component>", "data": { ... }, "feedback": { ... } }"""

    internal const val FEEDBACK_FORMAT = """FEEDBACK (on interactive components):
  { "action": "name", "params": { ... } }
  Do NOT set a "label" field. The library computes the display summary automatically
  from the rendered inputs and the step "question" fields."""

    internal const val COMPONENTS = """AVAILABLE COMPONENTS:

Display:
  text(text)
  heading(text)
  caption(text)

Input:
  button_primary(label)
  button_secondary(label)
  quick_replies(options[]{label, feedback?})
  chip_select_single(key, options[]{label, value}, label?, selected?)
  chip_select_multi(key, options[]{label, value}, label?, selected?)
  radio_list(key, options[]{label, description?, value}, label?, selected?)
  checkbox_list(key, options[]{label, description?, value}, label?, selected?)
  input_text_single(key, label, placeholder?, submit_label?)
  input_slider(key, label, min, max, value?, step?)
  input_rating_stars(key, label?, value?)

Layout:
  divider()
  spacer()

Progress:
  stepper_horizontal(steps[]{label}, current)
  progress_bar(label, progress, max?)

Status:
  badge_success(text)
  status_banner_success(text)"""

    internal const val SHEET_FIELDS = """SHEET-ONLY FIELDS (top-level):
  sheet_title: string — title in the sheet header
  steps[]: array of steps (use instead of blocks for sheet display)
    step.label: string — short label for the stepper indicator
    step.question: string — question recorded in the feedback summary
    step.skippable: boolean — show a Skip button (default false)
    step.blocks[]: blocks for this step"""

    internal const val GUIDELINES = """GUIDELINES:
  - Start with text for context, then use components.
  - Use quick_replies at the end to suggest next steps.
  - Keep it concise: 2-5 blocks for inline, 3-8 for expanded, 3-10 per sheet step.
  - Use text-only responses when components add no value.
  - Every interactive component MUST have a feedback object.
  - Prefer inline. Escalate to expanded for rich content. Use sheet for focused multi-step input.
  - For sheet surveys, set a "question" on each step so the library can build the feedback summary."""

    /**
     * All component type strings supported by the library.
     *
     * This list is used by tests to verify that [COMPONENTS] stays in sync with the
     * actual component catalog. When a new [com.bennyjon.aui.core.model.AuiBlock] subclass
     * is added, add its type string here — the test will fail until you do.
     */
    internal val ALL_COMPONENT_TYPES = listOf(
        "text",
        "heading",
        "caption",
        "chip_select_single",
        "chip_select_multi",
        "button_primary",
        "button_secondary",
        "quick_replies",
        "input_rating_stars",
        "input_text_single",
        "input_slider",
        "radio_list",
        "checkbox_list",
        "divider",
        "spacer",
        "stepper_horizontal",
        "progress_bar",
        "badge_success",
        "status_banner_success",
    )
}
