#!/usr/bin/env python3
"""Build Woof's required base PK3 with reproducible ZIP metadata."""

from __future__ import annotations

import hashlib
import os
from pathlib import Path
import re
import sys
import zipfile


REPO_ROOT = Path(__file__).resolve().parents[1]
BASE_DIR = REPO_ROOT / "third_party" / "woof" / "base"
BASE_CMAKE = BASE_DIR / "CMakeLists.txt"
OUTPUT = REPO_ROOT / "app" / "src" / "main" / "assets" / "runtime" / "woof.pk3"


def source_paths() -> list[str]:
    cmake = BASE_CMAKE.read_text(encoding="utf-8")
    match = re.search(
        r"set\(BASE_SOURCES\s*\n(.*?)\)\s*\n\s*add_custom_command",
        cmake,
        re.DOTALL,
    )
    if match is None:
        raise RuntimeError("Could not find BASE_SOURCES in Woof's base CMake file")

    paths: list[str] = []
    for line in match.group(1).splitlines():
        value = line.split("#", 1)[0].strip()
        if value:
            paths.append(value)

    if not paths or len(paths) != len(set(paths)):
        raise RuntimeError("Woof BASE_SOURCES is empty or contains duplicate paths")
    return sorted(paths)


def build() -> str:
    sources = source_paths()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUT.with_suffix(".pk3.part")

    try:
        with zipfile.ZipFile(
            temporary,
            mode="w",
            compression=zipfile.ZIP_DEFLATED,
            compresslevel=9,
        ) as archive:
            for relative in sources:
                source = BASE_DIR / relative
                if not source.is_file():
                    raise RuntimeError(f"Missing Woof base asset: {relative}")

                info = zipfile.ZipInfo(relative, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                archive.writestr(info, source.read_bytes(), compresslevel=9)

        os.replace(temporary, OUTPUT)
    finally:
        temporary.unlink(missing_ok=True)

    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    return digest


if __name__ == "__main__":
    try:
        print(f"{build()}  {OUTPUT}")
    except Exception as error:
        print(f"Failed to build Woof base PK3: {error}", file=sys.stderr)
        raise SystemExit(1) from error
