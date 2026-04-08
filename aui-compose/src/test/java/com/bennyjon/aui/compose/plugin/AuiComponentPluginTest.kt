package com.bennyjon.aui.compose.plugin

import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuiComponentPluginTest {

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Minimal component plugin for testing. Render is a no-op since we only test registry lookup. */
    private fun componentPlugin(
        id: String,
        type: String,
        schema: String = "",
    ): AuiComponentPlugin<String> = object : AuiComponentPlugin<String>() {
        override val id = id
        override val componentType = type
        override val promptSchema = schema
        override val dataSerializer: KSerializer<String> = String.serializer()

        @androidx.compose.runtime.Composable
        override fun Render(
            data: String,
            onFeedback: (() -> Unit)?,
            modifier: androidx.compose.ui.Modifier,
        ) = Unit
    }

    private fun actionPlugin(
        id: String,
        action: String,
    ) = object : AuiActionPlugin() {
        override val id = id
        override val action = action
        override val promptSchema = ""
        override fun handle(feedback: AuiFeedback) = true
    }

    // ── Component plugin lookup via extension functions ──────────────────────

    @Test
    fun `componentPlugin returns registered plugin by type`() {
        val registry = AuiPluginRegistry()
        val plugin = componentPlugin(id = "review", type = "card_product_review")
        registry.register(plugin)

        assertEquals(plugin, registry.componentPlugin("card_product_review"))
    }

    @Test
    fun `componentPlugin returns null for unknown type`() {
        val registry = AuiPluginRegistry()

        assertNull(registry.componentPlugin("nonexistent"))
    }

    @Test
    fun `second component plugin with same type replaces first`() {
        val registry = AuiPluginRegistry()
        val first = componentPlugin(id = "v1", type = "card_basic", schema = "v1")
        val second = componentPlugin(id = "v2", type = "card_basic", schema = "v2")

        registry.register(first)
        registry.register(second)

        assertEquals(second, registry.componentPlugin("card_basic"))
        assertEquals(1, registry.allComponentPlugins().size)
    }

    @Test
    fun `allComponentPlugins returns only component plugins`() {
        val registry = AuiPluginRegistry()
        val comp = componentPlugin(id = "review", type = "card_product_review")
        val act = actionPlugin(id = "nav", action = "navigate")
        registry.registerAll(comp, act)

        val components = registry.allComponentPlugins()

        assertEquals(1, components.size)
        assertEquals(comp, components[0])
    }

    @Test
    fun `allComponentPlugins returns empty for no component plugins`() {
        val registry = AuiPluginRegistry()
        registry.register(actionPlugin(id = "nav", action = "navigate"))

        assertTrue(registry.allComponentPlugins().isEmpty())
    }

    // ── Mixed registry ──────────────────────────────────────────────────────

    @Test
    fun `mixed registry - both action and component plugins retrievable`() {
        val registry = AuiPluginRegistry()
        val comp = componentPlugin(id = "fact", type = "demo_fun_fact")
        val act = actionPlugin(id = "nav", action = "navigate")
        registry.registerAll(comp, act)

        assertEquals(comp, registry.componentPlugin("demo_fun_fact"))
        assertEquals(act, registry.actionPlugin("navigate"))
        assertEquals(2, registry.allPlugins().size)
    }

    @Test
    fun `component plugin slotKey is the componentType`() {
        val plugin = componentPlugin(id = "my-plugin", type = "card_fancy")

        assertEquals("card_fancy", plugin.slotKey)
    }

    @Test
    fun `component plugin dedup uses componentType not id`() {
        val registry = AuiPluginRegistry()
        val first = componentPlugin(id = "alpha", type = "card_basic")
        val second = componentPlugin(id = "beta", type = "card_basic")
        registry.register(first)
        registry.register(second)

        assertEquals(1, registry.allPlugins().size)
        assertEquals(second, registry.componentPlugin("card_basic"))
    }

    @Test
    fun `component and action plugins with same id do not collide`() {
        // They have different slotKeys (componentType vs action), so both survive.
        val registry = AuiPluginRegistry()
        val comp = componentPlugin(id = "shared_id", type = "my_component")
        val act = actionPlugin(id = "shared_id", action = "my_action")
        registry.registerAll(comp, act)

        assertEquals(2, registry.allPlugins().size)
        assertEquals(comp, registry.componentPlugin("my_component"))
        assertEquals(act, registry.actionPlugin("my_action"))
    }
}
