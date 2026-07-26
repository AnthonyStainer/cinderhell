## ADDED Requirements

### Requirement: Game and mod profiles
Cinderhell SHALL let the user create and edit a named profile containing exactly one installed game, zero or more installed mod or patch items, and a deterministic load order.

#### Scenario: Create a modded profile
- **WHEN** the user selects a game, adds multiple mods or patches, orders them, and saves the profile
- **THEN** Cinderhell persists the profile with that exact game and load order

### Requirement: Persistent profile state
Cinderhell SHALL retain profiles, the last selected profile, per-profile settings, and recent-session metadata across launcher and device restarts.

#### Scenario: Relaunch after choosing a profile
- **WHEN** the user closes and later reopens Cinderhell
- **THEN** the prior profiles and last selected profile are restored

### Requirement: Curated presentation presets
Cinderhell SHALL expose Original, Enhanced, and Handheld presets as versioned collections of Woof settings.

#### Scenario: Choose Original
- **WHEN** the user applies the Original preset
- **THEN** Cinderhell configures classic aspect and presentation, pixel-oriented scaling, no vertical mouselook, and conservative compatibility defaults

#### Scenario: Choose Enhanced
- **WHEN** the user applies the Enhanced preset
- **THEN** Cinderhell configures widescreen, high-resolution rendering, uncapped interpolated frame rate, improved audio, and modern controller defaults

#### Scenario: Choose Handheld
- **WHEN** the user applies the Handheld preset
- **THEN** Cinderhell configures enhanced presentation plus a battery-conscious internal resolution, larger interface presentation, always-run behavior, and handheld stick defaults

### Requirement: One-action play
Cinderhell SHALL allow the selected valid profile to start from the main launcher with a single Play action and SHALL avoid requiring source-port or command-line knowledge.

#### Scenario: Play selected profile
- **WHEN** the user activates Play for a valid selected profile
- **THEN** Cinderhell launches that profile without showing an engine chooser or command-line editor

### Requirement: Continue recent play
Cinderhell SHALL show Continue only when the most recent profile has resumable state and SHALL identify the game, profile, and latest known level.

#### Scenario: Continue a saved session
- **WHEN** the user activates Continue for a profile with resumable state
- **THEN** Cinderhell launches the same profile and requests that Woof resume that state

#### Scenario: No resumable state
- **WHEN** no successful session has produced resumable state
- **THEN** the launcher omits or disables Continue without implying that a save exists

### Requirement: Preflight launch validation
Cinderhell SHALL validate that a profile's game, ordered files, preset/configuration, and runtime assets are present and supported before starting the game process.

#### Scenario: Profile references missing content
- **WHEN** the user activates Play for a profile that references missing or unsupported content
- **THEN** Cinderhell remains in the launcher and explains which profile entry must be repaired

### Requirement: Advanced settings remain secondary
Cinderhell SHALL keep supported low-level Woof settings accessible through an Advanced route while keeping them out of the primary Play and Continue flow.

#### Scenario: Use the normal home screen
- **WHEN** the user navigates the normal home screen
- **THEN** source-port terminology and raw engine variables are not required to select or launch a profile
