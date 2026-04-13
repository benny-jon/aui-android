package com.bennyjon.aui.compose.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineMarkdownTest {

    private val bodyStyle = TextStyle(fontSize = 14.sp)
    private val codeStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    private val linkColor = Color.Blue

    private fun parse(source: String) = parseInlineMarkdown(
        source = source,
        bodyStyle = bodyStyle,
        codeStyle = codeStyle,
        linkColor = linkColor,
    )

    // ── Plain text ──────────────────────────────────────────────────────────

    @Test
    fun `plain text passes through unchanged`() {
        val result = parse("Hello, world!")
        assertEquals("Hello, world!", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── Bold ────────────────────────────────────────────────────────────────

    @Test
    fun `bold text has FontWeight Bold span`() {
        val result = parse("This is **bold** text")
        assertEquals("This is bold text", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        assertEquals(8, boldSpans[0].start)  // "bold" starts at index 8
        assertEquals(12, boldSpans[0].end)   // "bold" ends at index 12
    }

    // ── Italic ──────────────────────────────────────────────────────────────

    @Test
    fun `italic with asterisks`() {
        val result = parse("This is *italic* text")
        assertEquals("This is italic text", result.text)

        val italicSpans = result.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italicSpans.size)
        assertEquals(8, italicSpans[0].start)
        assertEquals(14, italicSpans[0].end)
    }

    @Test
    fun `italic with underscores`() {
        val result = parse("This is _italic_ text")
        assertEquals("This is italic text", result.text)

        val italicSpans = result.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italicSpans.size)
        assertEquals(8, italicSpans[0].start)
        assertEquals(14, italicSpans[0].end)
    }

    // ── Code ────────────────────────────────────────────────────────────────

    @Test
    fun `code span uses code style`() {
        val result = parse("Use `println()` here")
        assertEquals("Use println() here", result.text)

        val codeSpans = result.spanStyles.filter {
            it.item.fontFamily == FontFamily.Monospace
        }
        assertEquals(1, codeSpans.size)
        assertEquals(4, codeSpans[0].start)
        assertEquals(13, codeSpans[0].end)
    }

    // ── Links ───────────────────────────────────────────────────────────────

    @Test
    fun `link renders label only with url annotation`() {
        val result = parse("Click [here](https://example.com) now")
        assertEquals("Click here now", result.text)

        val links = result.getLinkAnnotations(6, 10)
        assertEquals(1, links.size)
        val annotation = links[0].item
        assertTrue(annotation is LinkAnnotation.Url)
        assertEquals("https://example.com", (annotation as LinkAnnotation.Url).url)
    }

    @Test
    fun `mailto link is supported`() {
        val result = parse("Email [us](mailto:hi@example.com)")
        assertEquals("Email us", result.text)

        val links = result.getLinkAnnotations(6, 8)
        assertEquals(1, links.size)
        val annotation = links[0].item as LinkAnnotation.Url
        assertEquals("mailto:hi@example.com", annotation.url)
    }

    // ── Graceful degradation ────────────────────────────────────────────────

    @Test
    fun `unterminated bold renders literally`() {
        val result = parse("**unterminated")
        assertEquals("**unterminated", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `unterminated italic asterisk renders literally`() {
        val result = parse("*unterminated")
        assertEquals("*unterminated", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `unterminated code renders literally`() {
        val result = parse("Use `println() here")
        assertEquals("Use `println() here", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    fun `nested emphasis renders outer bold with literal inner stars`() {
        // Nested emphasis is out of scope. The outer ** pair wins;
        // inner *italic* renders as literal *italic*.
        val result = parse("**bold with *italic* inside**")
        assertEquals("bold with *italic* inside", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        assertEquals(0, boldSpans[0].start)
        assertEquals(25, boldSpans[0].end)
    }

    @Test
    fun `code with stars inside stays literal`() {
        val result = parse("`code with *stars* inside`")
        assertEquals("code with *stars* inside", result.text)

        // Only the code span, no italic
        val codeSpans = result.spanStyles.filter {
            it.item.fontFamily == FontFamily.Monospace
        }
        assertEquals(1, codeSpans.size)
        val italicSpans = result.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicSpans.isEmpty())
    }

    @Test
    fun `snake_case_identifier renders as literal text`() {
        val result = parse("Use snake_case_identifier here")
        assertEquals("Use snake_case_identifier here", result.text)

        val italicSpans = result.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italicSpans.isEmpty())
    }

    @Test
    fun `javascript url rejected renders as literal text`() {
        val result = parse("[label](javascript:alert(1))")
        // The entire [label](javascript:alert(1)) renders literally
        assertEquals("[label](javascript:alert(1))", result.text)
        assertTrue(result.getLinkAnnotations(0, result.text.length).isEmpty())
    }

    // ── Multiple formats ────────────────────────────────────────────────────

    @Test
    fun `multiple formats in one string`() {
        val result = parse("**Bold** and *italic* and `code`")
        assertEquals("Bold and italic and code", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)

        val italicSpans = result.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italicSpans.size)

        val codeSpans = result.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }
        assertEquals(1, codeSpans.size)
    }

    @Test
    fun `empty string produces empty result`() {
        val result = parse("")
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `http link is supported`() {
        val result = parse("[site](http://example.com)")
        assertEquals("site", result.text)

        val links = result.getLinkAnnotations(0, 4)
        assertEquals(1, links.size)
    }
}
