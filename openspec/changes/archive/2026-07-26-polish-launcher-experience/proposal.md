## Why

Cinderhell's launcher is functionally complete, but its flat collection of
Material controls still reads like a development utility rather than the
focused handheld game promised by the product. The first public preview needs
a distinctive, legible presentation that makes Play, Continue, selection, and
recovery states immediately understandable from controller distance.

## What Changes

- Establish a reusable Cinderhell presentation system for colour, typography,
  spacing, surfaces, focus, selection, and status feedback.
- Recompose the launcher home screen around a prominent selected-session hero,
  clear Play and Continue actions, and scannable game/profile cards.
- Give library, profile, advanced, notices, loading, empty, busy, and error
  states consistent hierarchy and controller-readable guidance.
- Preserve deterministic controller focus and stable automation identifiers
  while making focused, selected, pressed, and disabled states visually
  distinct.
- Add no-device automated coverage for presentation state and compile all
  launcher and Android-test surfaces without interacting with the physical
  Thor.
- Correct release-readiness documentation now that an owner-controlled preview
  signing identity exists, while leaving publication behind the deferred
  physical smoke gate.

## Capabilities

### New Capabilities

- `launcher-presentation`: Defines the visual hierarchy, responsive landscape
  composition, reusable presentation states, and user-facing feedback of the
  Android launcher.

### Modified Capabilities

- `controller-first-experience`: Strengthens the visible focus contract so
  focus, selection, enabled state, and the currently available controller
  actions cannot be confused.

## Impact

The change affects Compose theme and launcher UI code, controller-aware
components, launcher presentation models and tests, Android-test compilation,
README/release screenshots guidance, and acceptance documentation. It adds no
runtime service, storage permission, engine capability, telemetry, network
dependency, Bluetooth promise, or physical-device interaction.
