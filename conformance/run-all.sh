#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA="$ROOT/java-kernel"
PY="$ROOT/sqlite-kernel/kernel.py"
CAPS=(double-entry-accounting-capsule-v1 bill-of-materials-capsule-v1 self-model-capsule-v1 enterprise-cloud-gtm-capsule-v9 value-equivalence-capsule-v1)

rm -rf "$ROOT/conformance/java-generated" "$ROOT/conformance/sqlite-generated"
mkdir -p "$ROOT/conformance/java-generated" "$ROOT/conformance/sqlite-generated"

echo '===== JAVA COMPILE + UNIT/NEGATIVE TESTS ====='
cd "$JAVA"
rm -rf out; mkdir out
javac -d out $(find src -name '*.java')
TEST_COUNT=0
while IFS= read -r f; do
  cls="$(basename "$f" .java)"
  echo "JAVA TEST: $cls"
  java -cp out "$cls"
  TEST_COUNT=$((TEST_COUNT+1))
done < <(find src -name '*Test.java' -type f | sort)

echo '===== JAVA SHARED CAPSULES ====='
for cap in "${CAPS[@]}"; do
  rm -rf "$ROOT/capsules/$cap/generated"; mkdir -p "$ROOT/capsules/$cap/generated"
  java -cp out RunCapsule "$ROOT/capsules/$cap"
  mkdir -p "$ROOT/conformance/java-generated/$cap"
  cp "$ROOT/capsules/$cap/generated/"*.csv "$ROOT/conformance/java-generated/$cap/"
done

echo '===== PYTHON + SQLITE SHARED CAPSULES ====='
cd "$ROOT"
for cap in "${CAPS[@]}"; do
  rm -rf "$ROOT/capsules/$cap/generated"; mkdir -p "$ROOT/capsules/$cap/generated"
  python3 "$PY" "$ROOT/capsules/$cap"
  mkdir -p "$ROOT/conformance/sqlite-generated/$cap"
  cp "$ROOT/capsules/$cap/generated/"*.csv "$ROOT/conformance/sqlite-generated/$cap/"
done

echo '===== DIRECT CROSS-KERNEL COMPARISON ====='
COMPARE_COUNT=0
for cap in "${CAPS[@]}"; do
  python3 - "$ROOT" "$cap" <<'PY'
import json,subprocess,sys
from pathlib import Path
root=Path(sys.argv[1]); cap=sys.argv[2]; cr=root/'capsules'/cap
m=json.load(open(cr/'capsule.json'))
for o in m['outputs']:
    cmd=['python3',str(root/'conformance'/'compare.py'),str(cr/o['schema_path']),str(root/'conformance'/'java-generated'/cap/(Path(o['generated_path']).name)),str(root/'conformance'/'sqlite-generated'/cap/(Path(o['generated_path']).name))]
    subprocess.run(cmd,check=True)
    print('MATCH:',cap,o['id'])
PY
  n=$(python3 -c "import json; print(len(json.load(open('$ROOT/capsules/$cap/capsule.json'))['outputs']))")
  COMPARE_COUNT=$((COMPARE_COUNT+n))
done

echo '===== PYTHON + SQLITE NEGATIVE TESTS ====='
python3 "$ROOT/conformance/negative_tests.py"

echo '===== V1 CONFORMANCE PASSED ====='
echo "java_test_classes_passed=$TEST_COUNT"
echo "shared_capsules_passed=${#CAPS[@]}"
echo "cross_kernel_outputs_matched=$COMPARE_COUNT"
echo "pysql_negative_tests_passed=3"
