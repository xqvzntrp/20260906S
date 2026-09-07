# Relational Constitution V1

This repository is a self-contained V1 implementation and conformance package for a small relational capsule architecture.

The governing document is `RELATIONAL_CONSTITUTION_V1.md`.

## Contents

- `RELATIONAL_CONSTITUTION_V1.md` — normative semantic authority.
- `CAPSULE_FORMAT_V1.md` — normative V1 capsule interchange representation.
- `java-kernel/` — the existing Java witness kernel from the V3 baseline.
- `sqlite-kernel/` — a clarity-first Python + SQLite second conforming kernel.
- `capsules/` — shared capsules executed by both kernels, including a self-model capsule.
- `conformance/` — scripts and checkpoint notes for cross-kernel comparison.

## Authority

`RELATIONAL_CONSTITUTION_V1.md` defines V1 semantic meaning.

`CAPSULE_FORMAT_V1.md` defines the V1 capsule interchange representation.

No kernel defines either contract. Kernels conform to both.

## Quick start

Run all V1 conformance checks from this directory:

```bash
./conformance/run-all.sh
```

Requirements:

- JDK 21+
- Python 3.11+ with the standard-library `sqlite3` module
- Bash

No third-party Python packages are required.
