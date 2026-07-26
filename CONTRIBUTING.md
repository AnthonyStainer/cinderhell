# Contributing to Cinderhell

Thank you for helping make Cinderhell a focused, controller-first Doom
experience.

## Before starting

Open an issue before undertaking a large change. Cinderhell deliberately
targets one engine, `arm64-v8a`, Android-native content importing, and a
controller-first launcher. Multiple engines, multiplayer, mod downloading,
GZDoom/ZScript compatibility, and editable touch controls are currently
outside the product scope.

Do not attach or commit commercial Doom data, signing files, passwords, device
identifiers, or logs containing private filesystem paths.

## Development

Clone with submodules and fetch the pinned build inputs:

```sh
git clone --recurse-submodules https://github.com/AnthonyStainer/cinderhell.git
cd cinderhell
./scripts/fetch-jdk.sh
./scripts/fetch-native-dependencies.sh
```

Before submitting a change, run:

```sh
./scripts/verify-bootstrap.sh
python3 -m unittest discover -s scripts/tests
JAVA_HOME="$PWD/.toolchains/jdk-17.0.19+10" ./gradlew testDebugUnitTest
openspec validate --all --strict
```

Native and lifecycle changes should also follow the relevant gates in
`docs/acceptance-gates.md`. Physical validation evidence must identify the
device and Android version without including copyrighted content.

## Changes and licensing

Keep commits focused and explain behavior changes in tests and documentation.
Significant product behavior is specified under `openspec/specs/`; propose or
update an OpenSpec change before implementing it.

By contributing, you agree that your contribution is licensed under
GPL-2.0-or-later. Third-party files retain their existing licenses and notices.
