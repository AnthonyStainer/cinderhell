from __future__ import annotations

import importlib.util
import pathlib
import sys
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "resolve-release-version.py"
SPEC = importlib.util.spec_from_file_location("resolve_release_version", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class ReleaseVersionTest(unittest.TestCase):
    def test_first_preview(self) -> None:
        version = MODULE.resolve("v0.1.0-preview.1")

        self.assertEqual("0.1.0-preview.1", version.version_name)
        self.assertEqual(1_000_001, version.version_code)
        self.assertEqual("0.1.0-preview.1", version.artifact_version)

    def test_versions_increase_by_preview_and_patch(self) -> None:
        preview_one = MODULE.resolve("v0.1.0-preview.1")
        preview_two = MODULE.resolve("v0.1.0-preview.2")
        next_patch = MODULE.resolve("v0.1.1-preview.1")

        self.assertLess(preview_one.version_code, preview_two.version_code)
        self.assertLess(preview_two.version_code, next_patch.version_code)

    def test_rejects_ambiguous_or_reserved_versions(self) -> None:
        invalid = (
            "0.1.0-preview.1",
            "v00.1.0-preview.1",
            "v0.1.0",
            "v0.1.0-preview.0",
            "v0.1.0-preview.9999",
            "v0.100.0-preview.1",
            "v21.0.0-preview.1",
        )

        for tag in invalid:
            with self.subTest(tag=tag):
                with self.assertRaises(MODULE.VersionError):
                    MODULE.resolve(tag)


if __name__ == "__main__":
    unittest.main()
