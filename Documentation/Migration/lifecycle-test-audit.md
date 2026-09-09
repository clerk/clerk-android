# Lifecycle and connectivity assertion audit

This audit reviews all ten assertions/tests in the old `configuration/connectivity/NetworkConnectivityMonitorTest.kt` and tests the new lifecycle adapter through the packaged QuickJS core. It does not retire the old connectivity tests or claim complete feature equivalence.

## Current lifecycle proof

`NativeCoreTests/Android/LifecycleContractTest.kt` drives a real AndroidX `LifecycleRegistry` through CREATED/STARTED/RESUMED/DESTROYED. The production lifecycle adapter receives that owner instead of the process owner; its default remains `ProcessLifecycleOwner`. Requests execute the packaged core, and tests assert generated resources:

- STARTED refreshes available sessions; RESUMED does not trigger another foreground reload.
- Refreshing the session list leaves session adoption explicit. A listed session does not silently become the active session.
- Closing the runtime removes the observer; later lifecycle transitions cause no further client reads.
- A failed foreground reload exposes `lastLifecycleError` while the runtime remains available. A generated reset succeeds and a later activation can recover the available-session list.

Both tests pass on an Android 16 emulator. These are synthetic lifecycle events with fixture HTTP, not proof of real process suspension/death, network reconnection, or a signed-in app upgrade. The separate JavaScript `Session.test.ts` and `tokenCache.test.ts` run passes 166 tests, including native background suppression and subsequent foreground token recovery. No Kotlin polling/backoff implementation is recreated.

## Old connectivity assertions

| Old assertion | Current disposition |
| --- | --- |
| Initial state is online; reset restores online | The removed private connectivity singleton/test reset is not retained. The embedded core treats unknown connectivity as eligible to attempt HTTP. |
| Configure registers a network callback, checks initial capabilities and does not call recovery initially | There is no independent native connectivity subscription in the current profile. Lifecycle wiring is tested separately; a lifecycle event is not a network restoration event. |
| Configure detects initial offline; querying connectivity uses current active network | No exposed native connectivity status API is retained. HTTP failures flow through the shared core. The HTTP contract suite checks failures, but does not prove offline preflight detection. |
| Stop unregisters callback | The old connectivity callback no longer exists. The new lifecycle observer is removed on explicit runtime close, checked through absence of subsequent HTTP. |
| Offline-to-online invokes recovery callback | Automatic recovery triggered only by connectivity restoration is not implemented in the current host. Recovery occurs on a new foreground transition or subsequent core operation. This old assertion remains evidence of that behavioral difference. |
| Losing one network while another exists does not invoke restoration | No separate native network-state algorithm is retained. There is no claim to preserve this callback behavior. |
| Repeated configure replaces callback without registering again | Removed private singleton mutation API. A connection has its own runtime/observer; it is closed explicitly by the application. |
| Capability changes update connected state | No independent observed connectivity state is exposed. This remains outside the current host profile. |

Keep the old connectivity suite until the release policy for automatic reconnect recovery is settled. These tests do not prove cleanup of lifecycle observers after garbage collection without an explicit `close()`; applications should retain one owner and close it when replacing it. They also do not replace token freshness, client response ordering, storage continuity, or live shared-session synchronization audits.
