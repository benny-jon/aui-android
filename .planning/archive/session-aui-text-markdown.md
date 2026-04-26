# Session: Inline Markdown support for `AuiText`

Read `CLAUDE.md` first.

## Goal

Make `AuiText` (the `text` block component in `aui-compose`) render basic
inline Markdown as styled Compose text, per the spec's promise that `text`
"supports basic markdown (bold, italic, code, links)".

**In scope (inline only):**

- `**bold**`
- `*italic*` and `_italic_`
- `` `code` ``
- `[label](https://example.com)`

**Explicitly out of scope** (document this in KDoc on `AuiText`):

- Headings, lists, blockquotes, tables, images, HTML — the AI uses separate
  blocks (`heading`, `list_simple`, `image_single`, etc.) for structure.
- Reference-style links, escapes, nested emphasis edge cases. If a host app
  needs full CommonMark, it can register an `AuiComponentPlugin` override
  for the `text` component type.

## Approach

Add a pure-Kotlin inline parser that produces a Compose `AnnotatedString`,
then have `AuiText` render it. No new dependencies.

### Files to create

1. `aui-compose/src/main/kotlin/com/bennyjon/aui/compose/text/InlineMarkdown.kt`
   - Single public function:
     ```kotlin
     internal fun parseInlineMarkdown(
         source: String,
         bodyStyle: TextStyle,
         codeStyle: TextStyle,
         linkColor: Color,
     ): AnnotatedString
     ```
   - Single-pass scanner. Branch order inside the loop:
     1. Code span (`` ` ... ` ``) — consumed first so `*` inside code stays literal.
     2. Link (`[label](url)`) — only accept `http://`, `https://`, or `mailto:` URLs; anything else falls through as literal text.
     3. Bold (`**...**`) — checked before italic so `**` isn't mis-parsed as two italics.
     4. Italic (`*...*` or `_..._`) — `_` only treats as italic when flanked by non-word characters on the opening side (prevents `snake_case_names` from becoming italic).
     5. Literal character — append as-is.
   - Use `AnnotatedString.Builder` with `withStyle(SpanStyle(...))` for bold/italic/code,
     and `withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))))` for links.
     (Requires Compose UI 1.7+; confirm the version in `aui-compose/build.gradle.kts`
     and upgrade if needed — it's already a de facto baseline for modern Compose apps.)
   - Graceful degradation: if a delimiter never closes, emit the opening
     delimiter and everything after it as literal text. No exceptions thrown.

### Files to modify

2. `aui-compose/src/main/kotlin/com/bennyjon/aui/compose/components/AuiText.kt`
   - Replace the plain `Text(data.text, ...)` call with:
     ```kotlin
     val theme = AuiTheme.current
     val annotated = remember(data.text, theme) {
         parseInlineMarkdown(
             source = data.text,
             bodyStyle = theme.typography.body,
             codeStyle = theme.typography.code,
             linkColor = theme.colors.primary,
         )
     }
     Text(text = annotated, style = theme.typography.body, modifier = modifier)
     ```
   - Update the KDoc to describe supported Markdown and the "use other blocks
     for structure" rule. Add a line noting that hosts wanting full CommonMark
     should register a component plugin override.
   - Update the `@Preview` to include a sample with all four formatting types
     plus one with a malformed unclosed `**` to verify graceful degradation
     renders visibly sane.

### Tests

3. `aui-compose/src/test/kotlin/com/bennyjon/aui/compose/text/InlineMarkdownTest.kt`
   - Pure Kotlin tests (no Compose runtime needed — `AnnotatedString` and
     `SpanStyle` are instantiable in a unit test).
   - Cover:
     - Plain text passes through unchanged.
     - `**bold**` produces one `FontWeight.Bold` span over the right range.
     - `*italic*` and `_italic_` both produce an italic span.
     - `` `code` `` produces a span matching `codeStyle`.
     - `[label](https://x.com)` produces a `LinkAnnotation.Url` over `label`, and `label` is what appears in the rendered string (no brackets, no URL).
     - `**unterminated` renders literally, no crash.
     - `**bold with *italic* inside**` — the outer bold wins; inner `*italic*` renders as literal `*italic*`. (Document this limitation in the parser's KDoc; nested emphasis is explicitly out of scope.)
     - `` `code with *stars* inside` `` — stars stay literal inside code.
     - `snake_case_identifier` renders as literal text with no italic span
       (word-flanked `_` rule).
     - `[label](javascript:alert(1))` renders as literal text — unsupported URL scheme falls through.
   - Assert via `AnnotatedString.spanStyles` and `AnnotatedString.getLinkAnnotations(...)`.

### Don't touch

- `rich_text` — that's a separate block with structured spans, unrelated.
- The parser lives in `aui-compose` because `AnnotatedString` is a Compose type.
  Do **not** move it to `aui-core` — that module stays Compose-free.
- `aui-spec-v1.md` — wire format is unchanged. The `text` block already
  documents Markdown support; no spec edits needed.

## Acceptance

- `./gradlew :aui-compose:test` passes, including every case above.
- `./gradlew :aui-compose:lint` clean.
- Previews for `AuiText` render all four formatting types plus the
  graceful-degradation case.
- Git diff review: no new dependencies in any `build.gradle.kts`.

## Session log entry

Append one line under the current phase in `CLAUDE.md`:

```
- Session 26: AuiText inline Markdown (bold/italic/code/links) via AnnotatedString parser
```

Update the "Next:" pointer to whatever comes after this session.
