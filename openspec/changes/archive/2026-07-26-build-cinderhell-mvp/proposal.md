## Why

Android Doom launchers tend to expose the complexity of supporting many engines, while handheld players need a focused experience that feels like a polished game. Cinderhell will make classic Doom content immediately playable through one curated Woof-based runtime, a controller-first launcher, and safe Android-native content import.

## What Changes

- Create an Android application named Cinderhell with a Kotlin/Compose launcher and a separate native SDL3/Woof game process.
- Provide an immediately playable Freedoom experience and import user-selected Doom IWADs through Android's system document picker.
- Catalogue imported content in app-owned storage without broad filesystem permissions.
- Let users create and launch profiles containing one game, ordered PWAD/DEH/BEX additions, and a curated Original, Enhanced, or Handheld preset.
- Make both launcher and game controller-first, including sensible handheld defaults, Android Back handling, fullscreen presentation, audio/lifecycle behavior, and clean return to the launcher.
- Target `arm64-v8a` initially and support Doom, Doom II, TNT, Plutonia, Freedoom, and Woof-compatible vanilla/Boom/MBF/MBF21 content.
- Keep multiplayer, downloads, multiple engines, GZDoom/ZScript compatibility, editable touch controls, a main-screen command-line interface, and validated Bluetooth-gamepad support out of the MVP.

## Capabilities

### New Capabilities

- `content-library`: Import, identify, copy, catalogue, and manage supported game and mod files in app-owned storage.
- `play-profiles`: Select games, compose ordered mod sets, apply curated presets, and start or continue play with minimal launcher interaction.
- `native-game-runtime`: Package and run SDL3/Woof as an isolated Android game process with shared saves/configuration and robust lifecycle behavior.
- `controller-first-experience`: Navigate and play without touch using the target AYN handheld's built-in controls and curated handheld mappings.

### Modified Capabilities

None.

## Impact

- Introduces a greenfield Android Gradle project, Kotlin/Compose UI, persistent library/profile data, and Storage Access Framework integration.
- Introduces an NDK/CMake native build for Woof and its required libraries, packaged as `libmain.so` for `arm64-v8a`.
- Adds GPL-compatible source and distribution obligations for the combined application, plus bundled Freedoom attribution and licensing.
- Requires on-device validation on the target AYN handheld for its built-in controller mapping, audio, suspend/resume, display refresh rates, and frame pacing. Bluetooth-gamepad compatibility remains a future validation feature.
