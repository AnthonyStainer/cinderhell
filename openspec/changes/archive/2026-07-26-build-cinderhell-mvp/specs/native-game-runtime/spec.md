## ADDED Requirements

### Requirement: Arm64 Woof runtime
Cinderhell SHALL package a pinned SDL3-based Woof build and its required native dependencies as an Android `arm64-v8a` game runtime.

#### Scenario: Install on a supported arm64 device
- **WHEN** a release APK is installed on a compatible `arm64-v8a` Android device
- **THEN** it can start the packaged Woof runtime without downloading an engine

### Requirement: Supported engine scope
The runtime SHALL support Doom, Doom II, TNT, Plutonia, Freedoom, and Woof-compatible vanilla, Boom, MBF, and MBF21 content, and SHALL make no compatibility promise for GZDoom/ZScript content.

#### Scenario: Launch compatible MBF21 content
- **WHEN** a valid profile combines a supported IWAD with compatible MBF21 content
- **THEN** the runtime passes the ordered content to Woof and starts the game

#### Scenario: Launch known incompatible content
- **WHEN** preflight identifies content that requires an unsupported engine feature
- **THEN** Cinderhell blocks launch and describes the unsupported requirement

### Requirement: Isolated game sessions
Cinderhell SHALL run the native game activity in a private `:game` process and SHALL ensure that every new session begins after the prior game process has terminated.

#### Scenario: Quit and start another profile
- **WHEN** the user exits one game session and launches another
- **THEN** the second session starts in a fresh native process without inherited Woof global state

### Requirement: Validated session envelope
The launcher SHALL create an immutable, validated session descriptor for each launch, and the game activity SHALL derive Woof arguments only from that descriptor and known application paths.

#### Scenario: Start a valid session
- **WHEN** the game activity receives a valid pending session identifier from Cinderhell
- **THEN** it loads the corresponding descriptor and constructs the ordered Woof invocation

#### Scenario: Receive an invalid session request
- **WHEN** the game activity receives a missing, stale, malformed, or externally forged session request
- **THEN** it refuses to start Woof and returns a structured failure to the launcher

### Requirement: Shared private runtime data
Cinderhell SHALL keep configs, saves, screenshots, and session results in stable app-private paths that are accessible to both launcher and game processes and isolated by profile where needed.

#### Scenario: Save and relaunch a profile
- **WHEN** Woof writes a save and the user later launches the same profile
- **THEN** the runtime uses the same profile save location and the save remains available

### Requirement: Android lifecycle behavior
The runtime SHALL pause gameplay and audio when it loses foreground ownership, restore them on resume when possible, and safely handle surface recreation or Android-requested destruction.

#### Scenario: Suspend and resume
- **WHEN** the user switches away from an active game and later returns
- **THEN** gameplay does not advance in the background, audio does not continue playing, and the session resumes without starting a second engine instance

#### Scenario: Surface is recreated
- **WHEN** Android recreates the game surface during a session
- **THEN** the runtime restores rendering or exits with a recoverable launcher error rather than corrupting persistent data

### Requirement: Fullscreen and frame pacing
The game activity SHALL use immersive landscape presentation, respect display safe areas, and select a supported display mode suitable for the profile's frame-rate target.

#### Scenario: Start on a high-refresh display
- **WHEN** a profile requests a target frame rate supported by the device
- **THEN** Cinderhell requests an appropriate display refresh rate and presents frames without an unintended launcher overlay

### Requirement: Clean session completion
The runtime SHALL return from SDL/Woof normally, atomically record the session result, finish the game activity, terminate the dedicated process, and reveal the launcher.

#### Scenario: Quit from the Doom menu
- **WHEN** the user confirms Quit in Woof
- **THEN** the game session records a clean exit and the launcher resumes with refreshed recent-session state

#### Scenario: Native startup failure
- **WHEN** a native library, runtime asset, or Woof initialization step fails
- **THEN** the game process terminates and the launcher presents a recoverable error with diagnostic context
