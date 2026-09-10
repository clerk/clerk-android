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

## Platform execution gate

The `packaged-core-platforms` job in the Android test workflow runs independently of live-service keys on API 24 and API 36 x86_64 Google APIs emulators. Both images were confirmed in [Google's SDK repository](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml). The job checks the actual emulator API level before running two probes separately:

1. `PackagedCoreTest.generatedApiUsesPackagedCore`: the actual JNI runtime and packaged JavaScript execute generated authentication, callback, reset, stale-group, finalization, token, resource and sign-out operations against deterministic host fixtures. Fixture browser and biometric results do not exercise OS prompts.
2. `PlatformCapabilityTest`: API 24 must reject passkeys before presentation; API 36 must advertise passkeys and have the biometric permission granted in the installed target.

Each probe requires the exact expected named XML case with no failure, error or skip. Missing, duplicate, unexpected and malformed results fail the shared checker. The core probe's report is copied before the second instrumentation run replaces the output directory. CI retains both reports. The live integration checker uses the same strict case verification.

The equivalent two probes passed separately on the available API 36 arm64 emulator, and their XML passed the new checker. Five result-checker unit tests passed, including rejection variants for absent, incomplete, duplicate, unexpected, failed, errored, skipped and malformed reports. `actionlint` passed for the changed build/test workflows with the repository's custom Blacksmith label declared; ShellCheck was unavailable. The two probes also passed on both API 24 and API 36 x86_64 CI emulators; the retained evidence is described below. These probes use debug instrumentation and do not replace release-mode device validation.

## Local evidence, September 10, 2026

- Release lint and assembly passed: zero errors, seven warnings. Six warnings concern Kotlin URI helpers and Gradle version-catalog conventions. The remaining package-visibility warning is on callback `resolveActivity`; the current code restricts that intent to the application's own package. This change does not suppress that warning. Gradle also reports deprecated features incompatible with Gradle 10.
- [The release artifact report](../Measurements/android-release-aar.json) records the AAR hash, core revision, library hashes, and three 16,384-byte load-segment alignments for each of arm64-v8a, armeabi-v7a, and x86_64.
- The three `PasskeyRequestTest` cases passed on the Android 16 arm64 emulator, covering malformed credential IDs and actual Credential Manager registration/assertion request construction.
- A separate `PlatformCapabilityTest` run passed the supported-platform case: passkeys and Google identity are advertised, and the installed test target has `USE_BIOMETRIC` granted. The below-API-28 case is filtered out on this local device; the separate API 24 CI run verifies it. A combined class-selector run reported only the passkey cases, so the platform case was run separately and its XML result checked.
- Five deliberately corrupted temporary copies of the built AAR were rejected: missing biometric permission, missing 32-bit library, incorrect core bytes, incorrect ELF machine, and 4 KB load-segment alignment.

These checks do not establish execution on 32-bit ARM or a 16 KB device; final APK zip alignment; successful real biometric or passkey prompts; or signed-in upgrade and physical-device performance gates. Those remain release requirements. The API 24 result establishes the tested old-API rejection path, not full API coverage on that OS.

## CI evidence, September 10, 2026

[The build workflow](https://github.com/clerk/clerk-android/actions/runs/34503505287) passed all jobs at native revision `ee3b7edbcdd090eaa41e33d107770f0a77b3ac66`, including API release lint, assembly and AAR verification. [The Linux artifact report](evidence/android-ci-20260910/release-aar-report.json) records the verified bundle and library hashes. The Linux and macOS builds embed identical core bytes but have different native library hashes; these results do not establish cross-host bit reproducibility.

[The initial test workflow](https://github.com/clerk/clerk-android/actions/runs/34503501613) passed the API JVM job and both platform jobs. Each platform ran its packaged-core and capability probes separately. API 36 additionally passed the rendered authentication journey. [The proof record](evidence/android-ci-20260910/proof.json) retains the exact five XML cases, artifact hashes and individual job URLs. The overall initial workflow failed one UI JVM test whose stale mock expected fractional page `1.5`; [the pagination audit](organization-list-pagination.md) explains the correction without changing production behavior.

[The corrected test workflow](https://github.com/clerk/clerk-android/actions/runs/34504818112) passed all four jobs at revision `9e90a8b22650a758248c157345cd0a8a0c1c46e0`: 12 API JVM cases, 491 UI JVM cases, and all five named instrumentation probes. Downloaded XML reports contain no failures, errors or skips. The proof record includes the corrected workflow, report hashes and exact pagination and instrumentation XML. The release build above tested the parent revision; the intervening change only corrected a test and its documentation.
