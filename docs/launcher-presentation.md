# Launcher presentation

Cinderhell uses a code-native **ember and iron** presentation: near-black
backdrops, iron surfaces, restrained ember accents, high-contrast controller
focus, and warm ash text. It deliberately avoids downloaded cover art, custom
font binaries, or proprietary Doom artwork.

## Information hierarchy

The normal home route is ordered around play:

1. Selected game and profile, curated preset, and mod count.
2. Primary **Play** and contextual **Continue**.
3. Installed game and mod-profile selection.
4. Quieter import, library, advanced, and notice utilities.

Wide landscape windows separate the play hero from selection and management.
Compact windows retain the same order in one vertically scrollable flow.
Focus, selection, disabled state, busy state, and notice severity have
independent treatments; selection is never represented by focus alone.

Stable Compose test tags and focus destinations remain part of the controller
contract. The presentation refactor does not change profile persistence,
engine arguments, saves, package identities, or controller mappings.

## No-device verification

While the target handheld is unavailable, run the complete non-physical gate:

```sh
./scripts/verify-no-device.sh
```

The script fetches and verifies pinned inputs, applies the Android native patch
for the duration of the build, runs JVM and presentation tests, compiles the
Android-test APK, builds debug/release/preview APKs, inspects their packaged
runtime and legal contents, verifies the development preview signature,
packages corresponding source, and strictly validates OpenSpec.

It does not invoke ADB, `connectedAndroidTest`, the physical compatibility
matrix, or any other device command. If it applied the Woof patch itself, it
reverses that patch on exit so the submodule returns to its pinned clean state.

## Physical presentation verification

On 2026-07-26 the debug build passed all 13 instrumentation tests on the AYN
Thor. A direct 1920×1080 landscape capture also verified warm-ash text against
the iron panels, persistent selected state, high-contrast controller focus,
the selected-profile Play hero, and the built-in `Odin Controller` status.

Run physical instrumentation with:

```sh
ANDROID_SERIAL=<serial> ./scripts/run-device-tests.sh
```

The wrapper wakes the selected device, holds it awake over USB for the test
run, and restores the previous stay-awake setting on exit.

## Release screenshots

The exact signed `v0.1.0-preview.2` build was captured on the AYN Thor during
its physical publication gate:

- `preview2-home.png` shows the selected Freedoom profile and Play hero.
- `preview2-continue-focused.png` shows a real MAP01 Continue card.
- `preview2-unselected-profile-action-focused.png` shows controller focus on
  the unselected Add mod set action.
- `preview2-profile-editor.png` shows the deterministic base-game-only load
  order.
- `preview2-library.png` shows included content without exposing private
  filesystem paths.

The captures and gate logs are retained locally under
`build/release-device-gate/`. Controller evidence must come from physical
hardware, not an emulator screenshot. Bluetooth remains explicitly
unvalidated and the published prerelease does not claim compatible mappings,
rumble, or reconnect behavior.
