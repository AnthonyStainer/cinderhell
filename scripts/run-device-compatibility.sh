#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
adb_bin="${ADB:-${ANDROID_HOME:-/var/home/anthony/Android/Sdk}/platform-tools/adb}"
serial="${ANDROID_SERIAL:-}"
package_name="${CINDERHELL_PACKAGE:-dev.cinderhell.debug}"
case_name="${1:-}"
output_dir="$repo_root/build/device-gates"
device_tmp="/data/local/tmp/cinderhell-compat"

if [[ -z "$case_name" ]]; then
    echo "Usage: ANDROID_SERIAL=<serial> $0 <freedoom|vanilla|boom-deh|mbf-bex|mbf21-deh|pk3|zip>" >&2
    exit 2
fi

case "$case_name" in
    freedoom)
        compatibility="mbf21"
        fixtures=()
        roles=()
        ;;
    vanilla)
        compatibility="vanilla"
        fixtures=("vanilla-map.wad")
        roles=("MOD")
        ;;
    boom-deh)
        compatibility="boom"
        fixtures=("boom-map.wad" "noop.deh")
        roles=("MOD" "PATCH")
        ;;
    mbf-bex)
        compatibility="mbf"
        fixtures=("mbf-map.wad" "strings.bex")
        roles=("MOD" "PATCH")
        ;;
    mbf21-deh)
        compatibility="mbf21"
        fixtures=("mbf21-map.wad" "mbf21.deh")
        roles=("MOD" "PATCH")
        ;;
    pk3)
        compatibility="mbf21"
        fixtures=("maps.pk3")
        roles=("MOD")
        ;;
    zip)
        compatibility="boom"
        fixtures=("maps.zip")
        roles=("MOD")
        ;;
    *)
        echo "Unknown compatibility case: $case_name" >&2
        exit 2
        ;;
esac

adb=("$adb_bin")
if [[ -n "$serial" ]]; then
    adb+=("-s" "$serial")
fi

"${adb[@]}" get-state >/dev/null
if ! "${adb[@]}" shell run-as "$package_name" true >/dev/null 2>&1; then
    echo "$package_name must be an installed debuggable Cinderhell build." >&2
    exit 1
fi

# Keep the app foreground while the debug-only launch broadcast is delivered.
# Starting it before staging also lets normal launcher recovery finish before
# temporary corpus blobs (which intentionally have no Room catalogue row)
# enter the private content store.
"${adb[@]}" shell am start -W \
    -n "$package_name/dev.cinderhell.LauncherActivity" >/dev/null
sleep 0.5

mkdir -p "$output_dir"
host_tmp="$(mktemp -d "${TMPDIR:-/tmp}/cinderhell-compat.XXXXXX")"
cleanup() {
    "${adb[@]}" shell rm -rf "$device_tmp" >/dev/null 2>&1 || true
    rm -rf "$host_tmp"
}
trap cleanup EXIT

"${adb[@]}" shell mkdir -p "$device_tmp"
"${adb[@]}" shell run-as "$package_name" mkdir -p \
    files/content/sha256 \
    "files/configs/compat-$case_name" \
    "files/saves/compat-$case_name" \
    "files/screenshots/compat-$case_name"

iwad_sha="a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b"
iwad_path="/data/user/0/$package_name/files/content/sha256/$iwad_sha"
if ! "${adb[@]}" shell run-as "$package_name" test -f "files/content/sha256/$iwad_sha"; then
    echo "Bundled Freedoom is not installed in the content store." >&2
    exit 1
fi

ordered_content="{\"contentId\":\"freedoom-0.13.0-phase2\",\"role\":\"GAME\",\"path\":\"$iwad_path\",\"sha256\":\"$iwad_sha\"}"
for index in "${!fixtures[@]}"; do
    fixture="${fixtures[$index]}"
    fixture_path="$repo_root/test-corpus/generated/$fixture"
    fixture_sha="$(sha256sum "$fixture_path" | cut -d' ' -f1)"
    remote_tmp="$device_tmp/$fixture"
    "${adb[@]}" push "$fixture_path" "$remote_tmp" >/dev/null
    "${adb[@]}" shell chmod 644 "$remote_tmp"
    "${adb[@]}" shell run-as "$package_name" cp \
        "$remote_tmp" "files/content/sha256/$fixture_sha"
    ordered_content+=",{\"contentId\":\"compat-$fixture_sha\",\"role\":\"${roles[$index]}\",\"path\":\"/data/user/0/$package_name/files/content/sha256/$fixture_sha\",\"sha256\":\"$fixture_sha\"}"
done

now_millis="$(date +%s%3N)"
expires_millis="$((now_millis + 300000))"
session_id="$(printf '%s' "$case_name-$now_millis" | sha256sum | cut -c1-32)"
nonce="$(printf '%s' "nonce-$case_name-$now_millis" | sha256sum | cut -c1-64)"
descriptor="$host_tmp/$session_id.json"

printf '%s\n' \
    "{\"schemaVersion\":1,\"sessionId\":\"$session_id\",\"nonce\":\"$nonce\",\"createdAtEpochMillis\":$now_millis,\"expiresAtEpochMillis\":$expires_millis,\"profileId\":\"compat-$case_name\",\"presetVersion\":1,\"orderedContent\":[$ordered_content],\"configPath\":\"/data/user/0/$package_name/files/configs/compat-$case_name/woof.cfg\",\"saveDirectory\":\"/data/user/0/$package_name/files/saves/compat-$case_name\",\"screenshotDirectory\":\"/data/user/0/$package_name/files/screenshots/compat-$case_name\",\"options\":{\"mode\":\"NORMAL\",\"targetRefreshRate\":120,\"skill\":3,\"warp\":\"MAP01\",\"compatibility\":\"$compatibility\",\"loadGameSlot\":null}}" \
    >"$descriptor"

remote_descriptor="$device_tmp/$session_id.json"
"${adb[@]}" push "$descriptor" "$remote_descriptor" >/dev/null
"${adb[@]}" shell chmod 644 "$remote_descriptor"
"${adb[@]}" shell run-as "$package_name" cp \
    "$remote_descriptor" "files/sessions/pending/$session_id.json"

"${adb[@]}" logcat -c
"${adb[@]}" shell am broadcast \
    -a dev.cinderhell.DEBUG_LAUNCH_SESSION \
    -n "$package_name/dev.cinderhell.DeviceGateReceiver" \
    --es dev.cinderhell.extra.SESSION_ID "$session_id" >/dev/null

game_pid=""
for _ in {1..20}; do
    game_pid="$("${adb[@]}" shell pidof "$package_name:game" 2>/dev/null | tr -d '\r' || true)"
    [[ -n "$game_pid" ]] && break
    sleep 0.25
done
if [[ -z "$game_pid" ]]; then
    "${adb[@]}" logcat -d >"$output_dir/$case_name.log"
    echo "$case_name failed: the game process did not remain active." >&2
    exit 1
fi

sleep 2
"${adb[@]}" exec-out screencap -p >"$output_dir/$case_name.png"
"${adb[@]}" logcat -d >"$output_dir/$case_name.log"
if rg -q 'Fatal signal|Game startup failed|SDL_main returned [^0]' "$output_dir/$case_name.log"; then
    echo "$case_name failed: native startup/crash diagnostics were recorded." >&2
    exit 1
fi

# Exercise the exact profile-specific save path for every compatibility case.
# Confirming an empty description twice saves the first slot without requiring
# text entry; the load menu then reloads that same slot.
"${adb[@]}" shell input keyevent KEYCODE_F2
sleep 0.3
"${adb[@]}" shell input keyevent KEYCODE_ENTER
sleep 0.2
"${adb[@]}" shell input keyevent KEYCODE_ENTER
sleep 1
if ! "${adb[@]}" shell run-as "$package_name" ls "files/saves/compat-$case_name" |
    rg -q '(?i)\.dsg$'; then
    echo "$case_name failed: no save was written." >&2
    exit 1
fi

"${adb[@]}" shell input keyevent KEYCODE_F3
sleep 0.3
"${adb[@]}" shell input keyevent KEYCODE_ENTER
sleep 1
if ! "${adb[@]}" shell pidof "$package_name:game" >/dev/null; then
    echo "$case_name failed: the process stopped while loading its save." >&2
    exit 1
fi
"${adb[@]}" exec-out screencap -p >"$output_dir/$case_name-loaded.png"

if [[ "${CINDERHELL_KEEP_RUNNING:-0}" == "1" ]]; then
    printf 'LIVE %-10s pid=%s screenshot=%s\n' \
        "$case_name" "$game_pid" "$output_dir/$case_name.png"
    exit 0
fi

"${adb[@]}" shell input keyevent KEYCODE_F10
sleep 0.25
"${adb[@]}" shell input keyevent KEYCODE_Y

clean_exit=false
for _ in {1..40}; do
    if "${adb[@]}" logcat -d -s CinderhellGame:V '*:S' |
        tr -d '\r' |
        rg -q 'SDL_main returned 0'; then
        clean_exit=true
        break
    fi
    sleep 0.25
done
"${adb[@]}" logcat -d >"$output_dir/$case_name.log"

if [[ "$clean_exit" != true ]]; then
    echo "$case_name failed: no clean SDL_main result was observed." >&2
    exit 1
fi

printf 'PASS %-10s pid=%s screenshot=%s\n' \
    "$case_name" "$game_pid" "$output_dir/$case_name.png"
