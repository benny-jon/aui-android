package com.bennyjon.auiandroid.showcase

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.bennyjon.aui.core.AuiParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcaseAssetTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val parser = AuiParser()

    @Test
    fun `all blocks showcase parses and covers every supported demo type`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val raw = application.assets
            .open("all-blocks-showcase.json")
            .bufferedReader()
            .use { it.readText() }

        val entries = json.parseToJsonElement(raw).jsonArray
        val labels = mutableSetOf<String>()
        val displays = mutableSetOf<String>()
        val types = mutableSetOf<String>()

        entries.forEachIndexed { index, element ->
            val entry = element.jsonObject
            val label = entry.getValue("label").jsonPrimitive.content
            val aui = entry.getValue("aui")

            assertTrue("Duplicate showcase label: $label", labels.add(label))
            assertNotNull(
                "Showcase entry at index $index failed to parse: $label",
                parser.parseOrNull(aui.toString()),
            )

            val auiObject = aui.jsonObject
            displays += auiObject.getValue("display").jsonPrimitive.content
            types += collectBlockTypes(auiObject)
        }

        assertEquals(setOf("inline", "expanded", "survey"), displays)
        assertTrue(
            "Showcase asset is missing supported block types. Covered=$types",
            types.containsAll(
                setOf(
                    "text",
                    "heading",
                    "caption",
                    "file_content",
                    "button_primary",
                    "button_secondary",
                    "quick_replies",
                    "chip_select_single",
                    "chip_select_multi",
                    "radio_list",
                    "checkbox_list",
                    "input_text_single",
                    "input_slider",
                    "input_rating_stars",
                    "divider",
                    "stepper_horizontal",
                    "progress_bar",
                    "badge_info",
                    "badge_success",
                    "badge_warning",
                    "badge_error",
                    "status_banner_info",
                    "status_banner_success",
                    "status_banner_warning",
                    "status_banner_error",
                    "demo_fun_fact",
                )
            )
        )
    }

    private fun collectBlockTypes(aui: JsonObject): Set<String> {
        val types = mutableSetOf<String>()
        aui["blocks"]?.jsonArray?.forEach { block ->
            types += block.jsonObject.getValue("type").jsonPrimitive.content
        }
        aui["steps"]?.jsonArray?.forEach { step ->
            step.jsonObject.getValue("blocks").jsonArray.forEach { block ->
                types += block.jsonObject.getValue("type").jsonPrimitive.content
            }
        }
        return types
    }
}
