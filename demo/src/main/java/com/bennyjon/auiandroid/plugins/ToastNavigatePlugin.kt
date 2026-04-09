package com.bennyjon.auiandroid.plugins

import android.content.Context
import android.widget.Toast
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin

/**
 * Demo action plugin that shows a [Toast] with the target screen name.
 *
 * In a real app this would call `navController.navigate(screen)`. Since the demo
 * has no deep-linking routes, it toasts the screen name instead to prove the
 * action pipeline works end-to-end.
 */
class ToastNavigatePlugin(private val context: Context) : AuiActionPlugin() {
    override val id = "navigate"
    override val action = "navigate"

    override val promptSchema =
        "navigate(screen, ...params) — Navigate to a named screen in the app."

    override fun handle(feedback: AuiFeedback): Boolean {
        val screen = feedback.params["screen"] ?: return false
        Toast.makeText(context, "Navigate → $screen", Toast.LENGTH_SHORT).show()
        return true
    }
}
