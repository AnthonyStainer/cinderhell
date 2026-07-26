# Release Distribution Specification

## Purpose

Define Cinderhell's stable Android identities, versioned and signed GitHub preview pipeline, verified release assets, GPL source correspondence, private-data posture, and launcher branding.

## Requirements

### Requirement: Stable Android channel identity
Cinderhell SHALL use `io.github.anthonystainer.cinderhell` as its production application ID, `io.github.anthonystainer.cinderhell.preview` for public previews, and `io.github.anthonystainer.cinderhell.debug` for development builds.

#### Scenario: Inspect a preview APK
- **WHEN** a public preview APK is inspected
- **THEN** its package name is `io.github.anthonystainer.cinderhell.preview`

#### Scenario: Install preview beside a future production build
- **WHEN** preview and production APKs are installed on the same device
- **THEN** Android treats them as separate applications

### Requirement: Validated release versioning
The release pipeline SHALL accept preview tags only in the form `vMAJOR.MINOR.PATCH-preview.N`, SHALL derive the APK version name and a monotonic Android version code from that tag, and SHALL reject invalid or out-of-range versions before building.

#### Scenario: Build the first preview
- **WHEN** the pipeline resolves `v0.1.0-preview.1`
- **THEN** the preview APK has version name `0.1.0-preview.1` and the corresponding deterministic version code

#### Scenario: Receive an invalid release tag
- **WHEN** the release workflow receives a tag outside the supported preview format
- **THEN** it fails before creating or updating a GitHub Release

### Requirement: Stable preview signing
Every published preview APK SHALL be signed by the same dedicated preview signing identity, whose private material is supplied through GitHub Environment secrets and never committed to the repository or retained as a workflow artifact.

#### Scenario: Upgrade between previews
- **WHEN** a user installs a newer preview with a greater version code over an older published preview
- **THEN** Android accepts the upgrade without uninstalling because package identity and signer match

#### Scenario: Signing material is unavailable
- **WHEN** any required preview-signing secret is absent or invalid
- **THEN** the release workflow fails without publishing an unsigned or debug-signed release APK

### Requirement: Verified release assets
Each preview release SHALL contain one versioned arm64 APK, one corresponding-source archive, and a SHA-256 manifest covering both files. The pipeline SHALL run automated tests, native dependency verification, APK content inspection, and signature verification before uploading them.

#### Scenario: Complete a tag build
- **WHEN** all release validation steps pass
- **THEN** the draft release assets have versioned names and their digests match the attached SHA-256 manifest

#### Scenario: Validation fails
- **WHEN** a test, dependency check, APK inspection, or signature verification fails
- **THEN** the workflow does not create a publicly visible release

### Requirement: Deliberate prerelease publication
The tag workflow SHALL create GitHub Releases as drafts marked prerelease and SHALL require a separate human action to make them public.

#### Scenario: Successful tagged build
- **WHEN** the release workflow finishes successfully for a new preview tag
- **THEN** GitHub contains a draft prerelease for that tag with generated notes and verified assets

#### Scenario: Re-run a release workflow
- **WHEN** the workflow is re-run for an existing unpublished preview tag
- **THEN** it updates the matching draft idempotently instead of creating a duplicate release

### Requirement: GPL release correspondence
Cinderhell SHALL declare GPL-2.0-or-later at the repository root and SHALL distribute corresponding source sufficient to rebuild each released APK, including Cinderhell source, native patch sets, build metadata, documentation, and pinned third-party revisions.

#### Scenario: Inspect a release source archive
- **WHEN** a recipient extracts the corresponding-source archive
- **THEN** it contains the root license, README, build scripts, application and native source, OpenSpec contracts, and third-party revision metadata without signing credentials or proprietary game data

### Requirement: Explicit private-data posture
Cinderhell SHALL disable Android platform backup until a user-controlled export and restore capability defines how imported game data, profiles, and saves are transferred.

#### Scenario: Inspect application backup settings
- **WHEN** the release manifest is inspected
- **THEN** both backup and device-to-device transfer through Android's automatic backup mechanism are disabled

### Requirement: Release-ready launcher branding
Cinderhell SHALL provide adaptive, legacy, and monochrome launcher icon resources referenced by the application manifest.

#### Scenario: Display Cinderhell in a supported launcher
- **WHEN** Android renders the app in a legacy, adaptive, or themed-icon launcher
- **THEN** it uses a Cinderhell-owned icon resource rather than a generic application placeholder
