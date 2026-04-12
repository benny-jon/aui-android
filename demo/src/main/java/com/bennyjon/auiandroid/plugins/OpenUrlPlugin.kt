package com.bennyjon.auiandroid.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bennyjon.aui.core.model.AuiFeedback
import com.bennyjon.aui.core.plugin.AuiActionPlugin

/**
 * Demo action plugin that opens a URL in the device browser.
 *
 * Demonstrates how host apps register side-effect actions that the AI can
 * trigger via feedback params. When the AI sends a button with
 * `action = "open_url"` and `params = { "url": "..." }`, this plugin
 * launches the system browser.
 */
class OpenUrlPlugin(private val context: Context) : AuiActionPlugin() {
    override val id = "open_url"
    override val action = "open_url"
    override val isReadOnly = true

    override val promptSchema = "open_url(url) — Open the given URL in the device browser."

    override fun handle(feedback: AuiFeedback): Boolean {
        val url = feedback.params["url"] ?: return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
