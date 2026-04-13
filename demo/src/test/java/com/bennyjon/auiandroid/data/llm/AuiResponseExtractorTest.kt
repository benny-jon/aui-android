package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.model.AuiDisplay
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuiResponseExtractorTest {

    // ── Envelope format ──────────────────────────────────────────────

    @Test
    fun `text-only envelope returns text without AUI`() {
        val raw = """{ "text": "Hello world" }"""
        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Hello world", result.text)
        assertNull(result.auiJson)
        assertNull(result.auiResponse)
        assertNull(result.errorMessage)
    }

    @Test
    fun `envelope with text and aui returns both`() {
        val raw = """
        {
          "text": "Here's a poll:",
          "aui": {
            "display": "inline",
            "blocks": [
              { "type": "text", "data": { "text": "Hello" } }
            ]
          }
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Here's a poll:", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.INLINE, result.auiResponse!!.display)
        assertEquals(1, result.auiResponse!!.blocks.size)
        assertNull(result.errorMessage)
    }

    @Test
    fun `envelope with invalid aui JSON still returns text`() {
        val raw = """
        {
          "text": "Here's something:",
          "aui": { "display": "unknown_display" }
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Here's something:", result.text)
        assertNotNull(result.auiJson)
        assertNull(result.auiResponse)
        assertNull(result.errorMessage)
    }

    @Test
    fun `malformed JSON returns error`() {
        val result = AuiResponseExtractor.fromRawResponse("not json at all")

        assertNull(result.text)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `missing text field returns error`() {
        val raw = """{ "aui": { "display": "inline", "blocks": [] } }"""
        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertNull(result.text)
        assertNotNull(result.errorMessage)
        assertEquals("Missing 'text' field in LLM response", result.errorMessage)
    }

    @Test
    fun `error factory sets errorMessage and cause`() {
        val cause = RuntimeException("network failure")
        val result = AuiResponseExtractor.error("Something went wrong", cause)

        assertNull(result.text)
        assertNull(result.auiJson)
        assertNull(result.auiResponse)
        assertEquals("Something went wrong", result.errorMessage)
        assertEquals(cause, result.cause)
    }

    @Test
    fun `error factory without cause sets only errorMessage`() {
        val result = AuiResponseExtractor.error("timeout")

        assertEquals("timeout", result.errorMessage)
        assertNull(result.cause)
    }

    @Test
    fun `envelope with null aui field returns text only`() {
        val raw = """{ "text": "Just text", "aui": null }"""
        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Just text", result.text)
        assertNull(result.auiJson)
        assertNull(result.auiResponse)
    }

    @Test
    fun `extra fields in envelope are ignored`() {
        val raw = """{ "text": "Hello", "metadata": {"foo": "bar"}, "version": 2 }"""
        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Hello", result.text)
        assertNull(result.auiJson)
        assertNull(result.errorMessage)
    }

    @Test
    fun `envelope with id preserves id`() {
        val raw = """{ "id": "env-123", "text": "Hello" }"""
        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("env-123", result.id)
        assertEquals("Hello", result.text)
    }

    // ── Claude Messages API format ───────────────────────────────────

    @Test
    fun `claude api response extracts text and id`() {
        val raw = """
        {
          "id": "msg_019ubUrg77w9aLRva5GAihgf",
          "type": "message",
          "role": "assistant",
          "content": [
            { "type": "text", "text": "Hello from Claude!" }
          ],
          "stop_reason": "end_turn",
          "usage": { "input_tokens": 10, "output_tokens": 5 }
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_019ubUrg77w9aLRva5GAihgf", result.id)
        assertEquals("Hello from Claude!", result.text)
        assertNull(result.auiJson)
        assertNull(result.auiResponse)
        assertNull(result.errorMessage)
    }

    @Test
    fun `claude api response with structured envelope in content unwraps aui`() {
        val raw = """
        {
          "id": "msg_abc",
          "type": "message",
          "role": "assistant",
          "content": [
            {
              "type": "text",
              "text": "{\"text\": \"Here's a poll:\", \"aui\": {\"display\": \"inline\", \"blocks\": [{\"type\": \"text\", \"data\": {\"text\": \"Vote!\"}}]}}"
            }
          ],
          "stop_reason": "end_turn"
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_abc", result.id)
        assertEquals("Here's a poll:", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.INLINE, result.auiResponse!!.display)
    }

    @Test
    fun `claude api response with empty content array returns error`() {
        val raw = """
        {
          "id": "msg_empty",
          "type": "message",
          "content": [],
          "stop_reason": "end_turn"
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertNull(result.text)
        assertNotNull(result.errorMessage)
        assertEquals("No text content in Claude response", result.errorMessage)
    }

    @Test
    fun `claude api response without id still returns text`() {
        val raw = """
        {
          "type": "message",
          "content": [
            { "type": "text", "text": "No id here" }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertNull(result.id)
        assertEquals("No id here", result.text)
    }

    @Test
    fun `claude api response skips non-text content blocks`() {
        val raw = """
        {
          "id": "msg_multi",
          "type": "message",
          "content": [
            { "type": "tool_use", "id": "tool_1", "name": "search" },
            { "type": "text", "text": "Found it!" }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_multi", result.id)
        assertEquals("Found it!", result.text)
    }

    @Test
    fun `full claude api response with usage and stop fields`() {
        val raw = """
        {
          "model": "claude-sonnet-4-5-20250929",
          "id": "msg_019ubUrg77w9aLRva5GAihgf",
          "type": "message",
          "role": "assistant",
          "content": [
            {
              "type": "text",
              "text": "Hey there! I can help you with many things."
            }
          ],
          "stop_reason": "end_turn",
          "stop_sequence": null,
          "usage": {
            "input_tokens": 1761,
            "output_tokens": 240
          }
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_019ubUrg77w9aLRva5GAihgf", result.id)
        assertEquals("Hey there! I can help you with many things.", result.text)
        assertNull(result.auiJson)
        assertNull(result.errorMessage)
    }

    @Test
    fun `claude api response where content text is not an envelope stays as plain text`() {
        val raw = """
        {
          "id": "msg_plain",
          "content": [
            { "type": "text", "text": "Just a regular message, not JSON at all." }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_plain", result.id)
        assertEquals("Just a regular message, not JSON at all.", result.text)
        assertNull(result.auiJson)
        assertNull(result.auiResponse)
    }

    // ── Markdown code fence stripping ───────────────────────────────

    @Test
    fun `claude api response strips markdown code fences around envelope`() {
        val raw = """
        {
          "id": "msg_fence",
          "content": [
            {
              "type": "text",
              "text": "```json\n{\"text\": \"Here's a poll:\", \"aui\": {\"display\": \"inline\", \"blocks\": [{\"type\": \"text\", \"data\": {\"text\": \"Vote!\"}}]}}\n```"
            }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_fence", result.id)
        assertEquals("Here's a poll:", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.INLINE, result.auiResponse!!.display)
    }

    @Test
    fun `claude api response strips code fences without language tag`() {
        val raw = """
        {
          "id": "msg_nolang",
          "content": [
            {
              "type": "text",
              "text": "```\n{\"text\": \"Hello from fenced!\"}\n```"
            }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_nolang", result.id)
        assertEquals("Hello from fenced!", result.text)
    }

    // ── Raw AUI JSON fallback ───────────────────────────────────────

    @Test
    fun `claude api response detects raw AUI JSON with display field`() {
        val raw = """
        {
          "id": "msg_raw_aui",
          "content": [
            {
              "type": "text",
              "text": "{\"display\": \"inline\", \"blocks\": [{\"type\": \"text\", \"data\": {\"text\": \"Hello\"}}]}"
            }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_raw_aui", result.id)
        assertEquals("", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.INLINE, result.auiResponse!!.display)
    }

    @Test
    fun `envelope with expanded AUI and special characters parses correctly`() {
        val raw = loadResource("claude_response_expanded_aui.json")

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("Here's a detailed comparison to help you decide:", result.text)
        assertNotNull("auiJson should not be null", result.auiJson)
        assertNotNull("auiResponse should not be null", result.auiResponse)
        assertEquals(AuiDisplay.EXPANDED, result.auiResponse!!.display)
        assertNull(result.errorMessage)
        val blocks = result.auiResponse!!.blocks
        assert(blocks.size > 10) { "Expected many blocks, got ${blocks.size}" }
    }

    @Test
    fun `claude api response wrapping expanded AUI envelope parses correctly`() {
        val envelope = loadResource("claude_response_expanded_aui.json")
        val raw = wrapInClaudeApiResponse("msg_01NEaZ42ggHy5yrrRkagC5km", envelope)

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_01NEaZ42ggHy5yrrRkagC5km", result.id)
        assertEquals("Here's a detailed comparison to help you decide:", result.text)
        assertNotNull("auiJson should not be null", result.auiJson)
        assertNotNull("auiResponse should not be null", result.auiResponse)
        assertEquals(AuiDisplay.EXPANDED, result.auiResponse!!.display)
        assertNull(result.errorMessage)
        val blocks = result.auiResponse!!.blocks
        assert(blocks.size > 10) { "Expected many blocks, got ${blocks.size}" }
    }

    @Test
    fun `claude api response with text before envelope extracts embedded JSON`() {
        val raw = """
        {
          "id": "msg_embedded",
          "content": [
            {
              "type": "text",
              "text": "Sure, here you go!\n\n{\"text\": \"A poll for you:\", \"aui\": {\"display\": \"inline\", \"blocks\": [{\"type\": \"text\", \"data\": {\"text\": \"Hello\"}}]}}"
            }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_embedded", result.id)
        assertEquals("A poll for you:", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.INLINE, result.auiResponse!!.display)
    }

    @Test
    fun `claude api response detects raw AUI JSON wrapped in code fences`() {
        val raw = """
        {
          "id": "msg_fenced_aui",
          "content": [
            {
              "type": "text",
              "text": "```json\n{\"display\": \"sheet\", \"sheet_title\": \"Survey\", \"steps\": [{\"label\": \"Q1\", \"question\": \"How?\", \"blocks\": [{\"type\": \"text\", \"data\": {\"text\": \"Hi\"}}]}]}\n```"
            }
          ]
        }
        """.trimIndent()

        val result = AuiResponseExtractor.fromRawResponse(raw)

        assertEquals("msg_fenced_aui", result.id)
        assertEquals("", result.text)
        assertNotNull(result.auiJson)
        assertNotNull(result.auiResponse)
        assertEquals(AuiDisplay.SHEET, result.auiResponse!!.display)
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun loadResource(name: String): String =
        this::class.java.classLoader!!
            .getResourceAsStream(name)!!
            .bufferedReader()
            .readText()

    /**
     * Wraps an envelope JSON string inside a Claude Messages API response,
     * the same way the real API returns it (envelope encoded as a JSON string
     * inside `content[0].text`).
     */
    private fun wrapInClaudeApiResponse(id: String, envelopeJson: String): String {
        val escapedText = Json.encodeToString(kotlinx.serialization.serializer<String>(), envelopeJson)
        return """
        {
          "id": "$id",
          "type": "message",
          "role": "assistant",
          "content": [{ "type": "text", "text": $escapedText }],
          "stop_reason": "end_turn"
        }
        """.trimIndent()
    }
}
