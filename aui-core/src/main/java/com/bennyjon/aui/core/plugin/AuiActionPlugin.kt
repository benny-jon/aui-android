package com.bennyjon.aui.core.plugin

import com.bennyjon.aui.core.model.AuiFeedback

/**
 * A plugin that handles a named action triggered by user interaction.
 *
 * Action plugins are for **side effects** — navigating to a screen, opening a URL,
 * sharing content, adding an item to a cart, etc. They participate in a
 * **chain-of-responsibility** with the host app's `onFeedback` callback:
 *
 * 1. When feedback fires, the renderer checks for a matching action plugin first.
 * 2. If a plugin is found, its [handle] method runs.
 *    - Return `true` to **claim** the feedback — `onFeedback` will **not** be called.
 *    - Return `false` to **pass through** — `onFeedback` will still be called.
 * 3. If no plugin matches the action, `onFeedback` is called directly.
 *
 * This prevents double-handling: a plugin that opens a URL can claim the event so
 * the host doesn't also send it to the AI conversation loop.
 *
 * Example — plugin that always claims:
 * ```kotlin
 * class OpenUrlActionPlugin(private val context: Context) : AuiActionPlugin() {
 *     override val id = "open_url"
 *     override val action = "open_url"
 *     override val promptSchema = "open_url(url) — Open the URL in the browser"
 *
 *     override fun handle(feedback: AuiFeedback): Boolean {
 *         val url = feedback.params["url"] ?: return false  // pass through if no URL
 *         context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
 *         return true  // claimed — onFeedback will not be called
 *     }
 * }
 * ```
 *
 * Example — plugin that conditionally passes through:
 * ```kotlin
 * class NavigatePlugin(private val navController: NavController) : AuiActionPlugin() {
 *     override val id = "navigate"
 *     override val action = "navigate"
 *     override val promptSchema = "navigate(screen) — Navigate to a named screen"
 *
 *     override fun handle(feedback: AuiFeedback): Boolean {
 *         val screen = feedback.params["screen"] ?: return false
 *         return if (navController.graph.findNode(screen) != null) {
 *             navController.navigate(screen)
 *             true   // known screen — claimed
 *         } else {
 *             false  // unknown screen — let onFeedback handle it
 *         }
 *     }
 * }
 * ```
 */
abstract class AuiActionPlugin : AuiPlugin {
    /** The action name this plugin handles (e.g. `"navigate"`, `"open_url"`). */
    abstract val action: String

    /**
     * Handle the action when triggered by user interaction.
     *
     * Called by the renderer before `onFeedback`, if [AuiFeedback.action] matches
     * this plugin's [action].
     *
     * @return `true` if the plugin fully handled the feedback (the host's `onFeedback`
     *   will **not** be called), or `false` to pass through (the host's `onFeedback`
     *   **will** be called).
     */
    abstract fun handle(feedback: AuiFeedback): Boolean

    /**
     * Action plugins use [action] as their slot key so that registering
     * two plugins for the same action replaces the first.
     */
    override val slotKey: String get() = action
}
