package com.bennyjon.aui.core.plugin

import com.bennyjon.aui.core.model.AuiFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuiPluginRegistryTest {

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun actionPlugin(
        id: String,
        action: String,
        schema: String = "",
    ) = object : AuiActionPlugin() {
        override val id = id
        override val action = action
        override val promptSchema = schema
        override fun handle(feedback: AuiFeedback) = true
    }

    // ── Action plugin registration ──────────────────────────────────────────

    @Test
    fun `register action plugin and retrieve by action name`() {
        val registry = AuiPluginRegistry()
        val plugin = actionPlugin(id = "nav", action = "navigate")
        registry.register(plugin)

        assertEquals(plugin, registry.actionPlugin("navigate"))
    }

    @Test
    fun `second action plugin with same action replaces first`() {
        val registry = AuiPluginRegistry()
        val first = actionPlugin(id = "nav-v1", action = "navigate", schema = "v1")
        val second = actionPlugin(id = "nav-v2", action = "navigate", schema = "v2")

        registry.register(first)
        registry.register(second)

        assertEquals(second, registry.actionPlugin("navigate"))
        assertEquals(1, registry.allActionPlugins().size)
    }

    @Test
    fun `registerAll with multiple plugins`() {
        val registry = AuiPluginRegistry()
        val nav = actionPlugin(id = "nav", action = "navigate")
        val url = actionPlugin(id = "url", action = "open_url")

        registry.registerAll(nav, url)

        assertEquals(nav, registry.actionPlugin("navigate"))
        assertEquals(url, registry.actionPlugin("open_url"))
        assertEquals(2, registry.allActionPlugins().size)
    }

    @Test
    fun `empty registry returns null for action lookup`() {
        val registry = AuiPluginRegistry()

        assertNull(registry.actionPlugin("navigate"))
    }

    @Test
    fun `empty registry returns empty action list`() {
        val registry = AuiPluginRegistry()

        assertTrue(registry.allActionPlugins().isEmpty())
    }

    @Test
    fun `allPlugins returns all registered plugins`() {
        val registry = AuiPluginRegistry()
        val nav = actionPlugin(id = "nav", action = "navigate")
        val url = actionPlugin(id = "url", action = "open_url")
        registry.registerAll(nav, url)

        val all = registry.allPlugins()

        assertEquals(2, all.size)
        assertTrue(all.contains(nav))
        assertTrue(all.contains(url))
    }

    @Test
    fun `allPlugins returns defensive copy`() {
        val registry = AuiPluginRegistry()
        registry.register(actionPlugin(id = "nav", action = "navigate"))

        val snapshot = registry.allPlugins()
        registry.register(actionPlugin(id = "url", action = "open_url"))

        assertEquals(1, snapshot.size)
        assertEquals(2, registry.allPlugins().size)
    }

    @Test
    fun `register returns this for fluent chaining`() {
        val registry = AuiPluginRegistry()
        val result = registry.register(actionPlugin(id = "nav", action = "navigate"))

        assertTrue(result === registry)
    }

    @Test
    fun `registerAll returns this for fluent chaining`() {
        val registry = AuiPluginRegistry()
        val result = registry.registerAll(actionPlugin(id = "nav", action = "navigate"))

        assertTrue(result === registry)
    }

    // ── Companion ───────────────────────────────────────────────────────────

    @Test
    fun `Empty companion has no plugins`() {
        assertTrue(AuiPluginRegistry.Empty.allPlugins().isEmpty())
    }

    // ── slotKey dedup ───────────────────────────────────────────────────────

    @Test
    fun `action plugin slotKey is the action name`() {
        val plugin = actionPlugin(id = "nav-plugin", action = "navigate")

        assertEquals("navigate", plugin.slotKey)
    }

    @Test
    fun `different id but same action still deduplicates`() {
        val registry = AuiPluginRegistry()
        val first = actionPlugin(id = "alpha", action = "navigate")
        val second = actionPlugin(id = "beta", action = "navigate")
        registry.register(first)
        registry.register(second)

        assertEquals(1, registry.allPlugins().size)
        assertEquals(second, registry.actionPlugin("navigate"))
    }
}
