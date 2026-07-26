## Why

Cinderhell can already produce a verified preview APK and corresponding-source archive, but those artifacts are only retained inside individual CI runs and preview signing is not stable between runners. A small, explicit release foundation is needed before iteration begins so testers receive upgrade-compatible, traceable builds with the licensing and Android product identity settled.

## What Changes

- **BREAKING**: Adopt `io.github.anthonystainer.cinderhell` as the permanent Android application ID before any public release.
- Add a root GPL-2.0-or-later license and include project-level legal/readme material in corresponding-source archives.
- Disable Android platform backup until Cinderhell provides an intentional export and restore contract.
- Add an adaptive Cinderhell launcher icon and release-ready application metadata.
- Derive release version metadata from validated Git tags while keeping local development builds deterministic.
- Sign preview releases with a dedicated, recoverable signing identity supplied through a protected GitHub environment.
- Publish verified tag builds as draft GitHub prereleases with an arm64 APK, corresponding source, checksums, and generated release notes.
- Harden the repository foundation with protected release permissions, vulnerability reporting, action pinning, contributor/security templates, and one low-frequency grouped dependency-update stream.

## Capabilities

### New Capabilities

- `release-distribution`: Defines Cinderhell's stable Android identity, versioning, signing, licensing, backup posture, and GitHub prerelease publication contract.

### Modified Capabilities

None.

## Impact

The Android build configuration and manifest, resource set, release/package scripts, GitHub Actions workflows, project documentation, repository policy files, and GitHub repository settings are affected. Existing development installs using `dev.cinderhell` will not upgrade in place, which is acceptable before the first public release.
