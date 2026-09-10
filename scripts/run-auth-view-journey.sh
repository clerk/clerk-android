#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# A rendered production AuthView backed by packaged QuickJS and fixture HTTP.
# AGP copies screenshots from this test-owned directory before uninstalling.
./gradlew :source:ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.clerk.ui.auth.AuthViewJourneyTest \
  -Pandroid.testInstrumentationRunnerArguments.additionalTestOutputDir=/sdcard/Android/media/com.clerk.ui.test/additional_test_output \
  --build-cache

python3 -B scripts/android_test_results.py \
  --results source/ui/build/outputs/androidTest-results/connected/debug \
  --expected 'com.clerk.ui.auth.AuthViewJourneyTest#emailCodeErrorCanBeRetriedAndPrebuiltFlowFinalizesExactlyOnce' \
  --expected 'com.clerk.ui.auth.AuthViewJourneyTest#automaticPasskeyPreparationFailureIsVisibleAndEmailSignInCompletes' \
  --expected 'com.clerk.ui.auth.AuthViewJourneyTest#disabledPasskeyDoesNotPrepareAndEmailSignInCompletes'
