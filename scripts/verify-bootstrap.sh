#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
lock_file="${repo_root}/third_party/dependencies.lock.toml"

required_lock_values=(
    "5f7a0def133056cb527312f2376b3088adb863fc"
    "15a36e6342f2b883143eba06c993ea3ff0c6cfa49a91de7a17b524979388b2ab"
    "8e37db5e797b6167f3a00d697d816a684bd259c7"
    "dc7d7054a5b4f3bec1dc23a42fd616a0847af948"
    "cfb8644b1a8dc7d7d2177e6a892ccaa2922bdaae"
    "f676e29b1b4eb990e24862b6af5f327379dc496c9f5f3e3f48ab75837f95844e"
    "3f9b264f3e3ce503b4fb7f6bdcb1f419d93c7b546f4df3e874dd878db9688f59"
    "85aa3f7b01e91d9a0b2e8079b065c594e579c43d53d5671c8f68b20074cc896e"
)

[[ -f "${lock_file}" ]] || { echo "Missing dependency lock: ${lock_file}" >&2; exit 1; }

for value in "${required_lock_values[@]}"; do
    grep -Fq "${value}" "${lock_file}" || {
        echo "Dependency lock is missing required pin: ${value}" >&2
        exit 1
    }
done

check_file() {
    local path="$1"
    local expected="$2"
    [[ -f "${path}" ]] || { echo "Missing required artifact: ${path}" >&2; exit 1; }
    echo "${expected}  ${path}" | sha256sum --check --status || {
        echo "Checksum mismatch: ${path}" >&2
        exit 1
    }
}

check_file \
    "${repo_root}/app/libs/SDL3-3.4.10.aar" \
    "e80e8ab1bc969bed28192d34c2029f223ce06cf106aa3d1167220334b581ddb0"
check_file \
    "${repo_root}/app/src/main/assets/runtime/freedoom2.wad" \
    "a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b"
check_file \
    "${repo_root}/app/src/main/assets/runtime/woof.pk3" \
    "15a36e6342f2b883143eba06c993ea3ff0c6cfa49a91de7a17b524979388b2ab"

manifest="${repo_root}/app/src/main/AndroidManifest.xml"
if grep -Fq "MANAGE_EXTERNAL_STORAGE" "${manifest}"; then
    echo "Broad storage access is forbidden." >&2
    exit 1
fi

grep -Fq 'abiFilters += "arm64-v8a"' "${repo_root}/app/build.gradle.kts" || {
    echo "arm64-v8a-only packaging policy is missing." >&2
    exit 1
}

required_legal_files=(
    "${repo_root}/app/src/main/assets/legal/THIRD_PARTY_NOTICES.txt"
    "${repo_root}/app/src/main/assets/legal/CORRESPONDING_SOURCE.txt"
    "${repo_root}/third_party/woof/COPYING"
    "${repo_root}/third_party/openal-soft/COPYING"
    "${repo_root}/third_party/freedoom/COPYING.adoc"
)
for legal_file in "${required_legal_files[@]}"; do
    [[ -s "${legal_file}" ]] || {
        echo "Missing license or source metadata: ${legal_file}" >&2
        exit 1
    }
done

[[ -x "${repo_root}/scripts/build-test-corpus.py" ]] || {
    echo "The legal compatibility corpus generator is missing." >&2
    exit 1
}

echo "Bootstrap pins, assets, ABI policy, and manifest constraints verified."
