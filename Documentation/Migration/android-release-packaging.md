# Android release packaging verification

Release lint caught a missing `USE_BIOMETRIC` permission in the new manifest and three uses of AndroidX authenticator constants passed to the platform biometric prompt. The manifest now declares the permission, and the platform prompt uses platform constants. AndroidX remains responsible for the existing support query.

Passkey capability advertisement and invocation now require API 28. Both request builders reject older platforms with `capability_unavailable`, and the host rejects the operation before requesting an Activity or constructing Credential Manager. Google identity advertisement keeps its existing Activity-provider requirement.

## Repeatable release checks

Run from the Android repository with the configured JDK and Android SDK:

```sh
./gradlew :source:api:lintRelease :source:api:assembleRelease
python3 scripts/verify-native-core-aar.py source/api/build/outputs/aar/api-release.aar
```

The Android build workflow now runs both commands for the API module. The AAR verifier checks the merged minSdk 24 declaration, required permissions, exactly the three selected QuickJS libraries, their ELF classes and architectures, all load segments' 16 KB alignment, and the bundled core and notice hashes. It also requires the QuickJS license. The check examines the distributed AAR rather than assuming the source manifest or linker settings reached it.

## Local evidence, September 10, 2026

- Release lint and assembly passed: zero errors, seven warnings. Six warnings concern Kotlin URI helpers and Gradle version-catalog conventions. The remaining package-visibility warning is on callback `resolveActivity`; the current code restricts that intent to the application's own package. This change does not suppress that warning. Gradle also reports deprecated features incompatible with Gradle 10.
- [The release artifact report](../Measurements/android-release-aar.json) records the AAR hash, core revision, library hashes, and three 16,384-byte load-segment alignments for each of arm64-v8a, armeabi-v7a, and x86_64.
- The three `PasskeyRequestTest` cases passed on the Android 16 arm64 emulator, covering malformed credential IDs and actual Credential Manager registration/assertion request construction.
- A separate `PlatformCapabilityTest` run passed the supported-platform case: passkeys and Google identity are advertised, and the installed test target has `USE_BIOMETRIC` granted. The below-API-28 case is filtered out on this device; it is **not verified**. A combined class-selector run reported only the passkey cases, so the platform case was run separately and its XML result checked.
- Five deliberately corrupted temporary copies of the built AAR were rejected: missing biometric permission, missing 32-bit library, incorrect core bytes, incorrect ELF machine, and 4 KB load-segment alignment.

These checks do not establish execution on API 24, 32-bit ARM, x86_64, or a 16 KB device; final APK zip alignment; successful real biometric or passkey prompts; or signed-in upgrade and physical-device performance gates. Those remain release requirements. The platform test includes the old-API rejection case for a compatible device run.
