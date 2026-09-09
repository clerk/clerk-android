# Android arm64 release application footprint

Measured on 2026-09-09 using the `samples/core-footprint` matched application,
Android API 36 `sdk_gphone64_arm64` emulator, AGP 9.4.0, Gradle 9.7.1, NDK
27.1.12297006, Java 17, and R8/resource shrinking. Both release APKs use the local
debug signing certificate but are not debuggable. The installed embedded package
reports `extractNativeLibs=false` and install-time dex optimization mode `verify`.

The production native sources are based on `4bb5982e`; the exact bundled
TypeScript revision is `617418e19c`. The [machine-readable report](android-arm64-release-footprint.json)
contains both APK hashes, fixture hash, core hash, ZIP breakdown and raw installed
size samples. The harness source accompanying this report defines the measured
application; it does not represent every downstream application's dependency set.

| Measurement | Baseline | Embedded core | Increase |
| --- | ---: | ---: | ---: |
| APK file bytes | 62,000 | 2,425,202 | 2,363,202 |
| Gzip APK bytes | 57,883 | 1,664,634 | 1,606,751 |
| Installed `appBytes` after first ready | 212,992 | 4,087,808 | 3,874,816 |
| App data bytes | 57,344 | 57,344 | 0 |
| Cache bytes (included in app data) | 32,768 | 32,768 | 0 |

The installed delta is 3.70 MiB, below the 8 MiB engineering budget for this
sample. `StorageStatsManager.queryStatsForUid` reports each app's own installed
app bytes; the measurement is separate from data and cache. These are fresh
installs of the two dedicated measurement applications. They are not measurements
of shared system libraries or the size of a production app upgraded in place.

The embedded JavaScript is 1,314,690 bytes and 234,247 bytes with deterministic
gzip level 9, below the 384 KiB compressed-bundle budget. SHA-256:
`85fd61ac8a7d5ca95c5be7c8bbfa17d2004eb13c280f587ebc8d24d50f9aa52c`.

The release AAR's stripped QuickJS libraries are 1,013,440 bytes (arm64-v8a),
714,072 bytes (armeabi-v7a), and 1,090,576 bytes (x86_64). All three ELF files were
checked for debug sections. Only arm64 is packaged into this measured APK.

## Execution and limits

Both applications logged ready after fresh installation. The embedded variant
executed real QuickJS, loaded the packaged core through fixture capabilities,
called generated `signIn.reset()` and verified nested-group invalidation. Its
manual live-key branch keeps the production OS host dependencies reachable by R8;
that branch was not executed. No real account or external service was involved.

APK gzip is a reproducible compression diagnostic, not a Play/App Bundle split
size estimate. The installed result is one emulator configuration. Physical-device
latency and memory, other installed ABIs, Play delivery, iOS application footprint,
and real account upgrades remain unverified. Prebuilt Compose UI assets are not
included in this API-only harness.

## Initial-run corrections

The initial baseline failed because the harness declared coroutine core without
its Android Main dispatcher. Both variants now explicitly depend on the same
Android dispatcher. This was a harness defect, not a core-authentication failure.

The initial embedded APK contained a 5,892,648-byte unstripped QuickJS library.
Gradle logged `Unable to strip the following libraries, packaging them as they
are: libclerk_quickjs.so`. ELF inspection found `.debug_loc`, `.debug_abbrev`,
`.debug_info`, `.debug_ranges`, `.debug_str` and `.debug_line`. Pinning the app's
NDK to the library's installed version restored stripping. The published AAR was
already stripped correctly; no production engine change was needed. The APK
measurement script now rejects this condition instead of reporting a misleading
release footprint. The corrected build still emits existing root Gradle
registration deprecations and unused-expression Kotlin warnings.
