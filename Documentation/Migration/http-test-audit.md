# HTTP host audit

This audit covers the new OkHttp host and the assertions in the old `VersioningUserAgentMiddlewareTest` (4 tests) and `RequestLoggingMiddlewareTest` (2 tests), read from the main-based checkout. It does not retire the old request/response state tests or claim full replacement coverage.

## Reproduced failures and fixes

`NativeCoreTests/Common/AndroidHTTPCapabilityTest.kt` invokes the production `AndroidCapabilities.perform("http", ...)` with a real OkHttp client and a fixture interceptor. The public host constructor is unchanged; an internal constructor accepts the test client. The same source runs in JVM unit tests and Android instrumentation.

Two tests failed before the fixes:

- After OkHttp delivered response headers, cancellation was no longer connected to the call. A response-body read remained active after the coroutine was cancelled. The host now keeps a cancellation child active through body consumption, closes the call when its owning coroutine is cancelled, and disposes that child for every normal/error/redirect exit. Interrupted body reads preserve coroutine cancellation; other body I/O failures produce `network_error`, matching failures before headers.
- Invalid UTF-8 became replacement characters through `ByteArrayOutputStream.toString`. A strict decoder now throws `invalid_http_response`, matching Apple's UTF-8 response validation.

The cancellation fixture waits until body consumption begins before cancelling. It checks that the underlying call is cancelled while the read is active; merely observing a cancelled caller would not catch the original bug. The fixture has a three-second deadline so failures do not leave a hung test. These tests use synthetic response bytes and do not claim to measure live-network socket interruption latency.

The suite also checks GET/POST/PATCH/DELETE, preserved query/body bytes and Authorization, HTTP 422 status/body delivery, lowercase trace headers, omitted Set-Cookie, and structured failures both before and after headers.

## Validation

All four HTTP tests passed on the JVM and an Android 16 emulator; the same run
also passed the four existing configuration unit tests. The JVM OkHttp platform
probe prints an Android `Log.isLoggable` not-mocked diagnostic before falling back;
the instrumentation run verifies these cases on Android itself. Gradle also
reports existing incubating configuration features and Gradle 10 deprecations.

## Old assertion dispositions

| Original assertion | Current owner / disposition |
| --- | --- |
| Normal requests receive persisted device token and client id | TypeScript owns credential/client selection. The native host forwards the supplied Authorization and headers; it does not perform a second storage read to select identity. The host forwarding test does not prove every core request-selection race. |
| Internal skip-client-id marker is removed and client id omitted | This was a private native middleware protocol. It is not retained as a public option. Core requests must supply the intended final headers directly. |
| Custom Expo host/version headers are added | Expo uses an attached transport to its existing JavaScript owner. There is no independent native middleware configuration for the attached owner. Core metadata remains the TypeScript owner's responsibility. |
| Custom headers replace Authorization/client id case-insensitively | Arbitrary configuration overrides for native authentication headers are not retained. No second native identity source is added to preserve this internal behavior. |
| Sensitive request bypasses body logging | The new host does not install a body-logging interceptor or log HTTP bodies. Sensitive request tagging is removed with the old middleware. |
| Ordinary request uses body logging | General request-body logging is not retained. This is an intentionally removed debugging behavior, not missing authentication behavior. |

The old files remain while the broader networking/state migration is incomplete. Incoming client synchronization, device-token adoption, live shared-session convergence and old API DTO tests need their own assertion review. These host tests do not prove TLS policy, real backend authentication, proxy support, or platform UI. The existing 16 MiB response bound is checked during streaming on Android; no peak-memory performance result is inferred from these tests.
