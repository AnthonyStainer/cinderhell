# Launcher Presentation Specification

## Purpose

Define Cinderhell's distinctive, responsive, controller-readable launcher presentation and its automated no-device verification boundary.

## Requirements

### Requirement: Distinctive launcher hierarchy
Cinderhell SHALL present a cohesive launcher identity and SHALL visually prioritize the selected play profile and its Play or Continue action over content-management utilities.

#### Scenario: Open the normal home screen
- **WHEN** a valid profile is selected
- **THEN** the launcher identifies the selected game, profile, preset, and mod count in a prominent play surface with Play as the primary action

#### Scenario: Management actions are available
- **WHEN** the user needs to import content, inspect the library, view notices, or open advanced settings
- **THEN** those actions remain reachable without competing visually with Play and Continue

### Requirement: Responsive handheld composition
Cinderhell SHALL keep launcher content readable and operable in compact and wide landscape windows without clipping required actions.

#### Scenario: Render in a compact window
- **WHEN** the available logical width cannot support the wide composition
- **THEN** required content uses a single scrollable flow and retains reachable primary and back actions

#### Scenario: Render in a wide window
- **WHEN** the available logical width can support multiple content regions
- **THEN** the launcher uses the additional space to separate play context from selection and management

### Requirement: Explicit presentation states
Cinderhell SHALL render focus, selection, enabled state, busy state, and notice severity as independent presentation states rather than relying on copy alone.

#### Scenario: Focus a selected card
- **WHEN** controller focus moves onto an already selected game or profile
- **THEN** both the persistent selected state and the transient focus state remain recognizable

#### Scenario: Operation is running
- **WHEN** an import, save, profile change, or launch preparation is in progress
- **THEN** the launcher shows an in-progress presentation and prevents conflicting actions

#### Scenario: Operation completes or fails
- **WHEN** a launcher operation reports success, information, warning, or failure
- **THEN** the message is displayed with a tone-appropriate label and visual treatment

### Requirement: Reusable code-native presentation
Cinderhell SHALL implement its launcher presentation with reusable Compose tokens and components and SHALL not require network-fetched artwork or newly bundled proprietary visual assets.

#### Scenario: Build while offline after dependencies are present
- **WHEN** the launcher resources are compiled
- **THEN** its background, surfaces, icons, selection marks, and focus treatment are produced from checked-in code or existing licensed resources

### Requirement: No-device presentation verification
Cinderhell SHALL provide automated presentation-state coverage and SHALL allow all launcher, Android-test, APK, and source-package build gates for this change to run without a physical Android device.

#### Scenario: Validate while the target device is unavailable
- **WHEN** maintainers run the documented no-device verification set
- **THEN** presentation-model tests, Android-test compilation, APK inspection, signature verification, source packaging, and specification validation complete without ADB
