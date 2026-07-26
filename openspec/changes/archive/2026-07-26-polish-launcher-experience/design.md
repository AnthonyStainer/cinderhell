## Context

The launcher already has complete routes and stable controller focus IDs, but
presentation is concentrated in one large screen file and every action uses
the same button treatment. Selection is encoded with a text checkmark, status
messages have no severity, and the primary Play path competes visually with
library management. The target is a landscape handheld viewed farther away
than a phone, with touch available for Android system UI but not required by
the product flow.

The physical Thor is reserved by another process and is explicitly outside
this change. Existing device evidence remains authoritative; this change may
compile instrumentation tests but must not run ADB or device tasks.

## Goals / Non-Goals

**Goals:**

- Make the selected game/profile and next action readable at a glance.
- Establish reusable visual tokens and components rather than accumulating
  route-specific colours and dimensions.
- Make focus, selection, enabled state, notice severity, and busy state
  independently recognizable.
- Preserve every stable test tag and deterministic focus destination.
- Keep compact layouts scrollable while using space more deliberately on wide
  landscape windows.
- Validate pure presentation decisions, JVM behaviour, Android-test
  compilation, APK content, and source packaging without a physical device.

**Non-Goals:**

- Bitmap artwork, downloaded covers, custom font binaries, or a new asset
  licensing surface.
- Changes to storage, profiles, native arguments, engine settings, saves, or
  the game process.
- Bluetooth support or revalidation of existing physical-device evidence.
- Publishing a release before the deferred signed-APK physical smoke gate.

## Decisions

### Use a code-native “ember and iron” presentation system

Theme tokens will define ember accents, soot backgrounds, iron borders,
muted ash text, shapes, and typography. A gradient backdrop and restrained
decorative geometry will be rendered with Compose, so the visual identity is
resolution independent and introduces no generated binary or third-party
asset.

An illustrated bitmap background was considered, but rejected for this
milestone because it adds density, cropping, attribution, and readability
decisions before the core information hierarchy is settled.

### Separate action, selection, and focus state

`ControllerButton` will accept a semantic visual role and a selected flag.
Focus will remain driven by the existing `FocusRequester` loop and test tag,
but use a high-contrast outline/elevation treatment. Selection will remain
visible when the card is not focused, and disabled actions will not resemble
either state.

Replacing the controller component or focus graph was rejected because the
current implementation has physical validation. This is a presentation
upgrade around that proven mechanism.

### Derive home presentation from immutable launcher state

A small pure presentation model will derive the selected game, selected
profile, preset label, mod count, Continue copy, and initial focus candidate
from `LauncherSnapshot`. This keeps composables declarative and creates
ordinary JVM-testable behaviour without introducing a UI test framework.

### Model notices explicitly

The activity will pass a `LauncherNotice` containing message and tone instead
of an unclassified string. Success, information, warning, and error use
consistent colour and icon/text labels; import progress is paired with the
existing busy state. Messages remain local and ephemeral.

Inferring tone from message text was rejected because wording changes would
silently change behaviour.

### Compose routes from reusable panels

Home will use a selected-profile hero, primary actions, game cards, profile
cards, and a quieter utility rail. Other routes will reuse a route heading,
panels, metadata rows, empty states, and footer actions. A width-aware layout
may form two columns when enough logical width exists, while the compact path
remains a single scrollable column.

No route or persistence model changes are required.

### Treat no-device validation as a first-class gate

The change will run unit tests, build debug/release/preview APKs and the
Android-test APK, inspect packaged runtime/legal contents, verify the
development-signed preview, package corresponding source, and validate
OpenSpec. Device instrumentation and ADB commands are excluded.

## Risks / Trade-offs

- **[Dense handheld layouts could clip at an untested logical width]** →
  preserve vertical scrolling, constrain text lines, and keep the compact
  composition as the default fallback.
- **[Animations could make focus feel delayed]** → animate only colour,
  border, and a very small scale/elevation delta; focus ownership changes
  synchronously.
- **[Refactoring a large composable could disturb stable automation]** →
  preserve route names, user-facing core action copy, focus destinations, and
  existing test tags; add presentation-model unit tests and compile the
  instrumentation suite.
- **[A polished UI may imply broader hardware support]** → continue to state
  the AYN built-in controller support boundary and keep Bluetooth explicitly
  deferred.
- **[Release documentation can get ahead of physical evidence]** → correct
  only signing ownership and automated gates now; leave the release draft
  unpublished and the physical smoke gate visibly pending.

## Migration Plan

The launcher presentation is replaced in place without a data or package
migration. Existing profiles, imported blobs, configs, saves, application IDs,
and preview signing remain unchanged. Rollback is a normal source revert.

The unpublished `preview.1` remains an internal candidate. A later
`preview.2` tag may be created from the polished `main` only after the Thor is
available and the maintainer chooses to resume the physical gate.

## Open Questions

None block implementation. Real-device spacing, brightness, and focus-motion
feedback are intentionally deferred until the Thor becomes available.
