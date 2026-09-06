# Bill of Materials / Assembly Cost Capsule V1

Package-first pressure test of the generic relation engine under the hard constraint that `src/` does not change.

## Domain

A finite, explicitly acyclic two-level bill of materials:

```text
purchased parts -> subassembly component cost -> subassembly cost
                                           \
root BOM + catalog + subassembly cost -> product component cost -> product cost judgment
```

The sample bicycle contains WHEEL and BRAKE subassemblies. Their costs are derived from purchased leaf components and then used as component costs of the root product.

## Evidence preservation

`GHOST_MODULE` is deliberately present in the BOM but absent from the part catalog. The model preserves the BOM edge, marks it `UNKNOWN_COMPONENT`, uses zero only as an explicit placeholder for the missing cost, and classifies the final result `INCOMPLETE_COMPONENT`.

Known cost remains inspectable:

```text
WHEEL = 83
BRAKE = 30
known bicycle cost = 376
```

## Important semantic boundary

This capsule demonstrates finite hierarchical composition using the existing acyclic relation graph. It does **not** claim arbitrary recursive BOM explosion. General recursive graph traversal or data-level cycle detection is outside the current engine vocabulary and would require separate generic semantic pressure before any engine extension is justified.
