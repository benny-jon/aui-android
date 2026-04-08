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
            "divider", "spacer",
            "stepper_horizontal", "progress_bar",
            "badge_success", "status_banner_success",
        )
        assertEquals(expected, AuiCatalogPrompt.ALL_COMPONENT_TYPES.toSet())
    }

    // ── Structural sections ──────────────────────────────────────────────────

    @Test
    fun `generate includes response format section`() {
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
    fun `generate includes block format`() {
        assertTrue(output.contains("BLOCK FORMAT:"))
        assertTrue(output.contains("\"type\":"))
        assertTrue(output.contains("\"data\":"))
    }

    @Test
    fun `generate includes feedback format`() {
        assertTrue(output.contains("FEEDBACK"))
        assertTrue(output.contains("\"action\":"))
        assertTrue(output.contains("Do NOT set a \"label\" field"))
    }

    @Test
    fun `generate includes sheet fields`() {
        assertTrue(output.contains("SHEET-ONLY FIELDS"))
        assertTrue(output.contains("sheet_title"))
        assertTrue(output.contains("step.label"))
        assertTrue(output.contains("step.question"))
        assertTrue(output.contains("step.skippable"))
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
    fun `generate with component plugin includes its schema`() {
        val plugin = fakeComponentPlugin(
            id = "fun_fact",
            promptSchema = "demo_fun_fact(title, fact, source?) — A colorful fun-fact card.",
        )
        val registry = AuiPluginRegistry().register(plugin)
        val result = AuiCatalogPrompt.generate(pluginRegistry = registry)

        assertTrue(result.contains("PLUGIN COMPONENTS:"))
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
        assertTrue(result.contains("Use only these action values"))
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
        val guidelinesIndex = result.indexOf("GUIDELINES:")

        // Plugin components appear after built-in components but before sheet fields
        assertTrue(pluginComponentsIndex > componentsIndex)
        assertTrue(pluginComponentsIndex < sheetFieldsIndex)

        // Available actions appear after sheet fields but before guidelines
        assertTrue(availableActionsIndex > sheetFieldsIndex)
        assertTrue(availableActionsIndex < guidelinesIndex)
    }

    // ── Built-in submit action ──────────────────────────────────────────────

    @Test
    fun `empty registry lists built-in submit in available actions`() {
        assertTrue(output.contains("AVAILABLE ACTIONS:"))
        assertTrue(output.contains("submit(payload)"))
        assertTrue(output.contains("Finalize the user's interaction"))
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
        assertFalse(result.contains("Finalize the user's interaction"))
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
        assertFalse(result.contains("Finalize the user's interaction"))
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
