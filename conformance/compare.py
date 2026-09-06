#!/usr/bin/env python3
"""Typed bag comparison for Java and Python+SQLite generated evidence."""
from __future__ import annotations
import csv,json,sys
from collections import Counter
from decimal import Decimal
from pathlib import Path

def schema(path):
    d=json.load(open(path)); out=[]
    for c in d['columns']:
        t=c['type'].upper(); t='DECIMAL' if t=='NUMBER' else t
        out.append((c['name'],t,bool(c.get('nullable',False))))
    return out

def val(x,t,nullable):
    if x in ('',r'\N'):
        if nullable:return None
        raise ValueError('unexpected NULL')
    if t=='DECIMAL':return Decimal(x)
    if t in ('INTEGER','BOOLEAN'):return int(x) if x.lower() not in ('true','false') else (1 if x.lower()=='true' else 0)
    return x

def rows(path,s):
    with open(path,newline='',encoding='utf-8') as f:
        r=csv.DictReader(f)
        names=[x[0] for x in s]
        if r.fieldnames!=names: raise ValueError(f'{path}: column mismatch')
        return Counter(tuple(val(row[n],t,nu) for n,t,nu in s) for row in r)

def main():
    if len(sys.argv)!=4:
        print('usage: compare.py SCHEMA JAVA_CSV SQLITE_CSV',file=sys.stderr); return 2
    s=schema(Path(sys.argv[1])); a=rows(Path(sys.argv[2]),s); b=rows(Path(sys.argv[3]),s)
    if a!=b:
        print('CROSS-KERNEL MISMATCH',file=sys.stderr)
        print('only_java=',a-b,file=sys.stderr); print('only_sqlite=',b-a,file=sys.stderr); return 1
    return 0
if __name__=='__main__': raise SystemExit(main())
