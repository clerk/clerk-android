# Email-link preparation failures

`NativeCoreTests/Android/EmailLinkFailureTest.kt` exercises four public generated calls on packaged QuickJS: sign-in and sign-up, each with a secure-storage failure or a preparation API failure. Storage failure must prevent preparation. A preparation rejection must preserve the exact verifier record already saved, including flow kind, flow ID and a 43-character verifier. Neither failure activates a session. Native exceptions preserve the storage or Clerk API error code.

The actual emulator run passes all four tests. Matching cases pass on JavaScriptCore and in the shared embedded suite. The shared authentication request matrix also covers code strategies and password request construction. No bundle or production runtime change is needed.

The host uses deterministic HTTP and storage fixtures. This verifies QuickJS execution and generated Kotlin result/error handling; it does not prove mail-client callback delivery, production credentials or an old-major app upgrade.
