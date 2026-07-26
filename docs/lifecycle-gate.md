# Physical lifecycle gate

Verified on 2026-07-25 using an AYN Thor running Android 13. The debug build
used the pinned native/runtime revisions in `third_party/dependencies.lock.toml`.

## Results

- Background/foreground: pressing Home caused SDL `onPause`, `nativePause`,
  `surfaceDestroyed`, and audio-focus abandonment. Returning through Recents
  caused `surfaceCreated`, `nativeResume`, and a new focus grant. The same
  `:game` PID remained active and no second engine thread was created.
- Screen off/on: the display cycle produced the same pause/destroy and
  create/resume sequence. Rendering resumed in the existing game process.
- Audio interruption: the debug-only transient-focus probe produced
  `AUDIOFOCUS_LOSS_TRANSIENT`, `nativePause`, `AUDIOFOCUS_GAIN`, and
  `nativeResume` while `GameActivity` remained foreground.
- Process death: killing only the `:game` PID removed the activity, returned to
  the still-running launcher, converted the orphaned active descriptor into an
  interrupted result, persisted `diagnostics/latest.json`, and displayed:
  “The previous game session stopped unexpectedly. Your saves are still
  available.”
- Android Back: a hardware Back key was translated before SDL surface
  dispatch and opened the Woof menu. Selecting and confirming Quit returned
  from `SDL_main` with status 0, removed the `:game` process, restored launcher
  system bars, and displayed “Game closed safely.”
- High refresh: the activity requested the device's supported 120.00001 Hz
  mode and `dumpsys display` reported active mode 2 at 120.00001 Hz.
  SurfaceFlinger recorded 126 consecutive BLAST presentation samples with a
  mean interval of 8.323 ms (7.563–9.097 ms) against an 8.333 ms refresh
  period.

The release manifest contains no lifecycle-test receiver. The transient audio
focus probe is declared only by `app/src/debug/AndroidManifest.xml`.
