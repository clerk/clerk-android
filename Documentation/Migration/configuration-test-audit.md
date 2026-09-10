# Configuration helper assertion audit

This review covers all assertion bodies in the old
`sdk/PublishableKeyHelperTest.kt` and `sso/RedirectConfigurationTest.kt` under
`source/api/src/test/java/com/clerk/api`. The foreground-policy suites are audited below; other configuration-switch tests remain part of the state/ownership audit.

| Old test | Current disposition |
| --- | --- |
| `extractApiUrl with valid test key returns correct URL` and the live-key variant | Both modes are tested through the public `ClerkConfiguration` and yield the exact HTTPS origin. The old fixture's final `x` is intentionally no longer accepted; publishable-key payloads must end in `$`. |
| `extractApiUrl with empty decoded string throws ClerkClientError` | Rejected by the public configuration test with `CoreException.code == invalid_publishable_key`. The old exception type is not preserved. |
| `extractApiUrl with no prefix still works correctly` | Intentionally changed: prefixless input is rejected. A configuration must identify the test/live key type. |
| `extractApiUrl with single character domain returns https prefix only` | Intentionally changed: a decoded `x` is rejected rather than producing a hostless origin. The new tests cover this exact value. |
| `defaultRedirectUrl_usesCallbackHost`, `legacyRedirectUrl_keepsOauthHost` | The application now supplies one full callback URL. There are no implicit default/legacy routes; migration documentation and each sample register their explicit routes. |
| `redirectUrlsReflectTheCurrentApplicationId` | No mutable process-global application ID participates in configuration. A core owner retains its explicit callback URL. |
| `defaultRedirectUrl_doesNotThrowWhenApplicationIdIsUnset` | The old `clerk://null.callback` fallback is absent. Missing application configuration must not silently construct this route. |
| `emailLinkRedirectUrl_usesProxyPortWhenConfigured` and `emailLinkRedirectUrl_omitsStandardHttpsPort` | Proxy configuration is unavailable in this prerelease. Callback URLs are explicit; those old proxy-derived defaults are not replacement coverage. |

`NativeCoreTests/Unit/ClerkConfigurationTest.kt` also verifies surrounding key
whitespace, malformed base64 and origins, callback scheme/credentials/fragment
validation, preservation of a configured route/query, and HTTPS callback acceptance.
All four test methods pass. The publishable-key and redirect files remain retained while the broader audit is in progress. The separate foreground assertion dispositions are recorded below; configuration-switch assertions remain unresolved.

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

`ConfigurationManagerAuthRaceTest.kt` and `ConfigurationManagerForegroundRefreshTest.kt` are retired after this audit and the packaged regression proof. The publishable-key, redirect, connectivity, and configuration-switch suites remain retained for their own audits. No old pending-service polling or native token-refresh implementation is restored.
