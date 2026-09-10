# Passkey registration and service assertion audit

The complete bodies of `PasskeyCreationServiceTest.kt` (nine declarations) and `PasskeyServiceTest.kt` (ten declarations) were reviewed and checked against baseline `1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`. Both files are retired. The creation suite had a class-level `@Ignore`; its declarations are not evidence of nine previously passing tests. [The proof](evidence/passkey-service/proof.json) preserves the original hashes, declarations, current source hashes and replacement reports.

## Current behavior and evidence

Six new `PasskeyRegistrationFailureTest` cases call generated `user.createPasskey()` through the actual packaged QuickJS runtime. They require:

- An initial HTTP 422 response retains the API code, message, long message, trace ID and typed parameter metadata. No credential is presented or submitted.
- A verification HTTP 422 response preserves the same fields after exactly one presentation and submission. The failed mutation is not replayed.
- An unexpected native provider exception becomes a structured `host_failure`, without submitting a credential or exposing the provider's private message.
- A malformed returned credential fails with `invalid_credential_result` before verification submission.
- Missing creation options fail before presentation, without inventing a successful passkey.
- An unsupported capability fails before creating a server passkey resource.

All six cases also require the selected user and session to remain the same. The fixture server does not add a passkey to the client in these failures; the tests require that the client projection does not invent one. They do not imply rollback of a server mutation that already succeeded.

`SignInCredentialContractTest` now has six cases, including its separate biometric case. The added unexpected-provider case requires `host_failure` at `requestingAuthorization`, no credential submission and no session adoption. Its preparation and submission failure checks now also require the API trace ID and long message. The shared TypeScript passkey test adds the matching provider-failure case and metadata assertions, including the existing second-factor paths.

The seventeen shared cases pass against the same bundle packaged by Android: nine sign-in/passkey cases, two registration success/cancellation cases, and six new registration failure cases. The Android proof contains nineteen packaged cases across the five passkey-related suites, and all 39 API unit cases pass. The fourteen native host cases from the [host audit](passkey-host-test-audit.md) continue to verify actual AndroidX request/response types, cancellation signals and missing-activity behavior.

The CI authentication runner now includes the three native passkey request cases, two registration success/cancellation cases, six registration failure cases and two session passkey verification cases. Each suite runs separately and must produce its exact named cases; a green Gradle result alone is insufficient. The existing sign-in suite includes the added provider-failure case too.

This work changes tests, CI test selection and migration evidence. The production TypeScript core, generated bindings, native hosts and packaged bundle are unchanged. Native code continues to own platform presentation and cancellation; the existing JavaScript implementation owns registration, sign-in and verification HTTP behavior.

## Every creation-service declaration

| Old declaration | Current disposition |
| --- | --- |
| `createPasskey succeeds when all operations are successful` | The generated registration case verifies the created passkey and verification state, then rename and deletion receipts. The core performs creation and verification; the native service is removed. |
| `createPasskey handles initial API failure gracefully` | The new creation-failure case requires the structured API response and zero presentations/verification requests. |
| `createPasskey uses correct request JSON` | The native host round trip compares the actual Credential Manager WebAuthn object, including the relying party, challenge, user, excluded credentials and authenticator options. The core owns binary conversion; raw JSON whitespace is not an API contract. |
| `parsePasskeyDataDirectFromBundle processes bundle correctly` | The native typed-response round trip preserves id, rawId, type and response fields. Packaged registration checks the credential submitted by TypeScript. The handwritten Bundle parser is removed. |
| `createPasskey handles cancellation without surfacing unknown failure` | Native CreateCredentialCancellationException maps to `user_cancelled`; the packaged registration cancellation case requires that code and no verification submission. |
| `createPasskey fails when no activity is available` | The native host tests require `presentation_unavailable` and no provider request for missing, finishing or destroyed activities. The core's unsupported-capability case separately checks zero server creation requests; these are different boundaries. |
| `createPasskey handles verification failure gracefully` | The new verification-failure case requires the API fields after one creation/presentation/submission and no retry. |
| `parsePasskeyDataDirectFromBundle throws when bundle is missing JSON` | The ignored test expected an exception to escape even though the baseline service caught generic exceptions and returned a failure. That assertion was not a verified contract. The private parser/message are removed; current typed-response mismatch and malformed-credential cases require structured failure before submission. |
| `createPasskey handles credential manager exception` | The new packaged provider-failure case throws a plain native exception, observes `host_failure`, and requires no verification submission. Its private message is not exposed. |

## Every service-facade declaration

| Old declaration | Current disposition |
| --- | --- |
| `signInWithPasskey delegates to PasskeyAuthenticationService with empty list` | The private facade is removed. Generated `signIn.passkey(...)` invokes the selected future-style core operation and receives the server's WebAuthn options. The packaged success case requires explicit session adoption. |
| `signInWithPasskey delegates to PasskeyAuthenticationService with credential IDs` | The old caller-supplied ID override is absent from `SignInFuturePasskeyParams`. The generated API follows that contract; no native-only overload is added. Server-provided allowed IDs remain covered by core and native request tests. |
| `signInWithPasskey can prefer immediately available credentials` | Generated sign-in forwards the option through the bridge, and the native host test requires the corresponding Credential Manager preference. |
| `authenticateWithPasskey delegates an existing sign in` | Call the generated operation on the current sign-in resource. Shared second-factor cases require preparation/submission on the existing attempt, without another sign-in creation; error cases preserve its handle. The private service/SignIn argument is removed. |
| `signInWithPasskey returns error when PasskeyAuthenticationService fails` | Packaged preparation/submission failures preserve status, API code, long message and trace ID through the future error-result binding. |
| `createPasskey delegates to PasskeyCreationService` | Generated `user.createPasskey()` returns the core-created resource. The native domain service is removed. |
| `createPasskey returns error when PasskeyCreationService fails` | The two registration API-failure cases retain structured errors and stop at the appropriate boundary. |
| `createPasskey handles unknown failure from PasskeyCreationService` | The registration provider-failure case requires a structured native-host failure without submission; it does not expose the deleted ClerkResult error category. |
| `signInWithPasskey with large credential list delegates correctly` | Like the smaller override, this native-only argument is absent from the selected TypeScript contract. The old test checked forwarding 100 values to a mock, not provider capacity. Neither equivalent override support nor an OS credential-list limit is claimed. |
| `signInWithPasskey handles unknown failure from PasskeyAuthenticationService` | The new generated sign-in provider-failure case requires a structured error with the correct presentation stage and no credential submission. |

`PasskeyAuthenticationServiceTest` and `PasskeyWebAuthnRequestTest` remain pending. Their automatic-prompt error policy, old relying-party fallback, credential filtering and second-factor session convenience assertions are not retired here. Real provider prompts, relying-party association, live service behavior and signed-in app upgrades remain release gates; fixture credentials do not prove them.
