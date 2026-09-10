# Lifecycle and connectivity assertion audit

This audit reviews all ten assertions/tests in the old `configuration/connectivity/NetworkConnectivityMonitorTest.kt` and tests the new lifecycle adapter through the packaged QuickJS core. It does not retire the old connectivity tests or claim complete feature equivalence.

## Current lifecycle proof

`NativeCoreTests/Android/LifecycleContractTest.kt` drives a real AndroidX `LifecycleRegistry` through CREATED/STARTED/RESUMED/DESTROYED. The production lifecycle adapter receives that owner instead of the process owner; its default remains `ProcessLifecycleOwner`. Requests execute the packaged core, and tests assert generated resources:

- STARTED refreshes available sessions; RESUMED does not trigger another foreground reload.
- Refreshing the session list leaves session adoption explicit. A listed session does not silently become the active session.
- Closing the runtime removes the observer; later lifecycle transitions cause no further client reads.
- A failed foreground reload exposes `lastLifecycleError` while the runtime remains available. A generated reset succeeds and a later activation can recover the available-session list.

The lifecycle suite now has five tests, including connectivity restoration,
last-owner collection, and [foreground during OAuth callback redemption](foreground-auth-recovery.md), and passes on an Android 16 emulator. The failure test
awaits `lastLifecycleError.value`, rather than checking the non-null StateFlow
container. These are synthetic lifecycle events with fixture HTTP, not proof of
real process suspension/death, network reconnection, or a signed-in app upgrade.
The separate JavaScript `Session.test.ts` and `tokenCache.test.ts` run passes 166
tests, including native background suppression and subsequent foreground token
recovery. No Kotlin polling/backoff implementation is recreated.

`releasingTheLastOwnerUnsubscribesLifecycleAndConnectivity` first waits for a weak
runtime reference to clear, then checks that the AndroidX lifecycle registry has
no observer and the network unsubscribe callback has run. A subsequent lifecycle
transition causes no HTTP. Before the fix, collection succeeded but unsubscribe
never happened. The cleanup record now retains teardown actions independently
of the runtime and dispatches them on the owner's dispatcher, including after
collection. Explicit close uses the same once-only cleanup and removes its
record from the collection queue's retained set. A failing teardown action does
not prevent remaining actions or transport closure.

The initial test attempt accidentally retained the weak-reference result while
requesting GC from a coroutine frame. The final test reads the reference in a
separate non-suspending helper; its reproduced failure was after confirmed
collection, at the unsubscribe checks. GC timing is not a public guarantee.

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

Keep the old suite as migration evidence for its removed private singleton and startup retry behavior. Current restoration covers an initialized owner; failed-connect retry policy remains distinct. Applications should retain one owner and explicitly close it when replacing it for deterministic cleanup. Collection cleanup is a fallback and is now tested, but does not replace token freshness, client response ordering, storage continuity, or live shared-session synchronization audits.
