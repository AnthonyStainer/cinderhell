# Compatibility matrix

The generated corpus comes entirely from redistributable Freedoom 0.13.0
content. `scripts/build-test-corpus.py` recreates it deterministically and
writes its SHA-256 manifest.

## Pinned Woof 15.2.0-dev SDL3 Android result

| Combination | Import/preflight | Launch/level entry | Save/load | Ordered load |
| --- | --- | --- | --- | --- |
| Freedoom Phase 2 alone | Pass | Pass, MAP01 | Pass | N/A |
| Freedoom + vanilla PWAD | Pass | Pass, MAP01 | Pass | Pass |
| Freedoom + Boom PWAD + DEH | Pass | Pass, MAP01 | Pass | Pass |
| Freedoom + MBF PWAD + BEX | Pass | Pass, MAP01 | Pass | Pass |
| Freedoom + MBF21 PWAD + MBF21 DEH | Pass | Pass, MAP01 | Pass | Pass |
| Freedoom + PK3 | Pass | Pass, MAP01 | Pass | Pass |
| Freedoom + ZIP | Pass | Pass, MAP01 | Pass | Pass |

Every row was run on an AYN Thor with:

```sh
ANDROID_SERIAL=11c5b80 ./scripts/run-device-compatibility.sh <case>
```

The gate verifies that the `:game` process remains live after a MAP01 warp,
captures a level screenshot, writes `woofsav0.dsg` in the case-specific save
directory, reloads slot 0, captures a second screenshot, and requires a clean
`SDL_main` status 0. Logs and screenshots are retained under
`build/device-gates/`.

The launch adapter uses the content list exactly in profile order and its unit
test checks the resulting interleaved `-file`/`-deh` order. The full product
flow separately created a save for `bundled-freedoom-handheld`, returned to a
launcher Continue card naming `Freedoom: Phase 2 — MAP01`, and loaded it in a
fresh `:game` process. The known commercial IWAD catalogue is unit-tested by
digest record without copying proprietary data into the corpus.

Malformed WAD offsets, excessive lump/archive counts, compression limits,
misleading names/MIME types, and known ZScript/DECORATE requirements are
covered by importer tests and fail before native startup with actionable
launcher messages.

Results were recorded on 2026-07-25 against Woof revision
`5f7a0def133056cb527312f2376b3088adb863fc`.

The gate found and made actionable two Android integration failures during
this run: content-addressed suffixless WADs were not recognized as WAD
containers, and friendly `MAP01` session warps were passed directly to Woof
instead of its numeric CLI form. Both adapters now have regression coverage;
the matrix above is the post-fix rerun.
