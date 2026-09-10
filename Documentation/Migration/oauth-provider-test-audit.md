# OAuth provider assertion audit

The complete bodies of all eleven declarations in `OAuthProviderTest.kt`, `ToOAuthProvidersListTest.kt` and `UserOAuthProviderTest.kt` have been reviewed. Their hashes match baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f` in [the original inventory](legacy-tests.json). The three old files are retired after the checks below passed. No production code changes were needed.

The generated `OAuthProvider` represents a provider key such as `google` or `custom_patreon`; a request strategy includes the `oauth_` prefix. Unknown keys remain distinct generated `Unrecognized` values. The old global environment lookup, `CUSTOM` sentinel, uppercase built-in serialization and handwritten parameter-map API are not retained in the new major.

## Assertion dispositions

| Legacy declaration | Current behavior and evidence |
| --- | --- |
| `fromStrategy preserves a custom OAuth strategy` | `distinctCustomProvidersRetainOrderAndRawKeys` checks the generated provider key and its distinction from a generic custom value. `generatedStrategySerializationPreservesCustomPrefix` checks the exact request strategy and round trip. The old enum name `CUSTOM` is intentionally removed. |
| `custom providers with different keys remain distinct` | `distinctCustomProvidersRetainOrderAndRawKeys` preserves both Patreon and LINE in configuration order and requires unequal values. |
| `custom provider reads configured name and logo by exact strategy` | `configuredDisplayNameAndLogoUseExactCustomStrategy` executes Compose with LINE before Patreon and asserts both exact name/logo pairs after rendering. The lookup belongs to native presentation and uses the supplied Clerk owner. This checks URL selection, not remote image loading. |
| `serialization preserves built-in format and custom strategy` | `generatedProviderSerializationUsesCanonicalValues` requires `google` and `custom_patreon` to round trip through generated encoding. `generatedStrategySerializationPreservesCustomPrefix` separately requires `oauth_custom_patreon`. Legacy `"GOOGLE"` serialization is an intentional source/wire-format break for this generated enum. |
| `returns only enabled and authenticatable providers` | `signInRequiresEnabledAndAuthenticatable` checks all four Boolean combinations through the actual sign-in presentation helper. |
| `returns empty list when no providers are enabled and authenticatable` | `signInHasNoChoicesWhenNoProviderQualifies` checks disabled and non-authenticatable providers together. |
| `returns empty list for empty map` | `emptyConfigurationHasNoChoices` checks both sign-in and connected-account presentation. |
| `returns all providers when all are enabled and authenticatable` | `allEligibleBuiltInProvidersArePreserved` requires both Google and Apple. |
| `preserves distinct custom provider strategies` | `distinctCustomProvidersRetainOrderAndRawKeys` checks the exact two ordered custom keys. |
| `create external account params preserve custom strategy` | `customExternalAccountPreservesStrategyThroughBrowserAndReload` executes the packaged QuickJS core via generated `User.createExternalAccount`. It inspects the actual POST strategy and configured callback, then requires one browser opening and nonce-bearing client reload. The returned generated account is the same object now exposed by the user. |
| `connecting one custom provider does not hide another` | `connectedCustomProviderDoesNotHideAnotherOrRequireSignInEligibility` keeps LINE available after Patreon is verified. Profile connection requires an enabled provider; it does not require sign-in eligibility. A disabled Google provider remains hidden. |

## Verification

All nine `OAuthProviderPresentationTest` cases and both `OAuthProviderRequestTest` cases pass without skips. The Google request case independently checks canonical built-in strategy and returned-resource identity. Both packaged cases check repeated `additional_scope` form fields, raw OIDC prompt/login hint, the configured callback and preservation of the selected user/session. The first request run exposed an incorrect comma-separated scope assertion; the expectation was corrected after inspecting the existing TypeScript `stringifyQueryParams`, which appends one field per array element.

[The proof](evidence/oauth-providers/proof.json) retains source hashes, all eleven baseline declarations and canonical XML for these executions. Presentation checks use generated types with mocked resource values; request checks execute the actual bundled core with deterministic native capabilities. Neither establishes live provider authorization or system-browser presentation.

`scripts/run-auth-entry-contract.sh` now includes both request cases and requires 65 named cases across nine suites. The separate legacy external-account and broader SSO-service suites remain retained for their own assertion review.

The complete 65-case emulator gate passes, and both complete unit-test modules pass: 12 API tests and 517 UI tests across 99 UI suites. The proof retains the nine gate reports and full unit-report hashes without adding overlapping targeted and gate counts together.

[CI run 34525136879](https://github.com/clerk/clerk-android/actions/runs/34525136879) passed all four jobs at exact revision `7b03cfa9ed132b22444c6a594db2617c78576405`. Downloaded XML independently confirms 12 API unit tests, 517 UI unit tests, two API 24 cases and 80 API 36 cases, including both provider request cases. Every expected instrumentation method passed without failures, errors or skips. The duplicate current-output report was excluded after byte equality with the retained per-class report was confirmed. [CI evidence](evidence/oauth-providers/ci/proof.json) retains workflow metadata, fifteen canonical instrumentation reports and artifact hashes.
