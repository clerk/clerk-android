#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

./gradlew :source:api:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.clerk.api.TokenRequestContractTest \
  --build-cache

python3 -B scripts/android_test_results.py \
  --expected 'com.clerk.api.TokenRequestContractTest#rejectedTokenKeepsSession' \
  --expected 'com.clerk.api.TokenRequestContractTest#unauthorizedTokenRefreshesCurrentSession' \
  --expected 'com.clerk.api.TokenRequestContractTest#revokedTokenUsesRefreshedClientState' \
  --expected 'com.clerk.api.TokenRequestContractTest#invalidAuthenticationUsesRefreshedClientState' \
  --expected 'com.clerk.api.TokenRequestContractTest#revokedTokenClearsSessionWhenRecoveryRemovesIt' \
  --expected 'com.clerk.api.TokenRequestContractTest#exhaustedNetworkFailureThrowsAndLaterCallRecovers' \
  --expected 'com.clerk.api.TokenRequestContractTest#closingOwnerRejectsForcedTokenAndIgnoresLateCredential' \
  --expected 'com.clerk.api.TokenRequestContractTest#closingOwnerReleasesSharedTokenWaiters' \
  --expected 'com.clerk.api.TokenRequestContractTest#cacheSeparatesSessionsTemplatesAndOrganizations' \
  --expected 'com.clerk.api.TokenRequestContractTest#sharedFailureReachesBothCallersAndNextCallRecovers' \
  --expected 'com.clerk.api.TokenRequestContractTest#sharedSuccessReachesBothCallersAndRemainsCached' \
  --expected 'com.clerk.api.TokenRequestContractTest#pendingSessionUsesCurrentCoreStateAndServerOutcome'
