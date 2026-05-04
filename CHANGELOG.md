# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog and the project follows pre-1.0
versioning while the public API and catalog continue to evolve.

## [0.1.0-alpha01] - 2026-05-04

First external Maven Central release of AUI Android.

### Added

- Published `com.bennyjon:aui-core:0.1.0-alpha01` and
  `com.bennyjon:aui-compose:0.1.0-alpha01`.
- Canonical host integration guidance in `README.md` and
  `docs/architecture.md`, including mixed assistant text + AUI responses and
  host-owned `expanded` / `survey` presentation.
- Explicit compatibility and fallback contract documentation for unknown
  blocks, malformed payloads, and `onParseError` / `onUnknownBlock` handling.
- Release-confidence renderer coverage for survey flows, expanded multi-input
  feedback aggregation, read-only action-plugin behavior, parse fallback
  callbacks, and chart accessibility summaries.

### Notes

- This is a pre-1.0 alpha release. API and behavior may still change between
  minor versions.
- `aui-compose` transitively depends on `aui-core`; most consumers only need
  `implementation("com.bennyjon:aui-compose:0.1.0-alpha01")`.
