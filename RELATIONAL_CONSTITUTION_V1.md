# Relational Constitution V1

## 1. Status and authority

This document is the normative semantic authority for Relational Constitution V1.

**No kernel defines the semantics. Kernels conform to the constitution.**

Java, Python, SQLite, file formats, CSV serialization, and any future implementation are mechanisms. They may witness the constitution; they may not redefine it.

## 2. Architectural invariants

1. Preserve evidence.
2. Make derivation explicit.
3. Keep domain meaning out of the engine.
4. Treat every transformation as producing a new relation.
5. Separate execution from correctness.
6. Fail rather than silently infer unsupported meaning.
7. Prefer composition over new runtime concepts.

These invariants govern every primitive, kernel, capsule, and optimization.

## 3. Development doctrine

1. Freeze the principles as invariants.
2. Define the formal objects: relation, tuple, evidence, derivation, constraint, failure.
3. Define the algebra: the small set of transformations allowed over relations.
4. Define provenance semantics: exactly how every output traces back to inputs/evidence.
5. Define correctness separately: what makes a derivation valid, invalid, or unsupported.
6. Prove the principles hold for each primitive operation.
7. Build the smallest executable kernel that implements only those primitives.
8. Express domain behavior through composition, not engine extensions.
9. Add optimizations only if they preserve observational semantics and provenance.

## 4. Capsule shape

A capsule consists of six authored concerns:

- **DATA** — supplied evidence.
- **SCHEMA** — declared admissible shape and types of evidence.
- **RELATION** — explicit derivations over relations.
- **MANIFEST** — package membership and execution boundary.
- **EXPECTED** — independent expected evidence used for correctness checks.
- **NEGATIVE TESTS** — witnesses for behavior that must be rejected or must not be silently inferred.

Generated output is not authored evidence. It is a consequence of executing a capsule under a conforming kernel.

## 5. Formal objects

### 5.1 Value

A value is either `NULL` or a member of a declared scalar domain. V1 scalar domains are:

- `TEXT`
- `INTEGER`
- `DECIMAL`
- `BOOLEAN`
- `DATE`

`DECIMAL` is exact decimal arithmetic. A conforming kernel must not substitute binary floating-point semantics where that can change an observable result.

### 5.1.1 Value equivalence

Value equivalence is defined within a declared scalar domain.

Two non-NULL values are equivalent when they denote the same value under that domain's V1 semantics.

For `DECIMAL`, representational scale is not part of value identity. For example, `1`, `1.0`, and `1.00` are equivalent decimal values when admitted by the applicable decimal contract.

For grouping, `NULL` values in the same column position belong to one equivalence class.

Ordinary comparison involving `NULL` evaluates `UNKNOWN`, except for explicit NULL predicates.

`GROUP BY` uses value equivalence, including the grouping rule for `NULL`. `COUNT_DISTINCT` first excludes `NULL` values and then counts equivalence classes among the remaining values. `JOIN` equality and `FILTER` comparison use comparison semantics. Cross-kernel conformance compares values using value equivalence.

### 5.1.2 Three-valued logic and NULL

V1 predicates evaluate to `TRUE`, `FALSE`, or `UNKNOWN`.

Ordinary comparison with `NULL` evaluates `UNKNOWN`.

A `FILTER` retains a tuple only when its predicate evaluates `TRUE`; both `FALSE` and `UNKNOWN` are not selected.

For ordinary equality joins, `NULL` does not equal `NULL`.

Conditional expressions select the `then` branch only when the condition evaluates `TRUE`; `FALSE` and `UNKNOWN` select the `else` branch.

### 5.1.3 Decimal arithmetic and numeric contracts

V1 decimal addition, subtraction, and multiplication are exact.

Decimal division uses a working precision of 38 significant decimal digits and `HALF_UP` rounding when an exact result cannot be represented within that working precision.

A declared `DECIMAL(p,s)` result contract is enforced when the value is materialized under that declaration. Scale `s` is applied using `HALF_UP` rounding when rounding is required. Precision `p` is checked after scale enforcement. A value that exceeds the declared precision causes an execution failure.

Intermediate arithmetic is not implicitly constrained by the precision or scale of its input columns. Precision and scale constrain a value only where the resulting column declaration explicitly carries that contract.

### 5.2 Schema

A schema is an ordered finite sequence of uniquely named columns. Each column declares a scalar domain and nullability. A decimal column may additionally declare precision and scale.

### 5.3 Tuple

A tuple is an ordered sequence of values conforming positionally to one schema.

### 5.3.1 Tuple occurrence

A tuple occurrence is one occurrence of a tuple within a relation's bag.

Distinct tuple occurrences may have equivalent values in every column. They remain distinct occurrences when multiplicity is greater than one.

Tuple occurrence distinction is semantic for multiplicity and provenance, but is not an authored column, domain identifier, or part of tuple value equality.

A primitive that preserves, selects, combines, or derives from tuples operates on tuple occurrences. Provenance therefore identifies source occurrences, not merely source tuple values.

### 5.4 Relation

A relation is a schema plus a finite **bag** of tuples: tuple multiplicity is preserved, but row order is not part of V1 relational meaning. Relational meaning does not depend on physical storage or serialization order. A kernel may emit rows deterministically for inspection, but cross-kernel conformance compares typed tuple bags.

### 5.5 Evidence

Evidence is supplied data admitted under a declared schema. Evidence is preserved: validation or derivation may judge evidence, but may not silently rewrite an unsupported value into a supported meaning.

### 5.6 Derivation

A derivation is an explicit finite acyclic composition of primitive transformations. Every primitive consumes one or more existing relations and produces a new relation.

### 5.7 Constraint

A constraint is a declared requirement on structure, type, reference, operation, or execution. Constraints do not repair evidence; they admit, reject, or classify it.

### 5.8 Failure

A failure is an explicit inability to produce the semantics required by a declaration. V1 distinguishes:

- **structural failure** — malformed or incomplete capsule structure;
- **semantic failure** — a declaration is invalid, ambiguous, cyclic, or unsupported;
- **execution failure** — a valid derivation cannot be evaluated for supplied evidence, such as division by zero or numeric contract violation;
- **verification failure** — generated evidence does not match independently expected evidence.

A kernel must not convert one failure class into a successful result by inventing meaning.

## 6. Relational algebra V1

The V1 primitive set is deliberately small:

`PROJECT`, `RENAME`, `FILTER`, `JOIN`, `LEFT_JOIN`, `CROSS_JOIN`, `AGGREGATE`, `DERIVE`.

The algebra is acyclic. Arbitrary recursive traversal is not a V1 primitive.

## 7. Primitive contracts

### 7.1 PROJECT

**Input:** one relation `R` and a list of existing columns.

**Output:** a new relation containing exactly the selected columns in declared column order and one output tuple for each input tuple.

**Provenance:** each output tuple traces to exactly one input tuple; each output field traces to the correspondingly named input field.

**Failure:** unknown column or duplicate output identity.

**Invariant argument:** PROJECT does not invent domain meaning; it only removes fields and optionally establishes observable order.

### 7.2 RENAME

**Input:** one relation and a finite mapping from existing column names to new names.

**Output:** a new relation with identical tuples and types under renamed schema identities.

**Provenance:** one-to-one tuple provenance; renamed fields trace directly to their source fields.

**Failure:** unknown source name or duplicate resulting column name.

**Invariant argument:** evidence values are unchanged.

### 7.3 FILTER

**Input:** one relation and a typed predicate.

**Output:** a new relation containing exactly the tuples for which the predicate evaluates `TRUE`.

**NULL:** a predicate evaluating `UNKNOWN` is not selected.

**Provenance:** every output tuple traces to exactly one unchanged input tuple.

**Failure:** invalid predicate, unsupported operator, incompatible types, or unavailable column.

**Invariant argument:** FILTER removes tuples; it does not mutate them.

### 7.4 JOIN

**Input:** left and right relations plus one or more typed join conditions.

**Output:** a new relation containing every matching left/right tuple pair. Same-named equality join keys are represented once in the result; other duplicate column identities are invalid unless explicitly renamed or projected away according to the normalized contract.

**NULL:** `NULL` does not equal `NULL` for ordinary equality joins.

**Provenance:** every output tuple traces to one left tuple and one right tuple.

**Failure:** missing condition, ambiguous output schema, invalid reference, or incompatible comparison.

**Invariant argument:** JOIN composes existing evidence; it does not infer a relationship absent a declared condition.

### 7.5 LEFT_JOIN

**Input:** left and right relations plus one or more typed join conditions.

**Output:** all JOIN matches plus one row for each unmatched left tuple, with retained right-side fields set to `NULL`.

**Schema:** left-side columns preserve their declared schema. Retained right-side columns preserve their name, scalar domain, and any declared DECIMAL precision and scale, but become nullable because an unmatched left tuple has no corresponding right-side value.

**Provenance:** matched tuples trace to one left and one right tuple; unmatched tuples trace to one left tuple and an explicit absence of a matching right tuple.

**Failure:** same structural and semantic failures as JOIN.

**Invariant argument:** absence is preserved as absence. An unknown right-side entity must not silently disappear when the domain requires preservation of the left evidence.

### 7.6 CROSS_JOIN

**Input:** two relations.

**Output:** the Cartesian product of their tuples.

**Provenance:** every output tuple traces to exactly one left tuple and one right tuple.

**Failure:** ambiguous output schema unless resolved by the declared projection/normalization contract.

**Invariant argument:** CROSS_JOIN adds no domain relationship claim beyond declared Cartesian composition.

### 7.7 AGGREGATE

**Input:** one relation, zero or more group columns, and one or more measures.

**V1 measures:** `SUM`, `COUNT`, `COUNT_DISTINCT`, `MIN`, `MAX`.

**Output:** one tuple per observed group. Group order follows first group appearance unless an explicit ordering contract states otherwise. For V1, an empty input produces no groups, including when `group_by` is empty.

**Result schema:** `COUNT` and `COUNT_DISTINCT` produce non-nullable `INTEGER` columns. `SUM` preserves whether its numeric source domain is `INTEGER` or `DECIMAL`, but does not inherit source DECIMAL precision or scale because a sum may exceed the contract of an individual source value. `MIN` and `MAX` preserve the source scalar domain and any declared DECIMAL precision and scale. `SUM`, `MIN`, and `MAX` result columns are nullable.

**NULL:** COUNT and COUNT_DISTINCT ignore NULL measure values. SUM, MIN, and MAX ignore NULL values and produce NULL when a non-empty observed group has no non-NULL value for that measure.

**Provenance:** each aggregate tuple traces to the finite set of source tuples sharing its group key; each measure traces to the source field values consumed by that measure.

**Failure:** unknown group/measure column, unsupported measure, type-incompatible measure, integer overflow, or declared numeric contract violation.

**Invariant argument:** aggregation summarizes declared evidence; it does not create unobserved groups.

### 7.8 DERIVE

**Input:** one relation and one or more typed scalar expressions.

**V1 scalar arithmetic:** `add`, `subtract`, `multiply`, `divide`, and scalar `max`, plus conditional expressions.

**Output:** a new relation containing the input fields plus declared derived fields, optionally projected and ordered by the step contract.

**Expression scope:** every expression declared by one `DERIVE` step is resolved against that step's input relation schema. Derived columns declared by the same step are siblings and are not visible to one another. A dependency on a derived column requires a subsequent `DERIVE` step.

**Result schema:** derived-column nullability is determined compositionally from the expression. A column reference inherits source nullability. A non-NULL literal is non-nullable and a NULL literal is nullable. Arithmetic is nullable when either operand is nullable. A conditional result is nullable when either branch is nullable. Scalar `max` is nullable only when both operands are nullable. Declared precision or scale is valid only for a DECIMAL result and is enforced when that derived value is materialized.

**NULL:** arithmetic with NULL produces NULL. Conditions use three-valued logic; an `if` condition is taken only when TRUE, otherwise the `else` branch is selected.

**Provenance:** each derived tuple traces to exactly one input tuple; every derived value traces to the fields and literals referenced by its expression tree.

**Failure:** unknown field/function, incompatible types, division by zero, or numeric precision/scale violation.

**Invariant argument:** all new values arise from an explicit expression tree; no hidden domain rule is permitted.

## 8. Predicate and expression semantics

V1 comparison operators are `=`, `!=`, `>`, `>=`, `<`, `<=`, and `is_not_null`.

Comparisons are typed by the declared schema, not by implementation-specific coercion. DATE values use their declared date semantics; TEXT comparison is lexical; INTEGER and DECIMAL comparisons are numeric; BOOLEAN comparison is boolean.

Conditional expressions are explicit trees containing a condition, `then`, and `else` branch. Nested conditions are ordinary expression composition, not a separate rule system.

## 9. Provenance semantics

Provenance is semantic, even when a kernel does not materialize it as an output file.

For every output tuple occurrence, provenance is obtained recursively from the primitive that created it:

- PROJECT / RENAME / FILTER / DERIVE: exactly one source tuple occurrence;
- JOIN / CROSS_JOIN: exactly one left tuple occurrence and one right tuple occurrence;
- LEFT_JOIN: exactly one left tuple occurrence and zero-or-one right tuple occurrence;
- AGGREGATE: the complete finite bag of source tuple occurrences belonging to the group.

Equivalent tuple values do not collapse provenance occurrences.

Field-level provenance is similarly recursive through projection, rename, expressions, join retention, and aggregate measures.

A conforming optimization may change execution strategy but must preserve this provenance relation.

## 10. Correctness is separate from execution

Execution answers: **what did this kernel derive from this capsule and evidence?**

Verification answers: **does that generated evidence match the independent EXPECTED witness under the declared schema?**

Successful execution is therefore not proof of correctness. EXPECTED must never be consulted to determine runtime semantics.

## 11. Manifest authority

The manifest declares capsule membership: input identities, relation identities, output identities, paths, formats, and expected witnesses. Directory presence alone does not establish semantic membership.

All paths must remain within the capsule root. A hardened kernel should evaluate containment using filesystem-resolved paths when executing untrusted capsules.

## 12. Observational equivalence across kernels

Given the same capsule and admitted input evidence, two conforming V1 kernels are observationally equivalent when they agree on:

1. output relation schemas, including column order, column identity, scalar domain, nullability, and any declared DECIMAL precision and scale;
2. output tuple bags, preserving multiplicity and ignoring serialization row order;
3. exact INTEGER and DECIMAL values after canonical numeric normalization;
4. NULL placement;
5. TEXT, BOOLEAN, and DATE values;
6. success versus the same semantic class of failure;
7. verification judgment.

Generated CSV byte representation is not itself semantic authority. For example, decimal `10`, `10.0`, and `10.00` are observationally equal when admitted under the same DECIMAL contract.

## 13. Kernel conformance

A V1 kernel must:

- implement only the V1 primitive semantics or explicitly reject unsupported declarations;
- validate before execution where a failure is statically knowable;
- preserve exact-decimal semantics;
- keep domain-specific concepts out of the primitive implementation;
- execute relation dependencies acyclically rather than by accidental file order;
- keep EXPECTED outside the execution authority boundary;
- produce observable results equivalent to other conforming kernels.

The Java kernel and Python + SQLite kernel included with this release are implementation witnesses. Neither is normative.

## 14. Optimization rule

An optimization is admissible only if it preserves both:

- **observational semantics**, and
- **provenance semantics**.

Performance alone never authorizes a semantic change.

## 15. Self-model rule

A self-model capsule may describe and test the architecture using ordinary capsule constructs. It does not establish or amend this constitution. The constitution remains independent authority; the self-model is executable documentation and a conformance witness.
