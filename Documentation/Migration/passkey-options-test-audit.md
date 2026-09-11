# Passkey assertion options audit

All five declarations in the retained `PasskeyWebAuthnRequestTest.kt` were checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. The file matched the baseline byte for byte before retirement. Its original hash and declarations are preserved in [the proof](evidence/passkey-options/proof.json).

## Current ownership and regression

The selected TypeScript assertion contract requires `rpId`. It supplies the native credential request after decoding the server's assertion options. The native host presents that request and returns credential data; it does not select another relying party, replace the credential list or set its own verification policy.

Five regression cases initially reached the native host with a missing, registration-style-only, empty, whitespace-only or non-string relying party. Shared mobile credential validation now returns `invalid_credential_options` at `requestingAuthorization` before invoking the host. The check is shared by embedded and attached mobile integrations and does not change the web credential path.

Eight shared runtime cases check those failures, valid options and two malformed credential-list cases. The valid case preserves the server's relying party, challenge, 45-second timeout, preferred verification policy, two credential IDs in order, and internal/hybrid transports. Missing and invalid-base64 credential IDs fail during shared preparation rather than silently removing entries. Every case leaves an incomplete sign-in, no selected session, and exactly one sign-in creation request with no credential submission.

`PasskeyOptionsTest` runs the same eight scenarios through generated Kotlin APIs and the packaged QuickJS runtime. Swift's `passkeyAssertionOptions(scenario:)` runs them through the generated API and packaged JavaScriptCore runtime. Fixture HTTP supplies server state; valid requests deliberately stop with a fixture cancellation at the capability boundary.

## Every old declaration

| Old declaration | Disposition and current evidence |
| --- | --- |
| `createWebAuthnRequest uses nested server relying party id` | Intentional contract change: assertion requests require top-level `rpId`; registration-style `rp.id` is not an assertion alias. The nested-only case fails before native presentation on both runtimes. |
| `createWebAuthnRequest uses top level server relying party id` | Preserved. The valid case checks the server value without substitution on both runtimes. |
| `createWebAuthnRequest uses server allowed credentials` | Server credential order and IDs remain; transports now remain too. The old helper dropped malformed entries and transport fields. Missing/invalid IDs now reject the list during shared preparation. Existing Android `PasskeyRequestTest` also checks rejection of an empty encoded ID at the Credential Manager request boundary; this is not a claim about Apple's OS handling of empty IDs. |
| `createWebAuthnRequest prefers explicit allowed credential ids over server list` | Removed with the private service helper. The selected generated `SignInPasskeyParams` exposes flow and immediate-credential preference, with no credential-ID override. The server supplies the list. |
| `createWebAuthnRequest falls back to Clerk domain without server relying party id` | Intentional contract change: no frontend-domain fallback. The missing-RP case checks no presentation, no credential submission and an incomplete sign-in. |

The old private helper's fixed 30-minute timeout and required user verification were not asserted by these five declarations. Current native requests preserve the server's timeout and verification choice, covered explicitly by the valid case.

## Verification limits and setup notes

The checks establish generated API, packaged-engine and capability-boundary behavior. They do not exercise a real passkey provider dialog or live server. The full migration's device, backend and release gates remain separate.

The initial Android command selected JDK 17 and stopped before testing; rerunning with JDK 21 fixed the environment. A broad formatting task also touched existing Kotlin files; those formatting-only changes were restored and the core was repackaged before the broader contract run. SwiftFormat could not write its cache inside the sandbox, but formatted the requested source successfully. JavaScript hooks reported the existing missing Husky bootstrap file; the remaining formatting checks and commit completed.

The shared core is pinned to JavaScript revision `cba4fd161f0b1d80f48a9128d47a85c9a4ca0521`, bundle SHA-256 `25001603e9c152c6aaa2f8bad982bbab2a7da122efd6774df89518c809b131a5`. Local verification passed all 585 shared runtime tests, 278 Swift tests, eight iOS Simulator cases and 131 exact named Android authentication cases. The generated API bytes match the generator output on both platforms.

## Published-commit CI

All four jobs in [test CI](https://github.com/clerk/clerk-android/actions/runs/34547003232) passed for Android revision `dc62ed84b77db84e0fe90ef921d6f2ec319b206e`. Independent inspection of the downloaded XML verifies 39 API unit tests, 527 UI unit tests, two API 24 emulator cases, and 152 API 36 emulator cases. The latter includes the exact same eight new assertion-option cases as the local run, plus the existing authentication contracts, token requests and rendered authentication/footer journeys. [CI proof](evidence/passkey-options/ci/proof.json) records exact report paths, hashes and expected-case verification.

All three jobs in [build CI](https://github.com/clerk/clerk-android/actions/runs/34547005227) passed. Inspection of both downloaded release AARs confirms identical bytes, SHA-256 `26fea14911df1e1c676aa5650c8d6e2994d51f61900ae73e75236e0f547d95a0`, and the expected bundled core `25001603e9c152c6aaa2f8bad982bbab2a7da122efd6774df89518c809b131a5`.
