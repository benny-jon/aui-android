package com.bennyjon.auiandroid.plugins

/**
 * Small app-owned bridge that lets DI-created action plugins trigger host navigation.
 *
 * The actual navigation callback is installed from the composable nav host, where the
 * active NavController exists.
 */
object DemoActionNavigator {
    private var navigateHandler: ((screen: String, params: Map<String, String>) -> Boolean)? = null

    fun setNavigateHandler(handler: (screen: String, params: Map<String, String>) -> Boolean) {
        navigateHandler = handler
    }

    fun clearNavigateHandler() {
        navigateHandler = null
    }

    fun navigate(screen: String, params: Map<String, String>): Boolean =
        navigateHandler?.invoke(screen, params) ?: false
}
