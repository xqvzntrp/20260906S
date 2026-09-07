# V1 Phase Checkpoints

1. **Constitution** — PASS: the system can be explained without Java or SQLite authority.
2. **Algebra** — PASS: eight V1 primitives are frozen.
3. **Primitive contracts** — PASS: input/output/provenance/failure/invariant obligations are stated. Provenance is normative, but V1 does not directly materialize or cross-kernel compare provenance.
4. **Java kernel conformance** — PASS for the frozen observable V1 surface exercised by the shared capsules and existing Java test suite.
5. **Cross-kernel equivalence** — PASS criterion is typed tuple-bag equality plus matching verification judgment; CSV byte formatting and row order are not semantic.
6. **SQLite compatibility** — PASS with explicit exact-decimal adapters; SQLite affinity/REAL behavior is not treated as constitutional semantics.
7. **Python + SQLite kernel** — PASS: standard-library, clarity-first second witness kernel exists.
8. **Shared capsule conformance** — PASS when every shared capsule declared by the conformance harness verifies independently under both kernels and all generated outputs match directly across kernels.
9. **Self-model** — PASS: ordinary capsule semantics model the eight primitives and two kernel witnesses.
10. **Cross-kernel self-model** — PASS when both kernels verify all self-model outputs.
11. **V1 freeze** — PASS when `run-all.sh` completes with no unresolved mismatch.
12. **Optimization** — NOT STARTED by design; any future optimization must preserve observational and provenance semantics.

## Deliberately not claimed in V1

- arbitrary recursive relation traversal;
- a materialized tuple/field provenance file format or standardized provenance query interface;
- direct cross-kernel certification of provenance equivalence;
- performance optimization as semantic authority;
- domain-specific primitives;
- SQLite native floating-point as DECIMAL semantics.
