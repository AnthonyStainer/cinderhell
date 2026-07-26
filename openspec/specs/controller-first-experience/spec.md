# Controller-First Experience Specification

## Purpose

Define the controller-operated launcher and gameplay experience validated for Cinderhell's target AYN handheld.

## Requirements

### Requirement: Controller-complete launcher
Every primary launcher action SHALL be reachable with a directional pad or left stick, confirm button, and back button, with a visible and deterministic focus state that is distinguishable from selection and disabled state.

#### Scenario: Launch without touching the screen
- **WHEN** the user starts Cinderhell with a recognized gamepad
- **THEN** the user can select a game or profile and activate Play using only the gamepad

#### Scenario: Navigate a reordered library
- **WHEN** library content changes while focus is active
- **THEN** focus moves to a visible valid target instead of becoming lost

#### Scenario: Focus a selected item
- **WHEN** controller focus moves between selected and unselected launcher items
- **THEN** focus remains high contrast while the selected item remains identifiable independently

#### Scenario: Encounter a disabled action
- **WHEN** an action is temporarily unavailable because the launcher is busy or its prerequisites are absent
- **THEN** the action is visibly disabled and cannot be mistaken for the focused or selected state

### Requirement: SDL gamepad input
The native runtime SHALL use SDL3's gamepad API and SHALL provide a tested baseline mapping for the target AYN handheld's built-in controller.

#### Scenario: Start with a recognized built-in controller
- **WHEN** Woof starts on the target handheld
- **THEN** movement, aiming, firing, use, weapon selection, pause, and menu navigation work with the curated default mapping

### Requirement: Handheld analogue defaults
The Handheld preset SHALL provide usable stick deadzones, response curves, sensitivity, and outer-zone behavior without requiring initial calibration.

#### Scenario: First play with Handheld preset
- **WHEN** a user launches a game with the Handheld preset and has not customized analogue settings
- **THEN** small stick noise is suppressed while full-range turning and movement remain available

### Requirement: Android Back behavior
The game activity SHALL translate Android Back into Doom menu navigation and SHALL not treat a single Back press as an immediate process exit.

#### Scenario: Press Back during gameplay
- **WHEN** the user presses Android Back during active gameplay
- **THEN** Woof opens or navigates its menu rather than immediately quitting to the launcher

### Requirement: Conditional feedback features
Cinderhell SHALL continue normally when the target handheld exposes no usable rumble capability. Bluetooth-controller rumble and controller-associated gyro are deferred.

#### Scenario: Rumble is unavailable
- **WHEN** the current controller exposes no usable rumble capability
- **THEN** gameplay continues without repeated errors or a blocked launch

### Requirement: Bluetooth gamepad support is deferred
The MVP SHALL make no compatibility guarantee for Bluetooth-controller mappings, external-controller rumble, or disconnect/reconnect behavior. External controllers discovered through SDL MAY work on a best-effort basis and SHALL NOT block the validated built-in-controller flow.

#### Scenario: Inspect the MVP support contract
- **WHEN** a user reviews supported controller hardware
- **THEN** Cinderhell identifies the target AYN built-in controller as validated and describes Bluetooth gamepads as a future feature

### Requirement: No virtual-control dependency
The MVP game runtime SHALL not require or expose an editable touchscreen HUD to complete normal gameplay.

#### Scenario: Inspect game presentation
- **WHEN** the user starts a normal game session
- **THEN** no virtual joystick, gameplay-button overlay, or touch-layout editor obscures the Doom view
