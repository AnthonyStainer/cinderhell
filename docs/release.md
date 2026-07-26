# Preview release

Cinderhell's preview artifact is an `arm64-v8a`-only APK. Run:

```sh
JAVA_HOME="$PWD/.toolchains/jdk-17.0.19+10" \
ANDROID_HOME=/path/to/Android/Sdk \
./scripts/build-preview.sh
```

This verifies dependency pins, runs JVM tests, builds the minified preview,
checks its ABI/runtime/legal contents, verifies its signature when `apksigner`
is available, and creates a deterministic corresponding-source archive.
Outputs and SHA-256 files are written under `build/release/`.

The preview build uses Android's development signing identity and the
`dev.cinderhell.preview` package ID. It is appropriate for internal testing
and GitHub preview downloads, not a public store release. Public distribution
must replace it with an owner-controlled signing configuration without
checking private key material into this repository.

The unmodified `release` build is unsigned so CI can reproducibly compile and
inspect it without release credentials. The source archive includes
Cinderhell's application/native build files, the Android patch, the exact
Woof/OpenAL Soft/Freedoom sources, the exact SDL source archive, build scripts,
notices, and dependency lock.

Device tests use:

```sh
ANDROID_SERIAL=<serial> ./gradlew connectedDebugAndroidTest
```

Because the APK intentionally contains no x86 ABI, instrumentation runs on a
real arm64 Android device (or an arm64 virtual device), not the usual x86 CI
emulator. Connected-test reports and logcat diagnostics are retained as CI
artifacts on device-capable runners.
