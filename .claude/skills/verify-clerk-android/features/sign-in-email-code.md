# Sign in with an email code

An existing user enters their email, asks for a one-time code instead of the email link, and signs in with the code.

## Sub-features

- `request-code` reaches the email code screen for an existing test user through `Use another method`, and stays on it.
- `complete` types `424242` and lands signed in.

## How to get to it (user POV)

- Open sign-in, type the email, tap Continue. On the standard instance the email-link screen ("Check your email") opens first. Tap `Use another method`, then `Email code to <email>`.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and launches `screen: 'auth'`, `authMode: 'signIn'`.

- **Request code.** Run `control-clerk-android run sign-in-email-code/request-code`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, waits for `signInStatus` `needs_first_factor`, taps `clerk.auth.signIn.useAnotherMethod` and `clerk.auth.signIn.alternativeMethod.email_code`, expects `clerk.auth.signIn.code`, and expects it still on screen 3 seconds later, with `signedIn` false. Screenshot `code-screen`.
- **Complete.** Run `control-clerk-android run sign-in-email-code/complete`. It is tagged `form-entry`. After the code screen it fills `CLERK_TEST_CODE` and waits for `signedIn` with `sessionStatus` `active` and the seeded `userId`. Screenshots `code-screen` and `signed-in`.
- **Proof.** Both specs pass, and `states.jsonl` ends with the seeded user's id and an active session.

## Gotchas

- `request-code` checks the code field a second time after 3 seconds. A sign-in that shows the code screen for a moment and then returns to "Check your email" would pass a single visibility check.
- A runtime that runs `--skip form-entry` proves this flow only as far as the code screen.
