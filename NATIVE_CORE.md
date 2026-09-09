# TypeScript core prerelease

The API module now builds generated Kotlin resources from `NativeCore`, using the bundled QuickJS runtime and JavaScript asset. The authentication roots derive from `SignInFutureResource` and `SignUpFutureResource`. The former native domain implementation is excluded from the API module; Compose migration follows separately.

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

Generated suspending methods publish state before completion and expose structured `CoreException` failures. A successful verification may leave additional requirements. Explicit finalization can produce a pending session; render its task. Old attempt/group handles become invalidated on reset. Cancellation does not roll back server work. Observe a resource's `changes` flow to read updated state.

One owner should be retained for the application. Process lifecycle events suspend proactive token requests in background and reload the core on foreground. `CoreRuntime.lastLifecycleError` exposes recoverable reload failures. Call `clerk.close()` when permanently discarding the owner. Browser continuation state is intentionally in-memory; process death requires a fresh attempt instead of replaying an unsolicited callback.

## Credential continuity

The secure adapter encrypts an atomic, non-backed-up file with Android Keystore AES-GCM. It imports the old encrypted shared-session snapshot only when its instance hash matches and its auth/device identity is not cleared. The older `DEVICE_TOKEN` requires a matching `CACHED_CLERK_STATE.publishable_key`, or an explicitly matching `legacyPublishableKey` when no cached instance exists. Clear writes a durable empty record to prevent legacy reimport on restart. Missing or mismatched state starts signed out. Missing encryption keys, corruption and storage errors are surfaced; no development-key fallback is used.

Run `./gradlew :source:api:connectedDebugAndroidTest` with an emulator or device. Instrumentation executes the packaged AAR asset and JNI library, tests generated SSO/reset/finalize/token/sign-out behavior, and exercises previous-format encrypted storage migration, scoping and durable clears. It uses an isolated test application. A real old-major application upgrade with a signed-in account remains a release gate.

## Bundle and engine updates

From a clean JavaScript checkout run `node packages/mobile-runtime/pack.mjs IOS_REPOSITORY ANDROID_REPOSITORY`. The script rebuilds the bundle and pins the source commit, contract and bundle SHA-256. Commit generated Kotlin and assets together, review `NativeCore/public-api.txt`, and run the native tests. QuickJS-ng is vendored at v0.15.1, commit `fd0a0210b7be00957751871e7e01b8291268fc29`; preserve its license and run all ABI tests when updating. Remote executable-code updates are not supported.
