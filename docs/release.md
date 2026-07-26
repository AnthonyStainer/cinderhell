# Preview releases

Cinderhell distributes public test builds as `arm64-v8a` GitHub prereleases.
Preview releases are deliberately separate from a future stable channel:

| Channel | Android package ID | Signing identity |
| --- | --- | --- |
| Development | `io.github.anthonystainer.cinderhell.debug` | Local Android debug key |
| Preview | `io.github.anthonystainer.cinderhell.preview` | Dedicated preview key |
| Production | `io.github.anthonystainer.cinderhell` | Future production key |

This separation lets preview and stable builds coexist and keeps the preview
key out of the production trust boundary.

## Version contract

Preview tags must match `vMAJOR.MINOR.PATCH-preview.N`, for example
`v0.1.0-preview.1`. `scripts/resolve-release-version.py` rejects leading
zeroes, components outside the supported range, and preview ordinals above
9998.

The Android version code is:

```text
MAJOR × 100,000,000 + MINOR × 1,000,000 + PATCH × 10,000 + N
```

This makes previews monotonic and reserves ordinal 9999 for a future stable
release on the same patch line. Published version codes and tags are never
reused.

## Local release-equivalent build

After fetching pinned dependencies, run:

```sh
CINDERHELL_RELEASE_VERSION=0.1.0-preview.1 \
CINDERHELL_VERSION_NAME=0.1.0-preview.1 \
CINDERHELL_VERSION_CODE=1000001 \
JAVA_HOME="$PWD/.toolchains/jdk-17.0.19+10" \
ANDROID_HOME=/path/to/Android/Sdk \
./scripts/build-preview.sh
```

Without all four `CINDERHELL_PREVIEW_*` signing variables this uses the
development key. A distributable build additionally sets:

```text
CINDERHELL_REQUIRE_PREVIEW_SIGNING=true
CINDERHELL_PREVIEW_KEYSTORE_FILE=/absolute/path/to/preview.jks
CINDERHELL_PREVIEW_KEYSTORE_PASSWORD=...
CINDERHELL_PREVIEW_KEY_ALIAS=...
CINDERHELL_PREVIEW_KEY_PASSWORD=...
```

The script verifies dependency pins, release-version tests, JVM tests, the
minified preview, ABI/runtime/legal contents, and the APK signature. It writes:

```text
build/release/cinderhell-<version>-arm64.apk
build/release/cinderhell-<version>-corresponding-source.tar.gz
build/release/SHA256SUMS
```

## GitHub draft workflow

`.github/workflows/release-preview.yml` runs only for matching preview tags.
It reads the dedicated key from the `preview-release` GitHub Environment,
performs the full build and metadata checks, and creates or updates a draft
GitHub prerelease. It never publishes the release.

Maintainer procedure:

1. Update `CHANGELOG.md`, make sure `main` is clean, and wait for the required
   Android check.
2. Resolve the tag locally and confirm the intended version code:
   `./scripts/resolve-release-version.py v0.1.0-preview.1`.
3. Create and push an annotated tag:
   `git tag -a v0.1.0-preview.1 -m "Cinderhell 0.1.0 preview 1"` then
   `git push origin v0.1.0-preview.1`.
4. Wait for **Preview release / Build draft prerelease** to pass.
5. Download the draft assets, run `sha256sum --check SHA256SUMS`, install the
   APK, and complete the relevant physical smoke gates.
6. Confirm the package ID, version, and signer certificate.
7. Publish the draft in GitHub only after the smoke check. Keep it marked as a
   prerelease.

If a draft is bad, delete the unpublished draft and tag, fix `main`, and use a
higher preview ordinal. Never move a tag after an asset has been shared.

## Signing recovery

The preview keystore and its generated credentials are kept in an owner-only
directory outside the repository. The same four values are stored as encrypted
secrets in the `preview-release` GitHub Environment. The key, passwords, and
base64 encoding must never appear in commits, Actions artifacts, issues, or
logs.

After restoring a key, compare its SHA-256 certificate fingerprint with a
known published preview before releasing:

```sh
keytool -list -v -keystore /path/to/cinderhell-preview.jks
apksigner verify --print-certs cinderhell-<version>-arm64.apk
```

The expected preview certificate fingerprint is recorded below after initial
key provisioning:

```text
SHA-256: 02:9A:A1:B5:4B:A5:C2:2A:6D:CF:4B:79:57:51:0F:D0:64:9E:14:43:0F:4C:C8:96:CC:78:D9:00:9F:D3:41:48
```

Loss of the preview key requires a new preview package identity and forces
testers to reinstall. The future production key remains independent.

## Included runtime and corresponding source

The APK includes the pinned Woof, SDL3, OpenAL Soft, Freedoom, generated
`woof.pk3`, and legal notices, only for `arm64-v8a`. The deterministic
corresponding-source archive includes the root README/license, OpenSpec
contracts, Android/native source, Woof patch, pinned source trees and SDL
source, scripts, wrapper, lock metadata, CI, tests, and documentation.

It excludes commercial Doom data, local toolchains, signing material, and
generated caches. Physical AYN Thor evidence is recorded in
`docs/acceptance-gates.md`; Bluetooth controller certification remains
deferred.
