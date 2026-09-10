# Credential sign-in and sign-up presentation migration

All eleven declarations and their complete bodies in baseline `SignInCreateTest.kt`, `SignUpFieldCollectionTest.kt` and `SignUpEmailVerificationStrategyTest.kt` have been reviewed and retired. Their original names and hashes remain in [legacy-tests.json](legacy-tests.json), at baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`.

The generated API exposes `signIn.passkey`, `signIn.biometricCredential`, and the sign-up verification group. Compose owns field ordering and email-link screen selection. No production implementation changes were needed for these assertions.

## Assertion dispositions

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `create with passkey strategy delegates to PasskeyService` | `SignInCredentialContractTest.passkeyForwardsOptionsAndRequiresFinalization` executes the bundled core and verifies discoverable presentation, the immediate-credential flag, decoded binary challenge and credential ID, the submitted credential, and a complete sign-in with no selected session. The old Kotlin service delegation is removed. |
| `passkey strategy preserves legacy jvm constructor and copy signatures` | Intentionally removed in this new major. `SignIn.CreateParams.Strategy.Passkey` and its JVM constructor/copy ABI are replaced by `SignInPasskeyParams` on the generated method. This is not binary compatibility. |
| `create with biometric credential strategy forwards prompt configuration` | `biometricSignInForwardsSelectionAndPrompt` enrolls through the packaged core, signs out, selects by credential ID and identifier hint, and checks the host's key ID, reason and subtitle. `promptTitle` is now `reason`. The hint selects local metadata; only the trusted-device ID goes to the sign-in endpoint. Completion leaves finalization explicit. |
| `firstFieldToCollect ignores optional missing fields` | `SignUpPresentationTest.optionalMissingFieldsDoNotBlockRequiredPassword` decodes a modified TypeScript preview snapshot into real generated resources and confirms the Compose helper chooses password. |
| `firstFieldToCollect returns null when only optional fields are missing` | `onlyOptionalMissingFieldsDoNotBlockCompletion` confirms the generated optional fields do not block progress. |
| `emailVerificationStrategyUsesActiveVerificationStrategyWhenPresent` | Both active email-code and active email-link cases verify that an existing verification takes precedence over the opposite environment preference. |
| `emailVerificationStrategyFallsBackToEnvironmentStrategiesWhenNoActiveStrategyExists` | `missingActiveStrategyUsesEnvironmentEmailLink` checks the generated environment settings and null active strategy. The old API's Boolean convenience helper is removed; the UI uses the same selected strategy for routing. |
| `prepareVerificationMapsNativeEmailLinkPkceFields` | `SignUpVerificationRequestTest.emailLinkSendsConfiguredCallbackAndMatchingPkceChallenge` checks the actual preparation endpoint, strategy, configured redirect URI, S256 method and a challenge equal to SHA-256 of the securely retained verifier. The verifier is not sent in preparation. Per-call redirect overrides are replaced by owner configuration. |
| `sendCodeUsesEmailLinkWhenEmailLinkVerificationIsSupported` | The presentation test selects the email-link strategy; `SignUpCodeViewModelTest.emailLinkRoutesToLinkScreenWithoutSendingCode` hands off without sending a code or duplicate link. The link screen owns sending, and the packaged email-link request test verifies its generated operation. The old multiplexed `sendCode` helper is removed. |
| `sendCodeFallsBackToEmailCodeWhenEmailLinkVerificationIsNotSupported` | `missingEmailLinkConfigurationUsesEmailCode`, `emailCodeCallsCurrentSignUpVerificationGroup`, and `emailCodeUsesExplicitVerificationStrategy` cover selection, Compose dispatch and the actual form request. |
| `sendCodeUsesPhoneCodeForPhoneVerification` | `phoneCodeIgnoresEmailLinkPreference` verifies Compose dispatch, and `phoneCodeUsesExplicitVerificationStrategy` checks the actual phone-code preparation request. |

## Additional checks and limits

The credential suite also verifies cancellation causes no credential submission, and server failures retain the preparation or submission stage and structured error. The presentation suite checks required-field ordering. These eight instrumentation and ten UI cases all pass without skips; reports and hashes are retained in [the proof](evidence/signin-credentials/proof.json).

`bash scripts/run-auth-entry-contract.sh` includes the eight new instrumentation cases and now requires 40 named cases across six classes. Each class runs separately with exact-name/no-skip verification and preserved XML. The new UI suites run in the existing unit-test job.

The first biometric fixture assertion incorrectly expected the local identifier hint in the HTTP request. Source inspection confirmed the core selects the stored credential using the hint and submits its ID; the corrected test preserves that contract. No runtime fix resulted. UI tests require JDK 21; the initial Java 17 run stopped at plugin resolution before tests, then the JDK 21 run passed.

These deterministic capabilities do not exercise an OS biometric/passkey prompt or a live backend. Physical-device and released-app upgrade gates remain open. This audit does not retire the separate legacy passkey-service or biometric-service suites.
