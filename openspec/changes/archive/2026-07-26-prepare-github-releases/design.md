## Context

Cinderhell currently builds three Android variants and retains their outputs as GitHub Actions artifacts. The `preview` variant is minified but uses a runner-local Android debug key, version metadata is hard-coded, the application ID is still provisional, and there is no root project license or GitHub Release workflow. The existing build and inspection scripts already provide most of the release pipeline: pinned native inputs, tests, APK inspection, signature verification, corresponding-source packaging, and SHA-256 manifests.

The first public audience is a small group of handheld testers. They need repeatable arm64 preview builds that upgrade over one another, while final production distribution and store enrollment remain later work.

## Goals / Non-Goals

**Goals:**

- Establish permanent base and preview Android package identities before public distribution.
- Produce versioned, upgrade-compatible preview APKs signed by a recoverable dedicated key.
- Turn a validated preview tag into a draft GitHub prerelease with complete, checksummed artifacts.
- Make licensing, source correspondence, backup behavior, and release metadata explicit.
- Give the repository a sound maintenance baseline without expanding the game feature scope.

**Non-Goals:**

- Publish a production/stable channel or create its signing key.
- Publish automatically to GitHub, F-Droid, or Google Play without a human review step.
- Add in-app updates, telemetry, crash reporting, mod downloads, or Bluetooth certification.
- Promise data migration from existing pre-release `dev.cinderhell*` development installs.

## Decisions

### Permanent base ID with a separate preview channel

The base application ID will be `io.github.anthonystainer.cinderhell`. The preview variant will use the stable `.preview` suffix, while debug continues to use `.debug`. Kotlin source namespaces remain `dev.cinderhell`; Android application identity does not require a risky source-package migration.

This conventional reverse-domain identity is controlled by the project's GitHub owner and avoids reserving a production signer before it is needed. A separate preview package allows a production build to coexist with preview builds and prevents a preview key from becoming the production trust root.

### Tag-derived, monotonic preview versions

Release tags use `vMAJOR.MINOR.PATCH-preview.N`, beginning with `v0.1.0-preview.1`. A checked-in resolver validates the tag and derives:

`versionCode = MAJOR × 100,000,000 + MINOR × 1,000,000 + PATCH × 10,000 + N`

Preview ordinal is restricted to 1–9,998 and the result must not exceed Android's version-code ceiling. Final releases can later reserve the `+9,999` slot for a patch line. Gradle receives the resolved values through explicit environment variables; untagged local builds retain deterministic development defaults.

### Dedicated preview key in a GitHub Environment

A dedicated preview signing key is generated once, backed up outside the repository, and stored in the `preview-release` GitHub Environment as encrypted secrets. The release workflow reconstructs it only in the ephemeral runner. Ordinary CI may continue to use debug signing because its APK is not distributed as a GitHub Release.

The production key remains a later, separate decision. Losing the preview key would force testers to uninstall the preview package, so the local recovery copy and credentials are treated as release infrastructure.

### Tag-triggered draft prereleases

A separate workflow runs only for matching preview tags. It performs the same dependency verification, tests, native build, APK inspection, signature verification, and corresponding-source packaging as CI. It then creates or updates a GitHub Release as a draft prerelease and attaches:

- one versioned `arm64-v8a` preview APK;
- one corresponding-source archive;
- one combined `SHA256SUMS` file;
- generated GitHub release notes.

Draft status is a deliberate human gate. The workflow never publishes a release, and failure before upload leaves no public release. Re-running the same tag replaces draft assets idempotently.

### GPL project license and complete source bundle

The combined application is distributed under GPL-2.0-or-later, consistent with Woof's licensing terms. A full root `LICENSE` and SPDX metadata are added. Corresponding-source archives include the README, license, OpenSpec contracts, build scripts, Gradle wrapper, Android/native source, patch set, and submodule metadata while continuing to exclude toolchains, caches, credentials, generated APKs, and proprietary game data.

### Backups disabled until an export contract exists

Android platform backup is disabled for the application. Imported commercial WADs, user saves, and launcher state should not be copied to cloud or device-transfer services implicitly. A future explicit export/import feature can define inclusion, encryption, validation, and user consent.

### Native adaptive icon

The app receives a simple adaptive launcher icon built from repository-native Android vector resources. This avoids an opaque binary design dependency and gives Android a safe monochrome foreground for themed icons. Visual refinement can iterate without changing release identity.

### Low-noise repository maintenance baseline

GitHub Actions are pinned to immutable commit SHAs, Gradle caching is enabled, vulnerability alerts and private reporting are enabled, and security/contribution/issue guidance is added. Dependabot combines GitHub Actions and Gradle updates into at most one quarterly foundation PR with cooldowns, which the maintainer workflow owns through review and CI. Native submodules are excluded because changing them also requires coordinated lockfile checksums, corresponding-source review, Woof patch validation, and runtime compatibility testing. `main` requires the Android build check while allowing repository administrators to perform emergency maintenance.

## Risks / Trade-offs

- [Application ID change makes current development installs separate applications] → Make the change before public distribution and document that no migration is promised.
- [Preview-key loss breaks preview upgrades] → Keep an owner-readable recovery copy outside the repository and verify its certificate against published APKs.
- [Tag/version-code formula eventually constrains large version components] → Validate bounds centrally and replace the scheme before reaching them; current `0.x` development is far from the limit.
- [Draft releases still require a manual publish decision] → Prefer deliberate tester distribution over automatic publication; the release is otherwise complete and ready to inspect.
- [Pinned action SHAs are less readable] → Retain version comments and review upstream releases intentionally.
- [Grouped updates can combine incompatible toolchain changes] → Keep the group to one reviewable PR, require the Android check, and adjust or split the update during maintainer review.
- [Disabling backup removes convenient implicit device migration] → Avoid silently transferring copyrighted IWADs and add intentional export later.

## Migration Plan

1. Land build identity, version resolver, licensing, backup, icon, documentation, and workflows on `main`.
2. Generate and safely store the dedicated preview key, then populate the protected GitHub Environment secrets.
3. Push `main`, wait for the required Android check, and create `v0.1.0-preview.1` at the verified commit.
4. Confirm the tag workflow produces a draft prerelease whose assets, checksums, package ID, version, and signer match expectations.
5. Smoke-test installation and preview-to-preview upgrade before manually publishing the draft.

Rollback consists of deleting an unpublished draft and its tag, fixing `main`, and issuing a higher preview ordinal. Published Android version codes are never reused.

## Open Questions

None for the preview foundation. Production signing, stable-channel package distribution, and explicit data export are intentionally deferred.
