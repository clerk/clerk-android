# Lifecycle and connectivity assertion audit

All ten declarations and complete bodies in `source/api/src/test/java/com/clerk/api/configuration/connectivity/NetworkConnectivityMonitorTest.kt` were reviewed and hash-checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. The old private singleton suite is retired. [The proof](evidence/network-adapter/proof.json) retains its original hash and declarations, current source hashes, unit reports and packaged lifecycle results.

## Android adapter and API 24/25 recovery

`NativeCoreTests/Unit/AndroidNetworkTest.kt` exercises the production adapter through Robolectric's ConnectivityManager. Seven source methods produce thirteen cases across their selected API 24, 25 and 36 configurations. They check initial Internet/validated capabilities, offline availability, delivered capability changes, replacement-default event isolation, separate subscriptions and unregistration, and registration failure. The API module uses the same test-only Robolectric version already in the repository's catalog and UI test target.

The audit found an API 24/25 recovery defect. The adapter waited for a capabilities event after every availability event. Android only guarantees immediate capabilities delivery from API 26 onward. In the Android 7.0 and 7.1 implementations, the service's initial/default availability notification sends an availability message; the app-side dispatcher invokes only `onAvailable` for it. Capability changes are a separate event. A previously offline adapter could therefore remain ineligible for HTTP after availability returned without a capability change. See the [callback contract](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback), [Android 7.0 service](https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/services/core/java/com/android/server/ConnectivityService.java), and [Android 7.1 dispatcher](https://android.googlesource.com/platform/frameworks/base/+/android-7.1.0_r1/core/java/android/net/ConnectivityManager.java).

On API 24/25, availability now treats the incomplete observation as eligible to attempt HTTP. A later capabilities or loss event updates that eligibility. API 26 and later continue waiting for validated Internet capabilities. This does not assert server reachability or suppress HTTP errors; TypeScript still owns recovery and authentication behavior. No native polling, retry loop or synchronous query inside a network callback is added.

The same test source fails both API 24 and API 25 regression cases before the fix and passes afterward. [Before evidence](evidence/network-adapter/before/proof.json) records those two failures among thirteen cases. All 25 API unit cases pass with the change. The deprecated NetworkInfo APIs used solely to configure Robolectric's network fixture are locally suppressed in that helper; the production adapter uses Network and NetworkCapabilities.

## CI and release verification

All four jobs in [test run 34534856649](https://github.com/clerk/clerk-android/actions/runs/34534856649) passed at `e29045f95e59954df18d376a6a7a8a5ecaeb12e8`. The downloaded reports contain 25 API and 517 UI unit cases, plus two API 24 and 100 API 36 emulator cases, with no failures, errors or skips. The [CI proof](evidence/network-adapter/ci/proof.json) pins the workflow, report hashes and exact instrumentation declarations. Duplicate copies of the last instrumentation reports were checked for byte equality and counted once. All thirteen network adapter cases match the names in the before-fix report and pass in CI; their full XML is retained alongside the other API unit reports.

All three jobs in [build run 34534858716](https://github.com/clerk/clerk-android/actions/runs/34534858716) passed at runtime commit `e29045f95e59954df18d376a6a7a8a5ecaeb12e8`. Two clean builds in different paths produced byte-identical release AARs. Both downloaded artifacts independently passed the packaging verifier; [the release proof](evidence/network-adapter/release/proof.json) records their hashes and the checked workflow/report.

Comparison with the previously verified callback-receiver release at `c5a8631bc8123d7fc588f8ffb5f7dca0f6192301` found only `classes.jar` changed, and only the two compiled AndroidNetwork classes within it. The callback receiver remains exported with empty task affinity. The packaged JavaScript core remains revision `1c0d6fd280885b10b96854ec842c1e8f72067e48`, bundle SHA-256 `406af90fdb32effc6fb3c1b35b52ceea639a68ceb3874b4d1390fd958a339f7d`; QuickJS binaries and other AAR entries are unchanged. Packaging checks include the three declared ABIs and 16 KB ELF alignment, without claiming execution on a 32-bit or 16 KB-page device.

## Packaged core and lifecycle proof

`LifecycleContractTest` drives AndroidX LifecycleRegistry and the actual native runtime with the bundled QuickJS core and deterministic HTTP. All five current cases pass:

- Foreground activation refreshes available sessions while leaving session adoption explicit; resuming an already-started owner does not refresh again.
- Failed foreground reload exposes `lastLifecycleError` while the owner remains usable and can recover on a later activation.
- Offline-to-online restoration refreshes the owner; repeated online events and events after close do not add requests.
- Closing or collecting the owner removes its lifecycle/connectivity subscriptions. The collection test waits for the weak reference to clear before checking cleanup.
- Foreground recovery waits for an in-flight OAuth redemption, then performs one queued refresh without invalidating that authentication result.

The four shared `connectivity.test.mjs` cases also pass against a bundle byte-identical to the packaged asset. They verify no extra refresh from initial online state, foreground/background and offline deferral, duplicate recovery coalescing, structured invalid-event errors, disposal, and recovery after an in-flight reload fails. These current checks replace earlier historical suite totals as the evidence for this audit.

The owner-collection case originally exposed a cleanup defect: the runtime was collected but its subscription remained. The cleanup record now retains teardown actions independently and dispatches them on the owner dispatcher. Explicit close uses the same once-only cleanup. Collection timing remains nondeterministic, so explicit close is the deterministic replacement-owner operation.

## Every old assertion

| Old test declaration | Current disposition |
| --- | --- |
| `initial connectivity state is true by default` | The private singleton and its native StateFlow are removed. The core begins eligible to attempt HTTP; the shared initial-online check verifies that an unchanged online event does not add a refresh. Registration failure remains eligible rather than trapping the core offline. |
| `configure sets up connectivity monitoring and checks initial state` | `initialStateRequiresBothInternetAndValidation` checks all four capability combinations, the initial observation, one registered callback and removal. Shared initial-online evidence verifies no extra core recovery from that initial online observation. |
| `configure detects initial offline state correctly` | The modern availability and API 24/25 availability cases both require the initial no-network observation to be offline. They then exercise their respective availability rules. |
| `isCurrentlyConnected checks latest connectivity` | The synchronous private getter is removed. Current-network updates use delivered callback capabilities, as checked with an intentionally unchanged active-network snapshot. There is no public native singleton status API to poll. |
| `stop unregisters network callback and cleans up resources` | The independent-subscription test requires stopping one registration to remove only that callback. Every adapter test verifies no callbacks remain after cleanup. Packaged lifecycle tests verify owner-close/collection cleanup and no later HTTP. |
| `callback is invoked when connectivity is restored after being offline` | Adapter checks verify restoration eligibility, including the corrected API 24/25 path. Packaged and shared-core cases verify the resulting refresh, current resource state and explicit session adoption. |
| `callback is not invoked when network is lost but still has other connection` | The old monitor registered for matching networks and synchronously queried another active network in a loss callback. The current adapter observes only the default network. Its test announces a replacement default and verifies late loss/capability events for the previous one are ignored. This follows the default-callback contract; it does not retain the old synchronous requery. |
| `multiple configure calls only update callback without re-registering` | The mutable singleton reconfiguration API is removed. Separate core owners have independent subscriptions; the test requires two registrations and confirms stopping one leaves the other's updates intact. Applications should retain one owner per intended connection. |
| `resetForTesting resets all state` | The old singleton test-reset API is removed. Each test/owner controls its own subscription lifetime; no production network reset is introduced. |
| `network capabilities change triggers state update` | `currentNetworkCapabilityChangesUseTheDeliveredCapabilities` verifies loss and restoration using the capabilities supplied by the callback, rather than a possibly stale synchronous snapshot. |

The platform recommends using callback-supplied capabilities and warns against synchronous ConnectivityManager queries from callbacks because they can race. Its default callback describes the current best network, unlike a listener for every matching network. [Android callback reference](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback).

Robolectric supplies deterministic framework observations; the packaged tests inject lifecycle/connectivity signals and fixture HTTP. Neither claims real Wi-Fi transitions, live server reachability, failed-connect automatic retry, process death, shared-session synchronization or a signed-in old-major upgrade. Those remain distinct release/product checks.
