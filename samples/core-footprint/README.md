# Matched Android core footprint harness

This application has two locally signed, R8-optimized release variants with the
same Activity, Android coroutine dependency, fixture asset and compiler settings:

- `baseline`: no Clerk dependency.
- `embedded`: the generated Clerk API, bundled JavaScript, QuickJS and native
  platform dependencies. It connects through fixture HTTP/storage, executes a
  generated local reset and checks that the old method group is invalidated.

The embedded variant retains an explicit manual `publishableKey` intent branch
that calls the normal production connection factory. This keeps the native
storage, browser, credential and biometric host paths reachable during shrinking.
Automated runs do not provide that extra and cannot contact an external service.
The harness contains no prebuilt Compose UI; its measurements concern the API SDK.

Build the pair for one ABI (the default is `arm64-v8a`):

```sh
./gradlew :samples:core-footprint:assembleBaselineRelease \
  :samples:core-footprint:assembleEmbeddedRelease -PfootprintAbi=arm64-v8a
python3 scripts/measure-native-core-apk.py \
  samples/core-footprint/build/outputs/apk/baseline/release/core-footprint-baseline-release.apk \
  samples/core-footprint/build/outputs/apk/embedded/release/core-footprint-embedded-release.apk \
  build/core-footprint-arm64.json --abi arm64-v8a
```

Both release variants use the local debug signing certificate solely to allow
installation. They are not debuggable. Install the APKs onto the chosen test
device and launch `com.clerk.footprint.MainActivity`. The application IDs are
`com.clerk.footprint.baseline` and `com.clerk.footprint.embedded`; each logs a
`ClerkCoreFootprint` ready message only after its startup check succeeds. On
API 26+, each then logs its own UID's `StorageStatsManager` app/data/cache byte
counts. Capture exactly one fresh launch per variant and pass that log to the
script with `--installed-log path/to/log.txt`. Use a fresh installation and keep
the APK hashes with the report. Do not mix logs from earlier runs.

The report validates the expected native ABI and matching fixture bytes, records
APK and bundle hashes, rejects native ELF debug sections, and separates DEX, native libraries, core and other ZIP
entries. It checks the compressed JavaScript bundle budget and, when a fresh runtime log
is supplied, compares the observed installed app-byte delta to the installed-size
budget. One emulator sample does not establish all-device acceptance. Whole-APK gzip
is a transfer diagnostic, not an App Bundle/Play delivery estimate. Expanded ZIP
bytes are not installed size. This harness does not establish physical-device
startup, memory, prompt, or upgrade acceptance.

The pinned NDK is also needed for app-level stripping when consuming the SDK as
a Gradle project. A missing compatible strip tool can cause Gradle to package
its unstripped intermediate library with a warning. The published release AAR
is separately stripped by the library build.
