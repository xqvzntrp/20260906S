#!/usr/bin/env python3
"""Clarity-first Python + SQLite witness kernel for Relational Constitution V1.

Python owns capsule parsing, type contracts, exact-decimal adapters, and verification.
SQLite owns relational composition: projection, rename, filter, joins, cross joins,
aggregation, derivation, and ordering.
"""
from __future__ import annotations

import argparse, csv, json, sqlite3, sys
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation, localcontext, ROUND_HALF_UP
from pathlib import Path
from typing import Any

TYPES = {"TEXT", "INTEGER", "DECIMAL", "BOOLEAN", "DATE"}
OPS = {"project","rename","filter","join","left_join","cross_join","aggregate","derive"}

@dataclass(frozen=True)
class Column:
    name: str
    type: str
    nullable: bool = False
    precision: int|None = None
    scale: int|None = None

Schema = list[Column]


def qi(name:str)->str:
    return '"' + name.replace('"','""') + '"'

def canon_decimal(x: Any) -> str:
    d = x if isinstance(x, Decimal) else Decimal(str(x))
    if d == 0: return "0"
    s = format(d.normalize(), 'f')
    return s.rstrip('0').rstrip('.') if '.' in s else s

def dec(x: Any) -> Decimal|None:
    if x is None: return None
    return Decimal(str(x))

def dec_binary(op):
    def f(a,b):
        if a is None or b is None: return None
        x,y=dec(a),dec(b)
        if op=='add': z=x+y
        elif op=='subtract': z=x-y
        elif op=='multiply': z=x*y
        elif op=='divide':
            if y == 0: raise ValueError('division by zero')
            with localcontext() as ctx:
                ctx.prec=38
                ctx.rounding=ROUND_HALF_UP
                z=x/y
        elif op=='max': z=max(x,y)
        else: raise ValueError(op)
        return canon_decimal(z)
    return f

def dec_cmp(a,b):
    if a is None or b is None: return None
    x,y=dec(a),dec(b)
    return -1 if x<y else (1 if x>y else 0)


def dec_enforce(value, precision, scale):
    if value is None: return None
    d=dec(value)
    if precision is not None:
        precision=int(precision)
        if precision < 1 or precision > 38: raise ValueError('NUMBER precision out of range')
    if scale is not None:
        scale=int(scale)
        if precision is None: raise ValueError('NUMBER scale requires precision')
        q=Decimal(1).scaleb(-scale)
        d=d.quantize(q,rounding=ROUND_HALF_UP)
    if precision is not None:
        # Decimal.as_tuple digits after scale enforcement mirrors BigDecimal significant digits closely.
        digits=len(d.as_tuple().digits)
        if digits > precision: raise ValueError('NUMBER precision overflow')
    return canon_decimal(d)

class DecimalSum:
    def __init__(self): self.total=None
    def step(self, value):
        if value is not None:
            self.total = dec(value) if self.total is None else self.total + dec(value)
    def finalize(self):
        return None if self.total is None else canon_decimal(self.total)

class DecimalMin:
    def __init__(self): self.value=None
    def step(self,v):
        if v is not None:
            x=dec(v); self.value=x if self.value is None or x<self.value else self.value
    def finalize(self): return None if self.value is None else canon_decimal(self.value)
class DecimalMax:
    def __init__(self): self.value=None
    def step(self,v):
        if v is not None:
            x=dec(v); self.value=x if self.value is None or x>self.value else self.value
    def finalize(self): return None if self.value is None else canon_decimal(self.value)


def connect()->sqlite3.Connection:
    db=sqlite3.connect(':memory:')
    for name in ('add','subtract','multiply','divide','max'):
        db.create_function('dec_'+name,2,dec_binary(name))
    db.create_function('dec_cmp',2,dec_cmp)
    db.create_function('dec_enforce',3,dec_enforce)
    db.create_aggregate('dec_sum',1,DecimalSum)
    db.create_aggregate('dec_min',1,DecimalMin)
    db.create_aggregate('dec_max',1,DecimalMax)
    return db


def load_json(p:Path):
    with p.open(encoding='utf-8') as f: return json.load(f)

def load_schema(p:Path)->Schema:
    d=load_json(p); out=[]
    for c in d['columns']:
        t=c['type'].upper()
        if t=='NUMBER': t='DECIMAL'
        if t not in TYPES: raise ValueError(f'unsupported type {t} in {p}')
        out.append(Column(c['name'],t,bool(c.get('nullable',False)),c.get('precision'),c.get('scale')))
    names=[c.name for c in out]
    if len(names)!=len(set(names)): raise ValueError(f'duplicate columns in {p}')
    return out

def schema_map(s:Schema): return {c.name:c for c in s}

def parse_value(text:str, c:Column):
    if text in ('', r'\N'):
        if c.nullable: return None
        raise ValueError(f'NULL/empty not allowed for {c.name}')
    if c.type in ('TEXT','DATE'): return text
    if c.type=='INTEGER': return int(text)
    if c.type=='DECIMAL': return canon_decimal(Decimal(text))
    if c.type=='BOOLEAN':
        v=text.strip().lower()
        if v in ('1','true'): return 1
        if v in ('0','false'): return 0
        raise ValueError(f'invalid BOOLEAN {text!r}')
    raise AssertionError(c.type)

def sql_type(c:Column): return 'INTEGER' if c.type in ('INTEGER','BOOLEAN') else 'TEXT'

def create_input(db,name:str,schema:Schema,csv_path:Path):
    db.execute(f'CREATE TABLE {qi(name)} ('+', '.join(f'{qi(c.name)} {sql_type(c)}' for c in schema)+')')
    cols=[c.name for c in schema]
    rows=[]
    with csv_path.open(newline='',encoding='utf-8') as f:
        r=csv.DictReader(f)
        if r.fieldnames!=cols: raise ValueError(f'{csv_path}: CSV columns do not match schema')
        for raw in r: rows.append(tuple(parse_value(raw[c.name],c) for c in schema))
    if rows:
        db.executemany(f'INSERT INTO {qi(name)} VALUES ('+','.join('?' for _ in cols)+')',rows)


def literal_sql(value, typ=None):
    if value is None: return 'NULL','NULL'
    if isinstance(value,bool): return ('1' if value else '0'),'BOOLEAN'
    if isinstance(value,int): return str(value),'INTEGER'
    if isinstance(value,float): return "'"+canon_decimal(Decimal(str(value)))+"'",'DECIMAL'
    s=str(value).replace("'","''")
    return f"'{s}'", typ or 'TEXT'

def unify(a:str,b:str)->str:
    if a==b:return a
    if {a,b}<={'INTEGER','DECIMAL'}:return 'DECIMAL'
    raise ValueError(f'incompatible expression types: {a}, {b}')

def expr_sql(e:dict,schema:Schema):
    sm=schema_map(schema)
    if 'column' in e:
        c=sm.get(e['column'])
        if c is None: raise ValueError(f'unknown column {e["column"]}')
        return qi(c.name),c.type
    if 'value' in e: return literal_sql(e['value'])
    if 'condition' in e:
        cond=condition_sql(e['condition'],schema)
        ts,tt=expr_sql(e['then'],schema); es,et=expr_sql(e['else'],schema)
        return f'CASE WHEN {cond} THEN {ts} ELSE {es} END',unify(tt,et)
    if 'function' in e:
        fn=e['function'].lower(); args=e.get('args',[])
        if len(args)!=2: raise ValueError(f'{fn} requires exactly two arguments')
        a,at=expr_sql(args[0],schema); b,bt=expr_sql(args[1],schema)
        if fn in ('add','subtract','multiply','divide'):
            if at not in ('INTEGER','DECIMAL') or bt not in ('INTEGER','DECIMAL'): raise ValueError(f'{fn} requires numeric arguments')
            return f'dec_{fn}({a},{b})','DECIMAL'
        if fn=='max':
            t=unify(at,bt)
            if t=='DECIMAL': return f'dec_max({a},{b})','DECIMAL'
            return f'max({a},{b})',t
        raise ValueError(f'unsupported scalar function {fn}')
    raise ValueError(f'unsupported expression {e}')

def cmp_sql(left_sql,left_t,op,right_sql,right_t):
    if op=='is_not_null': return f'{left_sql} IS NOT NULL'
    if op not in ('=','!=','>','>=','<','<='): raise ValueError(f'unsupported operator {op}')
    if left_t in ('INTEGER','DECIMAL') and right_t in ('INTEGER','DECIMAL'):
        c=f'dec_cmp({left_sql},{right_sql})' if 'DECIMAL' in (left_t,right_t) else f'(({left_sql}) - ({right_sql}))'
        target={'=':'= 0','!=':'!= 0','>':'> 0','>=':'>= 0','<':'< 0','<=':'<= 0'}[op]
        return f'{c} {target}'
    if left_t!=right_t: raise ValueError(f'incompatible comparison types {left_t} and {right_t}')
    return f'{left_sql} {op} {right_sql}'

def condition_sql(c:dict,schema:Schema):
    sm=schema_map(schema)
    if 'column' not in c: raise ValueError('condition requires column')
    lc=sm.get(c['column'])
    if lc is None: raise ValueError(f'unknown condition column {c["column"]}')
    l=qi(lc.name); op=c['operator']
    if op=='is_not_null': return f'{l} IS NOT NULL'
    r,rt=literal_sql(c.get('value'))
    return cmp_sql(l,lc.type,op,r,rt)

def join_condition_sql(cond,left_schema,right_schema,la='l',ra='r'):
    lm,rm=schema_map(left_schema),schema_map(right_schema)
    lc=lm.get(cond['left']); rc=rm.get(cond['right'])
    if not lc or not rc: raise ValueError(f'unknown join columns {cond}')
    return cmp_sql(f'{la}.{qi(lc.name)}',lc.type,cond['operator'],f'{ra}.{qi(rc.name)}',rc.type)


def join_schema(left:Schema,right:Schema,conditions:list, left_join=False)->Schema:
    coalesced={c['left'] for c in conditions if c.get('operator')=='=' and c.get('left')==c.get('right')}
    out=list(left); names={c.name for c in out}
    for c in right:
        if c.name in coalesced: continue
        if c.name in names: raise ValueError(f'ambiguous join output column {c.name}; rename first')
        out.append(Column(c.name,c.type, c.nullable or left_join,c.precision,c.scale)); names.add(c.name)
    return out

def project_schema(schema,select):
    sm=schema_map(schema); out=[]
    for n in select:
        if n not in sm: raise ValueError(f'unknown projected column {n}')
        out.append(sm[n])
    if len({c.name for c in out})!=len(out): raise ValueError('duplicate projected column')
    return out

def infer_literal_type(v):
    if v is None:return 'NULL'
    if isinstance(v,bool):return 'BOOLEAN'
    if isinstance(v,int):return 'INTEGER'
    if isinstance(v,float):return 'DECIMAL'
    return 'TEXT'

def infer_expr(e,schema):
    if 'column' in e:return schema_map(schema)[e['column']].type
    if 'value' in e:return infer_literal_type(e['value'])
    if 'condition' in e:return unify(infer_expr(e['then'],schema),infer_expr(e['else'],schema))
    if 'function' in e:
        fn=e['function'].lower(); ats=[infer_expr(a,schema) for a in e.get('args',[])]
        if fn in ('add','subtract','multiply','divide'):return 'DECIMAL'
        if fn=='max':return unify(*ats)
    raise ValueError(f'cannot infer expression {e}')

def infer_expr_nullable(e, schema):
    if 'column' in e:
        c = schema_map(schema).get(e['column'])
        if c is None:
            raise ValueError(f'unknown column {e["column"]}')
        return c.nullable

    if 'value' in e:
        return e['value'] is None

    if 'condition' in e:
        return (
            infer_expr_nullable(e['then'], schema)
            or infer_expr_nullable(e['else'], schema)
        )

    if 'function' in e:
        fn=e['function'].lower()
        args=e.get('args',[])

        if len(args)!=2:
            raise ValueError(f'{fn} requires exactly two arguments')

        left_nullable=infer_expr_nullable(args[0],schema)
        right_nullable=infer_expr_nullable(args[1],schema)

        if fn in ('add','subtract','multiply','divide'):
            return left_nullable or right_nullable

        if fn=='max':
            return left_nullable and right_nullable

        raise ValueError(f'unsupported scalar function {fn}')

    raise ValueError(f'cannot infer expression nullability {e}')


def apply_step(db, relation_id, idx, step, available):
    op=step['op']; sid=step['id']; view=f'__{relation_id}__{idx}_{sid}'
    if op not in OPS: raise ValueError(f'unsupported op {op}')
    order=''
    def order_sql(schema):
        if not step.get('order_by'): return ''
        sm=schema_map(schema)
        for n in step['order_by']:
            if n not in sm: raise ValueError(f'unknown order_by column {n}')
        return ' ORDER BY '+', '.join(qi(n) for n in step['order_by'])

    if op in ('project','rename','filter','derive','aggregate'):
        src,ins=available[step['from']]
    if op=='project':
        outs=project_schema(ins,step['select'])
        sql='SELECT '+', '.join(qi(n) for n in step['select'])+f' FROM {qi(src)}'+order_sql(outs)
    elif op=='rename':
        ren=step['rename']; sm=schema_map(ins); outs=[]; sels=[]; used=set()
        for c in ins:
            nn=ren.get(c.name,c.name)
            if nn in used: raise ValueError(f'duplicate renamed column {nn}')
            used.add(nn); outs.append(Column(nn,c.type,c.nullable,c.precision,c.scale)); sels.append(f'{qi(c.name)} AS {qi(nn)}')
        for old in ren:
            if old not in sm: raise ValueError(f'unknown rename source {old}')
        sql='SELECT '+', '.join(sels)+f' FROM {qi(src)}'+order_sql(outs)
    elif op=='filter':
        outs=list(ins); sql=f'SELECT * FROM {qi(src)} WHERE '+condition_sql(step['where'],ins)+order_sql(outs)
    elif op in ('join','left_join','cross_join'):
        lsrc,ls=available[step['left']]; rsrc,rs=available[step['right']]
        conditions=step.get('on',[])
        if op!='cross_join' and not conditions: raise ValueError(f'{op} requires conditions')
        outs=join_schema(ls,rs,conditions,op=='left_join')
        coalesced={c['left'] for c in conditions if c.get('operator')=='=' and c.get('left')==c.get('right')}
        sels=[f'l.{qi(c.name)} AS {qi(c.name)}' for c in ls]
        sels += [f'r.{qi(c.name)} AS {qi(c.name)}' for c in rs if c.name not in coalesced]
        j='CROSS JOIN' if op=='cross_join' else ('LEFT JOIN' if op=='left_join' else 'JOIN')
        sql='SELECT '+', '.join(sels)+f' FROM {qi(lsrc)} l {j} {qi(rsrc)} r'
        if op!='cross_join': sql+=' ON '+' AND '.join(join_condition_sql(c,ls,rs) for c in conditions)
        if step.get('select'):
            inner=view+'_join'; db.execute(f'CREATE TEMP VIEW {qi(inner)} AS {sql}')
            outs=project_schema(outs,step['select']); sql='SELECT '+', '.join(qi(n) for n in step['select'])+f' FROM {qi(inner)}'+order_sql(outs)
        else: sql+=order_sql(outs)
    elif op=='aggregate':
        sm=schema_map(ins); group=step.get('group_by',[])
        for n in group:
            if n not in sm: raise ValueError(f'unknown group column {n}')
        outs=[sm[n] for n in group]; sels=[qi(n) for n in group]
        for m in step['measures']:
            c=sm.get(m['column']); fn=m['function'].upper()
            if c is None: raise ValueError(f'unknown measure column {m["column"]}')
            if fn=='COUNT': ex=f'COUNT({qi(c.name)})'; oc=Column(m['name'],'INTEGER',False)
            elif fn=='COUNT_DISTINCT': ex=f'COUNT(DISTINCT {qi(c.name)})'; oc=Column(m['name'],'INTEGER',False)
            elif fn=='SUM':
                if c.type=='DECIMAL': ex=f'dec_sum({qi(c.name)})'
                elif c.type=='INTEGER': ex=f'SUM({qi(c.name)})'
                else: raise ValueError('SUM requires numeric column')

                # SUM may exceed the precision/scale contract of one
                # source value, so V1 does not inherit source p/s.
                oc=Column(m['name'],c.type,True,None,None)
            elif fn in ('MIN','MAX'):
                ex=(f'dec_{fn.lower()}({qi(c.name)})' if c.type=='DECIMAL' else f'{fn}({qi(c.name)})')
                oc=Column(m['name'],c.type,True,c.precision,c.scale)
            else: raise ValueError(f'unsupported measure {fn}')
            sels.append(f'{ex} AS {qi(m["name"])}'); outs.append(oc)
        # SQLite normally emits one scalar aggregate row for empty input; V1 freezes Java's no-group behavior.
        sql='SELECT '+', '.join(sels)+f' FROM {qi(src)}'
        if group: sql+=' GROUP BY '+', '.join(qi(n) for n in group)
        else: sql+=f' HAVING COUNT(*) > 0'
    elif op=='derive':
        outs=list(ins); sels=['*']
        for d in step['columns']:
            ex,t=expr_sql(d['expression'],outs)
            if d['name'] in schema_map(outs): raise ValueError(f'derived column already exists {d["name"]}')
            if d.get('precision') is not None or d.get('scale') is not None:
                t='DECIMAL'
                p=d.get('precision'); sc=d.get('scale')
                ex=f'dec_enforce({ex},{"NULL" if p is None else int(p)},{"NULL" if sc is None else int(sc)})'
            nullable=infer_expr_nullable(d['expression'],outs)
            oc=Column(d['name'],t,nullable,d.get('precision'),d.get('scale'))
            outs.append(oc)
            sels.append(f'{ex} AS {qi(d["name"])}')
        sql='SELECT '+', '.join(sels)+f' FROM {qi(src)}'
        if step.get('select'):
            inner=view+'_derive'; db.execute(f'CREATE TEMP VIEW {qi(inner)} AS {sql}')
            outs=project_schema(outs,step['select']); sql='SELECT '+', '.join(qi(n) for n in step['select'])+f' FROM {qi(inner)}'+order_sql(outs)
        else: sql+=order_sql(outs)
    else:
        raise AssertionError(op)
    db.execute(f'CREATE TEMP VIEW {qi(view)} AS {sql}')
    return view,outs


def relation_sources(doc): return list(doc.get('inputs',{}).values())

def execute_capsule(root:Path, write_generated=True):
    root=root.resolve(); manifest=load_json(root/'capsule.json')
    inputs={i['id']:i for i in manifest.get('inputs',[])}
    rel_entries={r['id']:r for r in manifest.get('relations',[])}
    outputs=manifest.get('outputs',[])
    if len(inputs)!=len(manifest.get('inputs',[])) or len(rel_entries)!=len(manifest.get('relations',[])): raise ValueError('duplicate manifest identities')
    db=connect(); schemas={}; tables={}; path_to_input={}
    for iid,i in inputs.items():
        p=(root/i['path']).resolve(); sp=(root/i['schema_path']).resolve()
        if root not in p.parents or root not in sp.parents: raise ValueError('path escapes capsule root')
        s=load_schema(sp); create_input(db,iid,s,p); schemas[iid]=s; tables[iid]=iid; path_to_input[i['path']]=iid
    docs={rid:load_json(root/e['path']) for rid,e in rel_entries.items()}
    remaining=list(rel_entries); completed=set()
    while remaining:
        progress=False
        for rid in list(remaining):
            doc=docs[rid]; deps=[]
            for src in relation_sources(doc):
                if src in rel_entries: deps.append(src)
            if any(d not in completed for d in deps): continue
            avail={}
            for bind,src in doc.get('inputs',{}).items():
                sid=path_to_input.get(src,src)
                if sid not in tables: raise ValueError(f'{rid}: unavailable source {src}')
                avail[bind]=(tables[sid],schemas[sid])
            for idx,step in enumerate(doc.get('steps',[]),1):
                avail[step['id']]=apply_step(db,rid,idx,step,avail)
            if doc['output'] not in avail: raise ValueError(f'{rid}: output {doc["output"]} unavailable')
            tables[rid],schemas[rid]=avail[doc['output']]; completed.add(rid); remaining.remove(rid); progress=True
        if not progress: raise ValueError(f'cyclic or unresolved relation dependencies: {remaining}')

    results={}
    for o in outputs:
        rid=o['relation']; schema=schemas[rid]; rows=[tuple(r) for r in db.execute(f'SELECT * FROM {qi(tables[rid])}')]
        results[o['id']]=(schema,rows)
        if write_generated:
            gp=(root/o['generated_path']).resolve()
            if root not in gp.parents: raise ValueError('generated path escapes capsule root')
            gp.parent.mkdir(parents=True,exist_ok=True)

            write_csv(gp,schema,rows)

            schema_name = (
                gp.name[:-4] + '.schema.json'
                if gp.name.endswith('.csv')
                else gp.name + '.schema.json'
            )

            write_schema_json(
                gp.with_name(schema_name),
                schema)
    return manifest,results

def serialize(v,c):
    if v is None:return r'\N'
    if c.type=='DECIMAL':return canon_decimal(v)
    if c.type=='BOOLEAN':return 'true' if int(v) else 'false'
    return str(v)

def write_csv(path,schema,rows):
    with path.open('w',newline='',encoding='utf-8') as f:
        w=csv.writer(f,lineterminator='\n'); w.writerow([c.name for c in schema])
        for row in rows:w.writerow([serialize(v,c) for v,c in zip(row,schema)])

def write_schema_json(path, schema):
    path.parent.mkdir(parents=True,exist_ok=True)

    document = {
        "columns": [
            {
                "name": c.name,
                "type": c.type,
                "nullable": c.nullable,
                "precision": c.precision,
                "scale": c.scale
            }
            for c in schema
        ]
    }

    with path.open('w',encoding='utf-8') as f:
        json.dump(document,f,indent=2)
        f.write('\n')


def read_expected(path,schema):
    out=[]
    with path.open(newline='',encoding='utf-8') as f:
        r=csv.DictReader(f)
        if r.fieldnames != [c.name for c in schema]: raise ValueError(f'{path}: expected columns mismatch')
        for raw in r: out.append(tuple(parse_value(raw[c.name],c) for c in schema))
    return out

def norm(v,c):
    if v is None:return None
    if c.type=='DECIMAL':return Decimal(str(v))
    if c.type in ('INTEGER','BOOLEAN'):return int(v)
    return str(v)

def normalized_rows(schema,rows,ordered=False):
    x=[tuple(norm(v,c) for v,c in zip(row,schema)) for row in rows]
    return x if ordered else sorted(x,key=lambda r:tuple((v is not None,str(v)) for v in r))

def verify(root:Path,manifest,results):
    failures=[]
    byid={o['id']:o for o in manifest['outputs']}
    for oid,(schema,rows) in results.items():
        o=byid[oid]; esp=load_schema(root/o['schema_path'])
        # EXPECTED schema validates the witness; it does not redefine execution schema.
        # Names and scalar domains must agree. Nullability/precision metadata may be
        # stricter on the independent witness as long as the witness rows satisfy it.
        if [(c.name,c.type) for c in schema] != [(c.name,c.type) for c in esp]:
            failures.append(f'{oid}: schema mismatch'); continue
        expected=read_expected(root/o['expected_path'],esp)
        # output order is considered non-semantic unless relation step explicitly orders; expected checks are set-like here.
        if normalized_rows(schema,rows)!=normalized_rows(esp,expected): failures.append(f'{oid}: row mismatch')
    if failures: raise ValueError('verification failed: '+'; '.join(failures))


def main(argv=None):
    ap=argparse.ArgumentParser(); ap.add_argument('capsule'); ap.add_argument('--validate-only',action='store_true')
    ns=ap.parse_args(argv); root=Path(ns.capsule)
    try:
        manifest,results=execute_capsule(root,write_generated=not ns.validate_only)
        if ns.validate_only:
            print(f'PYSQL VALID: {manifest["capsule"]["id"]} relations={len(manifest.get("relations",[]))} outputs={len(manifest.get("outputs",[]))}')
        else:
            verify(root,manifest,results)
            print(f'PYSQL EXECUTION PASSED: {manifest["capsule"]["id"]}')
            print(f'PYSQL VERIFY PASSED: {len(results)} outputs')
        return 0
    except Exception as e:
        print(f'PYSQL INVALID: {e}',file=sys.stderr); return 1
if __name__=='__main__': raise SystemExit(main())
