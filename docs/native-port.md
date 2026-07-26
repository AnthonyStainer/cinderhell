# Woof Android port

Cinderhell pins Woof commit
`5f7a0def133056cb527312f2376b3088adb863fc`, the upstream SDL3 development
line current when the Android spike was completed. The latest tagged Woof
release at the time was still SDL2-based, so the unreleased revision is
intentional and recorded in `third_party/dependencies.lock.toml`.

The upstream source remains a clean submodule plus
`native/patches/woof-android.patch`. Run `scripts/apply-native-patches.sh`
after initializing submodules. The patch carries only these build deltas:

1. Reuse Cinderhell-provided `SDL3::SDL3` and `OpenAL::OpenAL` targets instead
   of searching for desktop installations.
2. Build the normal Woof sources as a shared library on Android and name its
   output `libmain.so`, as required by `SDLActivity`.
3. Omit the desktop setup executable and install rules on Android.
4. Route Woof's orderly `I_SafeExit` sequence back through `SDL_main` with an
   Android-only `setjmp`/`longjmp` boundary. This preserves all registered
   engine cleanup callbacks while allowing the Kotlin session layer to record
   the native return code before terminating `:game`.
5. Prefer an exact existing `-iwad` path before applying Woof's optional
   `.wad` suffix. The validated immutable content store deliberately names
   files by bare SHA-256 digest, and identify suffixless WAD data by its
   `IWAD`/`PWAD` signature when the resource loader opens it.

`native/CMakeLists.txt` also disables libsndfile, FluidSynth, libxmp,
Discord RPC, and font generation. The first slice uses Woof's built-in OPL
music path and bundled fallback libraries. OpenAL Soft is compiled statically
with OpenSL enabled and Oboe, utilities, examples, tests, and install data
disabled.

Woof's `woof.pk3` is built from its authoritative `BASE_SOURCES` list by
`scripts/build-woof-pk3.py`. The script normalizes ZIP ordering, timestamps,
and permissions and uses stored entries so the output has a stable SHA-256
digest across zlib and zlib-ng hosts.
Both that PK3 and Freedoom are copied from APK assets to application-private
ordinary files, then verified before `SDL_main` receives their paths.

The original SDL/OpenAL smoke renderer remains available for diagnostics with
`-DCINDERHELL_NATIVE_SMOKE=ON`; normal application builds compile Woof.
