# Shared network failure behavior

The generated SDK uses the canonical TypeScript request and resource handlers. Native HTTP performs the requested network operation; it does not add a second Clerk retry or invalid-auth recovery policy.

The core retries failed GET fetches, with four total attempts by default while online. Authentication mutations are not replayed at this transport layer. A received HTTP 4xx/5xx is reported as an API response rather than treated as a failed fetch; a Retry-After or rate-limit reset header does not independently schedule a native replay. Higher-level shared token/startup/authentication operations may have their own retry rules.

The migration audit reproduced an unauthorized recovery loop: a session request returned 401, its client refresh returned 401, and the request handler refreshed the client recursively. `BaseResource` now reports the rejected GET `/client` without launching another client refresh. Other unauthorized resource requests still use the existing recovery path. Two concurrent unauthorized operations each perform their own bounded recovery; the selected core does not promise global refresh coalescing.

`NativeCoreTests/Android/NetworkFailureTest.kt` first reproduced four client reads with the previous bundle (the fixture ends the broken recursion with a fourth-read 403). The fixed bundle terminates after startup plus one recovery read, delivers a structured 401 error, preserves the accepted session and permits a later successful reload. The equivalent Apple test verifies the same outcomes.

Shared core `cd3b7a30cc50c9afd731f181a6b20d681755b713`, bundle SHA-256 `a366249b593f33a00ef9eaa541fffe8f068b1f271a4fd91aa6b089a75a288e70`. All 539 embedded tests pass, including sixteen generated network-failure cases. The relevant TypeScript resource/request suites pass 62 tests with one existing skipped test and four existing TODO cases; type checking and bundle reproducibility pass. The complete native contract suites pass 104 tests on macOS, 101 on iOS Simulator and 112 on Android, including the new regression. The Android run excludes only the separately opt-in benchmark and live-startup classes.

This is deterministic packaged-engine evidence, not a live connectivity or server-availability test. It does not claim an assertion-level audit or retirement of Android's old networking tests. The subsequent [credential clear audit](client-credential-clear.md) resolves blank-Bearer transport handling for the supported attached client-destruction path.
