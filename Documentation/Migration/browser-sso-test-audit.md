# Browser OAuth and enterprise SSO migration

All thirteen declarations and complete bodies in baseline `SignInAuthenticateWithRedirectTest.kt`, `SignUpAuthenticateWithRedirectTest.kt`, `SSOServiceAuthenticateWithRedirectTest.kt` and `SSOExtensionsTest.kt` have been reviewed and retired. Their original names and hashes remain in [legacy-tests.json](legacy-tests.json), at baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`.

The generated future `signIn.sso` and `signUp.sso` methods own browser preparation and callback reconciliation. The shared `clerk.authenticateWithSSO` entry owns optional cross-flow transfer. Native code supplies the browser capability and configured callback. No production code changes were needed for this audit.

## Assertion dispositions

| Legacy declaration | Current disposition and evidence |
| --- | --- |
| Sign-in: `authenticateWithRedirect uses OAuth provider strategy` | `signInUsesGoogleBrowserStrategyAndConfiguredCallback` checks the actual Google strategy, both configured redirect fields, OIDC prompt, locale, browser URL and nonce reload. The result is complete but unselected. `browserTransferCreatesSignUpWithMetadataWithoutSecondPrompt` separately verifies the shared transfer-enabled entry. |
| Sign-in: `authenticateWithRedirect preserves custom OAuth provider strategy` | `signInPreservesCustomProviderStrategy` passes `SignInSSOParamsStrategy.Unrecognized("oauth_custom_patreon")` through generated dispatch, browser presentation and callback reload. The outgoing strategy remains exact. |
| Sign-in: `authenticateWithRedirect uses Enterprise SSO strategy` | `enterpriseSignInPreparesConnectionBeforeOpeningBrowser` uses the generated shared entry with transfer disabled. It creates the attempt, prepares its enterprise connection, opens the fresh URL and reconciles the nonce. It returns the owner's generated sign-in resource. |
| Sign-in: `authenticateWithPreparedRedirect continues prepared external verification` | The old direct prepared-URL method is removed. `enterpriseSignInRefreshesPreparedRedirectOnExistingAttempt` retains the existing attempt ID and refreshes its verification before opening. `oauthRetryDoesNotReplayStalePreparedRedirect` verifies OAuth instead creates a fresh attempt and never opens the stale URL. |
| Sign-in: `authenticateWithPreparedRedirect fails when external verification URL is missing` | The old private helper/error is removed. `missingRedirectLeavesAnIncompleteAttemptWithoutOpeningBrowser` verifies the selected future method's actual contract: preparation can resolve with remaining requirements and no browser request. The caller must inspect state; resolution alone does not establish authentication. |
| Sign-up: `authenticateWithRedirect uses sign-up redirect flow for OAuth provider` | `signUpPreservesMetadataAndBrowserStrategy` invokes the generated sign-up future, requires only sign-up endpoints, and checks completion remains unselected. The old OAuthResult wrapper is removed. |
| Sign-up: `authenticateWithRedirect preserves custom OAuth provider strategy` | `signUpPreservesCustomProviderStrategy` sends the exact custom strategy through `SignUpSSOParams` and completes the browser flow. |
| Sign-up: `authenticateWithRedirect forwards unsafe metadata` | Both sign-up cases inspect nested metadata JSON in the actual form request, alongside email, legal acceptance and locale. |
| Sign-up: `redirect params map transform unsafe metadata to json` | The same request assertions compare parsed JSON including a nested Boolean, rather than a handwritten parameter-map encoder. |
| Sign-up: `redirect params map preserves custom OAuth provider strategy` | The custom sign-up case checks the exact actual request strategy. The old `toMap` API is removed. |
| Service: `authenticateWithRedirect creates sign in then prepares OAuth first factor` | Intentionally changed to the current future contract. Google OAuth creates its redirect in one request and then reloads after the browser callback; the test rejects an extra preparation request. The locale and both callback fields are checked. Enterprise SSO retains its separate preparation request. |
| Converter: `signInToOAuthResult preserves unknown transport failure` | `signInTransportFailureDoesNotBecomeSuccess` throws a native IOException from HTTP and requires a generated `CoreException`, no browser and no retry. Kotlin throwable/object identity and the old ClerkResult error enum do not cross the JS boundary. |
| Converter: `signUpToOAuthResult preserves unknown transport failure` | `signUpTransportFailureDoesNotBecomeSuccess` verifies the same failed-call behavior on sign-up. There is no OAuthResult conversion layer in the new API. |

## Additional contract checks

Sign-in and sign-up cancellation cases require `user_cancelled` and no nonce reload or selection. An unrelated callback fails with `oauth_transport_callback_mismatch` before its nonce can reach HTTP. A rejected sign-up preparation preserves status 422, the server error code and trace, without opening a browser.

Browser transfer is controlled by the shared entry. With transfer enabled, the completed browser verification produces one transfer sign-up request carrying metadata, legal acceptance and locale, without opening a second browser. With transfer disabled, the verification error is returned, the sign-in remains transferable and no sign-up request is made. Successful flows leave finalization explicit.

All cases execute the bundled QuickJS engine and generated Kotlin APIs with deterministic native capabilities. They assert exact authentication request paths; the capability receives only the refreshed server URL. They do not exercise an actual browser activity or a live identity provider.

## Verification

All sixteen packaged scenarios pass without skips, as do twelve existing shared SSO tests covering the real future facades, remaining requirements, cancellation, callback matching, nonce semantics and raw OIDC prompts. Reports, source hashes and the thirteen retired declarations are retained in [the proof](evidence/browser-sso/proof.json).

`bash scripts/run-auth-entry-contract.sh` now requires 63 named cases across eight classes. Each class is checked separately for exact names, failures and skips, with its XML preserved before the next run. Live provider, system presentation and released-app upgrade gates remain open. The separate OAuth-provider display/serialization and broader SSO service suites are still retained for their own assertion audit.

[CI run 34523249674](https://github.com/clerk/clerk-android/actions/runs/34523249674) passed all four jobs at exact revision `d50b03e1041e201eae20d211fa4431778a6e5ec6`. Downloaded XML confirms 12 API unit tests, 508 UI unit tests, two API 24 packaged/platform cases and 78 API 36 cases, including all sixteen browser SSO scenarios. Every expected instrumentation method passed without failures, errors or skips; the duplicate current-output report was excluded only after a byte-for-byte comparison with its retained per-class report. [The CI proof](evidence/browser-sso/ci/proof.json) records the workflow metadata, canonical instrumentation reports and artifact hashes.
