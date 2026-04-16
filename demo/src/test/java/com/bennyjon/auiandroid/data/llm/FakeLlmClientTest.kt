package com.bennyjon.auiandroid.data.llm

import com.bennyjon.aui.core.model.AuiDisplay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FakeLlmClientTest {

    private val client = FakeLlmClient()

    @Test
    fun `first response returns raw content`() = runTest {
        val result = client.complete("system", emptyList())

        assertNotNull(result.rawContent)
        assertNull(result.errorMessage)
    }

    @Test
    fun `first response parses to text-only greeting`() = runTest {
        val result = client.complete("system", emptyList())
        val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)

        assertNotNull(parsed.text)
        assertNull(parsed.auiResponse)
    }

    @Test
    fun `second response parses to text and inline AUI poll`() = runTest {
        client.complete("system", emptyList()) // skip first
        val result = client.complete("system", emptyList())
        val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)

        assertNotNull(parsed.text)
        assertNotNull(parsed.auiResponse)
        assertEquals(AuiDisplay.EXPANDED, parsed.auiResponse!!.display)
    }

    @Test
    fun `third response parses to text and sheet AUI`() = runTest {
        repeat(2) { client.complete("system", emptyList()) }
        val result = client.complete("system", emptyList())
        val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)

        assertNotNull(parsed.text)
        assertNotNull(parsed.auiResponse)
        assertEquals(AuiDisplay.SHEET, parsed.auiResponse!!.display)
        assertNotNull(parsed.auiResponse!!.sheetTitle)
    }

    @Test
    fun `fourth response parses to text and inline confirmation AUI`() = runTest {
        repeat(3) { client.complete("system", emptyList()) }
        val result = client.complete("system", emptyList())
        val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)

        assertNotNull(parsed.text)
        assertNotNull(parsed.auiResponse)
        assertEquals(AuiDisplay.EXPANDED, parsed.auiResponse!!.display)
    }

    @Test
    fun `responses cycle back to first after fourth`() = runTest {
        repeat(4) { client.complete("system", emptyList()) }
        val result = client.complete("system", emptyList())
        val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)

        assertNotNull(parsed.text)
        assertNull(parsed.auiResponse)
    }

    @Test
    fun `all scripted responses return raw content and parse without errors`() = runTest {
        for (i in FakeLlmClient.SCRIPTED_RESPONSES.indices) {
            val result = client.complete("system", emptyList())
            assertNull("Response $i had error: ${result.errorMessage}", result.errorMessage)
            assertNotNull("Response $i missing rawContent", result.rawContent)

            val parsed = AuiResponseExtractor.fromRawResponse(result.rawContent!!)
            assertNull("Response $i parsed with error: ${parsed.errorMessage}", parsed.errorMessage)
            assertNotNull("Response $i parsed missing text", parsed.text)
        }
    }
}
