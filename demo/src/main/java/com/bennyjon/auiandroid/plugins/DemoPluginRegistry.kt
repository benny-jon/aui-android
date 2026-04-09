package com.bennyjon.auiandroid.plugins

import android.content.Context
import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Builds the demo app's [AuiPluginRegistry] containing all demo plugins.
 *
 * This is passed to both [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer] (for rendering
 * and action handling) and [AuiCatalogPrompt][com.bennyjon.aui.core.AuiCatalogPrompt] (for
 * generating the AI system prompt).
 */
object DemoPluginRegistry {

    /** Creates a registry with all demo plugins wired to the given [context]. */
    fun create(context: Context): AuiPluginRegistry =
        AuiPluginRegistry().registerAll(
            DemoFunFactPlugin,
            ToastNavigatePlugin(context),
            OpenUrlPlugin(context),
        )
}
