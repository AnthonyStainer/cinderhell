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

The signed development-key preview is an internal test artifact only. Do not
externally distribute it until an owner-controlled signing identity is
selected.

Validated Bluetooth-gamepad support is intentionally deferred and is not an
MVP acceptance dependency. External pads discovered through SDL may work on a
best-effort basis, but Cinderhell does not currently promise compatible
mappings, rumble, or physical disconnect/reconnect behavior.
