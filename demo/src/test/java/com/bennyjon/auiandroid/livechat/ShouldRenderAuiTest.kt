package com.bennyjon.auiandroid.livechat

import com.bennyjon.aui.core.model.AuiDisplay
import com.bennyjon.aui.core.model.AuiResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldRenderAuiTest {

    @Test
    fun `not spent inline is rendered`() {
        val response = AuiResponse(display = AuiDisplay.EXPANDED)
        assertTrue(shouldRenderAui(response, isSpent = false))
    }

    @Test
    fun `not spent sheet is rendered`() {
        val response = AuiResponse(display = AuiDisplay.SHEET)
        assertTrue(shouldRenderAui(response, isSpent = false))
    }

    @Test
    fun `spent inline is rendered grayed out`() {
        val response = AuiResponse(display = AuiDisplay.EXPANDED)
        assertTrue(shouldRenderAui(response, isSpent = true))
    }

    @Test
    fun `spent expanded is rendered grayed out`() {
        val response = AuiResponse(display = AuiDisplay.EXPANDED)
        assertTrue(shouldRenderAui(response, isSpent = true))
    }

    @Test
    fun `spent sheet is hidden`() {
        val response = AuiResponse(display = AuiDisplay.SHEET)
        assertFalse(shouldRenderAui(response, isSpent = false.not()))
    }
}
