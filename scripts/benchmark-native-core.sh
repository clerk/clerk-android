#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="${1:-$PWD/build/native-core-benchmark.json}"
adb_command="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/platform-tools/adb"
if [ ! -x "$adb_command" ]; then adb_command=adb; fi
./gradlew :source:api:assembleDebugAndroidTest
"$adb_command" install -r source/api/build/outputs/apk/androidTest/debug/api-debug-androidTest.apk
"$adb_command" shell run-as com.clerk.sdk.test rm -f files/clerk-core-benchmark.json
"$adb_command" shell am instrument -w -e class com.clerk.api.CoreBenchmarkTest -e clerkBenchmark true com.clerk.sdk.test/androidx.test.runner.AndroidJUnitRunner
mkdir -p "$(dirname "$output")"
"$adb_command" shell run-as com.clerk.sdk.test cat files/clerk-core-benchmark.json > "$output"
