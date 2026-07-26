## Context

Cinderhell is a greenfield Android application intended to feel like one polished handheld Doom game, not a general source-port frontend. The launcher owns Android-native concerns such as content import, catalogue state, profile composition, focus navigation, and errors. Woof owns gameplay, rendering, audio, saves, and its advanced engine settings.

Current upstream Woof uses CMake, SDL3 (currently at least 3.4), OpenAL Soft, a required base PK3, and several optional audio/data dependencies. It is a desktop executable rather than an upstream Android target, so the first technical milestone is a deliberately small Android port. SDL's official Android integration supports an `SDLActivity` subclass, CMake/Prefab, and converting an executable entry target into a shared library named `main`. Android also supports placing an activity in an application-private named process. These are the seams Cinderhell will use.

The first supported hardware target is an AYN handheld with a built-in Android gamepad. Commercial IWADs are never shipped; users import their own. A redistributable Freedoom IWAD provides the offline first-run game. The combined source distribution must satisfy Woof's GPL terms and reproduce the license notices for Freedoom and all packaged dependencies.

## Goals / Non-Goals

**Goals:**

- Produce a reproducible `arm64-v8a` APK that boots bundled Freedoom through Woof.
- Make importing a supported IWAD, choosing a profile, and playing require no engine knowledge.
- Make the launcher and game usable with the target AYN handheld's built-in controls.
- Isolate each native run so Woof can retain its process-global assumptions.
- Preserve games, profiles, configuration, and saves through ordinary lifecycle events and app upgrades.
- Establish a narrow architecture that can be polished and tested without becoming a multi-engine platform.

**Non-Goals:**

- Multiple source ports, Heretic/Hexen/Doom 64/Doom 3, GZDoom or ZScript compatibility.
- Multiplayer, mod discovery/downloads, cloud synchronization, or broad filesystem browsing.
- Editable virtual controls, device-gyro bridging, or complete phone-only gameplay for the MVP.
- Validated Bluetooth-gamepad mappings, external-controller rumble, and disconnect/reconnect guarantees for the MVP.
- Reimplementing Woof menus or every Woof option in Compose.
- A stable plugin API or arbitrary user-entered command lines.

## Decisions

### 1. Use two activities in two processes

`LauncherActivity` will be the exported launcher activity in the default process and use Kotlin/Compose. `GameActivity` will subclass the SDL3 activity, be non-exported, locked to landscape, and declare `android:process=":game"`.

The launcher writes a session descriptor and starts `GameActivity` with an explicit intent containing only an opaque session ID. On normal SDL/Woof return, the game activity records a result, finishes, and terminates only the dedicated process after the activity has completed. The launcher observes the result when it resumes. A new session is not launchable until the previous process is confirmed gone.

This avoids asking Woof to completely reset decades of process-global state. A single long-lived native process was rejected because reliable teardown would be a large, invasive engine change. A separate APK or bound-service protocol was rejected because it adds installation, IPC, storage, and signing complexity without improving the MVP.

### 2. Treat launch input as data, not a command line

The launcher's domain layer will atomically write a versioned JSON session descriptor under an app-private sessions directory. It contains the profile ID, content-addressed absolute paths in their load order, config/save paths, preset version, launch mode, and a random nonce. `GameActivity` accepts only a launcher-created pending session ID, validates the descriptor and paths again, and overrides SDL's argument hook to produce the small, known Woof argument set.

This avoids Binder size limits, intent/path spoofing, quoting ambiguity, and an accidental public command-line API. It also leaves a useful diagnostic record. Raw engine arguments remain unavailable from the normal UI.

### 3. Keep launcher-owned records separate from runtime files

Room will store normalized launcher state:

- `ContentItem`: digest, display name, internal blob path, size, type, detected identity, import time.
- `Profile`: name, game content ID, preset ID/version, selected state, and per-profile config path.
- `ProfileEntry`: profile ID, content ID, kind, and stable load position.
- `RecentSession`: profile ID, start/end time, result, latest known level, and resumable state reference.

Imported bytes will be immutable, content-addressed blobs under `files/content/sha256/`. The launcher is the only Room writer. The game process does not open the database; it reads a session snapshot and writes atomic result/state files. Configs, saves, and screenshots live under profile-specific app-private directories. Cross-process handoff files use write-to-temp plus rename.

JSON-only catalogue/profile storage was rejected because ordered relations, duplicate detection, migrations, and referential checks are clearer in Room. Letting both processes use preferences or DataStore was rejected because multi-process caching and concurrent writes add avoidable failure modes.

### 4. Copy imports and inspect them defensively

The Compose launcher will use `ActivityResultContracts.OpenDocument`. A background coroutine streams the content URI once, hashes it, and writes a temporary app-private file. Import never depends on a retained URI after commit.

Classification is content-first:

- WAD files: validate `IWAD`/`PWAD`, bounds-check the directory and lumps, and compare known hashes for supported IWAD releases.
- PK3/ZIP files: validate the archive structure and inspect only bounded metadata; do not extract archives during import.
- DEH/BEX files: validate size and recognizable text structure before cataloguing as patches.

The blob rename and Room transaction form the commit boundary. Failed imports remove their task-specific temporary file. No storage permission beyond the picker grant is requested. `MANAGE_EXTERNAL_STORAGE` is prohibited.

Keeping only a content URI was rejected because provider availability and user file movement would make profiles fragile. Trusting extensions was rejected because document providers can omit or falsify names and MIME types.

### 5. Pin the native stack and keep the port delta reviewable

Cinderhell will pin a specific Woof revision and the exact SDL3/OpenAL Soft revisions used to build it. The native layout will separate upstream code from Cinderhell's Android glue and document every carried patch. The preferred dependency path is:

- SDL3 packaged through its official Android AAR/Prefab integration.
- OpenAL Soft cross-compiled with the NDK and linked into the game runtime.
- Woof's built-in OPL music path for the first vertical slice.
- Optional FluidSynth, libsndfile, libxmp, Discord RPC, and other nonessential integrations disabled until the base runtime is stable.

The Android CMake wrapper will build Woof's main target as `libmain.so`, stage `woof.pk3` and the selected Freedoom IWAD as APK assets, and limit packaged ABIs to `arm64-v8a`. Runtime assets that require ordinary filesystem access will be copied and verified into app-private storage before launch.

Rewriting the engine or starting with a broader port was rejected. Vendoring unpinned release binaries was rejected because it harms reproducibility, auditability, and GPL source correspondence. The exact submodule/subtree or maintained-fork mechanism will be chosen during the port spike, but builds must remain pinned and repeatable.

### 6. Make presets versioned configuration templates

Original, Enhanced, and Handheld are versioned mappings from product choices to supported Woof configuration keys. Creating a profile materializes a profile-specific config from a preset. Later in-game changes persist for that profile; reopening the launcher does not silently overwrite them. Reapplying or changing a preset is an explicit action that previews which curated values will change.

Save directories are profile-specific to avoid filename collisions between different mod sets. The Continue action is driven by a game-written atomic session-state record and is offered only when the exact profile and save still exist.

A single global config/save directory was rejected because changing between incompatible mod sets would create surprising settings and save collisions. Exposing dozens of engine variables up front was rejected because it contradicts the product goal.

### 7. Build controller operation into the UI and runtime boundaries

Compose screens will define deterministic focus order, visible focus treatment, D-pad/left-stick navigation, confirm, and back behavior. Focus restoration keys will use stable database IDs so library changes do not strand focus.

Woof will continue to use SDL3's gamepad layer. Cinderhell will add a narrowly scoped mapping override only when device testing demonstrates that the target handheld is not correctly described by SDL. The Handheld preset supplies conservative stick curves/deadzones and a complete Doom action mapping. Android Back becomes SDL's back key event and enters Woof's menu; quitting remains a confirmed in-game action. SDL's existing external-controller detection and Cinderhell's capability-gated rumble paths may remain as a best-effort foundation, but Bluetooth compatibility, hot-plug behavior, and external-controller feedback are not product-supported or acceptance-gated in the MVP. Device gyro is also deferred.

A custom analogue input stack and touchscreen HUD were rejected because Woof and SDL already own the relevant controller behavior and the MVP is handheld-first.

### 8. Use staged verification gates

The implementation will advance through four gates:

1. Native smoke test: a debug APK launches a pinned Freedoom IWAD, renders, produces audio, accepts controls, saves, and returns cleanly.
2. Lifecycle gate: repeated launch/quit cycles, background/foreground, surface recreation, audio focus, and process death preserve data and never reuse Woof state.
3. Product gate: import, catalogue, profiles, presets, Continue, and all primary flows work controller-only.
4. Release gate: signed arm64 APK, clean-install/upgrade tests, license/source bundle, automated JVM/native tests, and a target-AYN physical-device matrix using its built-in controls.

Emulators remain useful for UI and import tests, but rendering, audio latency, refresh behavior, rumble, and built-in controls require physical hardware.

### 9. Defer validated Bluetooth-gamepad support

Bluetooth gamepads are a future product feature rather than an MVP release dependency. A follow-on change must select representative controller families and Android versions, then validate mappings, launcher and gameplay navigation, rumble capability reporting, disconnect/reconnect behavior, audio and lifecycle interactions, safe areas, and refresh modes. Until that matrix exists, external pads discovered through SDL are best-effort and Cinderhell makes no compatibility promise for them.

The existing generic SDL input and hot-plug plumbing will remain in place because it does not compromise the built-in-controller experience and provides a useful foundation for that work. Deferral changes the support contract and acceptance matrix; it does not require removing working generic input code.

## Risks / Trade-offs

- [Woof contains desktop-only assumptions beyond its build target] → Land the smallest render/audio/input smoke test first, isolate Android conditionals, and keep a documented upstream-friendly patch set.
- [OpenAL Soft or optional codecs expand the NDK port] → Start with required OpenAL plus built-in OPL and keep optional music/audio libraries disabled until lifecycle stability.
- [A dedicated Android process can remain cached after its activity ends] → Mark clean engine return explicitly, finish the activity, terminate only `:game`, and block relaunch until the old process is gone.
- [Process termination can lose the latest metadata] → Atomically flush saves and session result before signalling completion or terminating the dedicated process.
- [Malformed user files can trigger parser or engine bugs] → Bounds-check in the Kotlin preflight parser, never extract archives at import, retain immutable blobs, and provide a diagnostic launch failure.
- [Known hashes cannot identify every legitimate IWAD revision or mod] → Use hashes for confident naming, structural rules for safe classification, and keep the supported-game allowlist independently updateable.
- [Preset values drift as Woof changes] → Pin Woof, version each preset mapping, and migrate only through an explicit tested preset version.
- [SDL mappings vary by Android firmware] → Record the target AYN built-in controller descriptor now, carry only evidence-based overrides, and keep Bluetooth controllers explicitly best-effort until a follow-on physical matrix establishes support.
- [Bundled data and native code enlarge the APK] → Ship one single-player Freedoom IWAD and arm64 only initially; revisit asset delivery after measuring the release artifact.
- [GPL and bundled-asset obligations are missed] → Generate notices and corresponding-source documentation in CI and make release packaging fail when pinned-source/license metadata is incomplete.

## Migration Plan

This is a greenfield product, so no user-data migration is required for the first install. Implementation should ship as internal debug builds through the staged gates above before publishing a signed preview release.

Future schema, preset, or storage changes must use Room migrations and idempotent file migrations. An upgrade must never delete imported blobs or saves merely because migration fails; it should leave the prior data in place and present a recoverable error. Uninstall remains the Android-supported rollback for pre-release builds and removes app-owned copies, while users' original picked documents remain untouched.

## Open Questions

- Which exact Woof, SDL3, OpenAL Soft, NDK, minimum SDK, and target SDK revisions pass the initial physical-device spike?
- Should upstream sources be tracked as submodules, a subtree, or a small Cinderhell-maintained Woof fork after the Android delta is known?
- Which single-player Freedoom IWAD gives the best first-run balance between APK size and Doom/Doom II mod compatibility?
- What package ID, signing identity, and distribution channel should be fixed before the first external preview?
- Which Bluetooth-controller families and Android firmware combinations should define the future external-gamepad support matrix?
