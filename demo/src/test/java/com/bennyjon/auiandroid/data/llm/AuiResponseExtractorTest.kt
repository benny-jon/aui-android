package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.model.AuiDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuiResponseExtractorTest {

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
}
