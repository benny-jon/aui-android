package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [AuiCatalogPrompt.generate] produces a complete and correct prompt
 * that stays in sync with the actual component catalog and includes plugin schemas.
 */
class AuiCatalogPromptTest {

    private val output = AuiCatalogPrompt.generate()

    // ── Component coverage ───────────────────────────────────────────────────

    @Test
    fun `generate includes every registered component type`() {
        for (type in AuiCatalogPrompt.ALL_COMPONENT_TYPES) {
            assertTrue(
                "Missing component type '$type' in generated prompt",
                output.contains(type),
            )
        }
    }

    @Test
    fun `ALL_COMPONENT_TYPES matches AuiBlockSerializer type strings`() {
        // This list must match the type→serializer mapping in AuiBlockSerializer.
        // If a new AuiBlock subclass is added but not listed here, this test reminds
        // you to update ALL_COMPONENT_TYPES (which in turn fails the coverage test).
        val expected = setOf(
            "text", "heading", "caption",
            "chip_select_single", "chip_select_multi",
            "button_primary", "button_secondary",
            "quick_replies",
            "input_rating_stars", "input_text_single", "input_slider",
            "radio_list", "checkbox_list",
            "divider",
            "stepper_horizontal", "progress_bar",
            "badge_success", "status_banner_success",
        )
        assertEquals(expected, AuiCatalogPrompt.ALL_COMPONENT_TYPES.toSet())
    }

    // ── Meta-frame ────────────────────────────────────────────────────────────

    @Test
    fun `generate starts with meta-frame`() {
        assertTrue(output.startsWith("## Interactive UI (AUI) — optional"))
    }

    @Test
    fun `generate meta-frame describes envelope format`() {
        assertTrue(output.contains("\"text\": \"Your conversational message here\""))
        assertTrue(output.contains("\"aui\": { ... AUI payload (optional) ... }"))
        assertTrue(output.contains("The \"text\" field is REQUIRED"))
        assertTrue(output.contains("The \"aui\" field is OPTIONAL"))
    }

    @Test
    fun `generate uses consolidated AUI JSON schema header`() {
        assertTrue(output.contains("AUI payload schema"))
        assertTrue(output.contains("For \"sheet\" display (multi-step flows)"))
    }

    @Test
    fun `generate includes feedback-only comment in block format`() {
        assertTrue(output.contains("// feedback only on interactive components"))
    }

    @Test
    fun `generate does not contain redundant action closing line`() {
        assertFalse(output.contains("Use only these action values"))
    }

    @Test
    fun `generate does not contain redundant display-level guideline`() {
        assertFalse(output.contains("Prefer inline. Escalate to expanded"))
    }

    @Test
    fun `generate includes actions preamble`() {
        assertTrue(output.contains("Actions are registered by ID"))
        assertTrue(output.contains("never invent new action names"))
    }

    @Test
    fun `generate includes examples section with envelope format`() {
        assertTrue(output.contains("EXAMPLES:"))
        assertTrue(output.contains("Text-only reply:"))
        assertTrue(output.contains("Expanded poll (radio list + submit button):"))
        assertTrue(output.contains("Sheet survey (2-step feedback flow, second step skippable):"))
        assertTrue(output.contains("\"action\": \"submit\""))
        // Examples use envelope format with "text" and "aui" fields
        assertTrue(output.contains("\"text\": \"Let me know what you think:\""))
        assertTrue(output.contains("\"aui\":"))
    }

    @Test
    fun `generate includes guideline about step question`() {
        assertTrue(output.contains("set a \"question\" on each step describing what's being asked"))
        assertFalse(output.contains("so the library can build the feedback summary"))
    }

    // ── Structural sections ──────────────────────────────────────────────────

    @Test
    fun `generate includes schema format section`() {
        assertTrue(output.contains("\"display\":"))
        assertTrue(output.contains("\"blocks\":"))
        assertTrue(output.contains("\"steps\":"))
    }

    @Test
    fun `generate includes display levels`() {
        assertTrue(output.contains("DISPLAY LEVELS:"))
        assertTrue(output.contains("inline"))
        assertTrue(output.contains("expanded"))
        assertTrue(output.contains("sheet"))
    }

    @Test
    fun `generate documents inline as belonging in chat flow`() {
        assertTrue(output.contains("inline"))
        assertTrue(output.contains("belongs in the chat flow"))
    }

    @Test
    fun `generate documents card_title and card_description for expanded stub`() {
        assertTrue(output.contains("card_title"))
        assertTrue(output.contains("card_description"))
        assertTrue(output.contains("card stub"))
    }

    @Test
    fun `schema format documents inline as a display option`() {
        assertTrue(output.contains("\"display\": \"inline\" | \"expanded\" | \"sheet\""))
    }

    @Test
    fun `examples include an inline display example`() {
        assertTrue(output.contains("Inline quick replies"))
        assertTrue(output.contains("\"display\": \"inline\""))
    }

    @Test
    fun `expanded link buttons example shows card_title and card_description`() {
        assertTrue(output.contains("\"card_title\": \"Headphone picks\""))
        assertTrue(output.contains("\"card_description\":"))
    }

    @Test
    fun `generate includes block format`() {
        assertTrue(output.contains("BLOCK FORMAT:"))
        assertTrue(output.contains("\"type\":"))
        assertTrue(output.contains("\"data\":"))
    }

    @Test
    fun `generate includes feedback format with registered-ID model`() {
        assertTrue(output.contains("FEEDBACK (on interactive components):"))
        assertTrue(output.contains("\"action\": \"<registered_id>\""))
        assertTrue(output.contains("must be a registered action ID"))
        assertTrue(output.contains("Never invent action names"))
        assertFalse(output.contains("\"label\" field"))
        assertFalse(output.contains("display summary automatically"))
    }

    @Test
    fun `generate includes sheet fields`() {
        assertTrue(output.contains("SHEET-ONLY FIELDS"))
        assertTrue(output.contains("sheet_title"))
        assertTrue(output.contains("step.label"))
        assertTrue(output.contains("step.question: string — the question this step is asking the user"))
        assertTrue(output.contains("step.skippable"))
        assertFalse(output.contains("feedback summary"))
    }

    @Test
    fun `generate includes guidelines`() {
        assertTrue(output.contains("GUIDELINES:"))
        assertTrue(output.contains("quick_replies"))
        assertTrue(output.contains("feedback"))
    }

    // ── Component data fields ────────────────────────────────────────────────

    @Test
    fun `chip_select components document key and options fields`() {
        assertTrue(output.contains("chip_select_single(key, options[]{label, value}"))
        assertTrue(output.contains("chip_select_multi(key, options[]{label, value}"))
    }

    @Test
    fun `radio_list and checkbox_list document description field`() {
        assertTrue(output.contains("radio_list(key, options[]{label, description?, value}"))
        assertTrue(output.contains("checkbox_list(key, options[]{label, description?, value}"))
    }

    @Test
    fun `input_slider documents min max fields`() {
        assertTrue(output.contains("input_slider(key, label, min, max"))
    }

    @Test
    fun `input_rating_stars documents key field`() {
        assertTrue(output.contains("input_rating_stars(key"))
    }

    // ── Empty registry (default) ─────────────────────────────────────────────

    @Test
    fun `generate with empty registry omits plugin sections`() {
        assertFalse(output.contains("PLUGIN COMPONENTS:"))
        assertFalse(output.contains("PLUGIN ACTIONS:"))
    }

    @Test
    fun `generate with Empty companion omits plugin sections`() {
        val result = AuiCatalogPrompt.generate(pluginRegistry = AuiPluginRegistry.Empty)
        assertFalse(result.contains("PLUGIN COMPONENTS:"))
        assertFalse(result.contains("PLUGIN ACTIONS:"))
    }

    // ── Plugin component schemas ─────────────────────────────────────────────

    @Test
    fun `generate with component plugin includes its schema and host-app note`() {
        val plugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact, source?) — A colorful fun-fact card.",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("PLUGIN COMPONENTS:"))
        assertTrue(result.contains("Contributed by the host app"))
        assertTrue(result.contains("demo_fun_fact(title, fact, source?) — A colorful fun-fact card."))
    }

    @Test
    fun `generate with multiple component plugins includes all schemas`() {
        val plugin1 = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact) — Fun fact card.",
        )
        val plugin2 = fakeComponentPlugin(
            id = "product_review",
            promptSchema = "card_product_review(title, rating, text) — Product review.",
        )
        val registry = AuiPluginRegistry().registerAll(plugin1, plugin2)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("demo_fun_fact(title, fact) — Fun fact card."))
        assertTrue(result.contains("card_product_review(title, rating, text) — Product review."))
    }

    @Test
    fun `generate skips component plugin with empty promptSchema`() {
        val overridePlugin = fakeComponentPlugin(
            id = "my_card_basic",
            promptSchema = "",
        )
        val registry = AuiPluginRegistry().register(overridePlugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertFalse(result.contains("PLUGIN COMPONENTS:"))
    }

    @Test
    fun `generate skips component plugin with blank promptSchema`() {
        val plugin = fakeComponentPlugin(
            id = "my_card_basic",
            promptSchema = "   ",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertFalse(result.contains("PLUGIN COMPONENTS:"))
    }

    // ── Plugin action schemas ────────────────────────────────────────────────

    @Test
    fun `generate with action plugin includes its schema`() {
        val plugin = fakeActionPlugin(
            action = "navigate",
            promptSchema = "navigate(screen) — Navigate to a named screen",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("navigate(screen) — Navigate to a named screen"))
    }

    @Test
    fun `generate with multiple action plugins includes all schemas`() {
        val plugin1 = fakeActionPlugin(
            action = "navigate",
            promptSchema = "navigate(screen) — Navigate to a named screen",
        )
        val plugin2 = fakeActionPlugin(
            action = "open_url",
            promptSchema = "open_url(url) — Open URL in browser",
        )
        val registry = AuiPluginRegistry().registerAll(plugin1, plugin2)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("navigate(screen) — Navigate to a named screen"))
        assertTrue(result.contains("open_url(url) — Open URL in browser"))
    }

    @Test
    fun `generate with action plugin with empty schema falls back to action name`() {
        val plugin = fakeActionPlugin(
            action = "share_text",
            promptSchema = "",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("- share_text"))
    }

    // ── Mixed registry ───────────────────────────────────────────────────────

    @Test
    fun `generate with mixed registry includes both plugin sections`() {
        val componentPlugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact) — Fun fact card.",
        )
        val actionPlugin = fakeActionPlugin(
            action = "open_url",
            promptSchema = "open_url(url) — Open URL in browser",
        )
        val registry = AuiPluginRegistry().registerAll(componentPlugin, actionPlugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("PLUGIN COMPONENTS:"))
        assertTrue(result.contains("demo_fun_fact(title, fact) — Fun fact card."))
        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("open_url(url) — Open URL in browser"))
    }

    @Test
    fun `generate with only action plugins omits component plugin section`() {
        val plugin = fakeActionPlugin(
            action = "navigate",
            promptSchema = "navigate(screen) — Navigate",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertFalse(result.contains("PLUGIN COMPONENTS:"))
        assertTrue(result.contains("AVAILABLE ACTIONS:"))
    }

    @Test
    fun `generate with only component plugins includes available actions with built-in submit`() {
        val plugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact) — Fun fact card.",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("PLUGIN COMPONENTS:"))
        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("submit(payload)"))
    }

    @Test
    fun `plugin sections appear in correct order relative to built-in sections`() {
        val componentPlugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact) — Fun fact card.",
        )
        val actionPlugin = fakeActionPlugin(
            action = "open_url",
            promptSchema = "open_url(url) — Open URL in browser",
        )
        val registry = AuiPluginRegistry().registerAll(componentPlugin, actionPlugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        val componentsIndex = result.indexOf("AVAILABLE COMPONENTS:")
        val pluginComponentsIndex = result.indexOf("PLUGIN COMPONENTS:")
        val sheetFieldsIndex = result.indexOf("SHEET-ONLY FIELDS")
        val availableActionsIndex = result.indexOf("AVAILABLE ACTIONS:")
        val cheatSheetIndex = result.indexOf("WHEN TO REACH FOR WHICH COMPONENT:")
        val guidelinesIndex = result.indexOf("GUIDELINES:")
        val examplesIndex = result.indexOf("EXAMPLES:")

        // Plugin components appear after built-in components but before sheet fields
        assertTrue(pluginComponentsIndex > componentsIndex)
        assertTrue(pluginComponentsIndex < sheetFieldsIndex)

        // Available actions appear after sheet fields but before cheat sheet
        assertTrue(availableActionsIndex > sheetFieldsIndex)
        assertTrue(availableActionsIndex < cheatSheetIndex)

        // Cheat sheet appears after actions but before guidelines
        assertTrue(cheatSheetIndex < guidelinesIndex)

        // Examples appear after guidelines
        assertTrue(examplesIndex > guidelinesIndex)
    }

    // ── Built-in submit action ──────────────────────────────────────────────

    @Test
    fun `empty registry lists built-in submit in available actions`() {
        assertTrue(output.contains("AVAILABLE ACTIONS:"))
        assertTrue(output.contains("submit(payload)"))
        assertTrue(output.contains("collected input back"))
    }

    @Test
    fun `action plugins without submit still list built-in submit`() {
        val plugin = fakeActionPlugin(
            action = "navigate",
            promptSchema = "navigate(screen) — Navigate to a named screen",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("submit(payload)"))
        assertTrue(result.contains("navigate(screen) — Navigate to a named screen"))
    }

    @Test
    fun `action plugin claiming submit suppresses built-in submit`() {
        val plugin = fakeActionPlugin(
            action = "submit",
            promptSchema = "submit(data, validate?) — Custom submit with validation",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("submit(data, validate?) — Custom submit with validation"))
        assertFalse(result.contains("collected input back"))
    }

    @Test
    fun `multiple action plugins with one claiming submit suppresses built-in`() {
        val navigatePlugin = fakeActionPlugin(
            action = "navigate",
            promptSchema = "navigate(screen) — Navigate to a named screen",
        )
        val submitPlugin = fakeActionPlugin(
            action = "submit",
            promptSchema = "submit(payload, source) — Enhanced submit",
        )
        val registry = AuiPluginRegistry().registerAll(navigatePlugin, submitPlugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("navigate(screen) — Navigate to a named screen"))
        assertTrue(result.contains("submit(payload, source) — Enhanced submit"))
        assertFalse(result.contains("collected input back"))
    }

    // ── Component cheat sheet ───────────────────────────────────────────────

    @Test
    fun `generate includes component cheat sheet`() {
        assertTrue(output.contains("WHEN TO REACH FOR WHICH COMPONENT:"))
        assertTrue(output.contains("button_primary or button_secondary with action=open_url"))
        assertTrue(output.contains("radio_list or chip_select_single + submit"))
        assertTrue(output.contains("input_slider"))
    }

    // ── Collector vs trigger feedback clarification ─────────────────────────

    @Test
    fun `generate clarifies trigger vs collector feedback`() {
        assertTrue(output.contains("Triggers"))
        assertTrue(output.contains("MUST have a"))
        assertTrue(output.contains("feedback object"))
        assertTrue(output.contains("Collectors"))
        assertTrue(output.contains("do NOT need feedback on themselves"))
    }

    // ── New built-in examples ───────────────────────────────────────────────

    @Test
    fun `generate includes expanded tappable link buttons example`() {
        assertTrue(output.contains("Expanded response with tappable link buttons"))
        assertTrue(output.contains("\"display\": \"expanded\""))
        assertTrue(output.contains("View on Amazon"))
        assertTrue(output.contains("action\": \"open_url\""))
    }

    @Test
    fun `generate includes quick replies per-option feedback example`() {
        assertTrue(output.contains("Quick replies with per-option actions"))
        assertTrue(output.contains("Read the docs"))
        assertTrue(output.contains("Explain simply"))
    }

    // ── Aggressiveness ──────────────────────────────────────────────────────

    @Test
    fun `default generate uses Balanced framing`() {
        assertTrue(
            output.contains("Use AUI whenever a component makes the response more useful")
        )
    }

    @Test
    fun `Conservative aggressiveness uses conservative framing`() {
        val result = AuiCatalogPrompt.generate(
            config = AuiPromptConfig(aggressiveness = Aggressiveness.Conservative)
        )
        assertTrue(result.contains("AUI is OPTIONAL and ADDITIVE. Default to plain text"))
        assertFalse(result.contains("Prefer rich AUI components"))
        assertFalse(result.contains("reach for components when they help"))
    }

    @Test
    fun `Balanced aggressiveness uses balanced framing`() {
        val result = AuiCatalogPrompt.generate(
            config = AuiPromptConfig(aggressiveness = Aggressiveness.Balanced)
        )
        assertTrue(result.contains("reach for components when they help"))
        assertFalse(result.contains("Default to plain text"))
        assertFalse(result.contains("When in doubt, use a component"))
    }

    @Test
    fun `Eager aggressiveness uses eager framing`() {
        val result = AuiCatalogPrompt.generate(
            config = AuiPromptConfig(aggressiveness = Aggressiveness.Eager)
        )
        assertTrue(result.contains("Prefer rich AUI components"))
        assertTrue(result.contains("When in doubt, use a component"))
        assertFalse(result.contains("Default to plain text"))
        assertFalse(result.contains("reach for components when they help"))
    }

    @Test
    fun `all aggressiveness levels share envelope format and critical instructions`() {
        for (level in Aggressiveness.entries) {
            val result = AuiCatalogPrompt.generate(
                config = AuiPromptConfig(aggressiveness = level)
            )
            assertTrue(
                "$level missing envelope format",
                result.contains("\"text\": \"Your conversational message here\"")
            )
            assertTrue(
                "$level missing CRITICAL instruction",
                result.contains("CRITICAL: No prose wrapper")
            )
            assertTrue(
                "$level missing feedback loop",
                result.contains("The feedback loop:")
            )
        }
    }

    // ── Custom examples ─────────────────────────────────────────────────────

    @Test
    fun `custom examples are appended after built-in examples`() {
        val config = AuiPromptConfig(
            customExamples = listOf(
                AuiPromptExample(
                    title = "Product comparison",
                    json = """{ "text": "Compare these:", "aui": { "display": "expanded" } }"""
                ),
                AuiPromptExample(
                    title = "Booking flow",
                    json = """{ "text": "Book a room:", "aui": { "display": "sheet" } }"""
                )
            )
        )
        val result = AuiCatalogPrompt.generate(config = config)

        assertTrue(result.contains("Example: Product comparison"))
        assertTrue(result.contains("Compare these:"))
        assertTrue(result.contains("Example: Booking flow"))
        assertTrue(result.contains("Book a room:"))

        // Custom examples appear after built-in examples
        val builtInExampleIndex = result.indexOf("feature_choice")
        val customExample1Index = result.indexOf("Product comparison")
        val customExample2Index = result.indexOf("Booking flow")
        assertTrue(customExample1Index > builtInExampleIndex)
        assertTrue(customExample2Index > customExample1Index)
    }

    @Test
    fun `custom examples never replace built-in examples`() {
        val config = AuiPromptConfig(
            customExamples = listOf(
                AuiPromptExample(title = "My example", json = """{ "text": "test" }""")
            )
        )
        val result = AuiCatalogPrompt.generate(config = config)

        // Built-in signature phrases still present
        assertTrue(result.contains("feature_choice"))
        assertTrue(result.contains("Sheet survey"))
        assertTrue(result.contains("View on Amazon"))
        assertTrue(result.contains("Read the docs"))
        // Custom example also present
        assertTrue(result.contains("Example: My example"))
    }

    @Test
    fun `no custom examples means no extra example section`() {
        val result = AuiCatalogPrompt.generate(config = AuiPromptConfig())
        assertFalse(result.contains("Example: "))
    }

    // ── Default output section coverage ─────────────────────────────────────

    @Test
    fun `default generate includes all expected section headers`() {
        val expectedHeaders = listOf(
            "## Interactive UI (AUI)",
            "AUI payload schema",
            "DISPLAY LEVELS:",
            "BLOCK FORMAT:",
            "FEEDBACK (on interactive components):",
            "AVAILABLE COMPONENTS:",
            "SHEET-ONLY FIELDS",
            "AVAILABLE ACTIONS:",
            "WHEN TO REACH FOR WHICH COMPONENT:",
            "GUIDELINES:",
            "EXAMPLES:",
        )
        for (header in expectedHeaders) {
            assertTrue(
                "Missing section header: '$header'",
                output.contains(header)
            )
        }
    }

    @Test
    fun `default generate is non-empty`() {
        assertTrue(output.isNotEmpty())
        assertTrue(output.length > 1000)
    }

    // ── Plugin schemas still work with config ───────────────────────────────

    @Test
    fun `plugin schemas appear when registry and config are both provided`() {
        val plugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact) — Fun fact card.",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val config = AuiPromptConfig(
            aggressiveness = Aggressiveness.Eager,
            customExamples = listOf(
                AuiPromptExample(title = "Test", json = """{ "text": "test" }""")
            )
        )
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry, config = config)

        assertTrue(result.contains("PLUGIN COMPONENTS:"))
        assertTrue(result.contains("demo_fun_fact(title, fact) — Fun fact card."))
        assertTrue(result.contains("Prefer rich AUI components"))
        assertTrue(result.contains("Example: Test"))
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    /**
     * Creates a fake non-action plugin to simulate component plugins in aui-core tests.
     * (Real AuiComponentPlugin lives in aui-compose and can't be used here.)
     */
    private fun fakeComponentPlugin(
        id: String,
        promptSchema: String,
    ): AuiPlugin = object : AuiPlugin {
        override val id = id
        override val promptSchema = promptSchema
        override val slotKey = id
    }

    private fun fakeActionPlugin(
        action: String,
        promptSchema: String,
    ): AuiActionPlugin = object : AuiActionPlugin() {
        override val id = action
        override val action = action
        override val promptSchema = promptSchema
        override fun handle(feedback: AuiFeedback) = true
    }
}
