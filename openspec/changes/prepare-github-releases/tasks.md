## 1. Product Identity and Policy

- [x] 1.1 Adopt the permanent base, preview, and debug application IDs and update identity assertions/documentation
- [x] 1.2 Add the root GPL-2.0-or-later license and SPDX project metadata
- [x] 1.3 Disable Android backup and device-transfer behavior in the manifest
- [x] 1.4 Add adaptive, monochrome, and legacy Cinderhell launcher icon resources

## 2. Reproducible Versioning and Signing

- [x] 2.1 Add and test a strict preview-tag resolver for Android version name/code metadata
- [x] 2.2 Wire Gradle preview builds to explicit version and dedicated signing environment variables
- [x] 2.3 Generate a recoverable preview key outside the repository and configure the GitHub `preview-release` environment secrets

## 3. Release Packaging and Automation

- [x] 3.1 Update preview/source packaging so release filenames and source contents are tag-versioned and complete
- [x] 3.2 Add a least-privilege tag-triggered workflow that verifies and creates an idempotent draft GitHub prerelease
- [x] 3.3 Pin GitHub Actions to immutable revisions and add safe Gradle caching to continuous integration

## 4. Repository Foundation

- [x] 4.1 Add low-noise grouped Dependabot updates, security policy, contribution guide, changelog, ownership, and issue templates
- [x] 4.2 Update README and release documentation with release channels, versioning, signing recovery, and publication procedure
- [x] 4.3 Configure GitHub security automation and protect `main` with the Android build check

## 5. Verification and First Draft

- [x] 5.1 Run unit, build, APK inspection, archive-content, license, versioning, and OpenSpec validation gates
- [x] 5.2 Commit and push the release foundation, then confirm the required `main` workflow succeeds
- [ ] 5.3 Create `v0.1.0-preview.1`, verify the release workflow and draft assets, and retain the release as an unpublished prerelease
