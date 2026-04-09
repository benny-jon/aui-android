package com.bennyjon.aui.core

import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Generates the AI system prompt text describing the AUI response format and available components.
 *
 * Include the output of [generate] in your AI assistant's system prompt so it knows how to
 * produce valid AUI JSON responses. The generated text describes the response structure, all
 * supported component types with their data fields, feedback format, and usage guidelines.
 *
 * When a [AuiPluginRegistry] is provided, plugin component schemas and action schemas are
 * automatically included in the output so the AI knows about custom types and actions.
 *
 * The `submit` action is built into AUI as the universal "finalize interaction" action. It is
 * always advertised to the AI in the generated prompt **unless** a host has registered an
 * [AuiActionPlugin] with `action = "submit"`, in which case the host's plugin schema replaces
 * the built-in description (so the AI sees only one source of truth for what `submit` means).
 *
 * At runtime, if no plugin claims `submit`, the action falls through to the host's `onFeedback`
 * callback via the standard chain-of-responsibility routing. Hosts can handle `submit` in one
 * of two ways:
 *
 * **Sheet flow caveat**: Multi-step sheet flows (`SheetFlowDisplay`) consolidate all step
 * interactions into a single terminal [AuiFeedback]. The action name on that terminal feedback
 * comes from the final step's button, which means a host plugin registered for `submit` will
 * only catch sheet completions if the AI places `submit` on the final step's button.
 *
 * For reliable handling of sheet flow completions regardless of action name, hosts should branch
 * on the structural signal [AuiFeedback.stepsTotal] `!= null` inside their `onFeedback` callback,
 * rather than relying solely on action-name dispatch via plugins. See `SheetFlowDisplay` for
 * details on the consolidated feedback shape.
 *
 * 1. **Default**: handle `submit` payloads inside the `onFeedback` callback. No plugin needed.
 * 2. **Override**: register an [AuiActionPlugin] with `action = "submit"` to customize handling,
 *    provide a richer schema to the AI, or short-circuit `onFeedback`.
 *
 * Example:
 * ```kotlin
 * val registry = AuiPluginRegistry().registerAll(
 *     ProductReviewPlugin,
 *     NavigateActionPlugin(navController),
 *     OpenUrlActionPlugin(context),
 * )
 *
 * val systemPrompt = buildString {
 *     append("You are a helpful assistant for our shopping app.\n\n")
 *     append(AuiCatalogPrompt.generate(pluginRegistry = registry))
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
     * @param pluginRegistry The plugin registry containing component and action plugins.
     *   Plugin schemas are automatically included in the generated prompt so the AI
     *   knows about custom component types and available actions.
     * @return A multi-line string describing the AUI format, components, and guidelines.
     */
    fun generate(pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty): String = buildString {
        appendLine(META_FRAME)
        appendLine()
        appendLine(SCHEMA_FORMAT)
        appendLine()
        appendLine(DISPLAY_LEVELS)
        appendLine()
        appendLine(BLOCK_FORMAT)
        appendLine()
        appendLine(FEEDBACK_FORMAT)
        appendLine()
        appendLine(COMPONENTS)

        val componentSchemas = pluginRegistry.allPlugins()
            .filter { it !is AuiActionPlugin && it.promptSchema.isNotBlank() }
        if (componentSchemas.isNotEmpty()) {
            appendLine()
            appendLine("PLUGIN COMPONENTS:")
            appendLine("  (Contributed by the host app — availability varies per app.)")
            componentSchemas.forEach { plugin ->
                appendLine("  ${plugin.promptSchema}")
            }
        }

        appendLine()
        appendLine(SHEET_FIELDS)

        val actionPlugins = pluginRegistry.allActionPlugins()
        val hostHasSubmit = actionPlugins.any { it.action == "submit" }

        appendLine()
        appendLine("AVAILABLE ACTIONS:")
        appendLine(ACTIONS_PREAMBLE)
        appendLine()
        if (!hostHasSubmit) {
            appendLine(BUILTIN_SUBMIT_SCHEMA)
        }
        actionPlugins.forEach { plugin ->
            if (plugin.promptSchema.isNotBlank()) {
                appendLine("  ${plugin.promptSchema}")
            } else {
                appendLine("  - ${plugin.action}")
            }
        }

        appendLine()
        appendLine(GUIDELINES)
        appendLine()
        append(EXAMPLES)
    }

    // Keeping each section as a named constant makes the output easy to test against
    // and keeps the generate() function readable.

    internal const val META_FRAME = """## Interactive UI (AUI) — optional

In addition to normal text responses, you have access to AUI: a format for
responding with interactive native UI components (polls, forms, rating inputs,
quick replies, rich cards, multi-step surveys) that render inline in the chat.

AUI is OPTIONAL and ADDITIVE. Most responses should remain plain text. Only
use AUI when an interactive component genuinely serves the user better than
prose — for example: collecting structured input, offering quick-reply choices,
running a short survey, or displaying a rich card the user will act on.

When you DO use AUI, emit a single JSON object matching the schema below as
your ENTIRE response. No prose wrapper, no markdown code fence, no commentary
before or after — just the raw JSON object.

The feedback loop: when the user interacts with a rendered component
(selects an option, submits a form, taps a quick reply), their interaction
comes back to you as their next user message. Design each AUI response with
that follow-up turn in mind.

Default to plain text. Only emit AUI JSON when a component adds real value.

---"""

    internal const val SCHEMA_FORMAT = """AUI JSON schema:
{
  "display": "inline" | "expanded" | "sheet",
  "blocks": [ ... ]
}

For "sheet" display (multi-step flows), replace "blocks" with "steps" and
add a "sheet_title" at the top level:
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
  { "type": "<component>", "data": { ... }, "feedback": { ... } }
  // feedback only on interactive components"""

    internal const val FEEDBACK_FORMAT = """FEEDBACK (on interactive components):
  { "action": "<registered_id>", "params": { ... } }

  "action" must be a registered action ID (see AVAILABLE ACTIONS below).
  Never invent action names. "params" are passed to the action when the user
  interacts with the component."""

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
    step.question: string — the question this step is asking the user
    step.skippable: boolean — show a Skip button (default true)
    step.blocks[]: blocks for this step"""

    internal const val ACTIONS_PREAMBLE =
        """  Actions are registered by ID. Reference them by name in a component's
  "feedback" object — never invent new action names. The set of available
  actions depends on the host app; additional host-registered actions (if
  any) are listed after the built-in below."""

    internal const val BUILTIN_SUBMIT_SCHEMA =
        """  submit(payload) — Send the user's collected input back as their next
    message. Place on the final button of forms and multi-step flows."""

    internal const val GUIDELINES = """GUIDELINES:
  - Start with text for context, then use components.
  - Use quick_replies at the end to suggest next steps.
  - Keep it concise: 2-5 blocks for inline, 3-8 for expanded, 3-10 per sheet step.
  - Every interactive component MUST have a feedback object.
  - For sheet surveys, set a "question" on each step describing what's being asked."""

    internal const val EXAMPLES = """EXAMPLES:

Inline poll (radio list + submit button):
{
  "display": "inline",
  "blocks": [
    { "type": "text", "data": { "text": "Which feature should we build next?" } },
    { "type": "radio_list", "data": {
        "key": "feature_choice",
        "options": [
          { "label": "Dark mode", "value": "dark_mode" },
          { "label": "Export to PDF", "value": "export_pdf" },
          { "label": "Keyboard shortcuts", "value": "shortcuts" }
        ]
    }},
    {
      "type": "button_primary",
      "data": { "label": "Vote" },
      "feedback": { "action": "submit", "params": {} }
    }
  ]
}

Sheet survey (2-step feedback flow, second step skippable):
{
  "display": "sheet",
  "sheet_title": "Quick feedback",
  "steps": [
    {
      "label": "Rating",
      "question": "How would you rate your experience?",
      "blocks": [
        { "type": "input_rating_stars", "data": { "key": "rating", "label": "Your rating" } },
        {
          "type": "button_primary",
          "data": { "label": "Next" },
          "feedback": { "action": "submit", "params": {} }
        }
      ]
    },
    {
      "label": "Comment",
      "question": "Any additional comments?",
      "skippable": true,
      "blocks": [
        { "type": "input_text_single", "data": {
            "key": "comment",
            "label": "Comments",
            "placeholder": "Tell us more..."
        }},
        {
          "type": "button_primary",
          "data": { "label": "Finish" },
          "feedback": { "action": "submit", "params": {} }
        }
      ]
    }
  ]
}"""

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
        "stepper_horizontal",
        "progress_bar",
        "badge_success",
        "status_banner_success",
    )
}
