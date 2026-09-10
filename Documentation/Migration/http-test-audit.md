# HTTP host audit

This audit covers the new OkHttp host and the assertions in the old `VersioningUserAgentMiddlewareTest` (4 tests) and `RequestLoggingMiddlewareTest` (2 tests), read in full from baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Both outgoing suites are now retired after the declaration mapping below. Incoming response-state suites have a separate [completed assertion audit](incoming-response-test-audit.md).

## Reproduced failures and fixes

`NativeCoreTests/Common/AndroidHTTPCapabilityTest.kt` invokes the production `AndroidCapabilities.perform("http", ...)` with a real OkHttp client and a fixture interceptor. The public host constructor is unchanged; an internal constructor accepts the test client. The same source runs in JVM unit tests and Android instrumentation.

Two tests failed before the fixes:

- After OkHttp delivered response headers, cancellation was no longer connected to the call. A response-body read remained active after the coroutine was cancelled. The host now keeps a cancellation child active through body consumption, closes the call when its owning coroutine is cancelled, and disposes that child for every normal/error/redirect exit. Interrupted body reads preserve coroutine cancellation; other body I/O failures produce `network_error`, matching failures before headers.
- Invalid UTF-8 became replacement characters through `ByteArrayOutputStream.toString`. A strict decoder now throws `invalid_http_response`, matching Apple's UTF-8 response validation.

The cancellation fixture waits until body consumption begins before cancelling. It checks that the underlying call is cancelled while the read is active; merely observing a cancelled caller would not catch the original bug. The fixture has a three-second deadline so failures do not leave a hung test. These tests use synthetic response bytes and do not claim to measure live-network socket interruption latency.

The suite also checks GET/POST/PATCH/DELETE, preserved query/body bytes and Authorization, HTTP 422 status/body delivery, lowercase trace headers, omitted Set-Cookie, and structured failures both before and after headers.

## Validation

The initial four HTTP tests passed on the JVM and an Android 16 emulator; the same run
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

The incoming response-state files are retired in their [separate audit](incoming-response-test-audit.md). Incoming client synchronization, device-token adoption, live shared-session convergence and old API DTO tests need their own assertion review. These host tests do not prove TLS policy, real backend authentication, proxy support, or platform UI. The existing 16 MiB response bound is checked during streaming on Android; no peak-memory performance result is inferred from these tests.


## Outgoing middleware retirement

All four VersioningUserAgentMiddlewareTest bodies and both RequestLoggingMiddlewareTest bodies, including their response fixtures and reflection-heavy setup/teardown, were reviewed. Their private configuration-manager fields, mock interceptor chain, sensitive-request tag and body logger belong to the removed native middleware design.

The shared `AndroidHTTPCapabilityTest` now has six passing tests on both the JVM and Android 16 emulator. Its method/query/body/error matrix additionally checks the exact core-supplied client ID and Authorization values. Two new cases check mixed-case Cookie and Host overrides are filtered while core identity headers remain single-valued, and that other hosts, HTTP downgrade, nonmatching ports and URL user/password credentials are rejected. Invalid initial destinations never reach the fixture HTTP interceptor; invalid redirect destinations stop after the initial permitted request. The test storage adapter throws on any access, so these requests cannot succeed by silently performing a separate native credential read.

These tests invoke production AndroidCapabilities with an OkHttp fixture interceptor. They do not make cross-origin network requests, prove TLS or live socket behavior, or assert logging configuration for arbitrary app-supplied clients. The public native host constructs its own client without a body logger; its automatic redirect handling is disabled and the bounded native redirect loop checks each destination. No production implementation, generated API or bundle changed for this retirement.

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| `intercept adds client id and authorization for normal requests` | The core owns identity selection. The actual native host test checks supplied client ID and Authorization reach the request unchanged without native storage access. Packaged credential-ordering tests separately cover core acceptance. |
| `intercept omits client id and strips internal header for marked requests` | The private X-Clerk-SDK-Skip-Client-Id tag/header protocol is removed. It is not a public request option; the core supplies final headers. No native skip marker is generated or interpreted. |
| `intercept sets custom headers` | Arbitrary native header-configuration overrides are removed. Expo uses its attached JavaScript owner and its metadata; this profile has no independent Kotlin Expo-header configuration. |
| `intercept custom headers replace existing header values` | The old configuration override of Authorization/client ID, including case-insensitive replacement, is removed. The new host forwards the selected core identity; it does not install a second identity-override policy. |
| `sensitive request bypasses body logging` | The public host has no body-logging interceptor. The sensitive-request tag and conditional logger are removed rather than recreated around the new transport. |
| `ordinary request uses body logging` | Ordinary HTTP body logging is intentionally not retained. This is a removed debugging behavior, not a replacement test for authentication. |

The four DeviceTokenSavingMiddlewareTest and nine ClientSyncingMiddlewareTest declarations are retired after their [separate mapping](incoming-response-test-audit.md) of credential adoption, removed flow guards/events, null-client envelopes and completion behavior. Hosted-auth creation and manual native synchronization remain unavailable.
