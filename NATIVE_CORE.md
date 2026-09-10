# TypeScript core prerelease

The API module now builds generated Kotlin resources from `NativeCore`, using the bundled QuickJS runtime and JavaScript asset. The authentication roots derive from `SignInFutureResource` and `SignUpFutureResource`. The former native domain implementation is excluded from the API module. The complete Compose UI module now compiles against generated resources, including authentication, profile, organizations, account controls and session tasks. Quickstart, custom flows, Linear clone, prebuilt UI, workbench and the e2e app have migrated to retained generated owners; all six debug APK builds passed together on September 10. Live device UI journeys remain verification gates.

```kotlin
val clerk = Clerk.connect(
  context,
  ClerkConfiguration(publishableKey, "${context.packageName}.clerk://oauth/callback"),
  activity = { activity },
)
clerk.signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams(code))
if (clerk.signIn.status.rawValue == "complete") clerk.signIn.finalize()
```

Import `com.clerk.api.connect`. Consumers need no JavaScript toolchain. Enable core library desugaring in the Android application; the SDK retains minSdk 24 and packages arm64-v8a, armeabi-v7a and x86_64 libraries with 16 KB page support. The default manifest callback is `${applicationId}.clerk://oauth/callback`; register the same redirect with Clerk. A different callback requires a corresponding manifest intent filter. Browser and passkey presentation need the current Activity provider.

Passkeys require API 28 or later. The SDK manifest supplies the biometric permission. [Release packaging verification](Documentation/Migration/android-release-packaging.md) records the merged AAR checks, API guards, and remaining device-validation requirements.

[The rendered authentication journey](Documentation/Migration/rendered-auth-journey.md) verifies invalid-code retry and prebuilt finalization through actual Compose input and the packaged core. It includes screenshots and a repeatable test entrypoint.

Generated suspending methods publish state before completion and expose structured `CoreException` failures. A successful verification may leave additional requirements. Explicit finalization can produce a pending session; render its task. Old attempt/group handles become invalidated on reset. Cancellation does not roll back server work. Observe a resource's `changes` flow to read updated state.

One owner should be retained for the application. Process lifecycle events suspend proactive token requests in background and reload the core on foreground. `CoreRuntime.lastLifecycleError` exposes recoverable reload failures. Call `clerk.close()` when permanently discarding the owner. The [browser activity lifecycle checks](Documentation/Migration/browser-activity-lifecycle.md) cover callback delivery, cancellation, sequential authorization and rotation on the emulator. Browser continuation state is intentionally in-memory; process death requires a fresh attempt instead of replaying an unsolicited callback.

## Compose ownership

Supply the retained instance with `ClerkProvider(clerk, theme = theme) { ... }` from `com.clerk.ui.core.composition`. It observes core revisions, including changes to nested resources, and supplies that same instance to UI view models. Presentation tokens now belong to `com.clerk.ui.theme`. UI telemetry uses the generated TypeScript collector.

Authentication models call generated future methods, and `AuthView` explicitly finalizes completed attempts before presenting source-reported session tasks. Identifier fallback and Google One Tap transfer remain in the shared core. Saved navigation holds UI factor selections and form prefills, never live resource handles. Use `LocalClerk.isAuthFlowComplete` to include pending prebuilt presentation steps when choosing authenticated content.

Profile and organization actions call generated resources directly and catch structured exceptions. Organization lists translate page selections into the source pagination parameters; pending organization selection uses the core's current session. Device lists use `User.getSessions()` and `SessionWithActivities`; account deletion delegates cleanup to the source `User.delete()`. Previews decode fixtures produced by the TypeScript state serializer rather than constructing independent native domain objects.

## UI verification

Run `./gradlew :source:ui:testDebugUnitTest` with JDK 21. The migrated target retains all test files and passes 517 behavior tests across 99 suites, including assertions that execute after snapshot rendering. `:source:ui:connectedDebugAndroidTest` also verifies that removing a screen or replacing its core cancels the screen-owned model work.

`./gradlew :source:ui:verifyPaparazziDebug` currently reports 21 existing golden-image failures. An isolated, unchanged checkout of `main` at `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f` reproduces the same 21 failures; all 21 actual rendered PNG files match that baseline byte for byte. No golden files were replaced. This baseline comparison distinguishes preserved rendering from the repository's outstanding golden updates; the golden verification task itself is not green.

## Credential continuity

The secure adapter encrypts an atomic, non-backed-up file with Android Keystore AES-GCM. It imports the old encrypted shared-session snapshot only when its instance hash matches and its device-token record is set. The independent old auth/client projection may be cleared; the credential is used for canonical refresh. An explicit device-token clear still blocks import. See [token-only credential recovery](Documentation/Migration/token-only-credential-upgrade.md). The older `DEVICE_TOKEN` requires a matching `CACHED_CLERK_STATE.publishable_key`, or an explicitly matching `legacyPublishableKey` when no cached instance exists. Clear writes a durable empty record to prevent legacy reimport on restart. Missing or mismatched state starts signed out. Missing encryption keys, corruption and storage errors are surfaced; no development-key fallback is used.

Run `./gradlew :source:api:connectedDebugAndroidTest` with an emulator or device. Instrumentation executes the packaged AAR asset and JNI library, tests generated SSO/reset/finalize/token/sign-out behavior, and exercises previous-format encrypted storage migration, scoping and durable clears. It uses an isolated test application. A real old-major application upgrade with a signed-in account remains a release gate.

The opt-in `com.clerk.api.LiveStartupTest` accepts a development publishable key
through the `clerkLivePublishableKey` instrumentation argument. It exercises the
packaged engine and production HTTP adapter with memory-only credential storage,
allows only GET `/v1/environment` and `/v1/client`, and asserts a loaded, signed-out
owner. Without the argument it skips; sign-in and OS prompts are outside its scope.

## Native email links

Email-link preparation uses the configured callback URL and saves its PKCE verifier in a separate secure record scoped to the instance. The TypeScript core validates the saved record, expiration and callback before completing it. Forward incoming URLs to `clerk.handleAuthCallback`. It returns the generated sign-in or sign-up resource without activating a session; custom interfaces must explicitly finalize a complete result. The callback also remains available as `clerk.authCallback` until `clearAuthCallback(id)` consumes it. `AuthView` consumes this record and finalizes as part of its existing presentation flow.

Callback parameters may arrive in the query or fragment, with query values taking precedence. Empty and root-slash callback paths are equivalent; other paths must match the configured route.

Pending links survive process restart. The previous iOS pending-link record requires a matching `LegacyKeychainConfiguration.publishableKey`; Android uses the matching cached publishable key or explicit `legacyPublishableKey`. Clearing a pending link leaves the client credential intact. Android callers can supply `magicLinkAttestation` when their instance requires an attestation provider.

## Biometric credentials

The generated `clerk.biometricCredentials` resource owns enrollment, local selection, server validation, revocation and cleanup in TypeScript. Use its `canEnroll` state and asynchronous availability methods to drive presentation. `clerk.signIn.biometricCredential()` authenticates through the canonical future sign-in resource and leaves finalization explicit. Native hosts create device-held EC keys and return public material or challenge signatures; private keys and local key identifiers are excluded from observable snapshots.

The adapters preserve the previous Secure Enclave / Android Keystore key names and migrate scoped local metadata using the same explicit instance checks as pending email links. Biometric metadata has a separate secure-storage entry. Account deletion records unsuccessful local key cleanup for retry on restart. The system prompt still requires enrolled biometrics and an appropriate presentation host; device-passcode fallback follows the selected platform policy.

## Bundle and engine updates

From a clean JavaScript checkout run `node packages/mobile-runtime/pack.mjs IOS_REPOSITORY ANDROID_REPOSITORY`. The script rebuilds the bundle and pins the source commit, contract and bundle SHA-256. Commit generated Kotlin and assets together, review `NativeCore/public-api.txt`, and run the native tests. QuickJS-ng is vendored at v0.15.1, commit `fd0a0210b7be00957751871e7e01b8291268fc29`; preserve its license and run all ABI tests when updating. Remote executable-code updates are not supported.

## Previous native API

The [separate migration guide](Documentation/Migration/README.md) records the audited main baseline, old public declarations, call changes, unavailable features, and the legacy-test coverage audit. The old native test trees remain pending their explicit assertion-level migration.

## Performance measurements

Run `scripts/benchmark-native-core.sh OUTPUT_JSON` to collect raw fresh-engine startup and generated local-reset timings against the packaged deterministic fixture. The reset check verifies invalidation and the absence of HTTP. The first startup sample is separate from subsequent fresh engines in the same warm process. These are not cold-app or live-network timings.

The Android script defaults to a release library in the isolated instrumentation APK; set `CLERK_BENCHMARK_BUILD_TYPE=debug` for debug measurements. Set `ANDROID_SERIAL` when multiple devices are connected. The report includes sampled process PSS and native heap allocation, including the test runner and libraries. Emulator measurements do not establish compliance with release-device budgets.

Record the OS/device, build mode, core revision/hash and packaged artifact sizes with each run. The provisional release budgets and required physical-device measurement protocol are in [Documentation/Performance.md](Documentation/Performance.md).

Release performance limits and outstanding measurements are recorded in [the performance budgets](Documentation/Performance.md).

Packaging also regenerates the eight shared UI preview fixtures against the current protocol. The fixture generator uses a fixed clock and deterministic entropy; run `node scripts/generate-preview-fixtures.mjs JAVASCRIPT_REPOSITORY ANDROID_REPOSITORY` from the iOS repository to refresh them independently. The Swift proof can decode all eight with `swift run NativeCoreProof --preview-fixtures Sources/ClerkKitUI/Resources/Preview`.

Standalone owners now observe OS connectivity and recover through the shared core. See [connectivity and lifecycle recovery](Documentation/Migration/connectivity.md).
