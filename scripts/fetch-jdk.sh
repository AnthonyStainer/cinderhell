#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
archive_dir="${repo_root}/build/toolchain-downloads"
archive="${archive_dir}/OpenJDK17U-jdk_x64_linux_hotspot_17.0.19_10.tar.gz"
toolchain_dir="${repo_root}/.toolchains"
expected="d8afc263758141a66e0e3aafc321e783f7016696f4eaea067d340a269037d331"
url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.19%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.19_10.tar.gz"

mkdir -p "${archive_dir}" "${toolchain_dir}"

if [[ ! -f "${archive}" ]] || ! echo "${expected}  ${archive}" | sha256sum --check --status; then
    curl -L --fail --retry 3 --output "${archive}.part" "${url}"
    echo "${expected}  ${archive}.part" | sha256sum --check
    mv "${archive}.part" "${archive}"
fi

if [[ ! -x "${toolchain_dir}/jdk-17.0.19+10/bin/java" ]]; then
    tar -xzf "${archive}" -C "${toolchain_dir}"
fi

"${toolchain_dir}/jdk-17.0.19+10/bin/java" -version
