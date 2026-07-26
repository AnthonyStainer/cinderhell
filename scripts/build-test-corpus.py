#!/usr/bin/env python3
"""Generate a deterministic, redistributable Doom compatibility corpus."""

from __future__ import annotations

import hashlib
from pathlib import Path
import struct
import zipfile


ROOT = Path(__file__).resolve().parents[1]
FREEDOOM = ROOT / "app/src/main/assets/runtime/freedoom2.wad"
OUTPUT = ROOT / "test-corpus/generated"
MAP_END = "BLOCKMAP"


def read_wad(path: Path) -> list[tuple[str, bytes]]:
    data = path.read_bytes()
    magic, count, directory = struct.unpack_from("<4sII", data)
    if magic not in (b"IWAD", b"PWAD"):
        raise RuntimeError(f"Not a WAD: {path}")
    lumps: list[tuple[str, bytes]] = []
    for index in range(count):
        offset, size, raw_name = struct.unpack_from("<II8s", data, directory + index * 16)
        name = raw_name.rstrip(b"\0").decode("ascii")
        lumps.append((name, data[offset : offset + size]))
    return lumps


def write_wad(path: Path, lumps: list[tuple[str, bytes]]) -> None:
    payload = bytearray(b"PWAD" + struct.pack("<II", len(lumps), 0))
    records: list[tuple[int, int, str]] = []
    for name, value in lumps:
        records.append((len(payload), len(value), name))
        payload.extend(value)
    directory = len(payload)
    for offset, size, name in records:
        payload.extend(struct.pack("<II8s", offset, size, name.encode("ascii")[:8].ljust(8, b"\0")))
    struct.pack_into("<I", payload, 8, directory)
    path.write_bytes(payload)


def map01_lumps() -> list[tuple[str, bytes]]:
    source = read_wad(FREEDOOM)
    start = next(index for index, (name, _) in enumerate(source) if name == "MAP01")
    end = next(
        index for index in range(start + 1, len(source))
        if source[index][0] == MAP_END
    )
    return source[start : end + 1]


def write_zip(path: Path, entries: dict[str, bytes]) -> None:
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_STORED) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            # Stored entries are byte-identical across zlib implementations.
            info.compress_type = zipfile.ZIP_STORED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, entries[name])


def build() -> None:
    if hashlib.sha256(FREEDOOM.read_bytes()).hexdigest() != (
        "a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b"
    ):
        raise RuntimeError("The pinned Freedoom corpus source is missing or changed")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    base_map = map01_lumps()
    variants = {
        "vanilla-map.wad": [("CNDVAN", b"vanilla")] + base_map,
        "boom-map.wad": [("CNDBOOM", b"boom")] + base_map,
        "mbf-map.wad": [("CNDMBF", b"mbf")] + base_map,
        "mbf21-map.wad": [("CNDMBF21", b"mbf21")] + base_map,
    }
    for name, lumps in variants.items():
        write_wad(OUTPUT / name, lumps)

    deh = (
        "Patch File for DeHackEd v3.0\n"
        "# Deterministic no-op compatibility fixture\n"
        "Doom version = 19\n"
        "Patch format = 6\n"
    ).encode()
    bex = deh + b"\n[STRINGS]\nHUSTR_1 = Cinderhell corpus\n"
    mbf21 = (
        "Patch File for DeHackEd v3.0\n"
        "# MBF21 parser fixture\n"
        "Doom version = 2021\n"
        "Patch format = 7\n"
    ).encode()
    (OUTPUT / "noop.deh").write_bytes(deh)
    (OUTPUT / "strings.bex").write_bytes(bex)
    (OUTPUT / "mbf21.deh").write_bytes(mbf21)
    write_zip(
        OUTPUT / "maps.pk3",
        {
            "maps/MAP01.wad": (OUTPUT / "vanilla-map.wad").read_bytes(),
            "cinderhell.txt": b"Redistributable Cinderhell PK3 fixture\n",
        },
    )
    write_zip(
        OUTPUT / "maps.zip",
        {
            "maps/MAP01.wad": (OUTPUT / "boom-map.wad").read_bytes(),
            "cinderhell.txt": b"Redistributable Cinderhell ZIP fixture\n",
        },
    )

    manifest = "\n".join(
        f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}"
        for path in sorted(OUTPUT.iterdir())
        if path.is_file() and path.name != "SHA256SUMS"
    ) + "\n"
    (OUTPUT / "SHA256SUMS").write_text(manifest, encoding="utf-8")
    print(manifest, end="")


if __name__ == "__main__":
    build()
