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
  cancelledPickerDoesNotOpenBrowser blankIdentityTokenDoesNotAuthenticate resetIgnoresLateIdentityToken \
  transferableSignupReturnsToSignInWithOneIdentityPrompt signupEntryTransfersExistingAccountToSignIn \
  transferableStatusWithoutAccountExistsErrorDoesNotTransfer unsupportedNativeCredentialDoesNotAuthenticate \
  malformedNativeCredentialDoesNotAuthenticate unknownNativeCredentialFailureDoesNotFallback unavailableNativeProviderDoesNotFallback

run_suite GoogleIdentityRequestTest \
  requestsFreshNonceAndReturnsDecodedIdentityToken blankClientIdentifierFailsBeforeCredentialRequest \
  missingActivityDoesNotAdvertiseOrPresentGoogleIdentity

run_suite AuthEntryPointTest \
  emailCodeUsesExplicitPreparation phoneCodeUsesExplicitPreparation \
  signupPreservesNestedMetadata signupOmitsUnspecifiedMetadata unrelatedAndOAuthCallbacksAreNotRedeemedAsEmailLinks

run_suite SessionSelectionContractTest \
  selectsExistingSessionWithoutFollowupRefresh explicitSelectionOverridesStaleClientSelection \
  emptyTouchClientDoesNotRestoreSession laterEmptyRefreshClearsSelectedSession \
  forcedPersonalSelectionKeepsCurrentSession selectedSessionSignoutLeavesRemainingSessionUnselected \
  omittedOrganizationStillSelectsSessionWhenOrganizationsAreForced returnedOrganizationStateOverridesRequestedOrganization

run_suite SignInCredentialContractTest \
  passkeyForwardsOptionsAndRequiresFinalization cancelledPasskeyDoesNotSubmitCredential \
  passkeyPreparationPreservesFailureStage passkeySubmissionPreservesFailureStage \
  biometricSignInForwardsSelectionAndPrompt

run_suite SignUpVerificationRequestTest \
  emailLinkSendsConfiguredCallbackAndMatchingPkceChallenge emailCodeUsesExplicitVerificationStrategy \
  phoneCodeUsesExplicitVerificationStrategy

run_suite SignOutContractTest \
  successfulSignOutUsesServerClientWithoutRefresh rejectedSignOutClearsSelectionButPreservesKnownServerSessions \
  networkFailureIsNotReplayedOrReportedAsSuccess noSessionSignOutDoesNotRequestOrEraseClientCredential \
  blankBearerAuthorizationClearsStoredClientCredential scopedSignOutPreservesOtherSelectedSession \
  scopedRejectionPreservesOtherSelectedSession

run_suite BrowserSSOContractTest \
  signInUsesGoogleBrowserStrategyAndConfiguredCallback signInPreservesCustomProviderStrategy \
  signUpPreservesMetadataAndBrowserStrategy signUpPreservesCustomProviderStrategy \
  enterpriseSignInPreparesConnectionBeforeOpeningBrowser enterpriseSignInRefreshesPreparedRedirectOnExistingAttempt \
  oauthRetryDoesNotReplayStalePreparedRedirect missingRedirectLeavesAnIncompleteAttemptWithoutOpeningBrowser \
  cancelledSignInDoesNotReconcileOrActivate cancelledSignUpDoesNotReconcileOrActivate \
  unrelatedCallbackDoesNotSubmitNonce rejectedPreparationPreservesStructuredError \
  browserTransferCreatesSignUpWithMetadataWithoutSecondPrompt disabledBrowserTransferDoesNotCreateSignUp \
  signInTransportFailureDoesNotBecomeSuccess signUpTransportFailureDoesNotBecomeSuccess
