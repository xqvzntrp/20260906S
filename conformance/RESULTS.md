# V1 Conformance Results

Final pre-release pressure test:

- Existing Java test classes passed: **16**.
- Existing Java negative cases passed: **9** cases / **18** assertions.
- Shared capsules executed and verified by both kernels: **4**.
- Direct cross-kernel output matches:
  - Double-entry accounting: 4/4
  - Bill of materials: 5/5
  - Self-model: 5/5
  - Enterprise cloud GTM V9: 15/15
  - **Total: 29/29 outputs matched**
- Python + SQLite negative boundary cases passed: **3/3**.
- Self-model exercises all eight V1 relational primitives.
- Java kernel source is unchanged from the V3 baseline.

A breadth test against V9 exposed one useful implementation mismatch before freeze: the initial SQLite witness did not enforce derived NUMBER(p,s) scale after exact division. The second kernel was corrected to implement the already-existing Java numeric contract: 38-digit division, HALF_UP rounding, and declared precision/scale enforcement. After that correction, all 15 V9 outputs match directly across kernels.

This is evidence of conformance, not a proof that either implementation is the semantic authority. The constitution remains normative.
