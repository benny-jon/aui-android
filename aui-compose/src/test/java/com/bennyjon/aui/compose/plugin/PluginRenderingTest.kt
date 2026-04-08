package com.bennyjon.aui.compose.plugin

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRenderingTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Plugin data parsing ─────────────────────────────────────────────────

    @Serializable
    data class FunFactData(
        val title: String,
        val fact: String,
        val source: String? = null,
    )

    @Serializable
    data class ProductData(
        val name: String,
        val price: Double,
        @SerialName("in_stock") val inStock: Boolean = true,
    )

    @Test
    fun `plugin dataSerializer correctly parses rawData from Unknown block`() {
        val rawData = buildJsonObject {
            put("title", "Did You Know?")
            put("fact", "Honey never spoils.")
            put("source", "Wikipedia")
        }

        val result = json.decodeFromJsonElement(FunFactData.serializer(), rawData)

        assertEquals("Did You Know?", result.title)
        assertEquals("Honey never spoils.", result.fact)
        assertEquals("Wikipedia", result.source)
    }

    @Test
    fun `plugin dataSerializer handles optional fields`() {
        val rawData = buildJsonObject {
            put("title", "Fact")
            put("fact", "Water is wet.")
        }

        val result = json.decodeFromJsonElement(FunFactData.serializer(), rawData)

        assertEquals("Fact", result.title)
        assertEquals("Water is wet.", result.fact)
        assertNull(result.source)
    }

    @Test
    fun `plugin dataSerializer handles SerialName mapping`() {
        val rawData = buildJsonObject {
            put("name", "Widget")
            put("price", 9.99)
            put("in_stock", false)
        }

        val result = json.decodeFromJsonElement(ProductData.serializer(), rawData)

        assertEquals("Widget", result.name)
        assertEquals(9.99, result.price, 0.001)
        assertEquals(false, result.inStock)
    }

    @Test
    fun `plugin dataSerializer ignores unknown fields in data`() {
        val rawData = buildJsonObject {
            put("title", "Fact")
            put("fact", "Cats sleep 16h/day.")
            put("unknown_field", "should be ignored")
        }

        val result = json.decodeFromJsonElement(FunFactData.serializer(), rawData)

        assertEquals("Fact", result.title)
        assertEquals("Cats sleep 16h/day.", result.fact)
    }

    @Test
    fun `plugin data parse failure does not throw when caught`() {
        val rawData = buildJsonObject {
            put("wrong_field", "no title or fact")
        }

        val result = runCatching {
            json.decodeFromJsonElement(FunFactData.serializer(), rawData)
        }

        assertTrue(result.isFailure)
    }

    // ── Plugin resolution via registry ──────────────────────────────────────

    @Test
    fun `componentPlugin finds plugin for matching unknown block type`() {
        val plugin = testComponentPlugin(id = "fact", type = "demo_fun_fact")
        val registry = AuiPluginRegistry().register(plugin)

        assertEquals(plugin, registry.componentPlugin("demo_fun_fact"))
    }

    @Test
    fun `componentPlugin returns null for built-in type with no plugin`() {
        val registry = AuiPluginRegistry()

        assertNull(registry.componentPlugin("text"))
    }

    @Test
    fun `plugin override shadows built-in type when registered`() {
        val override = testComponentPlugin(id = "custom_text", type = "text")
        val registry = AuiPluginRegistry().register(override)

        // A component plugin with componentType="text" would be found
        assertNotNull(registry.componentPlugin("text"))
        assertEquals(override, registry.componentPlugin("text"))
    }

    // ── Feedback routing — chain-of-responsibility ────────────────────────

    /**
     * Mirrors [AuiRenderer]'s routing logic so we can test it without Compose runtime.
     */
    private fun routeFeedback(
        feedback: AuiFeedback,
        registry: AuiPluginRegistry,
        onFeedback: (AuiFeedback) -> Unit,
    ) {
        val claimed = registry.actionPlugin(feedback.action)?.handle(feedback) ?: false
        if (!claimed) onFeedback(feedback)
    }

    @Test
    fun `plugin returns true — onFeedback is NOT called`() {
        var onFeedbackCalled = false
        var pluginCalled = false

        val plugin = object : AuiActionPlugin() {
            override val id = "open_url"
            override val action = "open_url"
            override val promptSchema = ""
            override fun handle(feedback: AuiFeedback): Boolean {
                pluginCalled = true
                return true // claim it
            }
        }
        val registry = AuiPluginRegistry().register(plugin)

        routeFeedback(
            AuiFeedback(action = "open_url", params = mapOf("url" to "https://example.com")),
            registry,
        ) { onFeedbackCalled = true }

        assertTrue("plugin must be called", pluginCalled)
        assertFalse("onFeedback must NOT be called when plugin claims", onFeedbackCalled)
    }

    @Test
    fun `plugin returns false — onFeedback IS called`() {
        var onFeedbackCalled = false
        var pluginCalled = false

        val plugin = object : AuiActionPlugin() {
            override val id = "nav"
            override val action = "navigate"
            override val promptSchema = ""
            override fun handle(feedback: AuiFeedback): Boolean {
                pluginCalled = true
                return false // pass through
            }
        }
        val registry = AuiPluginRegistry().register(plugin)

        routeFeedback(AuiFeedback(action = "navigate"), registry) { onFeedbackCalled = true }

        assertTrue("plugin must be called", pluginCalled)
        assertTrue("onFeedback must be called when plugin passes through", onFeedbackCalled)
    }

    @Test
    fun `no plugin registered — onFeedback IS called`() {
        var onFeedbackCalled = false
        val registry = AuiPluginRegistry()

        routeFeedback(AuiFeedback(action = "unknown_action"), registry) { onFeedbackCalled = true }

        assertTrue("onFeedback must be called when no plugin matches", onFeedbackCalled)
    }

    @Test
    fun `action plugin receives full feedback with params`() {
        var receivedFeedback: AuiFeedback? = null
        val plugin = object : AuiActionPlugin() {
            override val id = "open_url"
            override val action = "open_url"
            override val promptSchema = ""
            override fun handle(feedback: AuiFeedback): Boolean {
                receivedFeedback = feedback
                return true
            }
        }
        val registry = AuiPluginRegistry().register(plugin)

        val feedback = AuiFeedback(
            action = "open_url",
            params = mapOf("url" to "https://example.com"),
        )
        registry.actionPlugin(feedback.action)?.handle(feedback)

        assertNotNull(receivedFeedback)
        assertEquals("https://example.com", receivedFeedback!!.params["url"])
    }

    @Test
    fun `unknown action does not crash`() {
        val registry = AuiPluginRegistry()
        val feedback = AuiFeedback(action = "nonexistent_action")

        // Should return null, no exception
        val result = registry.actionPlugin(feedback.action)
        assertNull(result)
    }

    @Test
    fun `plugin claims conditionally — true blocks onFeedback, false passes through`() {
        val results = mutableListOf<String>()

        val plugin = object : AuiActionPlugin() {
            override val id = "nav"
            override val action = "navigate"
            override val promptSchema = ""
            override fun handle(feedback: AuiFeedback): Boolean {
                val screen = feedback.params["screen"]
                return if (screen == "home") {
                    results.add("plugin_handled_home")
                    true
                } else {
                    results.add("plugin_passed_through")
                    false
                }
            }
        }
        val registry = AuiPluginRegistry().register(plugin)

        // Known screen — plugin claims
        routeFeedback(
            AuiFeedback(action = "navigate", params = mapOf("screen" to "home")),
            registry,
        ) { results.add("onFeedback_called") }

        assertEquals(listOf("plugin_handled_home"), results)

        results.clear()

        // Unknown screen — plugin passes through
        routeFeedback(
            AuiFeedback(action = "navigate", params = mapOf("screen" to "unknown")),
            registry,
        ) { results.add("onFeedback_called") }

        assertEquals(listOf("plugin_passed_through", "onFeedback_called"), results)
    }

    // ── rawData in Unknown block ────────────────────────────────────────────

    @Test
    fun `Unknown block constructed with rawData preserves it`() {
        val rawData = buildJsonObject {
            put("title", "Test")
            put("fact", "Testing is fun")
        }
        val block = AuiBlock.Unknown(
            type = "demo_fun_fact",
            rawData = rawData,
            feedback = AuiFeedback(action = "tap"),
        )

        assertEquals("demo_fun_fact", block.type)
        assertNotNull(block.rawData)
        assertNotNull(block.feedback)
        assertEquals("tap", block.feedback!!.action)
    }

    @Test
    fun `Unknown block without rawData has null`() {
        val block = AuiBlock.Unknown(type = "empty_block")

        assertNull(block.rawData)
        assertNull(block.feedback)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun testComponentPlugin(
        id: String,
        type: String,
    ): AuiComponentPlugin<String> = object : AuiComponentPlugin<String>() {
        override val id = id
        override val componentType = type
        override val promptSchema = ""
        override val dataSerializer = String.serializer()

        @androidx.compose.runtime.Composable
        override fun Render(
            data: String,
            onFeedback: (() -> Unit)?,
            modifier: androidx.compose.ui.Modifier,
        ) = Unit
    }
}
