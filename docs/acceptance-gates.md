# Acceptance gates

Status as of 2026-07-26:

1. **Native smoke — pass.** Pinned Freedoom renders with audio and built-in
   controls, writes and reloads saves, and exits normally.
2. **Lifecycle — pass.** Repeated sessions, background/foreground, display
   cycles, surface recreation, audio focus, process death, Android Back, and
   120 Hz pacing passed on the AYN Thor. See `lifecycle-gate.md`.
3. **Product — pass.** Import/catalogue tests, profiles, versioned presets,
   controller-focus tests, one-action Play, a controller-created save, clean
   quit, and Continue passed. See `compatibility-matrix.md`.
4. **Release — pass for the MVP scope.** Reproducible arm64 builds, signature
   inspection, clean install, schema/preset upgrade preservation, notices,
   corresponding source, and the target AYN built-in-controller matrix pass.

An owner-controlled preview signing identity is provisioned. The published
`v0.1.0-preview.2` assets pass package, version, signature, checksum, and
corresponding-source inspection.

The polished launcher passed all 13 instrumentation tests on the AYN Thor on
2026-07-26 after a direct wide-landscape contrast and focus inspection. The
same change also passes `scripts/verify-no-device.sh`; that complementary gate
explicitly does not invoke ADB.

The exact signed GitHub `v0.1.0-preview.2` APK completed its physical
publication gate on the same AYN Thor on 2026-07-26. The installed package was
`io.github.anthonystainer.cinderhell.preview` version `0.1.0-preview.2`
(`1000002`) with SHA-256
`769b9ec82e57a840cfbd969d2cb872b54064493fde6da070ac786a453533d2aa`.
The gate verified:

- the expected dedicated preview signer and non-debuggable `arm64-v8a` package;
- the polished launcher, Odin Controller discovery, and native Freedoom
  rendering with Android game audio focus;
- genuine MAP01 entry, slot-0 save/load, launcher save discovery, and Continue
  into a fresh isolated game process;
- Home/Recents pause and resume with the original game PID, restored SDL
  surface, and restored audio focus;
- active 120.00001 Hz display mode and three clean `SDL_main` status-0 exits;
- no fatal Java/native, startup, or nonzero SDL exit diagnostics.

The verified draft was then published as a GitHub prerelease. The exact-build
screens and logs are retained locally under `build/release-device-gate/`.

Validated Bluetooth-gamepad support is intentionally deferred and is not an
MVP acceptance dependency. External pads discovered through SDL may work on a
best-effort basis, but Cinderhell does not currently promise compatible
mappings, rumble, or physical disconnect/reconnect behavior.
