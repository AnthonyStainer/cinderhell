#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/var/home/anthony/Android/Sdk}}"
adb_bin="${ADB:-${sdk_root}/platform-tools/adb}"
serial="${ANDROID_SERIAL:-}"

if [[ ! -x "$adb_bin" ]]; then
    echo "ADB is not available at $adb_bin." >&2
    exit 1
fi

if [[ -z "$serial" ]]; then
    serial="$("$adb_bin" get-serialno | tr -d '\r')"
fi
if [[ -z "$serial" || "$serial" == "unknown" ]]; then
    echo "Set ANDROID_SERIAL to one connected Android device." >&2
    exit 1
fi

adb=("$adb_bin" "-s" "$serial")
"${adb[@]}" get-state >/dev/null
original_stay="$("${adb[@]}" shell settings get global stay_on_while_plugged_in | tr -d '\r')"

restore_stay_awake() {
    if [[ "$original_stay" == "null" ]]; then
        "${adb[@]}" shell settings delete global stay_on_while_plugged_in >/dev/null
    else
        "${adb[@]}" shell settings put global stay_on_while_plugged_in "$original_stay" >/dev/null
    fi
}
trap restore_stay_awake EXIT

"${adb[@]}" shell settings put global stay_on_while_plugged_in 7
"${adb[@]}" shell input keyevent KEYCODE_WAKEUP
"${adb[@]}" shell wm dismiss-keyguard

(
    cd "$repo_root"
    ANDROID_HOME="$sdk_root" ANDROID_SERIAL="$serial" \
        ./gradlew connectedDebugAndroidTest --stacktrace
)
