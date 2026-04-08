package com.bennyjon.aui.core.plugin

/**
 * Central registry of all AUI plugins — both component plugins and action plugins.
 *
 * The host app builds **one** registry and passes it to both the renderer
 * (for rendering custom components and handling actions) and
 * [AuiCatalogPrompt][com.bennyjon.aui.core.AuiCatalogPrompt]
 * (for including plugin schemas in the AI system prompt). Single instance,
 * single source of truth.
 *
 * Example:
 * ```kotlin
 * val registry = AuiPluginRegistry().registerAll(
 *     ProductReviewPlugin,           // component plugin
 *     NavigateActionPlugin(navCtrl), // action plugin
 *     OpenUrlActionPlugin(context),  // action plugin
 * )
 *
 * // Pass to renderer
 * AuiRenderer(json = auiJson, pluginRegistry = registry, onFeedback = { ... })
 *
 * // Pass to prompt generator
 * val systemPrompt = AuiCatalogPrompt.generate(pluginRegistry = registry)
 * ```
 *
 * Duplicate handling: if a newly registered plugin has the same
 * [AuiPlugin.slotKey] as an existing one, the old plugin is removed
 * (last-registered wins).
 */
class AuiPluginRegistry {
    private val plugins = mutableListOf<AuiPlugin>()

    /**
     * Register a plugin. If another plugin with the same [AuiPlugin.slotKey]
     * is already registered, it is replaced (last-registered wins).
     *
     * @return this registry for fluent chaining.
     */
    fun register(plugin: AuiPlugin): AuiPluginRegistry {
        plugins.removeAll { it.slotKey == plugin.slotKey }
        plugins.add(plugin)
        return this
    }

    /**
     * Register multiple plugins at once. Each plugin follows the same
     * deduplication rules as [register].
     *
     * @return this registry for fluent chaining.
     */
    fun registerAll(vararg newPlugins: AuiPlugin): AuiPluginRegistry {
        newPlugins.forEach { register(it) }
        return this
    }

    /**
     * All registered plugins, in registration order.
     *
     * Used by [AuiCatalogPrompt][com.bennyjon.aui.core.AuiCatalogPrompt]
     * to read [AuiPlugin.promptSchema] from every plugin regardless of type.
     */
    fun allPlugins(): List<AuiPlugin> = plugins.toList()

    /**
     * Look up an action plugin by its [AuiActionPlugin.action] name.
     *
     * @return the matching plugin, or `null` if no action plugin is registered for [action].
     */
    fun actionPlugin(action: String): AuiActionPlugin? =
        plugins.filterIsInstance<AuiActionPlugin>().find { it.action == action }

    /** All registered action plugins. */
    fun allActionPlugins(): List<AuiActionPlugin> =
        plugins.filterIsInstance<AuiActionPlugin>()

    companion object {
        /** An empty registry with no plugins registered. */
        val Empty = AuiPluginRegistry()
    }
}
