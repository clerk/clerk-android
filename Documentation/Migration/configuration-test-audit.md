# Configuration helper assertion audit

This review covers all assertion bodies in the old
`sdk/PublishableKeyHelperTest.kt` and `sso/RedirectConfigurationTest.kt` under
`source/api/src/test/java/com/clerk/api`. Other configuration lifecycle/race tests
remain part of the state/ownership audit.

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
All four test methods pass. Old files remain retained while the broader audit is
in progress; this document does not retire configuration-switch or foreground
authentication race assertions.
