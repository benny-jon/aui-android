# File Content Block

This document defines the reusable contract for `file_content`, a built-in AUI
block for exact copyable artifacts such as markdown files, JSON payloads,
configs, scripts, and source files.

The goal is cross-platform parity: Android implements it now, and `aui-ios`
should treat this document as the reference for future implementation.

## Why It Exists

`text`, `heading`, and `divider` are presentation primitives. They are the wrong
abstraction for a complete file/document artifact because they:

- encourage the model to decompose one artifact into multiple UI blocks
- lose the "this is one deliverable" semantic
- make copy/download UX heuristic instead of intentional
- make filename/language metadata awkward or impossible

`file_content` fixes that by representing the artifact as one exact payload.

## Schema

```json
{
  "type": "file_content",
  "data": {
    "content": "# Project README\n\n## Setup\n\n1. Install dependencies",
    "filename": "README.md",
    "language": "markdown",
    "title": "Project README",
    "description": "Setup and usage guide"
  }
}
```

## Field Semantics

- `content`: Required. The exact file body. Hosts should preserve it byte-for-byte as far as practical.
- `filename`: Optional. Artifact name shown in the header and used for future save/export affordances.
- `language`: Optional. Format hint such as `markdown`, `json`, `yaml`, `swift`, `kotlin`, `bash`.
- `title`: Optional. Human-readable title when the host wants something friendlier than the filename.
- `description`: Optional. Supporting subtitle for preview and host chrome.

## Prompting Rules

When the user asks for a complete artifact such as:

- an `.md` file
- a README
- a JSON file
- a config file
- a script
- a source file

the model should prefer:

- `display = "expanded"`
- a single `file_content` block

The model should not decompose the artifact into `heading`, `text`, and `divider`
blocks unless the user explicitly asked for a structured visual walkthrough
instead of a file.

## Rendering Requirements

All platform hosts should render `file_content` with these minimum behaviors:

- monospaced body
- exact content preserved
- clear copy action in the header
- clear save/download action when the host platform supports writing a user-visible file
- selectable text
- filename/language/title visible when present

Recommended behavior:

- use the same visual treatment as copyable fenced-code fallbacks
- allow long content to scroll naturally with the host surface
- show a meaningful card stub when surfaced via `EXPANDED`

## Relationship To Fenced Markdown In `text`

Fenced triple-backtick blocks inside a `text` block remain a resilience/fallback
path. They are useful when a model ignores the schema and still emits markdown
fences inside prose.

They are not the canonical artifact contract.

Canonical rule:

- exact artifact requested by the user → `file_content`
- incidental fenced snippet inside prose → `text` with fenced fallback rendering

## Future iOS Notes

`aui-ios` should implement `file_content` with the same semantic contract:

- one artifact block
- one copy affordance
- preserved content
- equivalent title/filename/language display

Visual styling may differ by platform, but the data contract and interaction
behavior should stay aligned with this document.
