# SQLite Compatibility Notes

Relational Constitution V1 does not adopt SQLite implementation behavior as semantic authority.

The second kernel deliberately normalizes areas where native SQLite behavior can diverge from the constitution:

- SQLite uses dynamic typing and column affinity rather than rigid per-value domain enforcement.
- V1 DECIMAL is exact decimal; SQLite REAL is therefore not used as the semantic NUMBER representation.
- Exact decimal arithmetic is implemented with Python `Decimal` registered as SQLite scalar/aggregate functions while SQLite continues to perform the relational composition.
- Division uses 38 significant digits and HALF_UP rounding, matching the Java witness numeric contract.
- Declared NUMBER(p,s) scale is enforced with HALF_UP rounding before output becomes observable.
- V1 aggregate behavior on an empty input with no group key follows the Java witness: no observed group means no output row. SQLite's ordinary scalar aggregate row is suppressed with `HAVING COUNT(*) > 0`.
- Typed cross-kernel comparison ignores CSV formatting differences and row serialization order but preserves tuple multiplicity.

Reference documentation consulted during the V1 audit:

- https://www.sqlite.org/datatype3.html
- https://www.sqlite.org/lang_expr.html
- https://www.sqlite.org/lang_aggfunc.html
