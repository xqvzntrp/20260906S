#!/usr/bin/env python3

import json
import sys
from pathlib import Path


def load(path):
    with Path(path).open(encoding="utf-8") as f:
        document = json.load(f)

    return [
        (
            c["name"],
            c["type"].upper(),
            bool(c.get("nullable", False)),
            c.get("precision"),
            c.get("scale"),
        )
        for c in document["columns"]
    ]


def main():
    if len(sys.argv) != 3:
        print(
            "usage: compare-schema.py JAVA_SCHEMA SQLITE_SCHEMA",
            file=sys.stderr,
        )
        return 2

    java = load(sys.argv[1])
    sqlite = load(sys.argv[2])

    if java != sqlite:
        print("CROSS-KERNEL SCHEMA MISMATCH", file=sys.stderr)
        print("java =", java, file=sys.stderr)
        print("sqlite =", sqlite, file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
