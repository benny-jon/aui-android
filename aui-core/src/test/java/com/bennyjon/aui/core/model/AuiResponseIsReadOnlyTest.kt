package com.bennyjon.aui.core.model

import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.QuickRepliesData
import com.bennyjon.aui.core.model.data.QuickReplyOption
import com.bennyjon.aui.core.model.data.TextData
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuiResponseIsReadOnlyTest {

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

    @Test
    fun `no feedback blocks are read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.Text(data = TextData(text = "Hello")),
            ),
        )
        assertTrue(response.isReadOnly(registry))
    }

    @Test
    fun `submit action is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }

    @Test
    fun `only read-only plugin actions are read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Open"),
                    feedback = AuiFeedback(action = "open_url", params = mapOf("url" to "https://example.com")),
                ),
            ),
        )
        assertTrue(response.isReadOnly(registry))
    }

    @Test
    fun `mixed read-only and submit is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Open"),
                    feedback = AuiFeedback(action = "open_url"),
                ),
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Submit"),
                    feedback = AuiFeedback(action = "submit"),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }

    @Test
    fun `unknown action with no plugin is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Mystery"),
                    feedback = AuiFeedback(action = "unknown_action"),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }

    @Test
    fun `interactive plugin action is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.ButtonPrimary(
                    data = ButtonPrimaryData(label = "Add"),
                    feedback = AuiFeedback(action = "add_to_cart"),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }

    @Test
    fun `empty response is read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = emptyList(),
        )
        assertTrue(response.isReadOnly(registry))
    }

    @Test
    fun `empty registry with no-feedback blocks is read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(AuiBlock.Text(data = TextData(text = "Hello"))),
        )
        assertTrue(response.isReadOnly(AuiPluginRegistry.Empty))
    }

    // ── Nested feedback (QuickReplies) ──────────────────────────────────────

    @Test
    fun `quick replies with submit options is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.QuickReplies(
                    data = QuickRepliesData(
                        options = listOf(
                            QuickReplyOption(label = "Yes", feedback = AuiFeedback(action = "submit")),
                            QuickReplyOption(label = "No", feedback = AuiFeedback(action = "submit")),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }

    @Test
    fun `quick replies with only read-only options is read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.QuickReplies(
                    data = QuickRepliesData(
                        options = listOf(
                            QuickReplyOption(label = "Link 1", feedback = AuiFeedback(action = "open_url")),
                            QuickReplyOption(label = "Link 2", feedback = AuiFeedback(action = "open_url")),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(response.isReadOnly(registry))
    }

    @Test
    fun `quick replies with no feedback on options is read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.QuickReplies(
                    data = QuickRepliesData(
                        options = listOf(
                            QuickReplyOption(label = "Option A"),
                            QuickReplyOption(label = "Option B"),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(response.isReadOnly(registry))
    }

    @Test
    fun `quick replies mixed with submit block-level feedback is not read-only`() {
        val response = AuiResponse(
            display = AuiDisplay.EXPANDED,
            blocks = listOf(
                AuiBlock.Text(data = TextData(text = "Pick one:")),
                AuiBlock.QuickReplies(
                    data = QuickRepliesData(
                        options = listOf(
                            QuickReplyOption(label = "Yes", feedback = AuiFeedback(action = "submit")),
                            QuickReplyOption(label = "No", feedback = AuiFeedback(action = "submit")),
                        ),
                    ),
                ),
            ),
        )
        assertFalse(response.isReadOnly(registry))
    }
}
