---
name: session-token-fetcher-model
description: Model-check clerk-android's session token deduplication and fencing (SessionTokenFetcher getToken, reset, invalidateSession) with TLA+. Use when a change touches SessionTokenFetcher.kt, SessionTokensCache.kt, or the code that calls SessionTokenFetcher.reset() or invalidateSession(), or when asked whether concurrent getToken calls can return a stale token, hang, or see someone else's cancellation.
---

# Session token fetcher model

`formal/session-token-fetcher/` holds a TLA+ model of how concurrent `getToken` calls share one in-flight request, and how `reset()` and `invalidateSession()` fence those requests. TLC explores every interleaving of the model's steps within its bounds. Tests only cover the interleavings someone wrote.

## Properties

Each property is one invariant in `SessionTokenFetcher.tla`.

| Property | Plain meaning | Harm if broken |
| --- | --- | --- |
| `NoTokenFromAnotherGeneration` | A caller only gets a token minted in the generation it started in. A generation changes on `reset()` or `invalidateSession()`. | A caller that started after a local reset, a device-token change, or biometric reverification gets a token from before it. |
| `OnlyCancelledCallersSeeCancellation` | A caller only sees a cancellation if it was cancelled itself. | One coroutine's cancellation kills other callers waiting on the same request. |
| `CacheIsCurrentWhenQuiet` | Once no fence is running, `SessionTokensCache` holds no token from an older generation. | Later calls serve a stale token from the cache. |
| `AtMostOneSharedRequest` | Calls without `skipCache` make at most one network request per generation at a time. | Duplicate token requests (the deduplication is gone). |
| `EveryCallReturns` | Every `getToken` call eventually returns (liveness, under fair scheduling). | A caller hangs forever. |

## Does my change touch the verified code?

Coverage at the sync commit:

- `source/api/src/main/kotlin/com/clerk/api/session/SessionTokenFetcher.kt`: `reset`, `invalidateSession`, `getTokenResult`, `fetchTokenWithDeduplication`, `awaitSharedTask`, `supersededIfInvalidatedSince`, `isCurrentRuntime`, `fetchToken`, `reconcileTokenResponse`.
- `source/api/src/main/kotlin/com/clerk/api/session/SessionTokensCache.kt`: `storeIfFresher`, `removeTokens`, `clear`.
- The fence call sites: `Clerk.reset()` in `source/api/src/main/kotlin/com/clerk/api/Clerk.kt` (`SessionTokenFetcher.shared.reset()` then `SessionTokensCache.clear()`), `resetClientStateForDeviceTokenChange()` in `source/api/src/main/kotlin/com/clerk/api/configuration/ConfigurationManager.kt`, and `invalidateSession` in `source/api/src/main/kotlin/com/clerk/api/biometriccredential/BiometricSessionVerificationService.kt`.

**Sync commit: `618bb6d0`.** At this commit the model was checked line by line against the files above. The line numbers in the model's comments refer to it.

To see whether covered code has changed since then:

```bash
git log --oneline 618bb6d0..HEAD -- \
  source/api/src/main/kotlin/com/clerk/api/session/SessionTokenFetcher.kt \
  source/api/src/main/kotlin/com/clerk/api/session/SessionTokensCache.kt \
  source/api/src/main/kotlin/com/clerk/api/Clerk.kt \
  source/api/src/main/kotlin/com/clerk/api/configuration/ConfigurationManager.kt \
  source/api/src/main/kotlin/com/clerk/api/biometriccredential/BiometricSessionVerificationService.kt
```

For `Clerk.kt` and `ConfigurationManager.kt`, only changes to the reset paths named above matter. Also check for any new caller of `SessionTokenFetcher.shared.reset()`, `invalidateSession(`, or a `SessionTokensCache` write:

```bash
grep -rn "SessionTokenFetcher.shared\.\|SessionTokensCache\.\(setToken\|hydrate\|storeIfFresher\|remove\|clear\)" source/api/src/main
```

## Run it

You need Java 17+ (the same JDK as the Gradle build). On this team's Macs:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 formal/session-token-fetcher/check.sh
```

The first run downloads `tla2tools.jar` v1.7.4 into `~/.cache/clerk-tla/` and checks its SHA-256. The routine check runs two configs in about 40 seconds:

- `NoReset.cfg`: 3 callers (at most one with `skipCache`), one `invalidateSession`, one cancellation.
- `Reset.cfg`: 2 callers plus one `reset()` and its following `SessionTokensCache.clear()`, one `invalidateSession`, and one cancellation.

`TLC_CONFIG=Full.cfg` runs 3 callers with every fence. It takes about 25 minutes, so run it before merging a change to the fencing logic, not on every edit. `TLC_WORKERS` sets the thread count (default 2).

To confirm that every property can still fail, run the mutation check (about 15 seconds):

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 formal/session-token-fetcher/mutants.sh
```

Each file in `mutants/` swaps one model action for a version that mirrors a one-line regression in the Kotlin code, and names the property that must catch it. Every line must print `caught`. Run this after any edit to the model.

## Expected result today

- `NoReset.cfg`: `Model checking completed. No error has been found.`
- `Reset.cfg`: `Invariant NoTokenFromAnotherGeneration is violated`, with the trace below. This is a known finding, not a modeling error.

**Known finding: reset/clear window.** `Clerk.reset()` and `resetClientStateForDeviceTokenChange()` call `SessionTokenFetcher.shared.reset()` and then `SessionTokensCache.clear()` as two separate statements. The fetcher's generation moves forward before the cache is emptied. The trace goes like this:

1. Caller A fetches a token and `reconcileTokenResponse` stores it in `SessionTokensCache`.
2. `SessionTokenFetcher.reset()` bumps `runtimeGeneration`.
3. Before `SessionTokensCache.clear()` runs, caller B (holding a `Session`) calls `getTokenResult` and captures the new generation.
4. B's `fetchToken` reads A's cached token, passes `isCurrentRuntime`, and returns it.

The window is two adjacent statements with no suspension point between them, so it needs another thread inside a microsecond-scale gap. When `SessionTokensCache.clear()` is moved into `reset()`'s `synchronized(runtimeLock)` block, the model passes all three configs, including `Full.cfg`. If `Reset.cfg` passes, someone fixed this: delete this section.

Any other violation is new. Read the trace, then decide which case it is (see the next section).

## Bug or modeling error?

TLC prints a trace: a numbered list of states, each headed by the action that produced it (`Capture`, `Register`, `Reconcile`, `ResetFetcher`, and so on). Each action's comment names the code lines it stands for. Replay the trace against those lines.

- **Real bug:** every step maps to something the Kotlin code allows, and each step is one indivisible piece of the code (one `synchronized(runtimeLock)` block, one `ConcurrentHashMap` call, or one `CompletableDeferred` transition). Report it as a numbered list of steps in the code's own words. Do not weaken the property, and do not change product code unless asked.
- **Modeling error:** a step does something the code can't do. For example, it splits a `synchronized` block into two steps, misses a check the code makes, or allows cancellation where the code has no suspension point. Fix the model, rerun `check.sh` and `mutants.sh`, and say what you changed.

## Updating the model for an intended change

1. Change the matching action in `SessionTokenFetcher.tla`. Keep one action per indivisible step, and update the line-number comments.
2. If the change adds a new promise, add an invariant with a one-line plain meaning. Add it to all three `.cfg` files, and add a mutant in `mutants/` that breaks it. The mutant file needs a `\* Expect: <PropertyName>` line, and its definition must replace an action of the same name.
3. Run `check.sh` and `mutants.sh`, and `Full.cfg` if the fencing logic changed.
4. Re-read the covered functions at the new commit. Only once the model describes them, move the sync commit above to that commit.

## What this does not cover

- One session and one cache key with no template. Different keys don't share state in the fetcher except through `invalidateSession`, which the model covers for this session's keys.
- Bounds: 2 or 3 callers, at most one reset, one invalidation, and one cancellation per run.
- The cache's freshness ordering (`TokenFreshness.pickFreshest`), session snapshot hydration (`eligibleSnapshot` and `hydrate` in `fetchToken`), JWT expiry (modeled as a free choice), and the `forceOrigin`/minter request parameters.
- Sign-out. `Auth.signOut` and `SignOutService` don't call `reset()` or `invalidateSession()`, so a fetch in flight during sign-out completes normally. The model doesn't treat sign-out as a fence.
- Callers' exceptions other than cancellation, and anything outside `SessionTokenFetcher` and its cache.

Use `/maintain-formal-verification-skill` to bring this back in line after changes that skipped the recheck.
