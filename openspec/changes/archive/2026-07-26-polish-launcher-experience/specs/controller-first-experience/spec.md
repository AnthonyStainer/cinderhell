## MODIFIED Requirements

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
