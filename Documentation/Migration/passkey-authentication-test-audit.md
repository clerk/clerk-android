# Passkey authentication and automatic-entry audit

All 21 declarations and complete bodies in `PasskeyAuthenticationServiceTest.kt` were checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`, together with the old `GoogleCredentialAuthenticationService` and credential-error classifier. The obsolete service suite is retired. Its public-key success declaration was individually ignored; it is not counted as a previously passing case. Original hashes and declarations remain in [the inventory](legacy-tests.json) and [this audit's proof](evidence/passkey-authentication/proof.json).

## Reproduced presentation regression

The migrated `AuthStartViewModel` displayed automatic passkey errors only when their stage was `attemptingFirstFactor` or `attemptingSecondFactor`. This also hid API preparation failures and unexpected exceptions. Two new tests fail against that implementation: the view remains idle after `preparingFirstFactor` API failure and after an unexpected serialization failure.

The view now suppresses cancellation, unsupported capabilities and failures during credential presentation. Preparation errors and unexpected failures appear through the existing error presentation. Submission errors remain visible. This is native UI policy; the TypeScript core still determines authentication stages and request behavior. The prior and corrected unit reports are retained in the proof.

## Authentication ownership and recovery

The old service cleared `Clerk.client.signIn` by copying the native client after suppressed automatic failures. The selected future facade retains its incomplete sign-in. That state is not a successful authentication. Four `PasskeyRecoveryTest` cases execute generated Kotlin methods on packaged QuickJS after cancellation, provider failure, unavailable presentation and an invalid credential response. Each observes the failed passkey attempt, starts an email sign-in with a different server ID, and sends its email code. The exact three requests contain no passkey submission, and no session is selected. Matching shared-runtime cases verify the same sequence.

`AuthViewModelTest` verifies quiet prompt failures and successful subsequent manual entry, while preserving visible server submission errors. The new rendered `AuthViewJourneyTest` uses production `AuthView`, the packaged core and fixture HTTP to check an automatic preparation error, email entry, invalid-code feedback, retry and exactly one prebuilt finalization. This does not exercise an actual OS credential prompt.

Six `SessionPasskeyFailureTest` cases cover preparation/submission HTTP rejection, missing options, provider exceptions, malformed credentials and unsupported capabilities. They require the original user and active session to remain selected, no invented verification success, and no credential submission after an earlier failure. Rejected requests retain status, API code, message, long message, trace ID and typed parameter metadata; private provider and extra response details are omitted. Submission failure is not replayed. The selected `Session.verifyWithPasskey()` prepares first-factor verification before checking platform support, so its unsupported case requires that preparation request and zero presentations/submissions. It does not claim zero HTTP.

Native host tests also exercise actual `NoCredentialException`, `GetCredentialProviderConfigurationException` and `GetCredentialUnknownException` through AndroidX provider callbacks. The host propagates these without retry; the bridge reports a safe `host_failure`. Cancellation and missing Activity retain their separate structured codes. The old private throwable classes and message-substring classifier are removed.

## Every legacy declaration

| Old declaration | Current disposition |
| --- | --- |
| `signInWithPasskey succeeds with public key credential` | This ignored declaration is replaced by packaged sign-in success, request-option and submitted-credential checks. A complete sign-in remains unselected until explicit finalization. |
| `signInWithPasskey handles password credential` | The old private handler returned success without checking the password; its source described that branch as a demonstration. This is not retained. The actual native host rejects a password response to a passkey request with `invalid_credential_response`; the recovery case permits subsequent email sign-in. |
| `signInWithPasskey can prefer immediately available credentials` | Shared and packaged generated calls forward the preference, and the AndroidX host test inspects the actual request flag. The mock's password result is not retained as passkey success. |
| `authenticateWithPasskey uses second factor endpoints for an in-progress sign in` | The shared future-passkey success case prepares/submits the active second factor without creating another sign-in. Both second-factor API-failure cases retain the attempt and structured error. This differs from session reverification below. |
| `signInWithPasskey returns error when SignIn creation fails` | The packaged preparation failure requires the API status, code, long message and trace, with no presentation. The corrected UI displays preparation errors. |
| `signInWithPasskey handles NoCredentialException` | The actual AndroidX failure propagates without retry, then the bridge's `host_failure` replaces the old `NoSavedCredential` throwable. The automatic view remains idle and allows manual entry. |
| `automatic signInWithPasskey clears suppressed current sign in` | The native client-copy reset is removed. The generated recovery cases prove email sign-in can replace the incomplete attempt without submitting a passkey or adopting a session. |
| `automatic signInWithPasskey clears sign in on ProviderUnavailable` | Same ownership change. Actual provider-configuration failure is covered by the host; provider-failure recovery and quiet UI presentation are checked separately. |
| `automatic signInWithPasskey clears sign in on MissingActivity` | Same ownership change. Native Activity checks fail before Credential Manager; generated missing-presentation recovery and manual UI entry pass. |
| `automatic signInWithPasskey clears sign in on GetCredentialUnknownException` | Same ownership change. Actual unknown provider failure propagates without retry, and the shared failure stage permits recovery without a native client rewrite. |
| `signInWithPasskey handles cancellation without generic failure` | AndroidX cancellation maps to `user_cancelled`; generated sign-in checks no submission and no session adoption. |
| `automatic credential flow suppresses user cancellation` | The current view's cancellation case stays idle, including when a cancellation is returned during submission. |
| `automatic credential flow suppresses no saved credential` | Provider-stage `host_failure` stays idle. The old subtype is not part of the generated error API. |
| `automatic credential flow suppresses provider unavailable` | Actual provider failure and automatic presentation suppression are covered through their current owners. |
| `automatic credential flow suppresses missing activity` | `presentation_unavailable` stays idle, and the same screen can start manual sign-in. |
| `automatic credential flow suppresses unclassified credential ceremony failures` | An error at `requestingAuthorization` stays idle. The stage comes from the core; native UI does not infer authentication progress from platform exception text. |
| `automatic credential flow does not suppress non-credential failures` | This review reproduced the migrated UI's overbroad suppression. The fix and failing-before/passing-after tests preserve visible preparation and unexpected failures. |
| `signInWithPasskey fails when no activity is available` | The native host checks missing, finishing and destroyed Activities before a Credential Manager request. An unsupported core capability fails separately; loss of Activity after preparation does not imply server rollback. |
| `signInWithPasskey returns error for unknown credential type` | Native typed-response mismatch yields `invalid_credential_response`, and generated recovery works afterward. The old `ClerkResult.ErrorType.UNKNOWN` is removed. |
| `verifySessionWithPasskey returns clear error when prepared verification nonce is missing` | New shared and packaged missing-options cases require structured failure before presentation/submission, preserving the selected session. The old exception's exact text is not a public contract. |
| `verifySessionWithPasskey uses second factor endpoints when requested` | The selected TypeScript `Session.verifyWithPasskey()` has no level argument and always uses first-factor endpoints. The old native-only convenience is not restored. This remains an explicit migration capability gap, not proof that the backend disallows second-factor passkeys. |

## Remaining boundaries

Local verification passes 39 API unit cases, 522 UI unit cases, ten new packaged API cases, both rendered journeys and 37 shared-runtime cases. The 35-case authentication view-model report includes the two reproduced failures after the fix. The CI authentication runner now requires 109 exact named cases, and its separate rendered-journey runner requires both journeys. Reports, hashes and execution warnings are preserved in the proof. The [preparation error](evidence/passkey-authentication/screenshots/passkey-00-preparation-error.png), [invalid email code](evidence/passkey-authentication/screenshots/passkey-02-invalid-code.png) and [completed flow](evidence/passkey-authentication/screenshots/passkey-03-completed.png) captures were visually inspected.

`PasskeyWebAuthnRequestTest` remains pending for its relying-party fallback and credential filtering assertions. Second-factor session passkey convenience remains unavailable in the selected contract; the [migration guide](README.md) records it. Retiring the private service test does not close that capability gap. Real provider prompts, relying-party association, live authentication, released-app upgrades and device performance remain open release gates.
