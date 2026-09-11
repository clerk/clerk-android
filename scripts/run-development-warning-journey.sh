#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

./gradlew :source:ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.clerk.ui.core.footer.DevelopmentModeWarningTest \
  -Pandroid.testInstrumentationRunnerArguments.additionalTestOutputDir=/sdcard/Android/media/com.clerk.ui.test/additional_test_output \
  --build-cache

python3 -B scripts/android_test_results.py \
  --results source/ui/build/outputs/androidTest-results/connected/debug \
  --expected 'com.clerk.ui.core.footer.DevelopmentModeWarningTest#developmentShowsRequestedWarning' \
  --expected 'com.clerk.ui.core.footer.DevelopmentModeWarningTest#productionNeverShowsDevelopmentWarning' \
  --expected 'com.clerk.ui.core.footer.DevelopmentModeWarningTest#disabledWarningPreservesNormalBranding' \
  --expected 'com.clerk.ui.core.footer.DevelopmentModeWarningTest#environmentReloadSwitchesWarningAndRestoresBranding'

mkdir -p source/ui/build/development-warning-test-results/screenshots
cp source/ui/build/outputs/androidTest-results/connected/debug/TEST-*.xml \
  source/ui/build/development-warning-test-results/
cp -R source/ui/build/outputs/connected_android_test_additional_output/. \
  source/ui/build/development-warning-test-results/screenshots/
