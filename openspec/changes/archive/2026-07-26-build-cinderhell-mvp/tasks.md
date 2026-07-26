## 1. Project and Dependency Baseline

- [x] 1.1 Record the application ID, minimum/target SDK, JDK, Gradle, Android Gradle Plugin, Kotlin, Compose, NDK, and CMake versions in a reproducible toolchain manifest.
- [x] 1.2 Scaffold the single-app Android project with Compose, Room, Kotlin serialization, coroutines, unit/instrumentation tests, and `arm64-v8a` as the only packaged ABI.
- [x] 1.3 Pin Woof, SDL3, OpenAL Soft, and Freedoom revisions with checksums, source locations, licenses, and the chosen submodule/subtree/fork strategy.
- [x] 1.4 Add CI bootstrap checks that fail on an unpinned native dependency, missing runtime asset, unexpected ABI, or absent license metadata.

## 2. Native Android Port Spike

- [x] 2.1 Integrate the pinned SDL3 Android AAR/Prefab package and prove a minimal `SDLActivity` shared-library target launches on arm64.
- [x] 2.2 Cross-compile pinned OpenAL Soft with the NDK and verify native linkage and audio-device initialization in the SDL activity.
- [x] 2.3 Adapt the pinned Woof CMake target to produce `libmain.so`, disable nonessential optional integrations, and document every Android-specific source/build delta.
- [x] 2.4 Package or stage `woof.pk3` and the selected single-player Freedoom IWAD, including integrity verification before native startup.
- [x] 2.5 Complete the native smoke gate on physical hardware: render Freedoom, produce music and sound, accept gamepad input, write/read a save, and return from `SDL_main` normally.

## 3. Launcher and Game Process Boundary

- [x] 3.1 Add exported `LauncherActivity` and non-exported landscape `GameActivity` with `android:process=":game"` and no externally invokable game intent filter.
- [x] 3.2 Define versioned serializable session descriptor/result models and app-private directories for pending sessions, results, configs, saves, screenshots, and runtime assets.
- [x] 3.3 Implement atomic session/result file writes, random nonce generation, expiry, single-consumption semantics, and task-specific temporary-file cleanup.
- [x] 3.4 Implement launcher preflight and session creation using only canonical app-owned paths and a fixed allowlist of supported launch modes and Woof options.
- [x] 3.5 Implement `GameActivity` descriptor validation and SDL argument construction while rejecting missing, stale, malformed, replayed, or forged requests.
- [x] 3.6 Implement clean result flushing, activity finish, dedicated-process termination, launcher result ingestion, and a relaunch gate that prevents native process reuse.
- [x] 3.7 Add instrumentation coverage for repeated launch/quit cycles, invalid session IDs, native startup failure, process death, and switching profiles between sessions.

## 4. Runtime Lifecycle and Presentation

- [x] 4.1 Wire pause/resume, SDL quit, audio focus, and surface callbacks so background gameplay/audio stop and a single engine instance resumes safely.
- [x] 4.2 Implement immersive landscape mode, Android 15 safe-area handling, supported refresh-mode selection, and restoration of system UI after game exit.
- [x] 4.3 Translate Android Back into Woof menu input and ensure confirmed Doom-menu Quit returns through the clean session-completion path.
- [x] 4.4 Persist recoverable crash/startup diagnostics and present them in the launcher without exposing raw source-port terminology in the primary flow.
- [x] 4.5 Pass the lifecycle gate on physical hardware for suspend/resume, screen off/on, surface recreation, audio interruption, forced game-process death, and high-refresh frame pacing.

## 5. Content Catalogue and Bundled Freedoom

- [x] 5.1 Implement Room entities, DAOs, relations, migrations, and repositories for `ContentItem`, `Profile`, `ProfileEntry`, and `RecentSession`, with the launcher as the sole database writer.
- [x] 5.2 Bootstrap the verified bundled Freedoom asset into the content-addressed store and create its default Handheld profile idempotently on first launch.
- [x] 5.3 Implement the system document-picker flow and a cancellable background importer that streams once into a task-scoped temporary file while calculating SHA-256.
- [x] 5.4 Implement a bounds-checked WAD parser for header/lump validation and a versioned known-IWAD hash catalogue for Doom, Doom II, TNT, Plutonia, and Freedoom identities.
- [x] 5.5 Implement bounded PK3/ZIP inspection and DEH/BEX recognition without archive extraction, including size and malformed-input limits.
- [x] 5.6 Implement the atomic blob/catalogue commit, digest deduplication, friendly duplicate result, restart recovery, and orphan temporary-file cleanup.
- [x] 5.7 Implement content removal with affected-profile discovery, explicit confirmation, referential updates, and blob deletion only when no remaining record uses it.
- [x] 5.8 Add unit/instrumentation tests for renamed IWADs, misleading extensions/MIME types, duplicate bytes, interrupted imports, hostile WAD offsets, malformed archives, lost source URIs, and safe removal.

## 6. Profiles, Presets, Play, and Continue

- [x] 6.1 Implement profile creation/editing with exactly one installed game, ordered mod/patch entries, stable reorder operations, and persistence across restart.
- [x] 6.2 Define versioned Original, Enhanced, and Handheld mappings against the pinned Woof configuration keys and cover their promised values with tests.
- [x] 6.3 Materialize profile-specific config/save directories, preserve in-game user edits, and make preset reapplication an explicit previewed operation.
- [x] 6.4 Implement profile preflight for missing files, runtime assets, known unsupported engine requirements, invalid ordering, and unusable resume state.
- [x] 6.5 Implement one-action Play from the selected valid profile and keep the engine chooser and raw command-line controls absent.
- [x] 6.6 Implement atomic game-written recent-state metadata and launcher Continue behavior that names the game/profile/level and resumes only a still-valid save.
- [x] 6.7 Add a secondary Advanced route for supported Woof settings without moving low-level settings into the normal home flow.
- [x] 6.8 Add repository and UI tests for deterministic load order, profile persistence, preset versioning, blocked preflight, Play arguments, and Continue eligibility.

## 7. Controller-First Launcher and Gameplay

- [x] 7.1 Build first-launch and normal Compose home screens around Play, Import game, game selection, mod profiles, and contextual Continue.
- [x] 7.2 Implement reusable controller-focus components with visible focus, stable-ID restoration, D-pad/left-stick navigation, confirm, and back semantics.
- [x] 7.3 Make import results, profile editing/reordering, removal confirmation, errors, Advanced settings, and all primary launcher dialogs operable without touch.
- [x] 7.4 Define and verify the complete Handheld gameplay mapping for movement, aim, fire, use, weapons, pause, and menus using SDL3's gamepad API.
- [x] 7.5 Implement launcher and game controller hot-plug state, safe disconnect behavior, automatic reconnect, and capability-gated rumble.
- [x] 7.6 Capture the target AYN controller descriptor and add an SDL mapping override only if physical testing proves the stock mapping incorrect.
- [x] 7.7 Add Compose focus/navigation tests and pass a controller-only end-to-end flow from cold launch through import/profile launch, gameplay, save, quit, and Continue.

## 8. Compatibility, Quality, and Release

- [x] 8.1 Assemble a legal test corpus covering supported IWAD identities plus representative vanilla, Boom, MBF, MBF21, PWAD, PK3/ZIP, DEH, and BEX combinations.
- [x] 8.2 Run the compatibility corpus and record launch, level entry, save/load, load order, and actionable failure results against the pinned Woof build.
- [x] 8.3 Complete the physical-device matrix on the target AYN firmware using its built-in controller for controls, rumble capability behavior, audio, suspend/resume, safe areas, and refresh modes.
- [x] 8.4 Add reproducible debug/release CI builds, JVM/native/instrumentation test stages, APK ABI/content inspection, and retained diagnostic artifacts.
- [x] 8.5 Generate in-app notices and release corresponding-source/license bundles for Woof, Freedoom, SDL3, OpenAL Soft, and every packaged component.
- [x] 8.6 Verify clean install and database/preset upgrade paths preserve imported blobs, profiles, configs, saves, screenshots, and recent-session state.
- [x] 8.7 Produce a signed internal preview `arm64-v8a` APK and complete the four MVP acceptance gates documented in the design.

## 9. Deferred Future Feature

The following work is intentionally outside this change and does not block MVP completion or archival:

- Validate representative Bluetooth gamepads across selected Android firmware for mappings, controller-only flows, rumble, disconnect/reconnect, audio and lifecycle interactions, safe areas, and refresh modes.
- Promote Bluetooth gamepads from best-effort SDL compatibility to a documented support matrix only after that physical validation passes.
