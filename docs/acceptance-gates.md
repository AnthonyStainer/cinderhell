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

An owner-controlled preview signing identity is provisioned and the GitHub
draft assets pass automated package, version, signature, checksum, and
corresponding-source inspection. The draft remains unpublished until the exact
signed GitHub APK completes the maintainer's physical smoke gate.

The polished launcher passed all 13 instrumentation tests on the AYN Thor on
2026-07-26 after a direct wide-landscape contrast and focus inspection. The
same change also passes `scripts/verify-no-device.sh`; that complementary gate
explicitly does not invoke ADB. The exact signed GitHub APK must still complete
its final physical smoke before the draft release is published.

Validated Bluetooth-gamepad support is intentionally deferred and is not an
MVP acceptance dependency. External pads discovered through SDL may work on a
best-effort basis, but Cinderhell does not currently promise compatible
mappings, rumble, or physical disconnect/reconnect behavior.
