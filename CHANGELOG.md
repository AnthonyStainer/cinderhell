# Changelog

All notable Cinderhell changes are recorded here. The project uses
[Semantic Versioning](https://semver.org/) with numbered preview builds.

## [Unreleased]

### Planned

- Bluetooth gamepad compatibility, rumble, and reconnect validation.
- Explicit user-controlled backup/export and restore.

## [0.1.0-preview.2] - Unpublished

### Changed

- Reworked the launcher around a selected-profile Play hero, responsive
  game/profile cards, explicit controller states, and consistent route and
  operational feedback.
- Added a complete no-device verification gate for periods when the target
  handheld is unavailable.
- Aligned Compose compile and runtime libraries and hardened physical
  instrumentation against a sleeping target device.

## [0.1.0-preview.1] - Unpublished

### Added

- Controller-first Kotlin/Compose launcher with Play and Continue.
- Isolated SDL3/Woof game sessions for `arm64-v8a`.
- Bundled Freedoom and Android system-document content imports.
- Ordered WAD/PK3/ZIP/DEH/BEX play profiles and curated presets.
- AYN Thor controller and Android lifecycle validation.
- Reproducible, signed GitHub draft-prerelease pipeline with corresponding
  source and checksums.

[Unreleased]: https://github.com/AnthonyStainer/cinderhell/compare/v0.1.0-preview.2...HEAD
[0.1.0-preview.2]: https://github.com/AnthonyStainer/cinderhell/compare/v0.1.0-preview.1...v0.1.0-preview.2
[0.1.0-preview.1]: https://github.com/AnthonyStainer/cinderhell/releases/tag/v0.1.0-preview.1
