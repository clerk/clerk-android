#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Gradle reads CLERK_INTEGRATION_TEST_PK or .keys.json. Required mode fails
# before connecting when the development key is absent or invalid.
# Do not retry live mutations automatically: preserve the first failure and
# let the account-owning test perform its bounded cleanup.
./gradlew :source:api:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.clerk.api.integration \
  -PclerkIntegrationRequired=true \
  --build-cache

# This runner can exit successfully after discovering no tests or reporting
# assumption failures. Require both expected scenarios to have passed.
python3 scripts/verify-live-integration-results.py
