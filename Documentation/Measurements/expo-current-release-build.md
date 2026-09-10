# Current Expo release build — September 10, 2026

The existing synthetic Expo proof app was refreshed to JavaScript revision
`0b97ed0a260429dda4f230e32bb7e94fde3097f4` and Android implementation revision
`5ac6305b4a29bc6ba72896613790741af8c00ffe`. The [receipt](expo-current-release-build.json)
records the local tarballs, AARs, fixture inputs, installed attached module,
Hermes bytecode and APK hashes. No package was published to a remote registry.

The app uses Expo 57.0.21, React Native 0.86.0 and the configured R8 9.1.43
override. The arm64-only release build runs R8 and packages Hermes bytecode.
The API AAR's actual bundle hash matches its manifest. The final APK's native
manifest and Hermes bytecode both contain current contract
`0f8387f260072ba6f894442b73d50afa6bda5f1205ed3c9209f5d6b1cfba16ce`;
the previous contract is absent from the JavaScript bytecode. The final APK is
28,931,940 bytes and was installed over the existing proof app on the emulator.
It was not launched or interacted with in this run.

## Rebuild procedure and discovered cache issue

Build `@clerk/shared`, then `@clerk/clerk-js`, then `@clerk/expo` before packing.
Expo's declaration build depends on Clerk's completed declarations; the first
attempt overlapped them and failed with a missing `Clerk` export while the
Clerk output was being regenerated. The sequential rerun passed without a
source-code change.

Publish the current Android API/UI artifacts only to the local Maven fixture
repository, then install the new Clerk tarballs in the proof app. Its Expo/RN
versions remained unchanged. The local install reports an existing React type
peer mismatch (`@types/react` 19.1.17 versus RN's ^19.2.0 peer) and ignored optional
build scripts. No dependency-script approval was introduced.

A successful initial Gradle release build was insufficient: its
`createBundleReleaseJsAndAssets` task was `UP-TO-DATE`, preserving the old
`d0e44f4d…` JavaScript contract even though the APK contained the new native
AAR/contract. This occurred after replacing local same-version tarballs; it is
recorded as a local proof-app rebuild problem, not an established production
React Native bug. The stale bytecode was 2,692,892 bytes. Its APK hash was not
captured before replacement.

Force the bundle task when refreshing these local packages:

```sh
./gradlew :app:createBundleReleaseJsAndAssets --rerun :app:assembleRelease \
  -PreactNativeArchitectures=arm64-v8a \
  -Pandroid.enableMinifyInReleaseBuilds=true
```

The forced task executed and the release build passed. The final bytecode is
2,695,816 bytes. Both builds had no Kotlin metadata rewrite errors; existing
R8 async-parsing-provider and Gradle deprecation warnings remain. Verify the
packaged contracts and bytes after rebuilding, rather than relying on task exit
status or the installed node_modules alone.

## Evidence limits

This is current-artifact compilation and packaging evidence. It does not measure
Hermes operation latency, process memory, UI stalls or engine ownership at
runtime. The APK includes Expo and native UI; its total size is not an incremental
SDK download estimate. The app injects synthetic service responses and user
state, so it cannot prove real account acceptance or a released-app upgrade.

The earlier iOS shared-owner interaction recording used older pinned artifacts.
The Android interactive journey remains pending on Mac access. Physical-device
performance and upgrade gates remain separate; the emulator install is not a
physical-device run.
