# Lifecycle and connectivity assertion audit

This audit reviews all ten assertions/tests in the old `configuration/connectivity/NetworkConnectivityMonitorTest.kt` and tests the new lifecycle adapter through the packaged QuickJS core. It does not retire the old connectivity tests or claim complete feature equivalence.

## Current lifecycle proof

`NativeCoreTests/Android/LifecycleContractTest.kt` drives a real AndroidX `LifecycleRegistry` through CREATED/STARTED/RESUMED/DESTROYED. The production lifecycle adapter receives that owner instead of the process owner; its default remains `ProcessLifecycleOwner`. Requests execute the packaged core, and tests assert generated resources:

- STARTED refreshes available sessions; RESUMED does not trigger another foreground reload.
- Refreshing the session list leaves session adoption explicit. A listed session does not silently become the active session.
- Closing the runtime removes the observer; later lifecycle transitions cause no further client reads.
- A failed foreground reload exposes `lastLifecycleError` while the runtime remains available. A generated reset succeeds and a later activation can recover the available-session list.

The lifecycle suite now has three tests, including connectivity restoration, and passes on an Android 16 emulator. The failure test awaits `lastLifecycleError.value`, rather than checking the non-null StateFlow container. These are synthetic lifecycle events with fixture HTTP, not proof of real process suspension/death, network reconnection, or a signed-in app upgrade. The separate JavaScript `Session.test.ts` and `tokenCache.test.ts` run passes 166 tests, including native background suppression and subsequent foreground token recovery. No Kotlin polling/backoff implementation is recreated.

## Old connectivity assertions

| Old assertion | Current disposition |
| --- | --- |
| Initial state is online; reset restores online | The removed private connectivity singleton/test reset is not retained. The embedded core treats unknown connectivity as eligible to attempt HTTP. |
| Configure registers a network callback, checks initial capabilities and does not call recovery initially | The host subscribes to the OS default network after successful connection; initial online state does not cause another refresh. See [connectivity](connectivity.md). |
| Configure detects initial offline; querying connectivity uses current active network | No exposed native singleton status API is retained. OS state updates the shared core network environment; shared tests verify offline recovery deferral. |
| Stop unregisters callback | Connection-owned lifecycle and connectivity subscriptions are removed on explicit runtime close; the new connectivity test checks cleanup and no HTTP after later injected events. |
| Offline-to-online invokes recovery callback | Online restoration now triggers the shared core recovery while active, checked through both real packaged engines. A failed initial connect still returns failure and has no persistent owner to recover. |
| Losing one network while another exists does not invoke restoration | The default-network adapter ignores loss callbacks for a network already replaced by another default. This OS boundary rule does not duplicate Clerk recovery policy. |
| Repeated configure replaces callback without registering again | Removed private singleton mutation API. A connection has its own runtime/observer; it is closed explicitly by the application. |
| Capability changes update connected state | Default-network capability changes report eligibility to the core; no independent public connectivity StateFlow is introduced. |

Keep the old suite as migration evidence for its removed private singleton and startup retry behavior. Current restoration covers an initialized owner; failed-connect retry policy remains distinct. These tests do not prove cleanup of lifecycle observers after garbage collection without an explicit `close()`; applications should retain one owner and close it when replacing it. They also do not replace token freshness, client response ordering, storage continuity, or live shared-session synchronization audits.
