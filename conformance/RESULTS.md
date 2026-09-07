# V1 Conformance Results

Final V1 pressure-test result:

- Java test classes passed: **16**.
- Java negative cases passed: **9** cases / **18** assertions.
- Shared capsules executed and verified by both kernels: **7**.
- Direct cross-kernel output matches: **36/36**.
- Python + SQLite negative boundary cases passed: **5/5**.
- Self-model exercises all eight V1 relational primitives.

Shared cross-kernel capsule coverage:

- Double-entry accounting: 4 outputs.
- Bill of materials: 5 outputs.
- Self-model: 5 outputs.
- Enterprise cloud GTM V9: 15 outputs.
- Value equivalence: 2 outputs.
- Tuple occurrence: 4 outputs.
- Numeric result contract: 1 output.

The V1 pressure-test sequence additionally established:

- decimal value equivalence independent of representational scale;
- explicit NULL grouping and three-valued comparison behavior;
- tuple-occurrence multiplicity semantics;
- exact numeric result contracts and observable result schemas;
- DERIVE sibling-expression input scope;
- deterministic JOIN schema construction;
- AGGREGATE row order as nonsemantic;
- normative but non-materialized provenance obligations;
- deterministic CROSS_JOIN schema validity before execution;
- a normative V1 capsule interchange contract.

The shared conformance harness compares generated schemas and typed tuple bags independently of CSV row serialization order while preserving tuple multiplicity.

Provenance remains normative semantic meaning, but V1 does not define a materialized provenance interchange format or directly certify provenance equivalence across kernels.

These results are evidence of conformance, not semantic authority.

`RELATIONAL_CONSTITUTION_V1.md` defines V1 semantic meaning.

`CAPSULE_FORMAT_V1.md` defines the V1 capsule interchange representation.

Neither witness kernel defines either contract.
