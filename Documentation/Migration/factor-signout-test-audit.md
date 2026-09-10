# Initial sign-in factors and sign-out migration

All fifteen declarations and complete test bodies in baseline `SignInExtensionsTest.kt` and `SignOutServiceTest.kt` have been reviewed and retired. Their original names and hashes remain in [legacy-tests.json](legacy-tests.json), at baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`.

The seven initial-factor assertions now run directly against the Compose helper with generated factor types and an explicit Clerk owner. Sign-out executes the shared implementation through packaged QuickJS. No production code changed for this migration.

## Assertion dispositions

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `startingFirstFactor prefers password for email identifier when password is preferred` | The same assertion in `SignInStartingFactorTest` checks password preference through the UI owner. |
| `startingFirstFactor prefers email_link for email identifier when otp is preferred` | The same assertion checks OTP preference and email-link selection. |
| `startingFirstFactor does not force email_link for non-email identifiers` | The same assertion keeps the supported passkey choice for a username. |
| `startingFirstFactor chooses matching email_link when preferred password is unavailable` | The same assertion checks both strategy and the matching email-address ID, excluding the unrelated first link. |
| `startingFirstFactor does not use unrelated email_link for username sign-ins` | The same assertion selects the matching password factor. |
| `startingFirstFactor matches prepared factor by identifier when strategies repeat` | The same assertion selects the second email-address ID for the matching identifier. |
| `startingFirstFactor uses preferred password over prepared email_code` | The same assertion preserves the preferred password in initial routing. Existing `SignInFactorOneViewTest` separately preserves explicit in-progress user choices. |
| `signOut clears device token on successful server sign-out` | The old unconditional device-token deletion is removed. `successfulSignOutUsesServerClientWithoutRefresh` verifies the shared client credential rotates to the response credential while selection and sessions clear. `blankBearerAuthorizationClearsStoredClientCredential` verifies the explicit server clear signal. |
| `signOut clears device token even when server sign-out fails` | Intentionally changed: server rejection and network failure preserve the stored client credential. Dedicated packaged cases verify this and the failed result. The failed call does not prove server-session revocation. |
| `signOut clears session and user state on successful sign-out` | `successfulSignOutUsesServerClientWithoutRefresh` checks null selected session/user, an empty generated sessions list, stale prior handles and exactly one sign-out request. |
| `signOut clears session and user state even when server sign-out fails` | `rejectedSignOutClearsSelectionButPreservesKnownServerSessions` and `networkFailureIsNotReplayedOrReportedAsSuccess` verify selected session/user clear and errors surface, but the known server-session list remains. Kotlin no longer independently empties that list. |
| `signOut succeeds when no session exists` | `noSessionSignOutDoesNotRequestOrEraseClientCredential` succeeds with no HTTP or credential mutation. |
| `signOut preserves device ID while clearing device token` | The old installation-ID API is removed. The existing real-Keystore case `plaintextLegacyImportUpdateAndClearPreserveTheOriginalPreferences` was rerun: credential import, replacement and clear preserve the original preferences, including DEVICE_ID. The packaged blank-Bearer case separately verifies sign-out invokes credential removal. These are separate host and core proofs, not a live old-app upgrade. |
| `signOut refreshes client after local sign-out cleanup` | Intentionally removed: the core consumes the sign-out response client. The success case requires one POST with `_method=DELETE` and no follow-up GET. |
| `signOut tolerates client refresh failure` | There is no sign-out follow-up refresh to fail. The same exact-request assertion checks that replacement behavior; failures of the sign-out request itself are covered separately. |

## Shared policy and fixture boundaries

The all-session path clears the selected accessors before its request. On rejection, previously known server sessions can remain in `clerk.sessions`; the call throws and existing resource handles are invalidated. Removing another session preserves the current selection. `scopedSignOutPreservesOtherSelectedSession` and `scopedRejectionPreservesOtherSelectedSession` verify this distinction, the `/remove` endpoint, retained session IDs and response credential handling.

The transport fences earlier requests when authentication resets. It does not unconditionally remove the client credential: a nonempty response authorization value replaces it, and a blank `Bearer` value removes it. An empty header alone is not the native clear signal. No native client refresh, token fetch, retry or separate sign-out policy is added.

The first test fixture placed `clerk_trace_id` in a header and used an empty authorization header as a clear instruction. Source inspection showed the trace belongs in the error body and native clearing uses blank Bearer. Corrected fixtures pass without changing the runtime. The server-error cases assert status, code, long message and trace; the network case asserts a thrown structured error and no replay without inventing a stable network error code.

## Verification

Seven packaged sign-out cases, seven UI factor cases and one real-Keystore preservation case pass without skips. Four existing shared embedded sign-out/cache-policy cases also pass. Source hashes, exact declarations and reports are retained in [the proof](evidence/factor-signout/proof.json).

`bash scripts/run-auth-entry-contract.sh` now requires 47 named cases across seven classes, including the sign-out suite. It checks every class separately and preserves its XML. The UI factor suite runs in the existing UI unit-test job. This audit leaves live backend, system presentation and released-app upgrade gates open.
