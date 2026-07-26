#!/usr/bin/env python3
"""Resolve a Cinderhell preview tag into validated Android version metadata."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import asdict, dataclass


PREVIEW_TAG = re.compile(
    r"^v(?P<major>0|[1-9][0-9]*)"
    r"\.(?P<minor>0|[1-9][0-9]*)"
    r"\.(?P<patch>0|[1-9][0-9]*)"
    r"-preview\.(?P<ordinal>[1-9][0-9]{0,3})$"
)
ANDROID_MAX_VERSION_CODE = 2_100_000_000


class VersionError(ValueError):
    """Raised when a tag cannot safely identify an Android preview release."""


@dataclass(frozen=True)
class ReleaseVersion:
    tag: str
    version_name: str
    version_code: int
    artifact_version: str


def resolve(tag: str) -> ReleaseVersion:
    match = PREVIEW_TAG.fullmatch(tag)
    if match is None:
        raise VersionError(
            "expected vMAJOR.MINOR.PATCH-preview.N with no leading zeroes"
        )

    major, minor, patch, ordinal = (
        int(match.group(name))
        for name in ("major", "minor", "patch", "ordinal")
    )
    if major > 20 or minor > 99 or patch > 99:
        raise VersionError("major must be <= 20; minor and patch must be <= 99")
    if ordinal > 9_998:
        raise VersionError("preview ordinal must be between 1 and 9998")

    version_code = (
        major * 100_000_000
        + minor * 1_000_000
        + patch * 10_000
        + ordinal
    )
    if not 1 <= version_code <= ANDROID_MAX_VERSION_CODE:
        raise VersionError("derived Android version code is out of range")

    version_name = tag.removeprefix("v")
    return ReleaseVersion(
        tag=tag,
        version_name=version_name,
        version_code=version_code,
        artifact_version=version_name,
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("tag")
    parser.add_argument(
        "--format",
        choices=("json", "github"),
        default="json",
        help="Output JSON or key=value lines suitable for GITHUB_OUTPUT.",
    )
    args = parser.parse_args()

    try:
        version = resolve(args.tag)
    except VersionError as error:
        print(f"Invalid Cinderhell preview tag {args.tag!r}: {error}", file=sys.stderr)
        return 2

    values = asdict(version)
    if args.format == "github":
        for key, value in values.items():
            print(f"{key}={value}")
    else:
        print(json.dumps(values, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
