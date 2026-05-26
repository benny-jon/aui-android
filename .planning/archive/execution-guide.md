# How to Execute AUI with Claude Code

## The Approach

The key principle: **plan in sessions, execute in sessions, review in sessions.**
Never plan and execute in the same Claude Code session. Context gets muddled.

This file is the index. It tells you which phase plan to load and explains the
session workflow. **Detailed session prompts live in per-phase files**, not here.

---

## Phase Index

| Phase | Status | Sessions | Detailed plan |
|-------|--------|----------|---------------|
| 1 — Polls & Feedback Collection | ✅ | 1-7 | `.planning/phase1-polls.md` |
| 2 — Polls Polish | ✅ | 8-10 | `.planning/phase2-polls-polish.md` |
| 3 — Clean Library Boundary | ✅ | 11-14 | `.planning/phase3-host-integration.md` |
| 4 — Plugin System & Customization | ✅ | 15-20 | `.planning/phase4-customization.md` |
| 5 — Live Chat Demo | 🚧 | 21-26 | `.planning/phase5-live-chat.md` |

The single source of truth for "what phase are we on right now" is the
**Current Phase** section of `CLAUDE.md`. This index is for humans browsing
history; CLAUDE.md is what Claude Code reads.

Historical session prompts for completed phases are archived in
`.planning/archive/execution-guide-phases-1-4.md` for reference. Claude Code
does not need to read the archive.

---

## Before You Open Claude Code

### 1. Create the repo and Gradle skeleton manually

```bash
mkdir aui && cd aui
git init

mkdir -p aui-core/src/main/kotlin/com/bennyjon/aui/core
mkdir -p aui-core/src/test/kotlin/com/bennyjon/aui/core
mkdir -p aui-compose/src/main/kotlin/com/bennyjon/aui/compose
mkdir -p aui-compose/src/test/kotlin/com/bennyjon/aui/compose
mkdir -p demo/src/main/kotlin/com/bennyjon/aui/demo
mkdir -p spec/examples
mkdir -p spec/schema
mkdir -p docs
mkdir -p .planning
```

You don't need to write the build.gradle files yourself — Claude Code does that
well. Having the folders signals the architecture.

### 2. Drop in your key documents

- `CLAUDE.md` at root — Claude Code reads this automatically
- `spec/aui-spec-v1.md` — the wire-format spec
- `docs/architecture.md` — library architecture
- `.planning/phaseN-*.md` — the phase plan you're about to execute
- `spec/examples/*.json` — sample JSON files

### 3. Initial commit

```bash
git add .
git commit -m "Initial project structure with spec and architecture docs"
```

---

## How a Session Works

Every Claude Code session follows the same shape:

1. **Start fresh.** New invocation or `/clear`. Never plan and execute in the
   same session.
2. **Read the orienting docs.** The session prompt always begins:
   `Read CLAUDE.md and the current phase plan.` CLAUDE.md's Current Phase
   section names the exact phase plan file to load.
3. **Accomplish one focused goal.** One session = one session block from the
   phase plan. Don't multi-task.
4. **Verify.** Build compiles, tests pass, demo runs if relevant.
5. **Commit.** Single conventional-commit message describing the session's goal.
6. **Update CLAUDE.md.** Per the "Keeping This File Up To Date" rules in
   CLAUDE.md itself.

The session prompts in each phase plan file are designed to be copy-pasted
verbatim into Claude Code. Don't paraphrase them — the wording was tuned during
planning.

---

## Adding a New Phase

When starting a new phase:

1. **Plan first** in a Claude.ai conversation (separate from execution).
2. **Write the phase plan** as `.planning/phaseN-<slug>.md`. Match the shape of
   existing phase plans: short architecture intro, one fenced block per session
   with the prompt text, a deliverables checklist at the end.
3. **Add a row to the Phase Index** above. Mark status 🚧.
4. **Update CLAUDE.md's Current Phase section** to point at the new file.
5. **Do not duplicate session prompts into this guide.** The phase plan is the
   only home. If a session block exists in two places, this guide is wrong.

---

## What Goes Where

| Document | Purpose | Who reads it |
|---|---|---|
| `CLAUDE.md` | Project conventions, current phase pointer, session log | Claude Code, every session |
| `.planning/phaseN-*.md` | Detailed session prompts for one phase | Claude Code, during that phase |
| `execution-guide.md` (this file) | Workflow conventions + phase index | Humans, mostly |
| `spec/aui-spec-v1.md` | Wire format spec | Anyone integrating AUI |
| `docs/architecture.md` | Library internals | Library contributors |
| `.planning/archive/` | Historical session prompts from completed phases | Reference only |

The rule that keeps this scalable: **one home per piece of information**.
Indexing across files is fine; copy-pasting content across files is not.

---

## Maintaining CLAUDE.md

Claude Code updates CLAUDE.md automatically at the end of every session per the
"Keeping This File Up To Date" section in CLAUDE.md.

After each phase, manually verify:
- "Completed Phases" is accurate
- "Current Phase" points to the right next file
- "Known Issues" has no stale entries
- Session Log entries are specific, not vague
- The Phase Index in this guide reflects the new state
