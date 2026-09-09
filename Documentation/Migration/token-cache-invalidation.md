# Token cache invalidation

The generated `Session.clearCache()` now prevents pending token requests from updating a cache entry created after the clear. Previously, an earlier request could find the replacement entry and contribute its invalidated token to that entry's freshness baseline. The correction is in the canonical TypeScript cache; Kotlin adds no token policy.

`NativeCoreTests/Android/TokenInvalidationTest.kt` checks both response orders with two suspended template-token HTTP requests on the packaged QuickJS runtime. Both tests reproduce the old failure and pass after the fix on the API 36 emulator. Each original caller may receive its own response; subsequent calls must use the post-clear cached token without another request. This is cache invalidation, not cancellation of those original callers.

The source cache regression and generated embedded checks also fail before the fix and pass afterward. All 229 embedded-core tests and 195 focused source tests across `Session`, `tokenCache`, and `tokenFreshness` pass. The 11 existing `PackagedCoreTest` instrumentation tests also pass in a separate run. The package pins core `35639cd01a43caced4eb948891e404bff43271dd`, SHA-256 `0282374e1419ee72a7e8718fa7bd2936442e00f92dbecf125262f13b55462549`, with unchanged generated signatures.

These fixture checks do not establish old-major signed-in upgrades. No unaudited Android legacy test is retired.

## Proactive refresh follow-up

Proactive refresh previously registered its token only after its HTTP response, bypassing the first fix's capture point. The source now checks the cache generation captured when refresh starts before caching or dispatching that token. Refresh tracking also uses the generation, so an invalidated request cannot block a newer lifetime's refresh or release its in-flight marker.

The two new `TokenInvalidationTest` cases explicitly foreground the fixture runtime, advance one host refresh timer, and suspend its response across a cache clear. They check both an empty cache and a fetched replacement token. Both fail on the prior bundle by returning the invalidated token. These controlled timer fixtures do not claim real OS suspension coverage.

The shared follow-up passes 231 embedded-core tests and 198 focused token/session source tests, including overlapping refresh lifetimes. Android passes all four token invalidation tests and all 11 existing packaged-core tests in separate API 36 emulator runs. Its packaged revision is `750f50cf8b67f6251c3b5a4776d87cc90be680a8`, SHA-256 `fc41a7eee1663d8cc2016d3f1cc52fcca92fc58f68c1e5416a5a441406c05e59`, with unchanged generated public signatures.

## Coalesced caller cancellation

`NativeCoreTests/Android/TokenCancellationTest.kt` passes five scenarios on packaged QuickJS: canceling the first, second, or both callers sharing a template-token request, and a shared 403 response with either caller canceled. The canceled coroutine receives `CancellationException`; a surviving caller receives the token or structured `token_denied` response. If all callers cancel, the source request may still populate the cache for a later call. After a shared failure, a later call successfully fetches a new token.

These outcomes already worked and require no runtime change; the pinned core above is unchanged. All five embedded equivalents and all 236 embedded tests pass. Cancellation stops the waiter and does not promise to abort shared work, invalidate the cache, or roll back authentication.
