# Rendered authentication journey

`AuthViewJourneyTest` renders the production `AuthView` inside `ClerkProvider` on an Android emulator. It connects a real packaged QuickJS owner and uses the generated resources and state projection. HTTP responses and credential persistence are deterministic fixtures; the test never creates a live account.

The test interacts with the actual Compose text fields and Continue button. It checks three stages:

1. Enter an email address and open email-code verification. The attempt needs its first factor, with no selected session and no completion callback.
2. Enter `000000`. The fixture rejects the request; the native screen displays the structured error message. The session remains absent, and no finalization touch or completion callback occurs.
3. Replace the code with `424242`. The prebuilt flow calls generated finalization, then `LocalClerk.isAuthFlowComplete` reveals the test application's authenticated content. The same core exposes the active session and expected user. Exactly one touch and one completion callback occurred; the fixture received both submitted codes in order.

The test does not call `finalize()` itself or assign a selected session. The existing presentation-state tests separately cover pending session tasks, sign-up finalization, registration ownership and duplicate completion prevention.

## Run and evidence

With the configured JDK, Android SDK and emulator:

```sh
bash scripts/run-auth-view-journey.sh
```

The script requires the exact named passing XML case. It sets `additionalTestOutputDir` so Gradle collects screenshots before removing the test application, following [Android's test-output mechanism](https://developer.android.com/topic/performance/benchmarking/benchmarking-in-ci). The API 36 x86_64 CI job now invokes this script and archives its XML and screenshots; that remote run remains unverified.

On September 10, 2026, the test passed on the API 36 arm64 Pixel 9 Pro emulator. [The proof record](evidence/auth-view-journey/proof.json) identifies the tested source hash, TypeScript revision and packaged bundle. [The exact XML](evidence/auth-view-journey/TEST-auth-view-journey.xml) contains one case with zero failures, errors or skips. Its zero elapsed-time field is a runner artifact and is not a performance measurement. A subsequent run of the complete UI instrumentation target passed all 14 cases with zero failures, errors or skips, including ownership, organization-list and completion-state checks.

The retained screenshots were visually inspected:

- [Identifier entry](evidence/auth-view-journey/01-identifier.png)
- [Invalid-code error](evidence/auth-view-journey/02-invalid-code.png)
- [Authenticated test-host content](evidence/auth-view-journey/03-completed.png)

An initial fixture omitted `attempts` and `verified_at_client` from its verification object. The existing source assigns these fields directly, and the generated projection requires their nullable values. Preparation therefore could not publish a valid verification state. Reusing the complete fixture shape corrected the test; no production SDK behavior was changed to make it pass.

This is rendered fixture integration, not proof of live authentication, real OS prompts, an old-major app upgrade, all supported platforms, or physical-device performance.
