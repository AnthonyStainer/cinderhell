#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
download_dir="${repo_root}/build/dependency-downloads"
sdl_zip="${download_dir}/SDL3-devel-3.4.10-android.zip"
freedoom_zip="${download_dir}/freedoom-0.13.0.zip"

mkdir -p "${download_dir}" "${repo_root}/app/libs" "${repo_root}/app/src/main/assets/runtime"

fetch_checked() {
    local url="$1"
    local expected="$2"
    local output="$3"

    if [[ ! -f "${output}" ]] || ! echo "${expected}  ${output}" | sha256sum --check --status; then
        curl -L --fail --retry 3 --output "${output}.part" "${url}"
        echo "${expected}  ${output}.part" | sha256sum --check
        mv "${output}.part" "${output}"
    fi
}

fetch_checked \
    "https://github.com/libsdl-org/SDL/releases/download/release-3.4.10/SDL3-devel-3.4.10-android.zip" \
    "f676e29b1b4eb990e24862b6af5f327379dc496c9f5f3e3f48ab75837f95844e" \
    "${sdl_zip}"

unzip -p "${sdl_zip}" SDL3-3.4.10.aar > "${repo_root}/app/libs/SDL3-3.4.10.aar.part"
echo "e80e8ab1bc969bed28192d34c2029f223ce06cf106aa3d1167220334b581ddb0  ${repo_root}/app/libs/SDL3-3.4.10.aar.part" | sha256sum --check
mv "${repo_root}/app/libs/SDL3-3.4.10.aar.part" "${repo_root}/app/libs/SDL3-3.4.10.aar"

fetch_checked \
    "https://github.com/freedoom/freedoom/releases/download/v0.13.0/freedoom-0.13.0.zip" \
    "3f9b264f3e3ce503b4fb7f6bdcb1f419d93c7b546f4df3e874dd878db9688f59" \
    "${freedoom_zip}"

unzip -p "${freedoom_zip}" freedoom-0.13.0/freedoom2.wad > "${repo_root}/app/src/main/assets/runtime/freedoom2.wad.part"
echo "a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b  ${repo_root}/app/src/main/assets/runtime/freedoom2.wad.part" | sha256sum --check
mv "${repo_root}/app/src/main/assets/runtime/freedoom2.wad.part" "${repo_root}/app/src/main/assets/runtime/freedoom2.wad"

"${repo_root}/scripts/build-woof-pk3.py"

echo "Native release artifacts are present and verified."
