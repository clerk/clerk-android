# Migrating Clerk Android to the TypeScript core

This is a new major API. The audited baseline is `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f` from `clerk/clerk-android`. The API module builds generated Kotlin resources and the QuickJS host. Compose presentation remains native and consumes the same generated resource model.

[The source inventory](legacy-api.json) records all 160 old API Kotlin files, their hashes, and declared externally visible syntax, including default-public declarations, constructor properties, and extension functions. It excludes inherited/compiler-synthesized members and is not an ABI report. [The test inventory](legacy-tests.json) lists all 105 old JVM test/helper files with source declarations and audit categories. Counts do not establish behavioral coverage.

## Create and retain an owner

Replace `Clerk.initialize(...)`, the global `Clerk` object, and `Clerk.switchConfiguration(...)` with an application-owned connected instance. The function is suspending and throws on startup failure.

```kotlin
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfiguration
import com.clerk.api.connect

val clerk = Clerk.connect(
  context,
  ClerkConfiguration(publishableKey, "${context.packageName}.clerk://oauth/callback"),
  activity = { currentActivity },
)
```

Supply that same owner to `ClerkProvider(clerk) { ... }` from `com.clerk.ui.core.composition`. UI theme types now live in `com.clerk.ui.theme`; do not put a second domain owner in the API module to retain the old imports. Enable core library desugaring in the application. The default callback route is declared by the SDK manifest; custom routes need their own matching manifest configuration.

The Activity provider must return the current usable Activity for platform prompts. Retain the owner across recompositions and Activity recreation. Close and replace it when changing instances. `clerk.close()` disposes the runtime and invalidates handles; it does not sign the account out.

## Authentication call map

Builders and static resource factories become generated parameter objects on the retained owner's resources. Future authentication methods suspend, apply observable state, and return `Unit` or throw. Do not wrap them in the old `ClerkResult` pipeline.

| Previous API | Generated API / migration |
| --- | --- |
| `Clerk.auth.signIn { ... }`, `SignIn.create(...)` | `clerk.signIn.create(SignInCreateParams(...))` |
| `auth.signInWithPassword`, `SignIn.verifyWithPassword` | `clerk.signIn.password(SignInPasswordParams...)`; select the generated union case for the supplied identifier fields |
| `auth.signInWithOtp`, `SignIn.sendCode`, `sendEmailCode`, `sendPhoneCode` | Initialize the attempt if needed, then call the selected `signIn.emailCode.sendCode(...)` / `phoneCode.sendCode(...)` |
| `SignIn.verifyCode`, `attemptFirstFactor(EmailCode/PhoneCode)` | `signIn.emailCode.verifyCode(...)` / `phoneCode.verifyCode(...)` |
| `SignIn.prepareSecondFactor`, `sendMfaEmailCode`, `sendMfaPhoneCode` | The selected `signIn.mfa.sendEmailCode(...)` / `sendPhoneCode(...)` |
| `SignIn.verifyMfaCode`, `attemptSecondFactor(...)` | The selected `signIn.mfa.verifyEmailCode`, `verifyPhoneCode`, `verifyTOTP`, or `verifyBackupCode` |
| `SignIn.sendResetPasswordCode`, `resetPassword` | Selected `resetPasswordEmailCode` / `resetPasswordPhoneCode` group: `sendCode`, `verifyCode`, `submitPassword` |
| `auth.signInWithOAuth`, `SignIn.authenticateWithRedirect`, `authenticateWithPreparedRedirect` | `signIn.sso(...)`; use `clerk.authenticateWithSSO(...)` for the shared transfer-aware entry flow |
| `auth.signInWithEnterpriseSso` | `signIn.sso(...)` with `enterprise_sso`; core-owned preparation, browser callback, and nonce reconciliation |
| `SignIn.authenticateWithGoogleOneTap`, `authenticateWithGoogleCredential` | `clerk.authenticateWithSSO(...)` with `preferGoogleOneTap = true`; the core owns empty-picker fallback and transfer behavior |
| `auth.signInWithIdToken` | `signIn.create(...)` with the matching strategy and token |
| `auth.signInWithPasskey`, `SignIn.authenticateWithPasskey` | `signIn.passkey(...)`; native Credential Manager owns the prompt |
| `SignIn.verifyWithPasskey(rawCredential)` | The future passkey operation owns preparation and credential submission; do not submit serialized system credentials through an old resource method |
| `auth.signInWithBiometrics` | `signIn.biometricCredential(...)` |
| `auth.signInWithTicket` | `signIn.ticket(...)` |
| `auth.signUp { ... }`, `SignUp.create(...)` | `signUp.create(SignUpCreateParams(...))` |
| `SignUp.update` | `signUp.update(...)` |
| `SignUp.sendCode`, `prepareVerification`, `sendEmailCode`, `sendPhoneCode` | Selected `signUp.verifications.sendEmailCode()` / `sendPhoneCode()` |
| `SignUp.verifyCode`, `attemptVerification` | Selected `signUp.verifications.verifyEmailCode(...)` / `verifyPhoneCode(...)` |
| `auth.startEmailLinkSignIn`, `SignIn.sendEmailLink` | `signIn.emailLink.sendLink(...)` |
| `SignUp.sendEmailLink` | `signUp.verifications.sendEmailLink(...)` |
| `auth.handleMagicLinkDeepLink`, `completeMagicLink`, `handle` | `clerk.handleAuthCallback(URI)`; returns an optional generated sign-in/sign-up union without implicit finalization |
| `auth.signUpWithOAuth`, `signUpWithEnterpriseSso`, `SignUp.authenticateWithRedirect` | `signUp.sso(...)` |
| `auth.signUpWithGoogleOneTap`, `SignUp.authenticateWithGoogleOneTap` | `clerk.authenticateWithSSO(...)` with the appropriate start mode and Google picker preference |
| `auth.signUpWithIdToken` | `signUp.create(...)` with strategy and token |
| `auth.signUpWithTicket` | `signUp.ticket(...)` |
| `auth.setActive` | `clerk.setActive(...)` for explicit existing-session/organization selection |
| `auth.signOut` | `clerk.signOut(...)` |
| `auth.getToken`, `Session.fetchToken` | `clerk.session?.getToken(...)`; returns a nullable token string rather than `ClerkResult<TokenResource,...>` |
| `auth.revokeSession`, `Session.revoke` | Find the generated `SessionWithActivities` in `user.getSessions()` and call `revoke()` |
| `Session.delete` | `session.remove()` for a client session; distinct from account-session revocation |

Token failures throw `CoreException`; they are not converted to null. `GetTokenOptions` supports `template`, `skipCache` and `organizationId`; the old `expirationBuffer` option is removed because the shared cache owns expiration. After a 401, the refreshed client determines session selection. Pending sessions retain their tasks and use the canonical method/server outcome instead of the old Kotlin null shortcut. See the [token request migration](token-request-test-audit.md) for executed examples and intentional behavior changes.

Browser SSO uses the callback configured on the retained owner. There is no public method for replaying a prepared verification URL: OAuth retries replace a pending redirect, and enterprise SSO refreshes preparation on the current attempt. A resolved SSO call can still have remaining requirements. See [browser SSO behavior and migration evidence](browser-sso-test-audit.md).

Sign-out consumes the server response without a follow-up client refresh. An all-session failure clears the selected session but can leave known server sessions in `clerk.sessions`; it throws and does not prove server revocation. The client credential is retained or rotated unless the server explicitly clears it. Scoped removal of another session preserves the selected one. See [sign-out behavior and migration evidence](factor-signout-test-audit.md).

Custom flow completion remains explicit:

```kotlin
try {
  clerk.signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams(code))
  if (clerk.signIn.status == SignInStatus.Complete) {
    clerk.signIn.finalize()
  }
} catch (error: CoreException) {
  // Inspect structured error fields; never log the submitted code or password.
}
```

After finalization, inspect the session status and current task. Compose `AuthView` invokes the same generated finalization and presents pending tasks. A resolved verification call can leave an outstanding requirement. Let coroutine cancellation propagate; it does not undo server changes.

## Observation, resources and value types

| Previous surface | New surface / semantic change |
| --- | --- |
| `Clerk.userFlow`, `sessionFlow`, `clientFlow` | Observe the retained generated owner's `changes` and read its typed `user`, `session`, `sessions`, or resource state; Compose `ClerkProvider` handles revisions |
| `data class` resources, `copy()`, public backend-JSON constructors | Owner-bound generated resource references with identity, state and invalidation; do not reconstruct an authentication resource from a saved snapshot |
| Retaining a `SignIn` across reset | Reacquire `clerk.signIn`; old attempts and groups are invalidated |
| Static `User.get...`, `Organization.create/get`, `EmailAddress.create`, `PhoneNumber.create`, `Passkey.create` | Use the current generated `user`, `clerk.createOrganization/getOrganization`, `user.createEmailAddress/createPhoneNumber/createPasskey` |
| `User.get`, `SignIn.get/reload`, `SignUp.get` | `user.reload()` where exposed; authentication lifetime/refresh belongs to the future facade, with no public rotating-token nonce argument |
| `Organization.getOrganizationMemberships`, `createMembership`, `createInvitation`, `bulkCreateInvitations` | `getMemberships`, `addMember`, `inviteMember`, `inviteMembers` using generated params |
| Organization limit/offset queries | Convert explicitly to the generated query's pagination parameters; do not assume old result container shapes |
| `Organization.updateLogo`, `User.setProfileImage(File)` | `setLogo` / `setProfileImage` using a typed upload value with filename, content type and bytes |
| `deleteLogo`, `deleteProfileImage` | The corresponding setter with an explicit null file |
| `Organization.delete` | `organization.destroy()` |
| `OrganizationDomain.sendEmailCode/verifyCode` | `prepareAffiliationVerification(...)` / `attemptAffiliationVerification(...)` |
| `OrganizationMembership.updateMembership/delete` | `membership.update(...)` / `membership.destroy()` |
| `EmailAddress.sendCode/verifyCode/delete` | `prepareVerification(...)` / `attemptVerification(...)` / `destroy()` |
| `PhoneNumber.sendCode/verifyCode/delete` | `prepareVerification(...)` / `attemptVerification(...)` / `destroy()` |
| `ExternalAccount.delete` | `externalAccount.destroy()`; `reauthorize(...)` owns a complete native browser round trip |
| `Passkey.attemptVerification` | `user.createPasskey()` owns registration and platform verification |
| `User.createTotp`, `attemptTotpVerification`, `disableTotp`, `createBackupCodes` | `createTOTP`, `verifyTOTP`, `disableTOTP`, `createBackupCode` |
| `User.deletePassword` | `user.removePassword(...)` |
| `User.activeSessions/allSessions` | `user.getSessions()` returns generated `SessionWithActivities` records; client sessions remain on `clerk.sessions` |
| Session factor convenience helpers | `startVerification`, `prepareFirstFactorVerification`, `attemptFirstFactorVerification`, and second-factor equivalents; `verifyWithPasskey` handles the native prompt |
| `BiometricCredentials` singleton | `clerk.biometricCredentials`; local keys remain in Android Keystore while credential policy/state lives in TypeScript |
| `ClerkResult` / Retrofit call adapters / `fold` extensions | Suspending generated calls and structured `CoreException`; transport and returned domain errors retain distinct kinds |
| Backend `Long` timestamps | Generated `Instant` values where declared by TypeScript |
| Closed Kotlin enums / custom unknown sentinels | Generated raw-value types preserve unknown outputs; input codecs validate declared values |
| Nullable mutation fields | Ordinary nullable values when omission suffices; `Field<T>` when omission, null and a concrete value are separate states |

For methods retaining their names, inspect `NativeCore/public-api.txt`: parameter objects, result unions, capitalization and pagination can still change. Kotlin types now consistently belong to `com.clerk.api`. Theme/presentation utilities belong to the UI module. Resource method completions apply state before returning or throwing; observing a copied old DTO does not reproduce this contract.

Session selection follows the shared core. Signing out the selected session leaves other sessions available but unselected. An empty server client clears selection; the old native wrapper's session-restoration fallback is removed. `setActive` with an omitted organization selects the session without requesting an organization change; `Field.Null` explicitly requests a personal workspace and remains a no-op when organization selection is forced. Returned organization state is not overwritten with the requested ID. See the [authentication entry-point audit](auth-entry-test-audit.md) for exact old/new behavior and execution evidence.

## Credential continuity and unavailable surfaces

The new secure store uses an app-private non-backed-up encrypted record. It imports only matching prior-format identities and preserves durable clears. The previous cached publishable key or explicit `legacyPublishableKey` establishes instance scope; a token from another instance is not adopted. See [credential continuity](../../NATIVE_CORE.md#credential-continuity).

The selected prerelease profile does **not** expose the old hosted-portal authentication protocol; live shared-session ContentProvider synchronization; direct public device-token replacement; proxy/custom-header configuration; mutable global reinitialization; or persistent offline resource-cache bootstrap. The separate native `ClerkLog` and public JWT-decoding helpers are not compatibility APIs. The old installation-ID cache and automatic `x-native-device-id` injection are also removed; saved DEVICE_ID values are not bearer credentials and are left untouched during import. Saved-credential import does not prove any of those live behaviors. Browser OAuth/enterprise SSO through future `sso` is a different contract from hosted authentication.

Apps depending on these surfaces must remain on the previous major until their migration is explicitly supported. Generated signatures do not prove execution support for every optional capability. The source inventory makes removed types and methods reviewable; it is not a claim that previous-major support can end.

## Test audit and release gates

[The test audit](test-audit.md) assigns each old test category a replacement owner and names unresolved proofs. Preserve Compose presentation assertions. Run the current native JVM tests, packaged QuickJS instrumentation, and migrated UI suite separately; removed Retrofit/DTO tests are not evidence that the new resource behavior passed.

Before general release, validate a signed-in old-major app upgraded in place; real browser/passkey/biometric prompts; minSdk and all packaged ABIs; both directions of Expo/native state changes; and startup, memory, artifact size and call overhead against agreed budgets. Publish a supported-profile decision and bounded previous-major maintenance policy with the prerelease.

[Android release packaging verification](android-release-packaging.md) now checks merged permissions, all three native libraries, ELF alignment, and core asset integrity in CI. Passkeys require API 28. Device execution across the supported platform matrix remains open.

## Removed implementation

The replaced native domain source tree has been deleted from this major. The linked baseline and hashed source inventory preserve its public declarations for migration review. The generated `NativeCore` target is the implementation; no old native authentication fallback is packaged.

Of the 105 old JVM test/helper files, three have been migrated, 46 have been retired after assertion review, and 56 remain for the unfinished audit. The [test audit](test-audit.md) links the replacement evidence and intentional API changes. Retained files are not claimed to run against the generated API.

For Expo consumers, see the [D8/R8 compiler compatibility proof](../Measurements/expo-compiler-build.md). Kotlin source compiler compatibility alone does not establish that the app can process the SDK's Kotlin metadata.

The [user resource results](user-resource-results.md) record profile clearing, image deletion receipts, metadata replacement, collection filters and explicit enrollment results with packaged QuickJS evidence.

The [biometric persistence audit](biometric-persistence.md) records raw metadata preservation, key replacement, nonfatal cleanup and initiating-session rollback with generated QuickJS evidence.

The [Apple SSO verification errors](apple-auth-errors.md) records generated Apple transfer and restriction behavior, the shared verification-error serializer correction, and packaged-engine evidence.

The [email-link completion audit](email-link-completion.md) records legacy flow-kind migration, incomplete callback results, cleanup/ownership checks, and generated authentication parameter coverage.

The [token-only credential upgrade](token-only-credential-upgrade.md) preserves prior credentials awaiting canonical refresh while retaining explicit-clear and instance-scoping protections.

[Compose completion-gate verification](auth-presentation-test-audit.md) records active-user registration, pending-task completion, and prebuilt generated finalization checks.
