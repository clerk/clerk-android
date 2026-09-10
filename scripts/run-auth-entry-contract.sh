#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

run_suite() {
  local suite="$1"
  shift
  local expected=()
  for scenario in "$@"; do
    expected+=(--expected "com.clerk.api.${suite}#${scenario}")
  done
  ./gradlew :source:api:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=com.clerk.api.${suite}" --build-cache
  python3 -B scripts/android_test_results.py "${expected[@]}"
  mkdir -p "source/api/build/auth-entry-test-results/${suite}"
  cp source/api/build/outputs/androidTest-results/connected/debug/TEST-*.xml \
    "source/api/build/auth-entry-test-results/${suite}/"
}

run_suite GoogleIdentityContractTest \
  existingAccountRequiresFinalization missingAccountCreatesSignupWithMetadata \
  signInOnlyDoesNotCreateAccount rejectedIdentityDoesNotOpenBrowser \
  failedSignupPreservesStructuredError emptyPickerFallsBackToBrowser \
  cancelledPickerDoesNotOpenBrowser blankIdentityTokenDoesNotAuthenticate resetIgnoresLateIdentityToken

run_suite AuthEntryPointTest \
  emailCodeUsesExplicitPreparation phoneCodeUsesExplicitPreparation \
  signupPreservesNestedMetadata unrelatedAndOAuthCallbacksAreNotRedeemedAsEmailLinks

run_suite SessionSelectionContractTest \
  selectsExistingSessionWithoutFollowupRefresh explicitSelectionOverridesStaleClientSelection \
  emptyTouchClientDoesNotRestoreSession laterEmptyRefreshClearsSelectedSession \
  forcedPersonalSelectionKeepsCurrentSession selectedSessionSignoutLeavesRemainingSessionUnselected \
  omittedOrganizationStillSelectsSessionWhenOrganizationsAreForced returnedOrganizationStateOverridesRequestedOrganization
