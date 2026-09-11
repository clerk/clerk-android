# Sign-in state and preparation assertion audit

Three legacy files contain eight test declarations: `SignInSerializationTest.kt` (five), `SignInPrepareFirstFactorParamsMapTest.kt` (two), and `SignInPrepareFirstFactorTest.kt` (one). Each retained file matched baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f` byte for byte before retirement. [The proof](evidence/signin-state/proof.json) records every declaration and source hash.

## Current behavior

TypeScript owns server hydration and preparation. The generated sign-in resource keeps its identity as server responses replace state. Kotlin's generated `SignInStatus.Unrecognized` keeps unknown server strings, while known values decode to their named cases. Resources are not mutable serializable copies of native domain objects.

Six packaged QuickJS cases use the generated Kotlin API with fixture HTTP. They check known and unknown nested-client statuses; changes from one unknown status to another and then complete on the same resource; explicit email/phone reset-code selection and snake-case wire fields; and browser SSO from `needs_identifier` without supplying an email. Completion exposes the created session ID but does not activate a session without finalization.

The shared runtime adds three status checks and runs the existing thirteen selected-factor cases. The initial status test incorrectly expected the old `supportedIdentifiers` property. Inspection of `SignInFutureResource` and its implementation confirmed that the selected API does not expose that property. The corrected check verifies its absence instead of restoring a legacy field. This was a test-contract correction, not a production fix.

## Every legacy declaration

| Old declaration | Current owner and disposition |
| --- | --- |
| `serializer preserves the public descriptor name` | Removed with the old `com.clerk.api.signin.SignIn` DTO serializer. The generated API exposes an owned resource, not that public serializer descriptor. |
| `sign in decodes known status values` | `nestedSignInPreservesClientTrustStatus` requires both the typed `NeedsClientTrust` value and exact raw string after nested-client hydration. |
| `sign in preserves unknown status values` | `nestedSignInPreservesUnknownStatus` checks `Unrecognized` and its raw value, identifier and null created-session ID. The server-update case checks distinct unknown values on one resource. Shared tests also check an updated identifier. The old `supportedIdentifiers`, standalone JSON round-trip, structural resource equality and mutable `copy` are intentionally not part of the selected resource contract. |
| `client preserves an unknown nested sign in status` | Both native status cases hydrate the sign-in inside a client response through the real packaged core. |
| `copying with a different status uses its serialized value` | Direct native copies are removed. `serverUpdatesReplaceUnknownStatusOnTheSameResource` instead checks a server transition to typed `Complete`, the created-session ID, stable resource identity and no automatic activation. |
| `reset password email code maps email address id using snake case` | `resetEmailCodeSendsSelectedIdentifierAndStrategy` selects the second available factor and checks `email_address_id`, `reset_password_email_code`, one preparation POST, and absence of camel-case ID fields. |
| `reset password phone code maps phone number id using snake case` | `resetPhoneCodeSendsSelectedIdentifierAndStrategy` checks the corresponding selected `phone_number_id`, strategy and request constraints. Existing shared cases also cover default and unknown selections. |
| `oauth first factor can be prepared from needs identifier status` | The removed raw `prepareFirstFactor` helper is replaced by the selected `signIn.sso` flow. `browserSsoStartsFromNeedsIdentifierWithoutAnEmail` checks the initial status, one OAuth creation request with the configured redirect and no identifier, callback reconciliation, stable resource identity, and no implicit activation. It does not claim the old private prepare endpoint is still public. |

This audit adds verification and retires obsolete tests. It changes no production implementation, generated bindings, or packaged core bytes. The tested bundle remains `25001603e9c152c6aaa2f8bad982bbab2a7da122efd6774df89518c809b131a5`, built from JavaScript revision `cba4fd161f0b1d80f48a9128d47a85c9a4ca0521`. Fixture browser callbacks and HTTP establish the SDK contract, not a live provider journey.
