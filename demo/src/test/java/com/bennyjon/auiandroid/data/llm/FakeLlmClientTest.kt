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
    fun `first response is text-only greeting`() = runTest {
        val response = client.complete("system", emptyList())

        assertNotNull(response.text)
        assertNull(response.auiResponse)
        assertNull(response.errorMessage)
    }

    @Test
    fun `second response has text and inline AUI poll`() = runTest {
        client.complete("system", emptyList()) // skip first
        val response = client.complete("system", emptyList())

        assertNotNull(response.text)
        assertNotNull(response.auiResponse)
        assertEquals(AuiDisplay.INLINE, response.auiResponse!!.display)
    }

    @Test
    fun `third response has text and sheet AUI`() = runTest {
        repeat(2) { client.complete("system", emptyList()) }
        val response = client.complete("system", emptyList())

        assertNotNull(response.text)
        assertNotNull(response.auiResponse)
        assertEquals(AuiDisplay.SHEET, response.auiResponse!!.display)
        assertNotNull(response.auiResponse!!.sheetTitle)
    }

    @Test
    fun `fourth response has text and inline confirmation AUI`() = runTest {
        repeat(3) { client.complete("system", emptyList()) }
        val response = client.complete("system", emptyList())

        assertNotNull(response.text)
        assertNotNull(response.auiResponse)
        assertEquals(AuiDisplay.INLINE, response.auiResponse!!.display)
    }

    @Test
    fun `responses cycle back to first after fourth`() = runTest {
        repeat(4) { client.complete("system", emptyList()) }
        val response = client.complete("system", emptyList())

        assertNotNull(response.text)
        assertNull(response.auiResponse)
    }

    @Test
    fun `all scripted responses parse without errors`() = runTest {
        for (i in FakeLlmClient.SCRIPTED_RESPONSES.indices) {
            val response = client.complete("system", emptyList())
            assertNull("Response $i had error: ${response.errorMessage}", response.errorMessage)
            assertNotNull("Response $i missing text", response.text)
        }
    }
}
