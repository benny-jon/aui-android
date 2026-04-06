package com.bennyjon.aui.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [AuiCatalogPrompt.generate] produces a complete and correct prompt
 * that stays in sync with the actual component catalog.
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

    // ── availableActions parameter ───────────────────────────────────────────

    @Test
    fun `generate without availableActions omits actions section`() {
        assertFalse(output.contains("AVAILABLE ACTIONS:"))
    }

    @Test
    fun `generate with availableActions includes action list`() {
        val actions = listOf("navigate", "add_to_cart", "open_url")
        val result = AuiCatalogPrompt.generate(availableActions = actions)

        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        for (action in actions) {
            assertTrue(
                "Missing action '$action' in generated prompt",
                result.contains("- $action"),
            )
        }
        assertTrue(result.contains("Use only these action values"))
    }

    @Test
    fun `generate with empty availableActions includes empty actions section`() {
        val result = AuiCatalogPrompt.generate(availableActions = emptyList())
        assertTrue(result.contains("AVAILABLE ACTIONS:"))
        assertTrue(result.contains("Use only these action values"))
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
}
