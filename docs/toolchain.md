# Cinderhell toolchain

The MVP build is pinned to the following baseline:

| Component | Version |
| --- | --- |
| Application ID | `dev.cinderhell` (`.debug` suffix for debug builds) |
| Compile / target SDK | 35 |
| Minimum SDK | 26 |
| Java bytecode target | 17 |
| Verified Gradle JVM | Eclipse Temurin 17.0.19+10 |
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin / Compose compiler plugin | 2.1.21 |
| Compose BOM | 2024.12.01 |
| Android NDK | 27.0.12077973 |
| CMake | 3.31.6 |
| Packaged ABI | `arm64-v8a` only |

The checked-in Gradle wrapper is authoritative. The Gradle JVM archive used for
local verification is pinned by `scripts/fetch-jdk.sh` with SHA-256
`d8afc263758141a66e0e3aafc321e783f7016696f4eaea067d340a269037d331`.
The host's OpenJDK 25 is not compatible with this AGP/Kotlin baseline.
`local.properties` is developer-local; CI sets `ANDROID_HOME` explicitly.

The package ID is suitable for development and internal previews. It must be
revisited together with signing ownership before the first public store release.
