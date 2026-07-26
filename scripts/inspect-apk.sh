#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="${1:-${repo_root}/app/build/outputs/apk/preview/app-preview.apk}"

[[ -f "${apk}" ]] || { echo "APK does not exist: ${apk}" >&2; exit 1; }

apk_entries="$(unzip -Z1 "${apk}")"
mapfile -t abis < <(
    printf '%s\n' "${apk_entries}" |
        awk -F/ '$1 == "lib" && NF >= 3 { print $2 }' |
        sort -u
)
[[ "${#abis[@]}" -eq 1 && "${abis[0]}" == "arm64-v8a" ]] || {
    echo "Unexpected packaged ABIs: ${abis[*]:-none}" >&2
    exit 1
}

required_entries=(
    "lib/arm64-v8a/libmain.so"
    "lib/arm64-v8a/libSDL3.so"
    "assets/runtime/freedoom2.wad"
    "assets/runtime/woof.pk3"
    "assets/legal/THIRD_PARTY_NOTICES.txt"
    "assets/legal/CORRESPONDING_SOURCE.txt"
)
for entry in "${required_entries[@]}"; do
    grep -Fxq "${entry}" <<< "${apk_entries}" || {
        echo "APK is missing ${entry}" >&2
        exit 1
    }
done

check_asset() {
    local entry="$1"
    local expected="$2"
    local actual
    actual="$(unzip -p "${apk}" "${entry}" | sha256sum | awk '{print $1}')"
    [[ "${actual}" == "${expected}" ]] || {
        echo "APK asset checksum mismatch: ${entry}" >&2
        exit 1
    }
}

check_asset \
    "assets/runtime/freedoom2.wad" \
    "a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b"
check_asset \
    "assets/runtime/woof.pk3" \
    "15a36e6342f2b883143eba06c993ea3ff0c6cfa49a91de7a17b524979388b2ab"

echo "APK contains only arm64-v8a, the native runtime, verified data, and legal notices."
