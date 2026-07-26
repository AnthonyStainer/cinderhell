# Controller and physical-device matrix

Results were recorded on 2026-07-25 using the pinned debug build.

## Target device

- Device: AYN Thor, Android 13, security patch 2024-01-01.
- Firmware:
  `qti/kalama/kalama:13/TKQ1.231222.001/eng.Thor.20260206.163241:user/release-keys`.
- Built-in controller name: `Odin Controller`.
- Descriptor: `8e1073ea5832500672194344d81498833991c43c`.
- Identifier: USB bus `0x0003`, vendor `0x2020`, product `0x0111`.
- Android sources: keyboard, gamepad, and joystick.
- Axes: X/Y, Z/RZ, gas/brake, and hat X/Y.
- Displays: both panels expose 60.000004 Hz and 120.00001 Hz modes.

SDL3's stock mapping correctly exposes both sticks, D-pad, face/shoulder
buttons, triggers, and Start, so Cinderhell carries no device-specific mapping
override. Android does not expose a vibrator capability for this controller;
the runtime correctly reports `rumble=false` and leaves rumble disabled.

## Results

| Area | Built-in AYN controller |
| --- | --- |
| Launcher focus and visible restoration | Pass |
| D-pad/left-stick navigation, confirm, back | Pass |
| Movement, aim, fire, use, weapons, pause, menus | Pass |
| Controller-only new game, slot-0 save, confirmed quit, Continue | Pass |
| Controller monitor and reconnect state logic | Pass in instrumentation |
| Disconnect recovery path | Pass in instrumentation; Android Back remains available |
| Audio and audio-focus lifecycle | Pass |
| Suspend/resume and screen off/on | Pass |
| Landscape safe areas on both Thor panels | Pass |
| 60/120 Hz selection and 120 Hz frame pacing | Pass |
| Rumble | Correctly unavailable/capability-gated |

The product flow started from a cold launcher process, activated Play, entered
a real MAP01 game through the Doom menus, saved using D-pad/confirm without
text or touch input, quit through the confirmed in-game menu, displayed the
contextual Continue card, and loaded the save in a new game process.

## Future feature: validated Bluetooth gamepads

No representative Bluetooth gamepad was available during the MVP validation;
the Bluetooth service reported no connected controller. Validated external-pad
controls, rumble, and physical disconnect/reconnect are intentionally deferred
and do not block the AYN-focused MVP.

SDL may discover external controllers through its generic mappings, and the
existing capability-gated rumble and hot-plug plumbing remains available on a
best-effort basis. Cinderhell makes no Bluetooth compatibility promise until a
follow-on change selects representative controllers and Android firmware,
runs the full physical matrix, and records the supported combinations here.
