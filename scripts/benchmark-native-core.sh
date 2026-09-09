#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="${1:-$PWD/build/native-core-benchmark.json}"
adb_command="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/platform-tools/adb"
if [ ! -x "$adb_command" ]; then adb_command=adb; fi
build_type="${CLERK_BENCHMARK_BUILD_TYPE:-release}"
case "$build_type" in
  release) variant=Release ;;
  debug) variant=Debug ;;
  *) echo "CLERK_BENCHMARK_BUILD_TYPE must be release or debug" >&2; exit 1 ;;
esac
./gradlew ":source:api:assemble${variant}AndroidTest" "-PclerkTestBuildType=$build_type"
"$adb_command" install -r "source/api/build/outputs/apk/androidTest/$build_type/api-$build_type-androidTest.apk"
log=$(mktemp)
trap 'rm -f "$log"' EXIT
"$adb_command" shell am instrument -w -e class com.clerk.api.CoreBenchmarkTest -e clerkBenchmark true com.clerk.sdk.test/androidx.test.runner.AndroidJUnitRunner | tee "$log"
mkdir -p "$(dirname "$output")"
sed -n 's/^.*INSTRUMENTATION_STATUS: clerkBenchmarkReport=//p' "$log" > "$output"
if [ ! -s "$output" ]; then
  echo "Instrumentation did not return a benchmark report" >&2
  exit 1
fi
