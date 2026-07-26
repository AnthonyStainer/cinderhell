#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version="${CINDERHELL_RELEASE_VERSION:-0.1.0-preview.local}"
output_dir="${repo_root}/build/release"
download_dir="${repo_root}/build/dependency-downloads"
sdl_archive="${download_dir}/SDL-source-8e37db5.tar.gz"
sdl_url="https://github.com/libsdl-org/SDL/archive/8e37db5e797b6167f3a00d697d816a684bd259c7.tar.gz"
sdl_sha256="85aa3f7b01e91d9a0b2e8079b065c594e579c43d53d5671c8f68b20074cc896e"

mkdir -p "${output_dir}" "${download_dir}"
if [[ ! -f "${sdl_archive}" ]] ||
    ! echo "${sdl_sha256}  ${sdl_archive}" | sha256sum --check --status; then
    curl -L --fail --retry 3 --output "${sdl_archive}.part" "${sdl_url}"
    echo "${sdl_sha256}  ${sdl_archive}.part" | sha256sum --check
    mv "${sdl_archive}.part" "${sdl_archive}"
fi

stage="$(mktemp -d "${repo_root}/build/source-stage.XXXXXX")"
cleanup() {
    rm -rf -- "${stage}"
}
trap cleanup EXIT

mkdir -p "${stage}/cinderhell-${version}/app" "${stage}/cinderhell-${version}/third_party"
destination="${stage}/cinderhell-${version}"
cp -a \
    "${repo_root}/app/src" \
    "${repo_root}/app/schemas" \
    "${repo_root}/app/build.gradle.kts" \
    "${repo_root}/app/proguard-rules.pro" \
    "${destination}/app/"
cp -a \
    "${repo_root}/README.md" \
    "${repo_root}/LICENSE" \
    "${repo_root}/CHANGELOG.md" \
    "${repo_root}/CONTRIBUTING.md" \
    "${repo_root}/SECURITY.md" \
    "${repo_root}/native" \
    "${repo_root}/scripts" \
    "${repo_root}/docs" \
    "${repo_root}/openspec" \
    "${repo_root}/test-corpus" \
    "${repo_root}/.github" \
    "${repo_root}/gradle" \
    "${repo_root}/build.gradle.kts" \
    "${repo_root}/settings.gradle.kts" \
    "${repo_root}/gradle.properties" \
    "${repo_root}/gradlew" \
    "${repo_root}/gradlew.bat" \
    "${repo_root}/.gitmodules" \
    "${destination}/"
cp -a \
    "${repo_root}/third_party/dependencies.lock.toml" \
    "${repo_root}/third_party/woof" \
    "${repo_root}/third_party/openal-soft" \
    "${repo_root}/third_party/freedoom" \
    "${destination}/third_party/"
find "${destination}/third_party" -type d -name .git -prune -exec rm -rf -- {} +
tar -xzf "${sdl_archive}" -C "${destination}/third_party"
mv \
    "${destination}/third_party/SDL-8e37db5e797b6167f3a00d697d816a684bd259c7" \
    "${destination}/third_party/sdl"

archive="${output_dir}/cinderhell-${version}-corresponding-source.tar.gz"
tar \
    --sort=name \
    --mtime="@0" \
    --owner=0 \
    --group=0 \
    --numeric-owner \
    -C "${stage}" \
    -cf - \
    "cinderhell-${version}" |
    gzip -n -9 > "${archive}.part"
mv "${archive}.part" "${archive}"
sha256sum "${archive}" > "${archive}.sha256"
echo "${archive}"
