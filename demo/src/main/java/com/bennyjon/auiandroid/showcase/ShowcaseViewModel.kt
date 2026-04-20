package com.bennyjon.auiandroid.showcase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bennyjon.aui.core.AuiParser
import com.bennyjon.aui.core.plugin.AuiPluginRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

/**
 * Loads and parses the all-blocks showcase JSON asset into a list of [ShowcaseEntry]s.
 *
 * Each entry contains a parsed [com.bennyjon.aui.core.model.AuiResponse] ready for the
 * showcase screen to render directly or surface through a card stub.
 */
@HiltViewModel
@OptIn(ExperimentalSerializationApi::class)
class ShowcaseViewModel @Inject constructor(
    application: Application,
    val pluginRegistry: AuiPluginRegistry,
) : AndroidViewModel(application) {

    private val _entries = MutableStateFlow<List<ShowcaseEntry>>(emptyList())

    /** The parsed list of showcase entries. */
    val entries: StateFlow<List<ShowcaseEntry>> = _entries.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
    private val parser = AuiParser()

    init {
        loadShowcaseEntries()
    }

    private fun loadShowcaseEntries() {
        val raw = getApplication<Application>().assets
            .open("all-blocks-showcase.json")
            .bufferedReader()
            .use { it.readText() }

        val array = json.parseToJsonElement(raw) as JsonArray

        _entries.value = array.mapIndexed { index, element ->
            val obj = element.jsonObject
            val category = obj["category"]?.jsonPrimitive?.content ?: "Examples"
            val label = obj["label"]!!.jsonPrimitive.content
            val description = obj["description"]?.jsonPrimitive?.content
            val auiJsonElement = obj["aui"] ?: error("Missing aui payload for showcase entry: $label")
            val response = parser.parseOrNull(auiJsonElement.toString())
                ?: error("Invalid showcase entry at index $index: $label")
            ShowcaseEntry(
                category = category,
                label = label,
                description = description,
                response = response,
                sourceJson = prettyJson.encodeToString(JsonElement.serializer(), auiJsonElement),
            )
        }
    }
}
