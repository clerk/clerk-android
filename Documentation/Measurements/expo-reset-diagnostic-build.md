# Prepared Expo native-call diagnostic — September 10, 2026

The arm64 Android Release diagnostic was rebuilt with a fresh Hermes bundle
and R8 enabled, adding an explicit authenticated-fixture assertion. TypeScript
checking and the 55-second build passed. The refreshed APK is installed on the
emulator, but it has not been launched for this measurement: desktop UI access
still reports a locked Mac. JavaScript revision is
`0b97ed0a260429dda4f230e32bb7e94fde3097f4`; Android worktree revision is
`fd7e8dfe71f405915ab3317310327eaf7d1bfa38`, whose changes since the published local
API/UI fixture revision `5ac6305b` are documentation only.

## Workload and implementation

The removable diagnostic is installed in the fixture's Expo module, without
editing SDK sources. It uses the existing projected owner and connection,
records Choreographer frame gaps and process PSS/native-heap snapshots, and
applies a 60-second coroutine timeout. The refresh also removes the Android-only
button label so the same synthetic proof app can invoke the iOS diagnostic.

The original Android-only receipt and input snapshots remain in the local
fixture. The new APK has different bytes and must be matched to this report.
Build logs retain the R8 async-parsing-provider warning, Gradle deprecation
warning and conflicting `NO_COLOR`/`FORCE_COLOR` environment warnings.

Both diagnostics require synthetic session `sess_native` and user `user_native`,
perform 10 warmups and measure 300 generated native `signIn.reset()` calls.
Every measured call requires invalidation of its old email-code group,
advancing native revision, the same native owner/connection and unchanged
session/user wrappers. The JavaScript wrapper rejects any fixture HTTP or
JavaScript-owner change. Individual timers include native-to-Hermes transport,
dispatch, native state application and continuation resumption; the outer
JavaScript diagnostic invocation is excluded from per-operation samples.

## Build identity and reproduction

The [receipt](expo-reset-diagnostic-build.json) records native, Hermes and source
hashes. Linked diagnostic source snapshots make the measured boundaries
reviewable. The actual packaged native manifest and core bytes match the current
core. `hermesc -b -dump-bytecode` recognizes the packaged JavaScript as executable
Hermes bytecode; the current contract and diagnostic call are present. Native
code contains the measurement method and authenticated/collection report keys.
This identifies the compiled diagnostic, not a runtime outcome.

APK SHA-256: `74347ee41b1e0e2dc89f599c2d18b5a1a13f803fd78c2160a6099aad4a7986c2` (28,935,912 bytes).
Hermes SHA-256: `d3b92f72ce948afab129a2182966a65026f35643b870f6032ce1e42cd15acb66` (2,699,788 bytes).

The local fixture is `work/expo-native-proof/`. Its `measurement/README.md`
describes patching, building, inspecting, installing and executing the diagnostic.
Run only after normal desktop unlocking: open the proof app, wait for its shared
native owner, tap **Measure 300 native resets**, and retain the complete
`[CLERK_EXPO_NATIVE_MEASUREMENT]` report with this artifact receipt. No measurement
samples are supplied in this build report. No phone was used for these builds
or installations.

## Limits

No startup, latency, memory or responsiveness result is claimed. Frame/timer gaps
are diagnostics rather than trace attribution. Before/after process snapshots
are neither incremental owner memory nor continuously sampled startup peaks;
the iOS kernel high-water mark can precede the workload. Same-owner assertions
are not engine-creation counts. Physical-device performance, real service and
platform prompts, shared-owner lifecycle coverage and released-app credential
continuity remain separate acceptance gates.
