package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.ChartVariant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChartBlockParserTest {

    private lateinit var parser: AuiParser
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        parser = AuiParser()
    }

    @Test
    fun `parses bar chart with single series`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "chart",
                "data": {
                  "variant": "bar",
                  "title": "Quiz Scores",
                  "x_label": "Day",
                  "y_label": "Score %",
                  "series": [{
                    "label": "Score",
                    "values": [
                      { "x": "Mon", "y": 72 },
                      { "x": "Tue", "y": 85 }
                    ]
                  }]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Chart

        assertEquals(ChartVariant.Bar, block.data.variant)
        assertEquals("Quiz Scores", block.data.title)
        assertEquals("Day", block.data.xLabel)
        assertEquals("Score %", block.data.yLabel)
        assertEquals(1, block.data.series.size)
        assertEquals(2, block.data.series[0].values.size)
        assertEquals("Mon", block.data.series[0].values[0].x)
        assertEquals(72f, block.data.series[0].values[0].y)
    }

    @Test
    fun `parses line chart with two series`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "chart",
                "data": {
                  "variant": "line",
                  "series": [
                    { "label": "Android", "values": [{ "x": "W1", "y": 1200 }, { "x": "W2", "y": 1500 }] },
                    { "label": "iOS",     "values": [{ "x": "W1", "y": 900 },  { "x": "W2", "y": 1100 }] }
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Chart

        assertEquals(ChartVariant.Line, block.data.variant)
        assertEquals(2, block.data.series.size)
        assertEquals("iOS", block.data.series[1].label)
        assertEquals(1100f, block.data.series[1].values[1].y)
    }

    @Test
    fun `parses pie chart`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "chart",
                "data": {
                  "variant": "pie",
                  "series": [
                    { "label": "Organic", "values": [{ "x": "Organic", "y": 45 }] },
                    { "label": "Direct",  "values": [{ "x": "Direct", "y": 28 }] }
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Chart

        assertEquals(ChartVariant.Pie, block.data.variant)
        assertEquals(2, block.data.series.size)
    }

    @Test
    fun `optional fields default to null when absent`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "chart",
                "data": {
                  "variant": "bar",
                  "series": [{ "label": "S", "values": [{ "x": "a", "y": 1 }] }]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Chart

        assertNull(block.data.title)
        assertNull(block.data.xLabel)
        assertNull(block.data.yLabel)
    }

    @Test
    fun `unknown variant falls back to AuiBlock Unknown`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "chart",
                "data": {
                  "variant": "scatter",
                  "series": [{ "label": "S", "values": [{ "x": "a", "y": 1 }] }]
                }
              }]
            }
            """.trimIndent(),
        )
        assertTrue("Unknown variant should fall back to Unknown, got $block", block is AuiBlock.Unknown)
        assertEquals("chart", (block as AuiBlock.Unknown).type)
    }

    @Test
    fun `chart block round-trips through AuiBlockSerializer`() {
        val raw = """
            {
              "type": "chart",
              "data": {
                "variant": "bar",
                "series": [{ "label": "S", "values": [{ "x": "a", "y": 1.5 }] }]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(AuiBlock.serializer(), raw) as AuiBlock.Chart
        assertEquals(ChartVariant.Bar, decoded.data.variant)
        assertEquals("S", decoded.data.series.single().label)
        assertEquals(1.5f, decoded.data.series.single().values.single().y)
    }

    @Test
    fun `chart is advertised in catalog prompt`() {
        assertTrue("chart" in AuiCatalogPrompt.ALL_COMPONENT_TYPES)
        val output = AuiCatalogPrompt.generate()
        val componentsSection = output.substringAfter("AVAILABLE COMPONENTS:")
            .substringBefore("PLUGIN COMPONENTS:", output.substringAfter("AVAILABLE COMPONENTS:"))
        assertTrue("Generated prompt should document the chart component", componentsSection.contains("chart("))
    }

    private fun parseSingleBlock(raw: String): AuiBlock {
        val response = parser.parse(raw)
        assertNotNull(response.blocks)
        return response.blocks.single()
    }
}
