#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
woof_dir="${repo_root}/third_party/woof"
patch_file="${repo_root}/native/patches/woof-android.patch"

if [[ ! -d "${woof_dir}/.git" ]] && ! git -C "${woof_dir}" rev-parse --git-dir >/dev/null 2>&1; then
    echo "Woof submodule is not initialized: ${woof_dir}" >&2
    exit 1
fi

expected_revision="5f7a0def133056cb527312f2376b3088adb863fc"
actual_revision="$(git -C "${woof_dir}" rev-parse HEAD)"
if [[ "${actual_revision}" != "${expected_revision}" ]]; then
    echo "Woof revision mismatch: expected ${expected_revision}, found ${actual_revision}" >&2
    exit 1
fi

if git -C "${woof_dir}" apply --reverse --check "${patch_file}" >/dev/null 2>&1; then
    echo "Woof Android patch is already applied."
elif git -C "${woof_dir}" apply --check "${patch_file}"; then
    git -C "${woof_dir}" apply "${patch_file}"
    echo "Applied Woof Android patch."
else
    echo "Woof Android patch does not apply cleanly." >&2
    exit 1
fi
