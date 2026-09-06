# Double-Entry Accounting Capsule V1

Package-first pressure test of the generic relation engine, built under the hard
constraint that `src/` does not change.

## Modeled domain

```text
DEBIT  -> +quantity
CREDIT -> -quantity
other  -> invalid evidence (never silently treated as CREDIT)
```

For each transaction:

```text
transaction_balance = SUM(signed_quantity) GROUP BY transaction_id
```

Judgment precedence:

```text
invalid direction present -> INVALID_DIRECTION
otherwise balance = 0     -> BALANCED
otherwise                 -> UNBALANCED
```

## Relation flow

```text
entries
  -> signed_entries
  -> transaction_balance
  -> transaction_judgment
  -> unbalanced_transactions
```

The source evidence is preserved. Invalid domain values are surfaced in derived
judgment instead of being corrected or silently coerced. No accounting-specific
runtime operation is required.
