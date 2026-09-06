# Python + SQLite Witness Kernel

This is a clarity-first second implementation of Relational Constitution V1.

Python handles capsule parsing, schema contracts, exact-decimal adapters, CSV I/O, and verification. SQLite executes the relational composition itself.

The implementation intentionally uses only the Python standard library.

```bash
python3 kernel.py ../capsules/double-entry-accounting-capsule-v1
```

SQLite dynamic typing is not semantic authority. DECIMAL values are represented canonically and arithmetic/aggregation uses Python `Decimal` through registered SQLite functions/aggregates so binary floating point cannot silently redefine V1 results.
