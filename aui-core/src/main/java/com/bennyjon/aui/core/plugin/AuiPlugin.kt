package com.bennyjon.aui.core.plugin

/**
 * Marker interface for all AUI plugins.
 *
 * Every plugin — whether it renders a component or handles an action — implements
 * this interface. The shared [promptSchema] property lets [AuiCatalogPrompt][com.bennyjon.aui.core.AuiCatalogPrompt]
 * iterate the registry and include plugin descriptions in the AI system prompt
 * without knowing what kind of plugin each entry is.
 *
 * Subtypes:
 * - [AuiActionPlugin] — handles named actions triggered by user interaction (pure Kotlin, lives in `aui-core`).
 * - `AuiComponentPlugin` — renders a custom component type (lives in `aui-compose` because it has a `@Composable` method).
 */
interface AuiPlugin {
    /** Unique plugin identifier used for logging and debugging. */
    val id: String

    /**
     * Schema text included in [AuiCatalogPrompt][com.bennyjon.aui.core.AuiCatalogPrompt]
     * output so the AI knows about this plugin.
     *
     * Example: `"card_product_review(title, rating, review_text, author, date?, helpful_count?) — A customer review card."`
     *
     * Return an empty string for override plugins that shadow a built-in type
     * (the built-in's schema is already in the prompt).
     */
    val promptSchema: String

    /**
     * Key used by [AuiPluginRegistry] to detect duplicate registrations.
     *
     * Two plugins with the same [slotKey] occupy the same slot — registering
     * the second one replaces the first (last-registered wins). Each subtype
     * overrides this to return the value that defines its identity:
     * - [AuiActionPlugin] returns [AuiActionPlugin.action].
     * - `AuiComponentPlugin` returns `componentType`.
     *
     * Defaults to [id].
     */
    val slotKey: String get() = id
}
