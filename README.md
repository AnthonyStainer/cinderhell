# Cinderhell

[![Android](https://github.com/AnthonyStainer/cinderhell/actions/workflows/android.yml/badge.svg)](https://github.com/AnthonyStainer/cinderhell/actions/workflows/android.yml)
[![License: GPL-2.0-or-later](https://img.shields.io/badge/license-GPL--2.0--or--later-blue.svg)](LICENSE)

**One excellent Doom engine, presented as a polished Android game rather than
an engine-launching toolkit.**

Cinderhell is a controller-first Android frontend for classic Doom content. It
combines a focused Kotlin/Compose launcher with a pinned SDL3/Woof runtime,
bundled Freedoom, safe Android-native file importing, and isolated native game
sessions.

## Status

Cinderhell 0.1 is a working MVP targeting `arm64-v8a` handhelds:

- Freedoom boots and plays without a network connection or imported data.
- The complete controller-only flow has been validated on an AYN Thor.
- Doom, Doom II, TNT, Plutonia, Freedoom, and Woof-compatible vanilla, Boom,
  MBF, and MBF21 content are in scope.
- Preview builds are distributed through
  [GitHub Releases](https://github.com/AnthonyStainer/cinderhell/releases) as
  signed, checksummed `arm64-v8a` APKs with corresponding source.
- Bluetooth gamepads may work through SDL on a best-effort basis, but mappings,
  rumble, and reconnect behavior are not currently validated or supported.

See the [acceptance gates](docs/acceptance-gates.md) and
[controller matrix](docs/controller-matrix.md) for the recorded device results.

## What it does

- Imports `.wad`, `.pk3`, `.zip`, `.deh`, and `.bex` files through Android's
  system document picker without broad storage permissions.
- Copies accepted content into immutable, content-addressed app storage so
  profiles do not depend on the original document remaining available.
- Creates named profiles with one game, ordered mods or patches, and curated
  Original, Enhanced, or Handheld presets.
- Offers one-action **Play** and contextual **Continue** without requiring
  source-port or command-line knowledge.
- Runs each game in a private `:game` process so every Woof session starts with
  clean native state and returns safely to the launcher.
- Preserves profile-specific configuration, saves, screenshots, and recent
  session state across normal Android lifecycle events.

The MVP intentionally does not include multiple engines, multiplayer, mod
downloads, GZDoom/ZScript compatibility, editable touch controls, or a
main-screen command line.

## Architecture

```text
LauncherActivity — Kotlin / Compose
    │
    │ validated session descriptor
    ▼
GameActivity — private :game process
    │
    ├── SDL3
    ├── OpenAL Soft
    └── Woof
```

Android owns importing, profiles, focus navigation, and lifecycle presentation.
Woof owns gameplay, rendering, audio, saves, and advanced engine settings.

## Build

The checked-in Gradle wrapper and dependency lock are authoritative. A Linux
build requires Git, Python 3, curl, unzip, an Android SDK, and the Android
components listed in [docs/toolchain.md](docs/toolchain.md), including JDK 17,
SDK 35, build-tools 35.0.0, NDK 27.0.12077973, and CMake 3.31.6.

```sh
git clone --recurse-submodules https://github.com/AnthonyStainer/cinderhell.git
cd cinderhell

./scripts/fetch-jdk.sh
./scripts/fetch-native-dependencies.sh

JAVA_HOME="$PWD/.toolchains/jdk-17.0.19+10" \
ANDROID_HOME=/path/to/Android/Sdk \
./scripts/build-preview.sh
```

The locally signed preview APK, corresponding-source archive, and combined
`SHA256SUMS` manifest are written to `build/release/`. Without the maintainer's
dedicated signing environment, local preview builds use Android's development
key and are not suitable for distribution.

For a quicker development check after fetching dependencies:

```sh
./scripts/verify-bootstrap.sh
JAVA_HOME="$PWD/.toolchains/jdk-17.0.19+10" ./gradlew testDebugUnitTest
openspec validate --all --strict
```

Physical instrumentation requires an arm64 Android device:

```sh
./scripts/apply-native-patches.sh
ANDROID_SERIAL=<serial> ./gradlew connectedDebugAndroidTest
```

More detail is available in the
[native port notes](docs/native-port.md),
[release guide](docs/release.md), and
[compatibility matrix](docs/compatibility-matrix.md).

## Releases

Preview tags follow `vMAJOR.MINOR.PATCH-preview.N`. A matching tag builds and
verifies an upgrade-compatible preview APK, then creates an unpublished draft
GitHub prerelease. A maintainer smoke-tests and explicitly publishes that draft.
See the [release guide](docs/release.md) for the version-code scheme, signing
boundary, artifact contract, and recovery procedure.

Preview, production, and development builds deliberately use distinct Android
package IDs. Builds made before the first public preview used provisional IDs
and do not migrate in place.

## Game data and licensing

Cinderhell does not include commercial Doom game data. Importing a commercial
IWAD requires a copy you are entitled to use. Freedoom 0.13.0 provides the
redistributable first-run game.

Cinderhell is distributed under [GPL-2.0-or-later](LICENSE). SDL3, OpenAL Soft,
Freedoom, Woof, and the other packaged components retain their respective
licenses. Exact revisions, checksums, and SPDX-style license identifiers are
recorded in
[`third_party/dependencies.lock.toml`](third_party/dependencies.lock.toml).
Release artifacts include full notices and corresponding source; see
[`THIRD_PARTY_NOTICES.txt`](app/src/main/assets/legal/THIRD_PARTY_NOTICES.txt)
and
[`CORRESPONDING_SOURCE.txt`](app/src/main/assets/legal/CORRESPONDING_SOURCE.txt).
