package com.bennyjon.auiandroid.plugins

import android.content.Context
import android.widget.Toast
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin

/**
 * Demo action plugin that routes named screens through the host app's navigator bridge.
 *
 * Known targets are handled directly by the app. Unknown targets fall back to a toast so the
 * feedback still demonstrates that the action pipeline fired.
 */
class NavigatePlugin(private val context: Context) : AuiActionPlugin() {
    override val id = "navigate"
    override val action = "navigate"
    override val isReadOnly = true

    override val promptSchema = """
        navigate(screen, ...params) — Navigate to a supported demo screen. Only use one of:
          - home — Demo landing screen with entry points to the demos
          - live_chat — End-to-end live chat demo with provider switching and persistence
          - showcase — All Blocks Showcase with rendered AUI examples
          - settings — Demo settings screen for app configuration
          - system_prompt — Generated AUI system prompt inspector/copy screen
          - warm_organic — Theme showcase chat in the Warm Organic theme
          - earthy_green — Theme showcase chat in the Earthy Green theme
    """.trimIndent()

    override fun handle(feedback: AuiFeedback): Boolean {
        val screen = feedback.params["screen"] ?: return false
        if (DemoActionNavigator.navigate(screen, feedback.params)) return true
        Toast.makeText(context, "Unknown screen: $screen", Toast.LENGTH_SHORT).show()
        return true
    }
}
