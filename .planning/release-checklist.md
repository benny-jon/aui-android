# First Release Checklist

Operational checklist for publishing AUI Android to Maven Central. Pairs with
`.planning/first-release-readiness.md` (the broader release-readiness plan) and
is the source of truth for the install/publish mechanics.

---

## Distribution Decision (Session 53)

- **Target registry:** Maven Central (Sonatype Central Portal).
- **Group ID:** `com.bennyjon` (verified via owned domain `bennyjon.com`).
- **Artifact IDs:**
  - `com.bennyjon:aui-core`
  - `com.bennyjon:aui-compose`
  - The `demo` module is **not** published.
- **First version:** `0.1.0-alpha01`.
- **Versioning policy (pre-1.0):**
  - `0.x.y` is alpha-grade; APIs may change between minor versions.
  - Use `-alphaNN` / `-betaNN` suffixes for unstable iterations.
  - Move to `0.1.0` (no suffix) once the release-confidence tests in Session 56
    pass and the README/install path is stable for at least one external try.

Maven coordinate consumers should use after first publish:

```kotlin
dependencies {
    implementation("com.bennyjon:aui-compose:0.1.0-alpha01")
}
```

`aui-compose` re-exports `aui-core` via `api(project(":aui-core"))`, so most
adopters do not need to add `aui-core` directly.

---

## One-Time Setup (owner)

These steps are owner-only and only need to happen once. They are blockers for
the first publish; none of them block local development or CI compile/test.

1. **Sonatype Central account.**
   - Create an account at https://central.sonatype.com.
   - Verify the namespace `com.bennyjon` by adding the DNS TXT record Sonatype
     provides to the `bennyjon.com` zone. Sonatype lists the exact record after
     you submit the namespace request.
2. **Generate a user token.**
   - In the Central Portal, generate a publishing user token. Keep the
     `username` and `password` values — these are what Gradle/CI use, not your
     account password.
3. **GPG signing key.**
   - Generate an RSA 4096 key tied to the email on your Sonatype account:
     `gpg --full-generate-key`.
   - Publish the public key to a keyserver Sonatype scans:
     `gpg --keyserver keys.openpgp.org --send-keys <KEYID>`
     and (belt + suspenders) `keyserver.ubuntu.com`.
   - Export the secret key in ASCII-armored form (the only format Gradle's
     in-memory signing actually accepts — see warning below):
     `gpg --export-secret-keys --armor <KEYID>`.
     Do **not** pipe through `base64`. Despite some plugin docs suggesting
     base64 is supported, `vanniktech-maven-publish` 0.30.x passes the value
     straight to Gradle's `useInMemoryPgpKeys()`, which only understands the
     armored form. Feeding base64 produces the misleading
     "secret key ring doesn't start with secret key tag: tag 0xffffffff"
     error.
4. **Local secrets — use environment variables, not `~/.gradle/gradle.properties`.**

   The recommended path is shell environment variables, because `.properties`
   files have escape rules (`\` collapses, trailing whitespace counts, line
   continuations require `\` at EOL) that silently corrupt passphrases and
   multi-line armored keys. Add to `~/.zshrc` (or `~/.zprofile`):
   ```
   export ORG_GRADLE_PROJECT_mavenCentralUsername='<sonatype user token username>'
   export ORG_GRADLE_PROJECT_mavenCentralPassword='<sonatype user token password>'
   export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <KEYID>)"
   export ORG_GRADLE_PROJECT_signingInMemoryKeyId='<last 8 chars of GPG key id>'
   export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword='<GPG key passphrase>'
   ```
   Gradle reads `ORG_GRADLE_PROJECT_<name>` env vars as project properties
   with zero escaping. Use single quotes for literal values; double quotes
   only where you actually want shell expansion (as on the key line above).

   The legacy `signing.keyId` / `signing.password` / `signing.secretKeyRingFile`
   trio is not used here. GnuPG 2.1+ no longer creates `~/.gnupg/secring.gpg`
   by default, and Gradle's `useInMemoryPgpKeys` is what the
   `vanniktech-maven-publish` plugin invokes under the hood for both local
   and CI.

   Project-level publishing config (`SONATYPE_HOST=CENTRAL_PORTAL` and
   `RELEASE_SIGNING_ENABLED=true`) already lives in the checked-in root
   `gradle.properties`, so nothing else local is needed beyond these env vars.
5. **GitHub Actions secrets** (when CI publishing is wired up):
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
   - `SIGNING_IN_MEMORY_KEY` (the **ASCII-armored** secret key from step 3,
     newlines preserved — GitHub Actions secrets handle multi-line values
     fine. Do not base64-encode.)
   - `SIGNING_IN_MEMORY_KEY_ID`
   - `SIGNING_IN_MEMORY_KEY_PASSWORD`

---

## Per-Release Steps

Run these in order for each published version.

1. Bump `VERSION_NAME` in root `gradle.properties` (single source of truth for
   library coordinates).
2. Run the pre-tag verification command set from a clean checkout:
   ```
   ./gradlew clean \
     :aui-core:compileDebugKotlin \
     :aui-compose:compileDebugKotlin \
     :demo:compileDebugKotlin \
     :demo:assembleDebug \
     :aui-core:testDebugUnitTest \
     :aui-compose:testDebugUnitTest \
     :aui-core:lintDebug \
     :aui-compose:lintDebug \
     :aui-core:assembleRelease \
     :aui-compose:assembleRelease \
     :demo:assembleRelease \
     :aui-core:publishToMavenLocal \
     :aui-compose:publishToMavenLocal
   ```
   Why this exact set:
   - matches current CI coverage for compile checks (`:aui-core`, `:aui-compose`,
     `:demo`, plus demo resource assembly)
   - reruns the library unit-test and lint gates that protect the published
     renderer surface
   - exercises release assembly for both published artifacts and the R8-minified
     demo host
   - republishes both library modules to `mavenLocal()` so the packaged AAR/POM
     path is revalidated immediately before tagging
   - `apiCheck` is intentionally omitted because Binary Compatibility Validator
     is not wired up yet
3. Smoke-publish verification / artifact inspection:
   ```
   ls ~/.m2/repository/com/bennyjon/aui-compose/<version>/
   ```
   Confirm the POM has: name, description, url, license, scm, developers, and
   that `aui-compose` declares `aui-core` as a runtime dependency.
4. Publish to Central staging and release:
   ```
   ./gradlew publishAndReleaseToMavenCentral
   ```
   The `publishAndReleaseToMavenCentral` task stages + closes + releases in
   one shot. (The sibling task `publishToMavenCentral` stops at staging and
   requires a manual release in the Central Portal UI.) Targeting is driven
   by `SONATYPE_HOST=CENTRAL_PORTAL` in the root `gradle.properties`, not
   by a DSL call in the module build files.
5. Tag the release in git: `git tag v0.1.0-alpha01 && git push origin v0.1.0-alpha01`.
6. Wait ~10–30 minutes for the artifact to appear at
   `https://repo1.maven.org/maven2/com/bennyjon/aui-compose/<version>/`.
7. Smoke test from a throwaway consumer project:
   ```kotlin
   repositories { mavenCentral() }
   dependencies { implementation("com.bennyjon:aui-compose:<version>") }
   ```
   Compile + run a minimal `AuiRenderer` call.
8. Update `README.md` install snippet if the version moved, and append an entry
   to `CHANGELOG.md` (file to be created on first release).

---

## Pending Before First Publish

These must be true before `0.1.0-alpha01` can ship.

- [ ] Sonatype namespace `com.bennyjon` verified.
- [ ] GPG key generated and published to a keyserver.
- [ ] Publishing secrets present in the local shell environment
      (`ORG_GRADLE_PROJECT_*`) and GitHub Actions (CI).
- [ ] `vanniktech-maven-publish` plugin applied to `aui-core` and `aui-compose`
      with full POM metadata. *(Scaffolded in Session 53.)*
- [ ] `LICENSE`, `scm`, `url`, `developers` POM fields verified against actual
      repo metadata.
- [ ] `./gradlew publishToMavenLocal` produces a valid POM + signed AAR for
      both modules.
- [ ] Sessions 54 (canonical integration example), 55 (error contract docs),
      and 56 (release-confidence tests) landed — these are gating conditions
      from `first-release-readiness.md`, not optional.
- [ ] README install snippet matches the real published coordinate.

---

## Deferred / Not In Scope For First Release

- Automated release-on-tag GitHub workflow. Manual `publishAndReleaseToMavenCentral`
  from a local trusted machine is acceptable for `0.1.0-alpha*`.
- Snapshots repo. We can add `-SNAPSHOT` publishing later if it becomes useful.
- Dokka HTML docs site. POM-level Javadoc jar is required by Maven Central and
  will be generated by the publishing plugin; a hosted docs site can wait.
