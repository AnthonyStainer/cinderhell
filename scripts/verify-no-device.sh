#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
woof_dir="${repo_root}/third_party/woof"
patch_file="${repo_root}/native/patches/woof-android.patch"
patch_was_present=false

cd "${repo_root}"

if git -C "${woof_dir}" apply --reverse --check "${patch_file}" >/dev/null 2>&1; then
    patch_was_present=true
fi

cleanup() {
    if [[ "${patch_was_present}" == false ]] &&
        git -C "${woof_dir}" apply --reverse --check "${patch_file}" >/dev/null 2>&1; then
        git -C "${woof_dir}" apply --reverse "${patch_file}"
    fi
}
trap cleanup EXIT

./scripts/fetch-native-dependencies.sh
./scripts/apply-native-patches.sh
./scripts/verify-bootstrap.sh
python3 -m unittest discover -s scripts/tests
./scripts/build-test-corpus.py
(cd test-corpus/generated && sha256sum --check SHA256SUMS)

./gradlew \
    testDebugUnitTest \
    assembleDebug \
    assembleRelease \
    assemblePreview \
    assembleDebugAndroidTest \
    lintDebug \
    --stacktrace

./scripts/inspect-apk.sh app/build/outputs/apk/debug/app-debug.apk
./scripts/inspect-apk.sh app/build/outputs/apk/release/app-release-unsigned.apk
./scripts/inspect-apk.sh app/build/outputs/apk/preview/app-preview.apk

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "${sdk_root}" ]] && [[ -f local.properties ]]; then
    sdk_root="$(
        awk -F= '$1 == "sdk.dir" { print substr($0, index($0, "=") + 1); exit }' \
            local.properties
    )"
fi
apksigner="${sdk_root}/build-tools/36.0.0/apksigner"
if [[ ! -x "${apksigner}" ]]; then
    echo "Android build-tools 36.0.0 are required for signature verification." >&2
    exit 1
fi
"${apksigner}" verify --verbose app/build/outputs/apk/preview/app-preview.apk

./scripts/package-corresponding-source.sh
openspec validate --all --strict

echo "No-device launcher, APK, source, and specification gates passed."
