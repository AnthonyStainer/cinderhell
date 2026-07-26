# Legal compatibility corpus

Run `./scripts/build-test-corpus.py` to generate the corpus under `generated/`.
Every map is derived mechanically from Freedoom 0.13.0's BSD-licensed MAP01;
the patch and archive fixtures are original no-op test data. No commercial
Doom bytes are included.

The corpus covers:

- the pinned Freedoom Phase 2 IWAD and the known-hash catalogue entries for
  Doom, Doom II, TNT, Plutonia, and both Freedoom phases;
- deterministic PWAD containers labelled for vanilla, Boom, MBF, and MBF21
  launch combinations;
- deterministic PK3 and ZIP containers;
- DEH, BEX, and MBF21 patch parser/launch combinations.

The generated archive fixtures use stored ZIP entries so their checksums do
not vary between zlib implementations.

Known commercial IWAD identities are checked by catalogue/hash tests, not by
redistributing copyrighted IWADs. A user-owned IWAD can be substituted during
device testing without entering the repository or release artifacts.

The physical result matrix is recorded in `docs/compatibility-matrix.md`.
