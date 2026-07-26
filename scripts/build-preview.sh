#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

./scripts/verify-bootstrap.sh
./gradlew testDebugUnitTest assemblePreview
./scripts/inspect-apk.sh app/build/outputs/apk/preview/app-preview.apk
./scripts/package-corresponding-source.sh

mkdir -p build/release
cp app/build/outputs/apk/preview/app-preview.apk \
    build/release/cinderhell-0.1.0-preview-arm64.apk

build_tools="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/35.0.0"
if [[ -x "${build_tools}/apksigner" ]]; then
    "${build_tools}/apksigner" verify --verbose \
        build/release/cinderhell-0.1.0-preview-arm64.apk
fi

sha256sum build/release/cinderhell-0.1.0-preview-arm64.apk \
    > build/release/cinderhell-0.1.0-preview-arm64.apk.sha256
echo "Preview artifacts are in build/release."
