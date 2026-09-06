#!/usr/bin/env python3
from __future__ import annotations
import json,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
K=ROOT/'sqlite-kernel'/'kernel.py'
BASE=ROOT/'capsules'/'double-entry-accounting-capsule-v1'

def run(cap): return subprocess.run(['python3',str(K),str(cap)],text=True,capture_output=True)
def copycap(td,name):
    p=Path(td)/name; shutil.copytree(BASE,p); return p

def must_fail(label,result,contains):
    text=result.stdout+result.stderr
    if result.returncode==0 or contains.lower() not in text.lower():
        raise AssertionError(f'{label}: expected failure containing {contains!r}; got rc={result.returncode}\n{text}')
    print('NEGATIVE PASS:',label)

def main():
    with tempfile.TemporaryDirectory() as td:
        # Unsupported primitive/function must fail rather than invent meaning.
        p=copycap(td,'unsupported')
        f=p/'relations'/'signed_entries.json'; d=json.load(open(f)); d['steps'][1]['columns'][0]['expression']={'function':'mystery','args':[{'value':1},{'value':2}]}; f.write_text(json.dumps(d))
        must_fail('unsupported meaning',run(p),'unsupported scalar function')

        # Verification is independent of execution.
        p=copycap(td,'wrong_expected')
        f=p/'expected'/'transaction_judgment.csv'; s=f.read_text(); f.write_text(s.replace('T100,0.00,2,0,BALANCED','T100,0,2,0,WRONG'))
        must_fail('verification mismatch',run(p),'verification failed')

        # Manifest path escape is rejected.
        p=copycap(td,'path_escape')
        f=p/'capsule.json'; d=json.load(open(f)); d['inputs'][0]['path']='../outside.csv'; f.write_text(json.dumps(d))
        must_fail('path containment',run(p),'path escapes capsule root')

    print('PYSQL NEGATIVE TESTS PASSED: 3')
if __name__=='__main__': main()
