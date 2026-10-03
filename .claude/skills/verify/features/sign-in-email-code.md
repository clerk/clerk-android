# Sign in with an email code

An existing user enters their email, asks for a one-time code instead of the email link, and signs in with the code.

## Sub-features

- `request-code` reaches the email code screen for an existing test user through `Use another method`.
- `complete` types `424242` and lands signed in.

## How to get to it (user POV)

- Open sign-in, type the email, tap Continue. On `with-email-codes` the email-link screen ("Check your email") opens first. Tap `Use another method`, then `Email code to <email>`.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on `with-email-codes` and launches `screen: 'auth'`, `authMode: 'signIn'`.

- **Request code.** Run `bin/verify run sign-in-email-code/request-code`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, waits for `signInStatus` `needs_first_factor`, taps `clerk.auth.signIn.useAnotherMethod` and `clerk.auth.signIn.alternativeMethod.email_code`, expects `clerk.auth.signIn.code`, and expects it still on screen 3 seconds later. Screenshot `code-screen`.
- **Complete.** Run `bin/verify run sign-in-email-code/complete`. It is tagged `form-entry`. After the code screen it fills `CLERK_TEST_CODE` and waits for `signedIn` with `sessionStatus` `active` and the seeded `userId`. Screenshots `code-screen` and `signed-in`.
- **Proof.** Both specs pass, and `states.jsonl` ends with the seeded user's id and an active session.

## Gotchas

- Known SDK bug, reproduced on 2026-10-03 at clerk-android `275f08a8` and on trunk: choosing `Email code` under `Use another method` returns to "Check your email" instead of staying on the code screen. `request-code` fails on it with `observed: no node (match count 0)` for `clerk.auth.signIn.code`, and `complete` cannot pass until it is fixed. Treat a pass as the fix landing.
- The hold check (code field still visible 3 seconds later) exists because the code screen can render for a moment before the bounce, which would let a single visibility check pass.
