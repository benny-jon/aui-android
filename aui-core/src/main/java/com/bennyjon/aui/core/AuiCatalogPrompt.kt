package com.bennyjon.aui.core

import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Controls how eagerly the AI should reach for AUI components vs. plain text.
 *
 * Passed via [AuiPromptConfig.aggressiveness] to [AuiCatalogPrompt.generate]. Each level swaps
 * the framing paragraph near the top of the generated prompt, shifting the model's default
 * disposition without changing the available component set.
 */
enum class Aggressiveness {
    /** "Default to plain text. Only use AUI when it clearly adds value over prose." */
    Conservative,
    /** "Use AUI whenever a component makes the response more useful, actionable, or scannable." */
    Balanced,
    /** "Prefer rich components for responses involving links, lists, comparisons, or choices." */
    Eager
}

/**
 * A domain-specific example appended to the EXAMPLES section of the generated prompt.
 *
 * Built-in examples always remain (they teach envelope format and survey mechanics). Custom
 * examples are appended after them so the model learns domain-specific patterns without
 * losing foundational ones.
 *
 * @param title Short description of what the example demonstrates (rendered as a section label).
 * @param json Raw AUI response JSON — the full envelope, including the "text" field.
 */
data class AuiPromptExample(
    val title: String,
    val json: String
)

/**
 * Tuning knobs for [AuiCatalogPrompt.generate].
 *
 * The library owns the schema (components, actions, envelope format). Host apps own the voice —
 * [aggressiveness] tunes framing tone, and [customExamples] lets hosts teach the model
 * domain-specific patterns.
 *
 * @param aggressiveness Controls how eagerly the AI should reach for AUI components vs. plain
 *   text. Swaps the framing paragraph near the top of the prompt. Defaults to [Aggressiveness.Balanced].
 * @param customExamples Additional examples appended to the built-in EXAMPLES section. Useful for
 *   teaching the model domain-specific patterns (e.g. shopping cards, product lists) without
 *   losing the library's default examples, which teach envelope format and survey mechanics.
 */
data class AuiPromptConfig(
    val aggressiveness: Aggressiveness = Aggressiveness.Balanced,
    val customExamples: List<AuiPromptExample> = emptyList()
)

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
 * **Survey flow caveat**: Multi-page surveys (`SurveyFlowDisplay`) consolidate all step
 * interactions into a single terminal [AuiFeedback]. The library fires this feedback with
 * `action = "submit"` when the user taps the library-injected Submit button on the final
 * page, so a host plugin registered for `submit` will receive every survey completion.
 *
 * For reliable handling of survey completions regardless of action name, hosts should branch
 * on the structural signal [AuiFeedback.stepsTotal] `!= null` inside their `onFeedback` callback,
 * rather than relying solely on action-name dispatch via plugins. See `SurveyFlowDisplay` for
 * details on the consolidated feedback shape.
 *
 * 1. **Default**: handle `submit` payloads inside the `onFeedback` callback. No plugin needed.
 * 2. **Override**: register an [AuiActionPlugin] with `action = "submit"` to customize handling,
 *    provide a richer schema to the AI, or short-circuit `onFeedback`. Note that the library
 *    injects its own Submit button for surveys and always fires `submit` on survey completion —
 *    an override plugin will catch those too.
 *
 * Example:
 * ```kotlin
 * val registry = AuiPluginRegistry().registerAll(
 *     ProductReviewPlugin,
 *     NavigateActionPlugin(navController),
 *     OpenUrlActionPlugin(context),
 * )
 *
 * // Default — balanced tone
 * val systemPrompt = buildString {
 *     append("You are a helpful assistant for our shopping app.\n\n")
 *     append(AuiCatalogPrompt.generate(pluginRegistry = registry))
 * }
 *
 * // Eager tone with domain-specific examples
 * val eagerPrompt = buildString {
 *     append("You are a shopping assistant.\n\n")
 *     append(AuiCatalogPrompt.generate(
 *         pluginRegistry = registry,
 *         config = AuiPromptConfig(
 *             aggressiveness = Aggressiveness.Eager,
 *             customExamples = listOf(
 *                 AuiPromptExample(
 *                     title = "Product comparison",
 *                     json = """{ "text": "Here are your options:", "aui": { ... } }"""
 *                 )
 *             )
 *         )
 *     ))
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
     * @param config Tuning knobs for prompt tone and domain-specific examples.
     *   The library owns the schema; host apps own the voice via this config.
     * @return A multi-line string describing the AUI format, components, and guidelines.
     */
    fun generate(
        pluginRegistry: AuiPluginRegistry = AuiPluginRegistry.Empty,
        config: AuiPromptConfig = AuiPromptConfig(),
    ): String = buildString {
        appendLine(metaFrameFor(config.aggressiveness))
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
        appendLine(SURVEY_STRUCTURE)

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
        appendLine(COMPONENT_CHEAT_SHEET)
        appendLine()
        appendLine(GUIDELINES)
        appendLine()
        append(EXAMPLES)

        if (config.customExamples.isNotEmpty()) {
            appendLine()
            appendLine()
            for (example in config.customExamples) {
                appendLine("Example: ${example.title}")
                append(example.json)
                appendLine()
            }
        }
    }

    // Keeping each section as a named constant makes the output easy to test against
    // and keeps the generate() function readable.

    /**
     * Returns the meta-frame section with framing tone adjusted for [aggressiveness].
     *
     * The envelope format, feedback loop, and critical instructions are identical across
     * all levels — only the "when to use AUI" paragraph changes.
     */
    internal fun metaFrameFor(aggressiveness: Aggressiveness): String {
        val framing = when (aggressiveness) {
            Aggressiveness.Conservative -> FRAMING_CONSERVATIVE
            Aggressiveness.Balanced -> FRAMING_BALANCED
            Aggressiveness.Eager -> FRAMING_EAGER
        }
        return META_FRAME_TEMPLATE.replace("{{FRAMING}}", framing)
    }

    internal const val FRAMING_CONSERVATIVE =
        """AUI is OPTIONAL and ADDITIVE. Default to plain text. Only use AUI when an
interactive component clearly adds value over prose — for example: collecting
structured input, offering quick-reply choices, or running a short survey."""

    internal const val FRAMING_BALANCED =
        """Use AUI whenever a component makes the response more useful, actionable, or
scannable than prose alone. Plain text is fine for pure conversation, explanations,
and single-sentence answers — but reach for components when they help."""

    internal const val FRAMING_EAGER =
        """Prefer rich AUI components for any response involving links, lists, comparisons,
choices, or actionable items. Plain text is for pure conversation and short
explanations. When in doubt, use a component."""

    internal const val META_FRAME_TEMPLATE = """## Interactive UI (AUI) — optional

In addition to normal text responses, you have access to AUI: a format for
responding with interactive native UI components (polls, forms, rating inputs,
quick replies, rich cards, multi-page surveys) that render inline in the chat.

{{FRAMING}}

RESPONSE FORMAT — always respond with a JSON object using this envelope:
{
  "text": "Your conversational message here",
  "aui": { ... AUI payload (optional) ... }
}

The "text" field is REQUIRED — it is your spoken reply shown in the chat bubble.
The "aui" field is OPTIONAL — include it only when you want to render interactive UI.

For text-only replies: { "text": "Hello! How can I help?" }
For AUI replies:       { "text": "Here's a quick poll:", "aui": { "display": "inline", "blocks": [...] } }

CRITICAL: No prose wrapper, no markdown code fence, no commentary before or
after — just the raw JSON object. Never output anything outside this envelope.

The feedback loop: when the user interacts with a rendered component
(selects an option, submits a form, taps a quick reply), their interaction
comes back to you as their next user message. Design each AUI response with
that follow-up turn in mind.

---"""

    internal const val SCHEMA_FORMAT = """AUI payload schema (goes inside the "aui" field of the response envelope):
{
  "display": "inline" | "expanded" | "survey",
  "blocks": [ ... ]
}

For "expanded" display, you may also include an optional card stub used by hosts
that show EXPANDED content as a tappable card opening into a detail surface:
{
  "display": "expanded",
  "card_title": "Short stub title",
  "card_description": "One-line stub subtitle",
  "blocks": [ ... ]
}

For "survey" display (multi-page structured input), replace "blocks" with "steps"
and add a "survey_title":
{
  "display": "survey",
  "survey_title": "...",
  "steps": [
    {
      "label": "Short stepper label (optional)",
      "question": "Full question text?",
      "blocks": [ ... collector(s) ... ]
    }
  ]
}"""

    internal const val DISPLAY_LEVELS = """DISPLAY LEVELS:
  inline   — the default. Renders inside the AI's chat bubble and grows to fit.
             Use for nearly all chat-flow replies: quick replies, polls (even
             multi-option ones), short forms, confirmations, single cards,
             a handful of buttons. There is no size ceiling — a 3-option poll
             with a submit button is inline, not expanded.

  expanded — opt-in upgrade for content the user will want to study OUTSIDE
             the chat flow. Hosts may surface it as a tappable card stub that
             opens a bottom sheet (small screens) or side detail pane (large
             screens). Use ONLY when at least one is true:
               • 3+ rich cards (products, places, events, profiles) in one response
               • Content that benefits from a dedicated reading surface
                 (long comparison tables, image galleries, detailed specs)
               • A response the user will likely want to revisit or reference
             If the content fits naturally in the chat flow, keep it inline.
             When you do use expanded, always include card_title and
             card_description so the stub preview is meaningful.

  survey   — multi-page structured input. Use for polls, quizzes, feedback
             forms, onboarding questionnaires, or any flow that collects
             several answers across multiple questions.

             You only declare the questions and the collector component(s)
             for each. The library handles everything else: step indicator,
             free navigation between questions (back, forward, skip), a
             Submit button on the final question, consolidating answers,
             and sending the final submission back as the user's next turn.

             Every question is skippable — users can submit with any
             subset of questions answered. Unanswered questions simply
             won't appear in the feedback. If a skipped answer matters
             for your follow-up, note it in that turn.

             Do NOT include button_primary, submit actions, or any
             navigation controls in survey steps — the library injects them.
             Just list the questions.

             Use survey ONLY when collecting input across 2+ pages. For
             single-question input, use inline instead.

DEFAULT TO INLINE. Only escalate when the content genuinely needs its own
surface. When in doubt, inline."""

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

    internal const val SURVEY_STRUCTURE = """SURVEY STRUCTURE (when display = "survey"):
  survey_title: string — title shown at the top of the survey
  steps[]: one entry per question (minimum 2)
    step.question: string — the question being asked on this page
    step.blocks[]: the collector component(s) for this question
    step.label: string (optional) — short label shown inside the stepper
      indicator (e.g. "Rating", "Details"). Purely cosmetic — it does not
      affect entries, feedback, or navigation. Omit to fall back to the
      step number.

  No optional flags, no navigation buttons — the library injects Back / Next /
  Submit around each step."""

    internal const val ACTIONS_PREAMBLE =
        """  Actions are registered by ID. Reference them by name in a component's
  "feedback" object — never invent new action names. The set of available
  actions depends on the host app; additional host-registered actions (if
  any) are listed after the built-in below."""

    internal const val BUILTIN_SUBMIT_SCHEMA =
        """  submit(payload) — Send the user's collected input back as their next
    message. Place on the final button of forms and multi-step flows."""

    internal const val COMPONENT_CHEAT_SHEET = """WHEN TO REACH FOR WHICH COMPONENT:
  - Links / URLs → button_primary or button_secondary with action=open_url.
    Never render a URL as plain text when a tappable button is available.
  - Quick conversational branches / single-tap confirmations → inline, quick_replies
  - A poll or pick-one question (any number of options) → inline, radio_list + submit
  - Multi-select preferences → inline, checkbox_list + submit
  - Numeric input within a range → inline, input_slider + submit
  - Rating or single feedback score → inline, input_rating_stars + submit
  - Multi-question feedback / onboarding flow → survey with one question per step
  - A single product / place / link recommendation → inline, one card + button
  - 3+ rich cards in one response (products, places, profiles) → expanded
    (always with card_title + card_description)
  - Long comparison or gallery the user will want to study → expanded
  - Suggesting next actions → inline, quick_replies at the end"""

    internal const val GUIDELINES = """GUIDELINES:
  - Start with text for context, then use components.
  - Use quick_replies at the end to suggest next steps.
  - Choose display by content type, not block count — a 5-block inline poll is
    still inline. Reach for expanded only for 3+ rich cards or content that
    genuinely benefits from a dedicated reading surface. Keep each survey step
    focused on a single question.
  - Triggers (button_primary, button_secondary, quick_replies options) MUST have a
    feedback object — they fire actions when tapped.
  - Collectors (radio_list, checkbox_list, chip_select_*, input_*) passively gather
    state and do NOT need feedback on themselves. Pair them with a trigger button
    whose feedback carries the collected input via submit.
  - For surveys, set a "question" on each step and include only the collector
    component(s) in that step's blocks. Do NOT add button_primary or submit —
    the library injects navigation (Back/Next/Submit) around each step."""

    internal const val EXAMPLES = """EXAMPLES:

Text-only reply:
{ "text": "Sure, I can help with that!" }

Inline quick replies (chat-flow follow-up):
{
  "text": "Anything else I can do?",
  "aui": {
    "display": "inline",
    "blocks": [
      { "type": "quick_replies", "data": {
          "options": [
            { "label": "Yes",
              "feedback": { "action": "submit", "params": { "value": "yes" } } },
            { "label": "No thanks",
              "feedback": { "action": "submit", "params": { "value": "no" } } }
          ]
      }}
    ]
  }
}

Inline poll (radio list + submit button):
{
  "text": "Let me know what you think:",
  "aui": {
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
}

Survey (2-step feedback flow — library injects Back/Next/Submit):
{
  "text": "I'd love your feedback!",
  "aui": {
    "display": "survey",
    "survey_title": "Quick feedback",
    "steps": [
      {
        "question": "How would you rate your experience?",
        "blocks": [
          { "type": "input_rating_stars", "data": { "key": "rating", "label": "Your rating" } }
        ]
      },
      {
        "question": "Any additional comments?",
        "blocks": [
          { "type": "input_text_single", "data": {
              "key": "comment",
              "label": "Comments",
              "placeholder": "Tell us more..."
          }}
        ]
      }
    ]
  }
}

Expanded response with tappable link buttons (product recommendations):
{
  "text": "Here are three solid options:",
  "aui": {
    "display": "expanded",
    "card_title": "Headphone picks",
    "card_description": "Three top noise-cancelling models compared",
    "blocks": [
      { "type": "heading", "data": { "text": "Sony WH-1000XM5" } },
      { "type": "text", "data": { "text": "Top-tier noise cancellation, around $348." } },
      { "type": "button_primary",
        "data": { "label": "View on Amazon" },
        "feedback": { "action": "open_url", "params": { "url": "https://example.com/sony" } }
      },
      { "type": "divider" },
      { "type": "heading", "data": { "text": "Bose QuietComfort Ultra" } },
      { "type": "text", "data": { "text": "Excellent comfort for long wear, around $379." } },
      { "type": "button_primary",
        "data": { "label": "View on Amazon" },
        "feedback": { "action": "open_url", "params": { "url": "https://example.com/bose" } }
      }
    ]
  }
}

Quick replies with per-option actions (each chip fires its own feedback):
{
  "text": "Want to learn more?",
  "aui": {
    "display": "expanded",
    "blocks": [
      { "type": "quick_replies", "data": {
          "options": [
            { "label": "Read the docs",
              "feedback": { "action": "open_url", "params": { "url": "https://example.com/docs" } } },
            { "label": "See examples",
              "feedback": { "action": "open_url", "params": { "url": "https://example.com/examples" } } },
            { "label": "Explain simply",
              "feedback": { "action": "submit", "params": { "topic": "Explain simply" } } }
          ]
      }}
    ]
  }
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
