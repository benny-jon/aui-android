package com.bennyjon.aui.compose.plugin

import com.bennyjon.aui.core.plugin.AuiPluginRegistry

/**
 * Look up a component plugin by its [AuiComponentPlugin.componentType].
 *
 * Only available from `aui-compose` — core-level code cannot accidentally
 * try to render components.
 *
 * @return the matching plugin, or `null` if no component plugin handles [type].
 */
fun AuiPluginRegistry.componentPlugin(type: String): AuiComponentPlugin<*>? =
    allPlugins()
        .filterIsInstance<AuiComponentPlugin<*>>()
        .find { it.componentType == type }

/** All registered component plugins. */
fun AuiPluginRegistry.allComponentPlugins(): List<AuiComponentPlugin<*>> =
    allPlugins().filterIsInstance<AuiComponentPlugin<*>>()
