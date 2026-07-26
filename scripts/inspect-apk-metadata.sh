#!/usr/bin/env bash
set -euo pipefail

apk="${1:?usage: inspect-apk-metadata.sh APK PACKAGE VERSION_NAME VERSION_CODE}"
expected_package="${2:?expected package is required}"
expected_version_name="${3:?expected version name is required}"
expected_version_code="${4:?expected version code is required}"

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
apkanalyzer="${sdk_root}/cmdline-tools/latest/bin/apkanalyzer"
[[ -x "${apkanalyzer}" ]] || {
    echo "apkanalyzer was not found below ANDROID_HOME/ANDROID_SDK_ROOT" >&2
    exit 1
}

actual_package="$("${apkanalyzer}" manifest application-id "${apk}")"
actual_version_name="$("${apkanalyzer}" manifest version-name "${apk}")"
actual_version_code="$("${apkanalyzer}" manifest version-code "${apk}")"

[[ "${actual_package}" == "${expected_package}" ]] || {
    echo "Unexpected application ID: ${actual_package}" >&2
    exit 1
}
[[ "${actual_version_name}" == "${expected_version_name}" ]] || {
    echo "Unexpected version name: ${actual_version_name}" >&2
    exit 1
}
[[ "${actual_version_code}" == "${expected_version_code}" ]] || {
    echo "Unexpected version code: ${actual_version_code}" >&2
    exit 1
}

printf 'APK metadata verified: %s %s (%s)\n' \
    "${actual_package}" \
    "${actual_version_name}" \
    "${actual_version_code}"
