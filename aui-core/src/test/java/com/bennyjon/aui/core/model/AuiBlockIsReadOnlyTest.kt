package com.bennyjon.aui.core.model

import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.ButtonSecondaryData
import com.bennyjon.aui.core.model.data.ChipSelectSingleData
import com.bennyjon.aui.core.model.data.QuickRepliesData
import com.bennyjon.aui.core.model.data.QuickReplyOption
import com.bennyjon.aui.core.model.data.TextData
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuiBlockIsReadOnlyTest {

    private val readOnlyPlugin = object : AuiActionPlugin() {
        override val id = "open_url"
        override val action = "open_url"
        override val isReadOnly = true
        override val promptSchema = ""
        override fun handle(feedback: AuiFeedback) = true
    }

    private val interactivePlugin = object : AuiActionPlugin() {
        override val id = "add_to_cart"
        override val action = "add_to_cart"
        override val promptSchema = ""
        override fun handle(feedback: AuiFeedback) = true
    }

    private val registry = AuiPluginRegistry().registerAll(readOnlyPlugin, interactivePlugin)

    // ── Blocks with no feedback ────────────────────────────────────────────

    @Test
    fun `text block with no feedback is read-only`() {
        val block = AuiBlock.Text(data = TextData(text = "Hello"))
        assertTrue(block.isReadOnly(registry))
    }

    @Test
    fun `button with no feedback is read-only`() {
        val block = AuiBlock.ButtonPrimary(data = ButtonPrimaryData(label = "Click"))
        assertTrue(block.isReadOnly(registry))
    }

    // ── Blocks with submit feedback ────────────────────────────────────────

    @Test
    fun `button with submit action is not read-only`() {
        val block = AuiBlock.ButtonPrimary(
            data = ButtonPrimaryData(label = "Submit"),
            feedback = AuiFeedback(action = "submit"),
        )
        assertFalse(block.isReadOnly(registry))
    }

    @Test
    fun `secondary button with submit action is not read-only`() {
        val block = AuiBlock.ButtonSecondary(
            data = ButtonSecondaryData(label = "Submit"),
            feedback = AuiFeedback(action = "submit"),
        )
        assertFalse(block.isReadOnly(registry))
    }

    // ── Blocks with read-only plugin action ────────────────────────────────

    @Test
    fun `button with read-only plugin action is read-only`() {
        val block = AuiBlock.ButtonPrimary(
            data = ButtonPrimaryData(label = "Open Link"),
            feedback = AuiFeedback(action = "open_url", params = mapOf("url" to "https://example.com")),
        )
        assertTrue(block.isReadOnly(registry))
    }

    // ── Blocks with interactive plugin action ──────────────────────────────

    @Test
    fun `button with interactive plugin action is not read-only`() {
        val block = AuiBlock.ButtonPrimary(
            data = ButtonPrimaryData(label = "Add to Cart"),
            feedback = AuiFeedback(action = "add_to_cart"),
        )
        assertFalse(block.isReadOnly(registry))
    }

    // ── Blocks with unknown action ─────────────────────────────────────────

    @Test
    fun `button with unknown action and no plugin is not read-only`() {
        val block = AuiBlock.ButtonPrimary(
            data = ButtonPrimaryData(label = "Mystery"),
            feedback = AuiFeedback(action = "unknown_action"),
        )
        assertFalse(block.isReadOnly(registry))
    }

    // ── QuickReplies with nested feedback ──────────────────────────────────

    @Test
    fun `quick replies with submit options is not read-only`() {
        val block = AuiBlock.QuickReplies(
            data = QuickRepliesData(
                options = listOf(
                    QuickReplyOption(label = "Yes", feedback = AuiFeedback(action = "submit")),
                    QuickReplyOption(label = "No", feedback = AuiFeedback(action = "submit")),
                ),
            ),
        )
        assertFalse(block.isReadOnly(registry))
    }

    @Test
    fun `quick replies with only read-only options is read-only`() {
        val block = AuiBlock.QuickReplies(
            data = QuickRepliesData(
                options = listOf(
                    QuickReplyOption(label = "Link 1", feedback = AuiFeedback(action = "open_url")),
                    QuickReplyOption(label = "Link 2", feedback = AuiFeedback(action = "open_url")),
                ),
            ),
        )
        assertTrue(block.isReadOnly(registry))
    }

    @Test
    fun `quick replies with mixed read-only and submit options is not read-only`() {
        val block = AuiBlock.QuickReplies(
            data = QuickRepliesData(
                options = listOf(
                    QuickReplyOption(label = "Open", feedback = AuiFeedback(action = "open_url")),
                    QuickReplyOption(label = "Submit", feedback = AuiFeedback(action = "submit")),
                ),
            ),
        )
        assertFalse(block.isReadOnly(registry))
    }

    @Test
    fun `quick replies with no feedback on options is read-only`() {
        val block = AuiBlock.QuickReplies(
            data = QuickRepliesData(
                options = listOf(
                    QuickReplyOption(label = "A"),
                    QuickReplyOption(label = "B"),
                ),
            ),
        )
        assertTrue(block.isReadOnly(registry))
    }

    // ── Input blocks ───────────────────────────────────────────────────────

    @Test
    fun `chip select with no feedback is read-only`() {
        val block = AuiBlock.ChipSelectSingle(
            data = ChipSelectSingleData(
                key = "q1",
                options = listOf(),
            ),
        )
        assertTrue(block.isReadOnly(registry))
    }

    // ── Empty registry ─────────────────────────────────────────────────────

    @Test
    fun `block with no feedback is read-only with empty registry`() {
        val block = AuiBlock.Text(data = TextData(text = "Hello"))
        assertTrue(block.isReadOnly(AuiPluginRegistry.Empty))
    }

    @Test
    fun `block with open_url action is not read-only with empty registry`() {
        val block = AuiBlock.ButtonPrimary(
            data = ButtonPrimaryData(label = "Open"),
            feedback = AuiFeedback(action = "open_url"),
        )
        // No plugin registered for open_url → unknown action → not read-only
        assertFalse(block.isReadOnly(AuiPluginRegistry.Empty))
    }
}
