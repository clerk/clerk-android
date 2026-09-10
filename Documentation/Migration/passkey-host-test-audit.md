# Passkey host and helper assertion audit

The complete bodies of `PasskeyCredentialManagerTest.kt` (three declarations) and `PasskeyHelperTest.kt` (seven declarations) were reviewed and checked byte-for-byte against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Both suites are retired. [The proof](evidence/passkey-host/proof.json) records their original hashes and individual declarations, current source hashes, and the verified test reports.

## Native host boundary

`AndroidPasskeyHostTest` adds seven methods, each executed with Robolectric's API 28 and 36 configurations. All fourteen cases pass. They call the production `AndroidCapabilities.perform` and the pinned AndroidX Credential Manager 1.6.0 implementation. A test-only shadow substitutes provider discovery; a deterministic CredentialProvider supplies callbacks. The production host, request converter and manager coroutine/cancellation path are not replaced.

The cases verify:

- Creation and assertion preserve the supplied relying-party ID, challenge, user handle, multiple allowed credentials, excluded credentials, transports and verification options. Only bridge binary wrappers and native presentation flags are converted. Assertion presentation receives the requested immediate-credential preference. Both public-key response payloads arrive intact.
- Password responses to public-key requests fail with `invalid_credential_response`, with no retry.
- Both platform cancellation exception types produce `user_cancelled` while the calling coroutine remains active.
- Provider configuration and missing-credential failures propagate without retry; the host does not decide an authentication fallback.
- Caller cancellation cancels Credential Manager's CancellationSignal for both creation and assertion. A late successful callback cannot complete the cancelled call. A subsequent request on the same host receives a separate, uncancelled signal and succeeds.
- Missing, finishing and destroyed activities fail before a provider request. A host without an activity supplier does not advertise passkeys.
- Unsupported conditional UI and malformed bridge challenge values fail before a provider request.

The complete API unit suite passes all 39 cases. These additions require no production adapter change or new dependency. The native runtime and packaged assets remain unchanged from the [verified connectivity release](lifecycle-test-audit.md#ci-and-release-verification).

## Packaged core boundary

All twelve named cases in four current Android suites pass on the API 36 arm64 emulator: `PasskeyRequestTest` (three), `PasskeyRegistrationTest` (two), `SessionPasskeyVerificationTest` (two), and `SignInCredentialContractTest` (five, including its separate biometric case). These exercise the native WebAuthn request objects and generated QuickJS-backed registration, rename/deletion, sign-in, reverification, cancellation, error stages and explicit session-adoption boundary. Registration and verification use fixture credentials and HTTP; they do not present the system passkey UI.

The ten shared `passkey.test.mjs` and `passkey-registration.test.mjs` cases also pass against JavaScript bundle SHA-256 `406af90fdb32effc6fb3c1b35b52ceea639a68ceb3874b4d1390fd958a339f7d`, byte-identical to the Android asset. They additionally cover second-factor preparation/submission and their failure stages. The shared tests' synthetic autofill-capable host does not imply Android autofill support: the current Android adapter rejects conditional UI, which the new host test checks.

A combined Gradle class-filter invocation returned success but its XML contained only the three PasskeyRequestTest cases. The exact-case verifier rejected that as incomplete. The other suites were then run individually and their two, two and five named cases verified before preserving the reports. Gradle's existing deprecation summary and Node's existing `MODULE_TYPELESS_PACKAGE_JSON` warning remain recorded in the evidence logs.

## Every retired declaration

| Old declaration | Current disposition |
| --- | --- |
| `PasskeyCredentialManagerImpl can be used as PasskeyCredentialManager interface` | The private abstraction and implementation are deleted. The new test reaches the actual AndroidX manager through the production host; no replacement native domain facade is introduced. |
| `createCredential method exists and can be called` | The old smoke test caught any exception and asserted no result. Current creation checks require the complete native request and response, reject another credential type, distinguish platform/caller cancellation and verify the platform cancellation signal. |
| `getCredential method exists and can be called` | The old smoke test also caught any exception. Current assertion checks require the complete request, presentation preference and response, plus response-type, failure and cancellation behavior. |
| `getDomain returns domain without www prefix` | The private singleton URL-normalization helper is removed. Native code forwards the relying-party ID supplied through the TypeScript WebAuthn options; the host test deliberately uses a relying-party ID different from its HTTP origin. It does not retain a native `www`-stripping policy. |
| `getDomain returns domain without protocol and path` | The same private helper is removed. The native host accepts the core's WebAuthn request and does not derive a relying-party ID from Clerk's HTTP URL. |
| `getDomain returns empty string when host is null` | The helper's private empty-string fallback is removed, rather than claimed as equivalent behavior. Configuration validation and server-option handling belong to their current owners. |
| `getDomain handles malformed URL gracefully` | The deleted helper's malformed-URL fallback is not part of the generated API. This retirement does not claim all malformed server nonce paths are covered. |
| `GetPasskeyRequest serializes correctly` | Despite its name, the old test only checked four stored fields. The new host case checks the actual Credential Manager JSON, including both allowed credential IDs and the requested verification level, as well as timeout, relying-party ID and transports. |
| `PublicKeyCredentialData holds correct data` | The handwritten data class is removed. The host round trip requires the full typed credential response, including id, rawId, type and nested response data. Packaged and shared registration cases verify credential submission through the TypeScript core. |
| `constants have expected values` | Private constant-value checks are retired. Current packaged registration/sign-in/reverification cases assert the actual `strategy=passkey` HTTP field, owned by the existing JavaScript implementation. |

`PasskeyWebAuthnRequestTest` and the three legacy passkey service suites remain pending their own complete assertion audits. In particular, this change does not declare their old relying-party fallback or credential-filtering behavior migrated. Successful physical-device prompts, provider/account availability, real relying-party association and signed-in old-major upgrades remain release gates. The deterministic provider and fixture-core tests establish none of those live outcomes.
