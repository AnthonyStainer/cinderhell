#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

version="${CINDERHELL_RELEASE_VERSION:-0.1.0-preview.local}"
[[ "${version}" =~ ^[0-9A-Za-z][0-9A-Za-z.-]*$ ]] || {
    echo "Invalid Cinderhell artifact version: ${version}" >&2
    exit 1
}

woof_dir="${repo_root}/third_party/woof"
patch_file="${repo_root}/native/patches/woof-android.patch"
restore_woof_patch=false
if ! git -C "${woof_dir}" apply --reverse --check "${patch_file}" >/dev/null 2>&1; then
    restore_woof_patch=true
fi
cleanup() {
    if [[ "${restore_woof_patch}" == true ]] &&
        git -C "${woof_dir}" apply --reverse --check "${patch_file}" >/dev/null 2>&1; then
        git -C "${woof_dir}" apply --reverse "${patch_file}"
    fi
}
trap cleanup EXIT

./scripts/apply-native-patches.sh
./scripts/verify-bootstrap.sh
python3 -m unittest discover -s scripts/tests
./gradlew testDebugUnitTest assemblePreview
./scripts/inspect-apk.sh app/build/outputs/apk/preview/app-preview.apk

mkdir -p build/release
apk="build/release/cinderhell-${version}-arm64.apk"
cp app/build/outputs/apk/preview/app-preview.apk \
    "${apk}"

build_tools="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/35.0.0"
if [[ -x "${build_tools}/apksigner" ]]; then
    "${build_tools}/apksigner" verify --verbose \
        "${apk}"
fi

export CINDERHELL_RELEASE_VERSION="${version}"
source_archive="$(./scripts/package-corresponding-source.sh)"
(
    cd build/release
    sha256sum \
        "$(basename "${apk}")" \
        "$(basename "${source_archive}")" \
        > SHA256SUMS
)
echo "Preview artifacts are in build/release."
