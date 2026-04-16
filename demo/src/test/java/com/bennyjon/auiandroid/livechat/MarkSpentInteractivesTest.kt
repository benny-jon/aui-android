package com.bennyjon.auiandroid.livechat

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.model.AuiResponse
import com.bennyjon.aui.core.model.data.ButtonPrimaryData
import com.bennyjon.aui.core.model.data.TextData
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import com.bennyjon.auiandroid.data.chat.ChatMessage
import com.bennyjon.auiandroid.data.chat.ChatMessage.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkSpentInteractivesTest {

    // ── Helpers ─────────────────────────────────────────────────────────────

    private val emptyRegistry = AuiPluginRegistry()

    private val readOnlyRegistry = AuiPluginRegistry().register(
        object : AuiActionPlugin() {
            override val id = "open_url"
            override val action = "open_url"
            override val promptSchema = "open_url(url)"
            override val isReadOnly = true
            override fun handle(feedback: AuiFeedback) = true
        }
    )

    private fun interactiveAuiResponse() = AuiResponse(
        display = AuiDisplay.EXPANDED,
        blocks = listOf(
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Submit"),
                feedback = AuiFeedback(action = "submit"),
            )
        ),
    )

    private fun readOnlyAuiResponse() = AuiResponse(
        display = AuiDisplay.EXPANDED,
        blocks = listOf(
            AuiBlock.ButtonPrimary(
                data = ButtonPrimaryData(label = "Open"),
                feedback = AuiFeedback(action = "open_url", params = mapOf("url" to "https://example.com")),
            )
        ),
    )

    private fun textOnlyAuiResponse() = AuiResponse(
        display = AuiDisplay.EXPANDED,
        blocks = listOf(
            AuiBlock.Text(data = TextData(text = "Hello")),
        ),
    )

    private fun userMsg(id: String = "u1") = ChatMessage(
        id = id, createdAt = 0L, role = Role.USER, text = "hi",
    )

    private fun assistantMsg(
        id: String,
        auiResponse: AuiResponse? = null,
        text: String? = null,
    ) = ChatMessage(
        id = id, createdAt = 0L, role = Role.ASSISTANT,
        text = text, auiResponse = auiResponse,
    )

    // ── No AUI messages ─────────────────────────────────────────────────────

    @Test
    fun `empty list returns empty`() {
        val result = emptyList<ChatMessage>().markSpentInteractives(emptyRegistry)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `text-only messages are unchanged`() {
        val messages = listOf(
            userMsg("u1"),
            assistantMsg("a1", text = "Hello!"),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        result.forEach { assertFalse(it.isAuiSpent) }
    }

    // ── Single AUI message (last in list) ───────────────────────────────────

    @Test
    fun `single interactive AUI message that is last is not spent`() {
        val messages = listOf(
            userMsg(),
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertFalse(result[1].isAuiSpent)
    }

    @Test
    fun `single read-only AUI message that is last is not spent`() {
        val messages = listOf(
            userMsg(),
            assistantMsg("a1", auiResponse = readOnlyAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertFalse(result[1].isAuiSpent)
    }

    // ── AUI message followed by user reply — core spent scenario ────────────

    @Test
    fun `interactive AUI followed by user reply is spent`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u1"),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertTrue("Interactive AUI should be spent once user replies", result[0].isAuiSpent)
    }

    @Test
    fun `read-only AUI followed by user reply is NOT spent`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = readOnlyAuiResponse()),
            userMsg("u1"),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertFalse("Read-only should never be spent", result[0].isAuiSpent)
    }

    // ── Multiple interactive AUI messages ───────────────────────────────────

    @Test
    fun `older interactive messages are spent, last interactive is not`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertTrue("First interactive should be spent", result[0].isAuiSpent)
        assertFalse("Last message in list should NOT be spent", result[2].isAuiSpent)
    }

    @Test
    fun `three interactive messages — first two spent, last not`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
            userMsg("u2"),
            assistantMsg("a3", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertTrue(result[0].isAuiSpent)
        assertTrue(result[2].isAuiSpent)
        assertFalse(result[4].isAuiSpent)
    }

    // ── Read-only messages are never spent ──────────────────────────────────

    @Test
    fun `read-only messages are never spent even when not last`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = readOnlyAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertFalse("Read-only should never be spent", result[0].isAuiSpent)
        assertFalse("Last message should not be spent", result[2].isAuiSpent)
    }

    // ── Read-only after interactive ─────────────────────────────────────────

    @Test
    fun `interactive followed by read-only — interactive is spent, read-only is not`() {
        val messages = listOf(
            userMsg("u1"),
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u2"),
            assistantMsg("a2", auiResponse = readOnlyAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertTrue(
            "Interactive AUI is not the last message — should be spent",
            result[1].isAuiSpent,
        )
        assertFalse("Read-only is never spent", result[3].isAuiSpent)
    }

    @Test
    fun `two interactive then read-only — both interactive spent, read-only not`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
            userMsg("u2"),
            assistantMsg("a3", auiResponse = readOnlyAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertTrue("First interactive should be spent", result[0].isAuiSpent)
        assertTrue("Second interactive should be spent (not last message)", result[2].isAuiSpent)
        assertFalse("Read-only is never spent", result[4].isAuiSpent)
    }

    @Test
    fun `interactive sandwiched between two read-only — interactive spent, read-only not`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = readOnlyAuiResponse()),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
            assistantMsg("a3", auiResponse = readOnlyAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        assertFalse("Read-only is never spent", result[0].isAuiSpent)
        assertTrue("Interactive is not last message — should be spent", result[1].isAuiSpent)
        assertFalse("Read-only is never spent", result[2].isAuiSpent)
    }

    // ── Feedback-free AUI (text-only response) ──────────────────────────────

    @Test
    fun `text-only AUI response is read-only and never spent`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = textOnlyAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertFalse("Text-only AUI (no feedback blocks) is read-only — never spent", result[0].isAuiSpent)
        assertFalse("Last message should not be spent", result[2].isAuiSpent)
    }

    // ── User messages don't interfere ───────────────────────────────────────

    @Test
    fun `user messages between AUI messages are unaffected`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = interactiveAuiResponse()),
            userMsg("u1"),
            userMsg("u2"),
            assistantMsg("a2", auiResponse = interactiveAuiResponse()),
        )
        val result = messages.markSpentInteractives(emptyRegistry)
        assertFalse(result[1].isAuiSpent)
        assertFalse(result[2].isAuiSpent)
    }

    // ── Only read-only messages ─────────────────────────────────────────────

    @Test
    fun `all read-only messages — none are spent`() {
        val messages = listOf(
            assistantMsg("a1", auiResponse = readOnlyAuiResponse()),
            userMsg("u1"),
            assistantMsg("a2", auiResponse = readOnlyAuiResponse()),
        )
        val result = messages.markSpentInteractives(readOnlyRegistry)
        result.forEach { assertFalse(it.isAuiSpent) }
    }
}
