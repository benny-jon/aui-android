package com.bennyjon.auiandroid.showcase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Loads and parses the all-blocks showcase JSON asset into a list of [ShowcaseEntry]s.
 *
 * Each entry contains the raw AUI JSON payload string ready to be passed to
 * [AuiRenderer][com.bennyjon.aui.compose.AuiRenderer].
 */
@HiltViewModel
class ShowcaseViewModel @Inject constructor(
    application: Application,
    val pluginRegistry: AuiPluginRegistry,
) : AndroidViewModel(application) {

    private val _entries = MutableStateFlow<List<ShowcaseEntry>>(emptyList())

    /** The parsed list of showcase entries. */
    val entries: StateFlow<List<ShowcaseEntry>> = _entries.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadShowcaseEntries()
    }

    private fun loadShowcaseEntries() {
        val raw = getApplication<Application>().assets
            .open("all-blocks-showcase.json")
            .bufferedReader()
            .use { it.readText() }

        val array = json.parseToJsonElement(raw) as JsonArray

        _entries.value = array.map { element ->
            val obj = element.jsonObject
            val label = obj["label"]!!.jsonPrimitive.content
            val description = obj["description"]?.jsonPrimitive?.content
            val auiObj = obj["aui"]!!.jsonObject
            val display = auiObj["display"]?.jsonPrimitive?.content
            ShowcaseEntry(
                label = label,
                description = description,
                auiJson = auiObj.toString(),
                isSheet = display == "sheet",
            )
        }
    }
}
