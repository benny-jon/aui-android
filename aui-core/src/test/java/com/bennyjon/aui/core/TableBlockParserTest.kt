package com.bennyjon.aui.core

import com.bennyjon.aui.core.model.AuiBlock
import com.bennyjon.aui.core.model.data.BadgeTone
import com.bennyjon.aui.core.model.data.TableAlign
import com.bennyjon.aui.core.model.data.TableCell
import com.bennyjon.aui.core.model.data.TableColumnType
import com.bennyjon.aui.core.model.data.TableNumberFormat
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TableBlockParserTest {

    private lateinit var parser: AuiParser
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        parser = AuiParser()
    }

    @Test
    fun `parses table with bare primitive cells`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "title": "Scores",
                  "columns": [
                    { "label": "Name", "type": "text" },
                    { "label": "Score", "type": "number", "format": "integer" }
                  ],
                  "rows": [
                    ["Ada", 42],
                    ["Linus", 37]
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertEquals("Scores", block.data.title)
        assertEquals(2, block.data.columns.size)
        assertEquals(TableColumnType.Text, block.data.columns[0].type)
        assertEquals(TableColumnType.Number, block.data.columns[1].type)
        assertEquals(TableNumberFormat.Integer, block.data.columns[1].format)

        val row0 = block.data.rows[0]
        assertEquals(TableCell.Text("Ada"), row0[0])
        assertEquals(TableCell.Number(42.0), row0[1])
    }

    @Test
    fun `parses object cells for badges numbers and text`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [
                    { "label": "Name", "type": "text" },
                    { "label": "Revenue", "type": "number", "format": "currency" },
                    { "label": "Status", "type": "badge" }
                  ],
                  "rows": [
                    [
                      { "text": "Ada" },
                      { "value": 12450.5, "format": "currency" },
                      { "text": "Top", "tone": "success" }
                    ]
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        val row0 = block.data.rows[0]
        assertEquals(TableCell.Text("Ada"), row0[0])
        assertEquals(TableCell.Number(12450.5, TableNumberFormat.Currency), row0[1])
        assertEquals(TableCell.Badge("Top", BadgeTone.Success), row0[2])
    }

    @Test
    fun `unknown badge tone defaults to Info`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [{ "label": "Status", "type": "badge" }],
                  "rows": [[{ "text": "Hmm", "tone": "fuchsia" }]]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertEquals(TableCell.Badge("Hmm", BadgeTone.Info), block.data.rows[0][0])
    }

    @Test
    fun `malformed cells decode to Empty`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [
                    { "label": "A", "type": "text" },
                    { "label": "B", "type": "number" }
                  ],
                  "rows": [
                    [null, true],
                    [{ "nothing": "here" }, 1]
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertEquals(TableCell.Empty, block.data.rows[0][0])
        assertEquals(TableCell.Empty, block.data.rows[0][1])
        assertEquals(TableCell.Empty, block.data.rows[1][0])
        assertEquals(TableCell.Number(1.0), block.data.rows[1][1])
    }

    @Test
    fun `row length mismatch is preserved as-is by the parser`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [
                    { "label": "A", "type": "text" },
                    { "label": "B", "type": "text" },
                    { "label": "C", "type": "text" }
                  ],
                  "rows": [
                    ["a"],
                    ["a", "b", "c", "d"]
                  ]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertEquals(1, block.data.rows[0].size)
        assertEquals(4, block.data.rows[1].size)
    }

    @Test
    fun `optional column fields default to null`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [{ "label": "A", "type": "text" }],
                  "rows": [["x"]]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertNull(block.data.title)
        assertNull(block.data.columns[0].format)
        assertNull(block.data.columns[0].align)
    }

    @Test
    fun `table block round-trips through AuiBlockSerializer`() {
        val raw = """
            {
              "type": "table",
              "data": {
                "columns": [
                  { "label": "Name", "type": "text" },
                  { "label": "Stars", "type": "rating" }
                ],
                "rows": [["Ada", 4.5]]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(AuiBlock.serializer(), raw) as AuiBlock.Table
        assertEquals("Name", decoded.data.columns[0].label)
        assertEquals(TableColumnType.RatingStars, decoded.data.columns[1].type)
        assertEquals(TableCell.Text("Ada"), decoded.data.rows[0][0])
        assertEquals(TableCell.Number(4.5), decoded.data.rows[0][1])
    }

    @Test
    fun `legacy rating_stars column type still parses`() {
        val raw = """
            {
              "type": "table",
              "data": {
                "columns": [
                  { "label": "Stars", "type": "rating_stars" }
                ],
                "rows": [[4.5]]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(AuiBlock.serializer(), raw) as AuiBlock.Table
        assertEquals(TableColumnType.RatingStars, decoded.data.columns[0].type)
    }

    @Test
    fun `align accepts english aliases alongside canonical values`() {
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "columns": [
                    { "label": "A", "type": "text",   "align": "left"   },
                    { "label": "B", "type": "text",   "align": "right"  },
                    { "label": "C", "type": "text",   "align": "center" },
                    { "label": "D", "type": "number", "align": "end"    }
                  ],
                  "rows": [["x", "y", "z", 1]]
                }
              }]
            }
            """.trimIndent(),
        ) as AuiBlock.Table

        assertEquals(TableAlign.Start, block.data.columns[0].align)
        assertEquals(TableAlign.End, block.data.columns[1].align)
        assertEquals(TableAlign.Center, block.data.columns[2].align)
        assertEquals(TableAlign.End, block.data.columns[3].align)
    }

    @Test
    fun `table with english-alias align survives tolerant parse instead of dropping to unknown`() {
        // Regression: a model-emitted `"align": "right"` previously failed the whole
        // column's deserialization and dropped the table to AuiBlock.Unknown.
        val block = parseSingleBlock(
            """
            {
              "display": "inline",
              "blocks": [{
                "type": "table",
                "data": {
                  "title": "Portable Laptop Options",
                  "columns": [
                    { "label": "Model", "type": "text" },
                    { "label": "Weight", "type": "number", "format": "decimal", "align": "right" },
                    { "label": "Price", "type": "number", "format": "currency", "align": "right" }
                  ],
                  "rows": [["Apple MacBook Air M2", 2.7, 999]]
                }
              }]
            }
            """.trimIndent(),
        )

        assertTrue("Table should parse as Table, not Unknown", block is AuiBlock.Table)
    }

    @Test
    fun `table is advertised in catalog prompt`() {
        assertTrue("table" in AuiCatalogPrompt.ALL_COMPONENT_TYPES)
        val output = AuiCatalogPrompt.generate()
        val componentsSection = output.substringAfter("AVAILABLE COMPONENTS:")
            .substringBefore("PLUGIN COMPONENTS:", output.substringAfter("AVAILABLE COMPONENTS:"))
        assertTrue("Generated prompt should document the table component", componentsSection.contains("table("))
    }

    private fun parseSingleBlock(raw: String): AuiBlock {
        val response = parser.parse(raw)
        assertNotNull(response.blocks)
        return response.blocks.single()
    }
}
