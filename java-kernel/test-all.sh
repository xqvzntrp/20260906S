#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

TEST_COUNT=0
INTEGRATION_COUNT=0

echo "===== FULL TEST SUITE ====="

echo
echo "===== COMPILE ====="
rm -rf out
mkdir -p out
javac -d out $(find src -name '*.java')

echo
echo "===== DISCOVER TEST CLASSES ====="

mapfile -t TEST_FILES < <(
    find src -name '*Test.java' -type f | sort
)

if [ "${#TEST_FILES[@]}" -eq 0 ]; then
    echo "ERROR: no *Test.java files found under src"
    exit 1
fi

for file in "${TEST_FILES[@]}"; do
    class="$(basename "$file" .java)"

    echo
    echo "===== TEST: $class ====="
    java -cp out "$class"

    TEST_COUNT=$((TEST_COUNT + 1))
done

echo
echo "===== INTEGRATION: V9 VALIDATION ====="
java -cp out ValidateCapsule enterprise-cloud-gtm-capsule-v9
INTEGRATION_COUNT=$((INTEGRATION_COUNT + 1))

echo
echo "===== INTEGRATION: V9 EXECUTION + VERIFICATION ====="
java -cp out RunCapsule enterprise-cloud-gtm-capsule-v9
INTEGRATION_COUNT=$((INTEGRATION_COUNT + 1))

echo
echo "===== FULL TEST SUITE PASSED ====="
echo "test_classes_passed=$TEST_COUNT"
echo "integration_steps_passed=$INTEGRATION_COUNT"
