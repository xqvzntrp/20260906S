# Capsule Format V1

## 1. Status and authority

This document defines the normative interchange representation for Relational Constitution V1 capsules.

`RELATIONAL_CONSTITUTION_V1.md` remains the normative semantic authority.

This document defines how constitutional declarations are represented in files. It does not redefine relational meaning.

No kernel defines the capsule format. Kernels conform to both the constitution and this interchange contract.

## 2. Capsule root

A capsule is a directory whose membership is declared by `capsule.json`.

Files merely present in the directory are not semantically members of the capsule unless referenced by the manifest.

All manifest paths are relative to the capsule root and must remain within that root.

A V1 capsule may contain authored concerns for:

- input DATA;
- input SCHEMA;
- RELATION declarations;
- EXPECTED evidence;
- generated output paths.

Generated files are not authored evidence.

## 3. Manifest

The capsule manifest is UTF-8 JSON named:

```text
capsule.json
```

Its V1 shape is:

```json
{
  "capsule": {
    "id": "CAPSULE_ID",
    "version": "1.0.0"
  },
  "inputs": [
    {
      "id": "input_relation",
      "path": "data/input.csv",
      "schema_path": "data/input.schema.json",
      "format": "csv"
    }
  ],
  "relations": [
    {
      "id": "derived_relation",
      "path": "relations/derived_relation.json",
      "format": "json"
    }
  ],
  "outputs": [
    {
      "id": "derived_relation",
      "relation": "derived_relation",
      "generated_path": "generated/derived_relation.csv",
      "expected_path": "expected/derived_relation.csv",
      "schema_path": "expected/derived_relation.schema.json",
      "format": "csv"
    }
  ]
}
```

### 3.1 Input declaration

Each input contains:

- `id` — relation identity available to derivations;
- `path` — authored evidence path;
- `schema_path` — schema declaration path;
- `format` — `csv` in V1.

Input identities must be unique within the capsule.

### 3.2 Relation declaration

Each relation contains:

- `id` — declared derived relation identity;
- `path` — relation document path;
- `format` — `json` in V1.

Relation identities must be unique within the capsule.

### 3.3 Output declaration

Each output contains:

- `id` — output identity;
- `relation` — existing relation identity to publish;
- `generated_path` — path where kernel-generated evidence is written;
- `expected_path` — authored EXPECTED evidence;
- `schema_path` — authored expected output schema;
- `format` — `csv` in V1.

EXPECTED is verification evidence only and must not influence execution semantics.

## 4. Schema documents

A V1 schema document is UTF-8 JSON:

```json
{
  "schema_id": "orders",
  "version": "1.0.0",
  "relation": "orders",
  "columns": [
    {
      "name": "order_id",
      "type": "INTEGER",
      "nullable": false
    },
    {
      "name": "amount",
      "type": "DECIMAL",
      "precision": 18,
      "scale": 2,
      "nullable": true
    }
  ]
}
```

Columns form an ordered sequence. Column names must be unique.

Each column contains:

- `name`;
- `type`;
- `nullable`.

DECIMAL columns may additionally contain:

- `precision`;
- `scale`.

Precision and scale have the semantics defined by the constitution.

## 5. Scalar type spellings

Canonical V1 interchange type spellings are:

```text
TEXT
INTEGER
DECIMAL
BOOLEAN
DATE
```

For compatibility with existing V1 capsules:

```text
NUMBER
```

is an accepted interchange alias for constitutional `DECIMAL`.

A conforming reader must treat `NUMBER` and `DECIMAL` as the same constitutional scalar domain.

New V1 capsule documents should use `DECIMAL`.

## 6. CSV evidence representation

V1 CSV files are UTF-8 comma-separated files with a header row.

The header must contain exactly the schema column names in schema order.

Every data row represents one tuple occurrence.

Duplicate rows remain distinct tuple occurrences.

### 6.1 NULL

The V1 CSV NULL token is:

```text
\N
```

`\N` denotes constitutional NULL.

It is distinct from ordinary textual content.

A non-nullable column containing `\N` is invalid evidence.

### 6.2 Scalar lexical values

Values are interpreted according to the declared schema rather than implementation-specific coercion.

- `TEXT` — CSV text.
- `INTEGER` — base-10 integer lexical form.
- `DECIMAL` / `NUMBER` — exact base-10 decimal lexical form.
- `BOOLEAN` — lowercase `true` or `false`.
- `DATE` — calendar date in `YYYY-MM-DD` form.

The NULL token `\N` is interpreted before scalar lexical parsing.

A lexical value that does not conform to its declared scalar type is invalid evidence.

CSV formatting beyond these lexical requirements is not semantic authority.

## 7. Relation documents

A relation document is UTF-8 JSON with this shape:

```json
{
  "relation_id": "derived_relation",
  "description": "Optional human-readable description.",
  "inputs": {
    "local_name": "existing_relation"
  },
  "steps": [
    {
      "id": "step_id",
      "op": "project"
    }
  ],
  "output": "step_id"
}
```

`relation_id` identifies the relation declared by the manifest.

`inputs` maps local relation names used by the document to already available capsule relation identities.

`steps` is an ordered list of explicitly declared derivation steps.

`output` names the step whose produced relation is the relation document result.

Step dependency semantics are acyclic as defined by the constitution. File order does not establish semantic dependencies that are not explicitly referenced.

Every step has:

- `id` — unique step identity within the relation document;
- `op` — one V1 primitive operation.

V1 operation spellings are:

```text
project
rename
filter
join
left_join
cross_join
aggregate
derive
```

Unknown operation spellings must be rejected.

## 8. PROJECT

Shape:

```json
{
  "id": "selected",
  "op": "project",
  "from": "source",
  "select": [
    "column_a",
    "column_b"
  ]
}
```

Fields:

- `from` — input relation;
- `select` — ordered non-empty list of existing column names.

The `select` array determines output column identity and order.

## 9. RENAME

Shape:

```json
{
  "id": "renamed",
  "op": "rename",
  "from": "source",
  "rename": {
    "old_name": "new_name"
  }
}
```

`rename` is a finite mapping from existing column names to replacement names.

Columns not present in the mapping retain their existing names.

## 10. FILTER

Shape:

```json
{
  "id": "filtered",
  "op": "filter",
  "from": "source",
  "where": {
    "column": "status",
    "operator": "=",
    "value": "ACTIVE"
  }
}
```

`where` is one predicate as defined in section 15.

## 11. JOIN

Shape:

```json
{
  "id": "joined",
  "op": "join",
  "left": "left_relation",
  "right": "right_relation",
  "on": [
    {
      "left": "id",
      "operator": "=",
      "right": "id"
    }
  ]
}
```

`on` is a non-empty ordered list of typed join conditions.

A JOIN may optionally contain:

```json
{
  "select": [
    "column_a",
    "column_b"
  ]
}
```

The projection is applied only after the JOIN working schema is valid under the constitutional JOIN schema rule.

It does not legalize an otherwise ambiguous JOIN schema.

## 12. LEFT_JOIN

Shape:

```json
{
  "id": "joined",
  "op": "left_join",
  "left": "left_relation",
  "right": "right_relation",
  "on": [
    {
      "left": "id",
      "operator": "=",
      "right": "id"
    }
  ]
}
```

`on` follows the JOIN condition representation.

LEFT_JOIN schema, NULL extension, and provenance semantics are defined by the constitution.

## 13. CROSS_JOIN

Shape:

```json
{
  "id": "crossed",
  "op": "cross_join",
  "left": "left_relation",
  "right": "right_relation"
}
```

V1 does not define a same-step CROSS_JOIN `select` field.

The CROSS_JOIN working schema must therefore already have unique output column identities. If left and right inputs would produce duplicate output column identities, the inputs must be normalized by earlier relation steps before the CROSS_JOIN. An ambiguous CROSS_JOIN working schema is a semantic failure.

## 14. AGGREGATE

Ungrouped shape:

```json
{
  "id": "total",
  "op": "aggregate",
  "from": "source",
  "measures": [
    {
      "name": "total_amount",
      "function": "SUM",
      "column": "amount"
    }
  ]
}
```

Grouped shape:

```json
{
  "id": "grouped",
  "op": "aggregate",
  "from": "source",
  "group_by": [
    "account_id"
  ],
  "measures": [
    {
      "name": "total_amount",
      "function": "SUM",
      "column": "amount"
    }
  ]
}
```

`group_by` is optional. Absence is equivalent to zero group columns.

The V1 measure function spellings are:

```text
SUM
COUNT
COUNT_DISTINCT
MIN
MAX
```

Each measure contains:

- `name` — output column identity;
- `function`;
- `column` — source column.

Aggregate semantics and result typing are defined by the constitution.

## 15. Predicates

A binary predicate is represented as:

```json
{
  "column": "amount",
  "operator": ">",
  "value": 0
}
```

V1 comparison operator spellings are:

```text
=
!=
>
>=
<
<=
```

The NULL predicate is:

```json
{
  "column": "value",
  "operator": "is_not_null"
}
```

`is_not_null` has no `value` member.

FILTER predicates use `column` and, where required, `value`.

JOIN conditions use:

```json
{
  "left": "left_column",
  "operator": "=",
  "right": "right_column"
}
```

Comparison semantics and three-valued logic are defined by the constitution.

Unknown predicate operators must be rejected.

## 16. DERIVE

Base shape:

```json
{
  "id": "derived",
  "op": "derive",
  "from": "source",
  "columns": [
    {
      "name": "new_column",
      "expression": {
        "function": "add",
        "args": [
          {
            "column": "x"
          },
          {
            "value": 1
          }
        ]
      }
    }
  ]
}
```

A derived column contains:

- `name`;
- `expression`.

A DECIMAL derived column may additionally declare:

- `precision`;
- `scale`.

A DERIVE may additionally contain:

```json
{
  "select": [
    "column_a",
    "new_column"
  ],
  "order_by": [
    "column_a"
  ]
}
```

`select` determines the final output column set and order.

`order_by`, where present, is an ordered list of existing output column names used to establish deterministic emitted row order. It does not change the bag-valued relational meaning defined by the constitution.

All sibling expressions in one DERIVE resolve against the step input schema, not against sibling derived columns.

## 17. Expression syntax

V1 expressions are recursive JSON objects.

### 17.1 Column reference

```json
{
  "column": "amount"
}
```

### 17.2 Literal

```json
{
  "value": 10
}
```

or:

```json
{
  "value": "TEXT"
}
```

or:

```json
{
  "value": true
}
```

JSON literal type and the surrounding declared expression context determine the constitutional scalar type.

### 17.3 Scalar function

```json
{
  "function": "multiply",
  "args": [
    {
      "column": "quantity"
    },
    {
      "column": "unit_cost"
    }
  ]
}
```

V1 scalar function spellings are:

```text
add
subtract
multiply
divide
max
```

The semantic arity and type requirements are defined by the constitution.

Unknown scalar functions must be rejected.

### 17.4 Conditional expression

```json
{
  "condition": {
    "column": "state",
    "operator": "=",
    "value": "VALID"
  },
  "then": {
    "value": 1
  },
  "else": {
    "value": 0
  }
}
```

`then` and `else` are themselves expressions and may contain nested conditionals.

Conditional semantics use constitutional three-valued logic.

## 18. Ordering

Schema column order is semantic where declared by the constitution.

Relation tuple order is not part of V1 relational meaning.

`order_by` is an interchange facility for deterministic emitted row order where supported by the step format. It does not transform a relation bag into an ordered relational object.

V1 currently defines `order_by` only on DERIVE steps.

## 19. Paths and containment

Every path declared by the manifest must resolve within the capsule root.

A path that escapes the capsule root is invalid.

Implementations processing untrusted capsules should evaluate containment using filesystem-resolved paths.

## 20. Unknown and unsupported syntax

A conforming kernel must fail rather than silently infer meaning for:

- unknown manifest fields that would alter semantic interpretation;
- unknown primitive operations;
- unknown scalar functions;
- unknown predicate operators;
- invalid or unavailable relation identities;
- invalid or unavailable column identities;
- unsupported structural variants.

Extensions are not V1 semantics unless defined by a later normative format version.

## 21. Authority boundary

The V1 authority stack is:

```text
RELATIONAL_CONSTITUTION_V1.md
    semantic meaning

CAPSULE_FORMAT_V1.md
    interchange representation

capsules/
    authored examples and conformance witnesses

java-kernel/
sqlite-kernel/
    non-normative implementations
```

If implementation behavior conflicts with the constitution or this format contract, the implementation is wrong.

If a capsule example conflicts with either normative document, the example is wrong.

Neither witness kernel may amend V1 semantics or interchange syntax by implementation behavior.
