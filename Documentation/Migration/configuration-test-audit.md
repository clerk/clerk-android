# Configuration helper assertion audit

All eleven declarations and complete assertion bodies in `sdk/PublishableKeyHelperTest.kt` and `sso/RedirectConfigurationTest.kt` under `source/api/src/test/java/com/clerk/api` were checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Both files are retired; their original source hashes and declaration names are retained in [the inventory](legacy-tests.json) and [retirement proof](evidence/callback-receiver/proof.json). The foreground-policy suites are audited below; configuration-switch and device-token tests are covered by the [owner-isolation audit](configuration-ownership-test-audit.md).

| Old test declaration | Current disposition |
| --- | --- |
| `extractApiUrl with valid test key returns correct URL` | `validKeysNormalizeWhitespaceAndPreserveTheConfiguredCallback` tests the public `ClerkConfiguration` with a test key and exact HTTPS origin. The old fixture's trailing `x` is intentionally rejected; a canonical payload ends in `$`. |
| `extractApiUrl with valid live key returns correct URL` | The same public configuration test separately checks a live key. `invalidPublishableKeysAlwaysProduceStructuredErrors` rejects the old `clerk.example.comx` payload. |
| `extractApiUrl with empty decoded string throws ClerkClientError` | The malformed-key test requires `CoreException.code == invalid_publishable_key` for the exact empty payload. The old exception type is removed. |
| `extractApiUrl with no prefix still works correctly` | Intentionally changed: the exact prefixless base64 input is rejected. Configuration must identify a test or live publishable key. |
| `extractApiUrl with single character domain returns https prefix only` | The exact decoded `x` is rejected instead of yielding a hostless origin. |
| `defaultRedirectUrl_usesCallbackHost` | `ClerkConfiguration` requires an explicit URL. The SDK manifest supplies the current `${applicationId}.clerk://oauth/callback` route; `manifestExposesOnlyTheCurrentDefaultCallbackRoute` resolves it to the exported SDK receiver through Android PackageManager. The old `clerk://${applicationId}.callback` route is absent. |
| `legacyRedirectUrl_keepsOauthHost` | The old `clerk://${applicationId}.oauth` alias is removed. The same merged-manifest test explicitly requires that alias not to resolve in the SDK test application. Consumers must update their callback configuration. |
| `redirectUrlsReflectTheCurrentApplicationId` | The mutable global application ID API is removed. Configuration holds the caller's explicit URL in an immutable property; the public test checks exact route and query preservation. It does not derive a URL from another owner's application ID. |
| `defaultRedirectUrl_doesNotThrowWhenApplicationIdIsUnset` | The automatic `clerk://null.callback` fallback is removed. The constructor requires a URL; it does not synthesize a callback from a missing global application ID. This does not claim that a caller-provided host named `null.callback` is rejected. |
| `emailLinkRedirectUrl_usesProxyPortWhenConfigured` | Proxy configuration and automatic proxy-derived callback construction are unavailable. There is no retained helper that derives port 8443 from a proxy. Explicit custom routes require matching application manifest configuration. |
| `emailLinkRedirectUrl_omitsStandardHttpsPort` | The old proxy port-normalization helper is removed with proxy configuration. This is an intentional API removal, not passing coverage for a supported proxy feature. |

`NativeCoreTests/Unit/ClerkConfigurationTest.kt` also checks surrounding key whitespace, malformed base64 and origins, callback scheme/credentials/fragment validation, and HTTPS callback acceptance. All four methods pass. [Receiver lifecycle evidence](callback-receiver-test-audit.md) verifies manifest resolution and actual callback delivery without contacting a provider. The SDK's default manifest route and the constructor's required explicit URL are separate configuration responsibilities.

## Foreground policy and pending authentication

The following eight declarations were read in full at baseline
`1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Their removed private implementation was also reviewed: `ConfigurationManager` polled three pending-service booleans every 100 ms, waiting at most five seconds before a foreground refresh. The old tests asserted only the booleans/options; they did not exercise callback redemption concurrently with a refresh.

| Old test declaration | Disposition |
| --- | --- |
| `hasPendingAuthFlow returns true when SSO authentication is pending` | Retained race protection is now exercised through generated SSO, real packaged engines, and native lifecycle events; see [the reproduced regression](foreground-auth-recovery.md). No private service flag is retained. |
| `hasPendingAuthFlow returns true when external account connection is pending` | Generated external-account operations use the same invocation lifetime as all resource calls. Recovery deferral does not inspect private SSO-service state. The old boolean is removed; this row does not claim a live external-account browser flow. |
| `hasPendingAuthFlow returns true when hosted auth is pending` | The old `HostedAuthService` is not part of this generated major. Its private pending-state flag is removed, not claimed as supported hosted-auth coverage. |
| `hasPendingAuthFlow returns false when no auth flow is pending` | Idle owners run queued recovery; the packaged tests verify one client refresh after the generated call completes, and existing lifecycle tests verify ordinary foreground refresh. |
| `foreground refresh runs when no options are provided` | Standalone `Clerk.connect` installs lifecycle observation. Existing packaged lifecycle tests cover refresh on activation. |
| `foreground refresh runs by default` | Same current-owner lifecycle evidence; no mutable `ClerkConfigurationOptions` singleton is retained. |
| `foreground refresh is skipped when disabled via withForegroundRefreshDisabled` | The old framework-integration option is removed. Attached Expo UI uses its existing core owner and does not install a second embedded recovery owner. Standalone configuration has no disable-refresh option. |
| `withCustomHeaders preserves a disabled foreground refresh` | The old chained option builder and arbitrary custom-header configuration are removed. This private configuration assertion is intentionally retired, not approximated with a new native policy. |

`ConfigurationManagerAuthRaceTest.kt` and `ConfigurationManagerForegroundRefreshTest.kt` are retired after this audit and the packaged regression proof. The publishable-key and redirect suites are retired by the audit above; the connectivity suite is retired by its [adapter and lifecycle audit](lifecycle-test-audit.md); the configuration-switch suite is retired by the [owner-isolation audit](configuration-ownership-test-audit.md). No old pending-service polling or native token-refresh implementation is restored.
