# Library Sanity Checks Plan

Pre-release sanity audit for `aui-core` and `aui-compose` before tagging the
first release version. Each section below is a discrete session that can be
executed one at a time. Sessions are ordered roughly by risk: highest-impact
first (things that bake into the public ABI or crash on release builds),
followed by hygiene and verification.

Each session should:
- run the listed checks
- log findings inline (or link a follow-up task)
- fix issues that are clearly in-scope, or open a tracked follow-up if they
  expand the session
- end by updating `AGENTS.md` if status changed

The goal is **release confidence**, not a rewrite. Bias toward fixing trivial
issues in-session and deferring anything ambiguous to its own task.

---

## Session S1 — Localization & user-visible strings ✅

Goal: ensure no hardcoded user-visible strings remain in the library that the
host app cannot translate.

Checks:
- Audit literal strings in `aui-compose/src/main`, especially inside
  `Text(...)`, `contentDescription = "..."`, error/empty states, button labels
  ("Submit", "Loading…", "Retry", "Show more", etc.).
  - `grep -rn '"[^"]*"' aui-compose/src/main`
- Audit `aui-core` for default error/validation/prompt strings. Decide per
  string: developer-facing (English-only is fine) vs user-facing (must be
  parameterized or in `strings.xml`).
- For each user-visible string, choose:
  - move to `strings.xml` with `aui_` prefix so hosts can override per-locale,
    OR
  - take the label as a parameter from server payload / host call site.
- Confirm bundled `strings.xml` keys are stable and English-only (host owns
  translations).

Exit criteria:
- No user-visible literal strings remain in composables.
- Any new `strings.xml` entries use the `aui_` prefix.

Findings (executed 2026-04-28):
- `aui-core` had **no** user-visible literal strings — every literal is either
  KDoc, internal `error()` for developer crashes, or part of the catalog prompt
  served to the LLM (English-by-design).
- `aui-compose` had hardcoded user-visible strings in
  `AuiSurveyContent.kt` (Back / Next / Submit nav labels, "Survey skipped",
  "Survey submitted", "(N question(s) skipped)"),
  `AuiResponseCard.kt` ("Viewing", fallback titles "Survey" / "Tap to view",
  step-count summaries),
  `AuiFileContent.kt` (Download / Copy / Open / Dismiss action labels and
  "Saved to Downloads" / "Couldn't save to Downloads" notices, "File"
  fallback headline),
  `AuiInputTextSingle.kt` ("Submit" fallback button label),
  `AuiInputRatingStars.kt` (`"Star $n"` content description), and
  `AuiText.kt` ("Text" fallback fenced-code title).
- All of the above moved into a new `aui-compose/src/main/res/values/strings.xml`
  with `aui_` prefixed keys; counts that vary on quantity (`questions skipped`
  / `N question(s)` summary) use Android `<plurals>` so hosts get correct
  locale-aware forms automatically.
- `buildSurveyFormattedEntries` is non-Composable (called from tests and the
  finalize() path), so it now takes a `SurveyFormatStrings` data class with
  English defaults; the composable resolves the localized strings via
  `Context.getString` / `Resources.getQuantityString` and threads them through
  `SurveyFlowState.finalize(...)`. Existing `buildSurveyFormattedEntries` test
  signatures remain green via the default parameter.
- `AuiResponseCard` plural step-count summary now goes through
  `pluralStringResource(R.plurals.aui_card_step_count, ...)`; non-Composable
  helpers were narrowed to take resolved strings as parameters.
- Verified: `./gradlew :aui-core:test :aui-compose:test` green.

---

## Session S2 — Theming: no hardcoded colors / dimens / typography ✅

Goal: every visual property routes through `AuiTheme` (colors, spacing,
typography, shapes), not literals or `MaterialTheme.*` directly.

Checks:
- `grep -rn 'Color(' aui-compose/src/main` — every literal color should come
  from `AuiColors`.
- `grep -rn '\.dp' aui-compose/src/main | grep -v AuiSpacing` — paddings/sizes
  should come from `AuiSpacing` or be parameters.
- `grep -rn '\.sp' aui-compose/src/main` — font sizes should come from
  `AuiTypography`.
- `grep -rn 'MaterialTheme\.' aui-compose/src/main` — locked decision says
  route through AUI abstractions, not Material directly.
- `FontWeight`, `TextStyle`, `Shape` literals — same audit.

Exit criteria:
- All visual styling reads from `AuiTheme` or is an explicit parameter.

Findings (executed 2026-04-28):
- The audit found non-theme visual literals in `AuiResponseCard`, `AuiSurveyContent`,
  `AuiFileContent`, `AuiButtonSecondary`, `AuiInputRatingStars`,
  `AuiStepperHorizontal`, `AuiTable`, `AuiChart`, and `InlineMarkdown`.
  Offenders included hardcoded border widths / paddings / icon sizes, direct
  `CircleShape` / `RoundedCornerShape` usage, one `FontWeight` override in the
  survey shell, table header typography overrides, a `Color.Black` striped-row
  overlay, and a custom chart palette using literal teal/amber colors.
- `AuiSpacing` now owns the previously hardcoded dimensional tokens needed by
  renderer components, including micro-spacing, chart sizing, table width
  budgets, rating-star sizing, and stepper indicator sizing. Components now
  read those values through `LocalAuiTheme` instead of local `.dp` literals.
- `AuiTable` now routes its border shape through `theme.shapes.banner`, uses
  `theme.typography.label` for header cells, and uses a themed
  `surfaceVariant` overlay for alternating rows rather than `Color.Black`.
- `AuiChart` now reads sizing from `AuiTheme.spacing`, uses
  `theme.shapes.badge` for legend markers, and derives its secondary/tertiary
  series colors from semantic theme colors (`info` / `warning`) instead of
  literal `Color(...)` values.
- `InlineMarkdown` no longer hardcodes bold weight internally; callers provide
  the emphasis span style so text emphasis can stay aligned with AUI typography.
- Verification: `rg` scans for `Color(`, `.dp`, `MaterialTheme.*`, and
  `FontWeight` / shape literals outside `theme/` returned no matches after the
  refactor. `./gradlew :aui-compose:compileDebugKotlin` and
  `./gradlew :aui-compose:testDebugUnitTest` both passed.

---

## Session S3 — Accessibility ✅

Goal: components are usable with TalkBack, dynamic font scale, and RTL.

Checks:
- Every clickable icon/image has a `contentDescription` (or `null` only when
  truly decorative).
- Min touch target ≥ 48dp on interactive components (buttons, chips, radios,
  rating stars).
- Dynamic font scale: no font-size math that breaks at 1.3× / 2× scale; verify
  text doesn't clip in previews at large scales.
- RTL: use `start`/`end` not `left`/`right` in `Modifier.padding`, alignment,
  and any custom `Layout`.
- Semantics: input components expose `Role`, `stateDescription` where relevant;
  lists/tables expose row/column semantics.

Findings (audit on 2026-04-29):
- Clean: no `Left`/`Right` directional usage anywhere in `aui-compose/src/main`
  — no `TextAlign.Left`/`Right`, no `AbsoluteAlignment`, no `paddingLeft/Right`.
- Clean: no `.sp` literals or hand-rolled font-size math outside theme. Font
  scaling rides on Material3 typography defaults.
- Clean: all `Icon(...)` call sites set `contentDescription` (or explicit
  `null` for decorative icons inside larger semantic units).
- Clean: Material3 components (Button, IconButton, FilterChip, RadioButton,
  Checkbox, Slider, SuggestionChip) carry built-in 48dp interactive sizing.
  Custom click targets (rating stars, file action buttons, dismiss) already
  use `Modifier.size(theme.spacing.minimumTouchTarget)`.

Fixed in-session:
- `SelectionRow` (used by `AuiRadioList` + `AuiCheckboxList`) was using a raw
  `Modifier.clickable` so TalkBack didn't know each row acted as a radio /
  checkbox. Added a `role: Role` parameter and switched to
  `Modifier.selectable(selected, role, onClick)`. Wired `Role.RadioButton` from
  `AuiRadioList` and `Role.Checkbox` from `AuiCheckboxList`.
- `AuiInputRatingStars` star clickables now declare `role = Role.Button` so
  TalkBack announces each star as a button rather than a static image.
- `AuiFileContent` "Open" action in the download notice was a bare
  `Modifier.clickable` on a `Text` (no `Role.Button`, sub-48dp touch target).
  Replaced with a Material3 `TextButton` so it inherits proper sizing and
  semantics.
- Table `ReadonlyRatingStars` had every star icon set to
  `contentDescription = null` with no aggregate semantics, leaving the rating
  unannounced. Wrapped the row in
  `Modifier.semantics(mergeDescendants = true) { contentDescription = ... }`
  using a new `aui_table_rating_stars_content_description` string
  ("X of N stars").

Second-pass fixes (after device TalkBack pass on 2026-04-29):
- `AuiText` was being skipped by TalkBack — `AnnotatedString` with
  `LinkAnnotation` runs splits the semantic tree in ways that left the
  surrounding plain runs unannounced on some devices. Added
  `Modifier.semantics(mergeDescendants = true) { contentDescription =
  annotated.text }` so the merged subtree announces a single, link-stripped
  reading of the text.
- `AuiChart` Canvas now exposes a generated content description via
  `Modifier.clearAndSetSemantics`. Description summarizes the variant
  (bar / line / pie), optional title, axis labels (when present for
  bar/line), and per-series points or pie-slice percentages. The whole chart
  is now one focusable, readable element.
- `AuiBadge*` (success / info / warning / error) now apply
  `clearAndSetSemantics { contentDescription = "<intent>: <text>" }` so
  TalkBack announces the intent before the message.
- `AuiStatusBanner*` (success / info / warning / error) apply the same
  intent-prefixed `contentDescription` and add `liveRegion =
  LiveRegionMode.Polite` so banners are announced when they appear without
  stealing focus from the user's current interaction.
- `AuiInputRatingStars` now collapses to one accessible node via
  `Modifier.semantics(mergeDescendants = true)` with `contentDescription =
  "Rated 3 of 5 stars"` and `customActions` exposing five "Rate N stars"
  actions. TalkBack three-finger-tap (or the equivalent custom-action menu)
  surfaces all five rating choices without the user having to navigate to
  each star individually. Per-star icons keep their `Role.Button` for users
  who do prefer direct touch.
- `AuiStepperHorizontal` collapses to one accessible node via
  `Modifier.clearAndSetSemantics` with description "Step X of N: <label>.
  N steps remaining" (or "X of X steps complete: <label>" when current is
  past the last index). TalkBack now announces position, name, and
  remaining count as a single utterance instead of reading each circle's
  number and label separately.

Strings introduced:
`aui_input_rating_announce_rated`, `aui_input_rating_announce_unrated`,
`aui_input_rating_action_rate` (plural),
`aui_status_intent_{success,info,warning,error}`, `aui_status_announce`,
`aui_chart_variant_{bar,line,pie}`, `aui_chart_announce_titled`,
`aui_chart_announce_untitled`, `aui_chart_axis_labels`,
`aui_chart_series_label`, `aui_chart_pie_slice`, `aui_chart_point`,
`aui_chart_empty`, `aui_stepper_announce`,
`aui_stepper_announce_complete`, `aui_stepper_remaining` (plural).

Still deferred:
- `AuiQuickReplies` uses Material3 `SuggestionChip` (selected state colored
  but not in semantics). Acceptable for one-shot reply use, but worth
  revisiting if chips ever represent a true selection set.

Exit criteria:
- A11y review notes filed; obvious gaps fixed. Verified with
  `./gradlew :aui-compose:compileDebugKotlin` and
  `./gradlew :aui-compose:testDebugUnitTest`.

---

## Session S4 — Public API surface review

Goal: lock the public ABI deliberately. Anything `public` becomes a long-term
compatibility burden.

Checks:
- Audit visibility:
  - `grep -rn '^class \|^fun \|^object \|^val \|^var ' aui-core/src/main aui-compose/src/main`
- Move helpers under `compose/internal/**` to `internal` visibility (not just
  package name).
- Decide what (if anything) needs `@ExperimentalAuiApi` opt-in.
- Public data classes used from Compose: confirm `@Immutable` / `@Stable`
  annotations where appropriate.
- Add KDoc to every public symbol (at least a one-liner).
- Optional: wire up Binary Compatibility Validator (`apiCheck`) and commit a
  baseline `.api` file.

Exit criteria:
- Surface is intentional and documented; ideally an `.api` baseline is checked
  in.

---

## Session S5 — R8 / ProGuard / serialization keep rules

Goal: hosts running R8/minification don't hit release-only crashes.

Checks:
- Both modules ship a `consumer-rules.pro` (configured via
  `consumer-proguard-files`) with the keep rules consumers need.
- If kotlinx-serialization / Moshi / Gson is used in `aui-core`, the keep
  rules live in the **consumer** rules file, not just the library's own
  minify config.
- End-to-end smoke test: enable `minifyEnabled true` in the demo's release
  build, run a parse + render, confirm no crashes.

Exit criteria:
- Demo release build with R8 enabled renders the canonical responses without
  reflection-related crashes.

---

## Session S6 — Dependencies & manifest hygiene

Goal: the library exposes the minimum public dependency surface and ships a
clean manifest.

Checks:
- `api` vs `implementation` audit in both `build.gradle.kts`. Anything `api`
  becomes part of the public ABI.
- AndroidManifest: no `<application>` attributes, no permissions the library
  doesn't actually need, no copy-paste launcher intents.
- `namespace` is set explicitly per module.
- `minSdk` is honest — try the demo against `minSdk = 21` (or whatever value
  is declared) and confirm no `@RequiresApi` gates are missing.
- Resource prefix: set `android.resourcePrefix = "aui_"` in both modules so
  any `R.string`/`R.drawable` is namespaced and can't collide with the host.

Exit criteria:
- Dependency surface and manifest are minimal and intentional; resource prefix
  enforced.

---

## Session S7 — Code hygiene

Goal: no debug noise, no swallowed errors, no nullable footguns left in the
release build.

Checks:
- `grep -rn 'Log\.\|println(' aui-core/src/main aui-compose/src/main` — remove
  or route through a host-injected logger.
- `grep -rn 'TODO\|FIXME\|XXX' aui-core/src/main aui-compose/src/main`.
- `grep -rn '!!' aui-core/src/main aui-compose/src/main` — every `!!` on
  parser input or external JSON is a release crash waiting to happen.
- Audit each `try { … } catch (e: Exception) { }` — confirm intentional and
  wired to `onParseError` / `onUnknownBlock` per the contract docs.
- Locale-safe string ops: `toLowerCase()` / `toUpperCase()` use `Locale.ROOT`
  when comparing block types/keys; host locale only when displaying.

Exit criteria:
- No production logging, no unjustified `!!`, no silent catches outside the
  documented error contract.

---

## Session S8 — Compose-specific correctness

Goal: state, effects, and lifecycle don't bite hosts.

Checks:
- `LaunchedEffect` keys are correct; effects don't survive recomposition wrong.
- `rememberSaveable` for any user-edit state the library owns (input fields,
  slider values, radio/checkbox selections) so rotation doesn't drop input.
- No `Context` captured in remembered lambdas in a way that leaks across
  configuration changes.
- Previews compile and live in `debugImplementation`, not in `main`.

Exit criteria:
- Rotation/back-stack/recompose behaviors are reviewed for each input
  component.

---

## Session S9 — Build artifact inspection

Goal: the published AAR/JAR contains exactly what should be published — no
demo classes, no test fixtures, no debug junk — and POM metadata is complete.

Checks:
- `./gradlew :aui-core:publishToMavenLocal :aui-compose:publishToMavenLocal`
- Unzip the resulting AARs from `~/.m2/repository/com/bennyjon/...` and verify:
  - no `demo/` classes leaked
  - no test fixtures bundled
  - `META-INF/` is clean (no debug keystores, build logs)
  - sources jar and javadoc jar are present and non-empty
- Inspect the generated POM: name, description, url, license, scm, developers
  all populated.
- Smoke test: a tiny external sample project consuming `mavenLocal()` builds
  and renders against the library.

Exit criteria:
- AAR contents are minimal and POM is publish-ready.

---

## Session S10 — Verification commands

Goal: a documented, repeatable verification command set anyone can run before
tagging.

Checks / outputs:
- `./gradlew :aui-core:test :aui-compose:test` — green.
- `./gradlew :aui-core:lint :aui-compose:lint` — fix or baseline anything
  user-visible.
- If wired up: `./gradlew :aui-core:apiCheck :aui-compose:apiCheck` against
  the committed `.api` baseline.
- Add the chosen command set to `release-checklist.md` as the "pre-tag
  verification" step.

Exit criteria:
- A single, documented verification command set exists in
  `release-checklist.md` and runs green on a clean checkout.

---

## Execution Notes

- Run sessions in order; each one may surface follow-ups that are tracked
  separately rather than expanded inline.
- If a session yields no findings, record that explicitly ("S2: no
  hardcoded colors found outside `AuiColors`") so the next pass doesn't redo
  the audit.
- Update `AGENTS.md` `Next Task` to point at the next pending session at the
  end of each session.
