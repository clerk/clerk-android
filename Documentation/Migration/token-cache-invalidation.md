# Token cache invalidation

The generated `Session.clearCache()` now prevents pending token requests from updating a cache entry created after the clear. Previously, an earlier request could find the replacement entry and contribute its invalidated token to that entry's freshness baseline. The correction is in the canonical TypeScript cache; Kotlin adds no token policy.

`NativeCoreTests/Android/TokenInvalidationTest.kt` checks both response orders with two suspended template-token HTTP requests on the packaged QuickJS runtime. Both tests reproduce the old failure and pass after the fix on the API 36 emulator. Each original caller may receive its own response; subsequent calls must use the post-clear cached token without another request. This is cache invalidation, not cancellation of those original callers.

The source cache regression and generated embedded checks also fail before the fix and pass afterward. All 229 embedded-core tests and 195 focused source tests across `Session`, `tokenCache`, and `tokenFreshness` pass. The 11 existing `PackagedCoreTest` instrumentation tests also pass in a separate run. The package pins core `35639cd01a43caced4eb948891e404bff43271dd`, SHA-256 `0282374e1419ee72a7e8718fa7bd2936442e00f92dbecf125262f13b55462549`, with unchanged generated signatures.

These fixture checks do not establish background-refresh invalidation, cancellation across callers sharing one token request, or old-major signed-in upgrades. No unaudited Android legacy test is retired.
