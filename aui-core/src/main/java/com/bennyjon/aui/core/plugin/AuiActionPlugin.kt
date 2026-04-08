package com.bennyjon.aui.core.plugin

import com.bennyjon.aui.core.model.AuiFeedback

/**
 * A plugin that handles a named action triggered by user interaction.
 *
 * Action plugins are for **side effects** — navigating to a screen, opening a URL,
 * sharing content, adding an item to a cart, etc. They complement (not replace)
 * the `onFeedback` callback: when feedback fires, the host app's `onFeedback`
 * **always** runs (for the AI conversation loop), and if a matching action plugin
 * exists, its [handle] method also runs.
 *
 * Example:
 * ```kotlin
 * class OpenUrlActionPlugin(private val context: Context) : AuiActionPlugin() {
 *     override val id = "open_url"
 *     override val action = "open_url"
 *     override val promptSchema = "open_url(url) — Open the URL in the browser"
 *
 *     override fun handle(feedback: AuiFeedback) {
 *         val url = feedback.params["url"] ?: return
 *         context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
     * Called by the renderer after [AuiFeedback] is emitted, if [AuiFeedback.action]
     * matches this plugin's [action].
     */
    abstract fun handle(feedback: AuiFeedback)

    /**
     * Action plugins use [action] as their slot key so that registering
     * two plugins for the same action replaces the first.
     */
    override val slotKey: String get() = action
}
