# First Release Readiness Plan

**Goal:** prepare AUI Android for its first external release as a usable,
documented, test-backed library rather than just a promising demo repo.

This is a **release-readiness plan**, not a catalog-expansion phase. The bias is
to tighten the current product, reduce adopter risk, and make the install +
integration story crisp.

The release can still be a pre-1.0 version such as `0.1.0` or `0.1.0-alpha01`.
The point is not semantic finality; the point is that an external developer can
discover the repo, install it, follow the docs, and succeed without guessing.

---

## Why This Exists

The current codebase is already strong enough to demonstrate the core idea:

- pure-renderer library boundary is intact
- component catalog is broad enough for a first version
- plugin model exists
- demo proves end-to-end LLM integration and host-side routing
- public docs are mostly in sync

What is still missing is **release discipline**:

- installation story is not yet real because publishing is not live
- canonical consumer integration is implied, but not yet standardized as a
  reference sample
- parser/error/fallback behavior is implemented but not yet fully documented as
  a contract
- rendering confidence is good, but release-focused verification can be tighter

---

## Release Definition

The first release is successful when all of the following are true:

1. A developer can add AUI to a project using an official published artifact or
   an explicitly documented temporary install path.
2. README quick start and architecture docs are accurate and copy-pasteable.
3. There is one canonical host integration example for:
   - assistant text-only reply
   - assistant text + AUI in one turn
   - feedback routing back to the host
   - `expanded` and `survey` host-owned presentation
4. Error handling expectations are documented:
   - malformed known blocks
   - unknown blocks
   - total parse failure
   - `onParseError` / `onUnknownBlock` usage
5. Core built-in rendering behavior has release-confidence tests.
6. Publishing/versioning/release steps are defined and reproducible.

---

## Non-Goals

These are explicitly out of scope for first release unless they are nearly free:

- large catalog expansion just to increase the block count
- moving chat, persistence, or networking into library modules
- adapter SDKs for OpenAI / Anthropic as part of the library itself
- cross-platform work beyond keeping contracts reusable
- speculative complex blocks that are not needed for the current docs/story

Possible nice-to-haves such as `key_value_list`, `input_text_multi`, or a
library-provided `loading` block can wait unless one becomes critical during
release hardening.

---

## Product Positioning For V1

Ship AUI Android as:

- a pure renderer for AI-generated native Compose UI
- provider-agnostic
- host-owned for networking, conversation state, and layout decisions
- suitable for chat and other server-driven UI surfaces
- pre-1.0, additive, and still evolving

Do **not** position it as:

- a full AI SDK
- a chat framework
- a workflow engine
- a production-hardened general-purpose server-driven UI platform

---

## Locked Decisions

1. **Keep the library boundary pure.**
   No chat history, HTTP, or message persistence in `aui-core` or
   `aui-compose`.
2. **Prefer release polish over more blocks.**
   Reliability, docs, and installability matter more than moving from 27 built
   ins to 30+.
3. **Treat the demo as a reference host, not the product.**
   Demo improvements are valid only when they clarify host responsibilities.
4. **Document tolerant parsing as a contract.**
   The library should be explicit about what is salvageable and what is fatal.
5. **First release may still be pre-1.0.**
   Stability expectations should be honest.

---

## Workstreams

### 1. Distribution And Install Story

Questions to close:

- Will first release publish to Maven Central, GitHub Packages, or remain
  source-only for one more cycle?
- What exact dependency snippet should README show?
- What version string format will be used?
- What is the minimal release checklist for tagging/publishing?

Must-have output:

- one true install path in README
- repeatable publishing steps
- versioning decision recorded in docs

### 2. Canonical Consumer Integration

Questions to close:

- What is the official host-side message model example?
- How should a host render assistant text and AUI in the same turn?
- What is the minimal recommended plugin registry setup?
- What is the official guidance for `expanded` and `survey` routing?

Must-have output:

- one recommended integration example in README
- supporting architecture notes that align with real code

### 3. Error Handling And Compatibility Contract

Questions to close:

- What exactly happens for unknown block types?
- What malformed known blocks are salvaged vs dropped to `Unknown`?
- When should hosts use `parseOrNull`, `onParseError`, and `onUnknownBlock`?
- What should an app show when parsing fails entirely?

Must-have output:

- one documented compatibility/fallback section in public docs
- examples that show safe host behavior

### 4. Release-Confidence Testing

Questions to close:

- Which built-in categories need focused rendering verification?
- Which feedback aggregation paths are critical to protect?
- Do current tests prove enough about public behavior, not just internals?

Must-have output:

- high-signal renderer tests across major categories
- end-to-end feedback contract tests
- a documented release verification command set

### 5. Docs And Example Polish

Questions to close:

- Does README tell the shortest true story?
- Are there any remaining stale examples or pre-refactor API shapes?
- Do docs distinguish library behavior from demo behavior clearly enough?

Must-have output:

- polished README
- architecture + spec + live chat docs consistent with release
- one concise release notes template or changelog starter

### 6. Publishing And Release Mechanics

Questions to close:

- Are Gradle publishing plugins/config ready?
- Are POM metadata, license, and developer info complete?
- What tag/release flow should be used?
- Is there a simple smoke test after publishing?

Must-have output:

- working publishing configuration or explicit defer decision
- release checklist with owner steps

---

## Proposed Execution Order

The work should land in this order:

1. Decide distribution path and versioning target.
2. Lock the public integration story in docs/examples.
3. Tighten fallback/error-handling documentation.
4. Add release-confidence tests.
5. Finalize publishing mechanics.
6. Run a release-candidate docs/test/pass.

This order matters. There is no value polishing Maven instructions before the
artifact path is decided, and there is no value publishing if the consumer story
is still ambiguous.

---

## Session Plan

Continue session numbering after Session 52.

## Session 53: Release Scope + Distribution Decision

```
Read AGENTS.md, README.md, docs/architecture.md, docs/livechat.md, and
.planning/first-release-readiness.md.

Goal: lock the first-release scope and distribution path.

Tasks:
1. Inspect current Gradle/build setup for any existing publishing scaffolding.
2. Decide whether the first release target is:
   - Maven Central now
   - GitHub Packages / another interim registry
   - source-only for one more cycle
3. Record that decision in public docs and/or a release checklist doc.
4. If publishing now is realistic, scaffold the minimal publishing config.
5. If publishing is not yet realistic, make README brutally clear about the
   temporary install path and what remains before published release.

Constraints:
- Do not ship fake Maven coordinates in docs.
- Prefer one true install story over multiple half-supported options.

Deliverables:
- release-path decision documented
- dependency/install instructions aligned to reality
- publishing tasks enumerated if still pending
```

## Session 54: Canonical Host Integration Example

```
Read AGENTS.md, README.md, docs/architecture.md, docs/livechat.md, and
.planning/first-release-readiness.md.

Goal: make the consumer integration story concrete and copy-pasteable.

Tasks:
1. Add or refine one canonical host integration example showing:
   - assistant text-only rendering
   - assistant text + AUI rendering in one turn
   - onFeedback routing back to host logic
2. Clarify the recommended host-side message model shape in docs.
3. Document the recommended handling of EXPANDED and SURVEY responses.
4. Keep examples aligned with the current public API only.

Constraints:
- Do not bake demo-only architecture into the library guidance.
- Do not introduce a library-owned chat model.

Deliverables:
- updated README example(s)
- architecture notes matching the example
```

## Session 55: Error Handling + Compatibility Contract

```
Read AGENTS.md, README.md, docs/architecture.md, spec/aui-spec-v1.md, and
.planning/first-release-readiness.md.

Goal: document the parser and renderer fallback contract clearly enough for
external adopters.

Tasks:
1. Audit current tolerant parsing behavior in code/tests.
2. Add a public docs section covering:
   - unknown block behavior
   - malformed known block salvage behavior
   - full parse failure behavior
   - how hosts should use onParseError and onUnknownBlock
3. Add or tighten tests where the docs describe behavior that is not yet
   protected.

Constraints:
- Describe only behavior that the current implementation actually guarantees.
- Unknown types must remain non-fatal where applicable.

Deliverables:
- compatibility/fallback docs
- any required contract tests
```

## Session 56: Release-Confidence Renderer Tests

```
Read AGENTS.md, README.md, docs/architecture.md, and
.planning/first-release-readiness.md.

Goal: add the highest-value tests that protect the public renderer contract.

Tasks:
1. Identify gaps in coverage across built-in categories:
   display, input, status, chart/table, survey/expanded routing helpers.
2. Add a focused set of tests that verify user-visible behavior rather than only
   parser internals.
3. Add at least one end-to-end feedback aggregation test for multi-input flows.
4. Write down the release verification command set once the test matrix is clear.

Constraints:
- Prefer a small high-signal suite over broad brittle tests.
- Do not add heavyweight snapshot tooling unless clearly justified.

Deliverables:
- new tests
- documented verification commands
```

## Session 57: Publishing Mechanics + Release Checklist

```
Read AGENTS.md, README.md, docs/architecture.md, and
.planning/first-release-readiness.md.

Goal: make publishing and versioned release execution reproducible.

Tasks:
1. Implement or finalize Gradle publishing configuration if in scope.
2. Verify artifact metadata: name, description, license, SCM/project URLs.
3. Add a concise release checklist under docs or .planning.
4. Ensure README and architecture references match the chosen release path.

Constraints:
- Keep the first release path as simple as possible.
- Do not overbuild CI/CD if a manual first release is acceptable.

Deliverables:
- publishing config or explicit defer decision
- release checklist
- docs aligned to release mechanics
```

## Session 58: Release Candidate Sweep

```
Read AGENTS.md and .planning/first-release-readiness.md.

Goal: perform the final release-candidate sweep.

Tasks:
1. Run the agreed verification commands.
2. Review README, architecture, spec references, and release docs for drift.
3. Fix any last stale examples or contradictory wording.
4. Update AGENTS.md with release-readiness progress and next recommendation.

Constraints:
- No opportunistic new features.
- Only release blockers or obvious clarity fixes.

Deliverables:
- clean release-candidate state
- final blocker list (if any)
- updated project status
```

---

## Release Checklist

The release is ready only when every item below is true:

- install instructions are accurate
- artifact/version strategy is explicit
- README quick start is copy-pasteable
- one canonical host integration example exists
- fallback/error behavior is documented
- public docs do not contradict the code
- release-confidence tests pass
- release verification commands are written down
- publishing steps are either working or intentionally deferred and documented

---

## Current Recommendation

If only one thing is done next, do **Session 53** first.

Until the distribution path is decided, every install example and release claim
will remain unstable.
