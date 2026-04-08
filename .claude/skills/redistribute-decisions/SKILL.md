---
name: redistribute-decisions
description: Clean up a bloated CLAUDE.md (or similar persistent context file) by redistributing implementation details and format specs to where they really belong — KDoc on the actual code, spec documents, or README files. Use this whenever CLAUDE.md gets too long (over 120 lines), when "Key Design Decisions" or similar sections start accumulating implementation specifics (class names, parameter names, specific patterns), or when the user says the context file feels like a "dumping ground" or "bloated." Also trigger when starting a new phase of a long-running project as a hygiene pass. The goal is to keep CLAUDE.md focused on stable project-wide principles while moving implementation details closer to the code they describe.
---

# Redistribute Decisions

A cleanup skill for long-running projects where CLAUDE.md (or equivalent
persistent context files) have accumulated a mix of stable principles,
implementation details, and spec/format notes.

The core insight: **documentation should live as close as possible to the
code or spec it describes.** CLAUDE.md should only contain things that apply
to every session regardless of which file is being edited.

## When to Use This Skill

- CLAUDE.md is over 120 lines and growing
- "Key Design Decisions" section has 15+ bullets mixing principles with implementation details
- Bullets reference specific class names, parameter names, or implementation patterns
- Starting a new phase of a long-running project (hygiene pass)
- User says the context file feels "bloated," "messy," or "like a dumping ground"
- User asks to clean up, trim, or reorganize CLAUDE.md

## The Three Categories

Every bullet in "Key Design Decisions" (or similar) falls into one of three categories:

### Category A: Stable Architectural Principle → Keep in CLAUDE.md

These are universal guardrails that apply regardless of which file is being edited. They're stable, don't reference specific implementation details, and guide all future work.

**Examples:**
- "No variants — each visual variant is a separate component type"
- "Theme via CompositionLocalProvider. Components never hardcode values"
- "Library is a pure renderer with callback — no chat management"
- "Never launches coroutines — all async work is host app's responsibility"
- "Unknown types handled gracefully — never crash"

**Test:** Would this apply even after major refactoring? Does it guide design decisions in any file? If yes, Category A.

### Category B: Implementation Detail → Move to KDoc on the Code

These describe HOW specific classes, functions, or parameters work. They belong as `/** */` comments on the actual code so they're visible when editing.

**Examples:**
- "BlockRenderer accepts optional registryOverride parameter"
- "SheetFlowDisplay uses rememberSaveable for the showSheet flag"
- "AuiRenderer has two overloads: json and pre-parsed"
- "AuiFeedback has stepsTotal and stepsSkipped typed fields"
- "EXPANDED display uses a shared registry between bubble and content BlockRenderers"

**Test:** Does it reference specific class names, function names, or parameters? If yes, Category B.

### Category C: Spec/Format Detail → Move to Spec Document

These describe the data format, protocol, or contract — not the Kotlin/TypeScript/Python implementation.

**Examples:**
- "Sheet display uses `steps` array, not `blocks`"
- "Each step has `question`, `label`, `skippable` fields"
- "feedback.params always includes `steps_total` and `steps_skipped`"
- "Component types use snake_case in JSON"

**Test:** Would this still be true if the library was reimplemented in a different language? If yes, Category C.

## Process

### Step 1: Read the Current State

Read CLAUDE.md (or the equivalent context file the user identifies). Find the section that has the design decisions, principles, or patterns — usually called "Key Design Decisions," "Architecture Notes," "Conventions," or similar.

Also identify:
- Where the spec document lives (e.g., `spec/`, `docs/`, or in a README)
- The main source code structure (so you can locate the right files for Category B items)

### Step 2: Classify Every Bullet

Go through every bullet in the target section and classify it A/B/C. Produce a table like:

```
Bullet                                              Category  Destination
────────────────────────────────────────────────────────────────────────
No variants, separate component types               A         (keep in CLAUDE.md)
Unknown types → Unknown case, never crash           A         (keep in CLAUDE.md)
BlockRenderer accepts registryOverride param        B         → BlockRenderer.kt KDoc
SheetFlowDisplay uses rememberSaveable              B         → SheetFlowDisplay.kt KDoc
AuiRenderer has two overloads                       B         → AuiRenderer.kt KDoc
Sheet uses steps array not blocks                   C         → spec/aui-spec-v1.md
Each step has question/label/skippable              C         → spec/aui-spec-v1.md
feedback.params includes steps_total/steps_skipped  C         → spec/aui-spec-v1.md
Theme via CompositionLocalProvider                  A         (keep in CLAUDE.md)
Library is pure renderer with callback              A         (keep in CLAUDE.md)
```

### Step 3: Stop and Get User Approval

**Do NOT start making changes yet.** Present the classification table to the user and ask them to review it. They may:
- Agree with all classifications
- Reclassify some bullets (e.g., "I think that's actually a spec thing")
- Add context you don't have (e.g., "that pattern is deprecated, just delete it")

Wait for explicit approval before proceeding.

### Step 4: Execute the Moves

For each bullet, based on its category:

**Category A (keep):**
- Leave it in CLAUDE.md
- Tighten wording if it references specific class names (convert to principle form)
- Example: "AuiRenderer wraps content in CompositionLocalProvider" → "Theme is provided via CompositionLocalProvider"

**Category B (move to KDoc):**
- Find the relevant file(s) using search
- Locate the class, function, or parameter the bullet describes
- Add or update the KDoc with the information
- Use proper KDoc tags: `@param` for parameters, `@see` for cross-references, `@throws` for errors
- If KDoc already covers it, just remove the bullet from CLAUDE.md (no duplication)
- If KDoc is incomplete, improve it while you're there

**Category C (move to spec):**
- Find the relevant section of the spec document
- Add the information if it's not already there
- If the spec already covers it, just remove the bullet from CLAUDE.md
- Maintain the spec's existing structure and tone

### Step 5: Rewrite the CLAUDE.md Section

After moving B and C items out, rewrite the Key Design Decisions section with only Category A items. Requirements:
- Each bullet is 1-2 lines maximum
- No class names, function names, or parameter names
- No implementation-specific patterns
- Reads as a principle, not an observation
- Target: under 15 bullets total for the section

### Step 6: Verify Nothing Is Lost

Before finishing, verify:
- Every original bullet ended up somewhere (KDoc, spec, or stayed in CLAUDE.md with tightened wording)
- No bullet was duplicated across destinations
- The build still compiles: run the project's build command
- Tests still pass: run the project's test command
- CLAUDE.md line count decreased meaningfully (typically 30-50 lines smaller)

### Step 7: Update the Session Log

Add a minimal entry to the session log following the project's convention. Example: `- Cleanup: Redistributed Key Design Decisions to KDoc and spec.`

Don't write a detailed log of every move — the git diff is the detailed log.

## Rules

1. **Never delete information without relocating it.** Every bullet must end up somewhere. If something genuinely is obsolete, confirm with the user before removing.

2. **Never change code behavior.** This is a documentation refactor only. KDoc comments don't affect runtime — if you find yourself modifying logic, stop.

3. **Don't duplicate.** If a bullet's content is already covered by existing KDoc or spec text, just remove it from CLAUDE.md. Duplication causes drift.

4. **Get approval before moving.** Present the classification first. The user knows things you don't (deprecated patterns, planned changes, domain context).

5. **Stay in doubt on the side of relocation.** If you're unsure whether a bullet is a principle or an implementation detail, default to moving it out of CLAUDE.md. Lean CLAUDE.md is the goal.

6. **Preserve the project's conventions.** Match the existing KDoc style, the spec document's structure, and the session log format. Don't introduce new conventions during cleanup.

## Common Pitfalls

- **Moving too aggressively without user review.** The classification step exists for a reason. A bullet that looks like an implementation detail might actually be a guardrail the team relies on.

- **Creating duplicate documentation.** If the spec already says "sheets use steps array," don't re-add it there — just delete the bullet from CLAUDE.md.

- **Breaking KDoc formatting.** Kotlin KDoc uses specific tags. Don't invent new ones. Stick with `@param`, `@return`, `@throws`, `@see`, `@sample`.

- **Over-tightening Category A items.** A principle should be memorable and clear, not so abstract it loses meaning. "Components are themed" is too vague. "Theme via CompositionLocalProvider — components never hardcode values" is just right.

- **Forgetting to verify the build.** Always run build + tests after the cleanup. KDoc typos can sometimes cause doc-generation errors.

## Expected Outcome

After running this skill on a typical bloated CLAUDE.md:

- CLAUDE.md drops from 120-150 lines to 70-90 lines
- "Key Design Decisions" section shrinks from 15-25 bullets to 8-12 principles
- Relevant code files gain useful KDoc comments
- Spec documents become more complete and authoritative
- Future sessions read a leaner, more focused context file
- Implementation details are discoverable while editing the code (where they're actually needed)

The user should feel that their CLAUDE.md is clean and their code is better documented, without any code behavior changes.
