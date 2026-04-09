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
        appendLine(RESPONSE_FORMAT)
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
        appendLine("  Use only these action values in feedback objects.")

        appendLine()
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

    internal const val BUILTIN_SUBMIT_SCHEMA =
        """  submit(payload) — Finalize the user's interaction and send collected input to the host.
                    Used by polls, forms, and any component that collects user input.
                    For multi-step sheet flows, place submit on the final step's button
                    to mark the flow as complete.
                    The payload shape depends on the component (e.g., poll_single sends
                    the selected option id; poll_multi sends a list of option ids)."""

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
        "stepper_horizontal",
        "progress_bar",
        "badge_success",
        "status_banner_success",
    )
}
