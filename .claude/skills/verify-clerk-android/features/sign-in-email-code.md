# Sign in with an email code

An existing user enters their email, asks for a one-time code instead of the email link, and signs in with the code.

## Sub-features

- `complete` reaches the email code screen for an existing test user through `Use another method`, stays on it, types `424242`, and lands signed in.

## How to get to it (user POV)

- Open sign-in, type the email, tap Continue. On the standard instance the email-link screen ("Check your email") opens first. Tap `Use another method`, then `Email code to <email>`.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and launches with `authMode: 'signIn'`, then taps `e2e.auth.signInFullScreen`.

- **Request code.** Run `control-clerk-android run sign-in-email-code/complete`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, waits for `clerk.auth.signIn.useAnotherMethod`, taps it and `clerk.auth.signIn.alternativeMethod.email_code`, expects `clerk.auth.signIn.code`, and expects it still on screen 3 seconds later, with the email on the code screen. Screenshot `code-screen`.
- **Complete.** After the code screen it fills `CLERK_TEST_CODE` and waits for the home to show `Signed in as` the seeded user's email with that user's ID. Screenshot `signed-in`.
- **Proof.** The spec passes, and screenshot `signed-in` shows the home with the seeded user's email and ID.

## Gotchas

- The spec checks the code field a second time after 3 seconds. A sign-in that shows the code screen for a moment and then returns to the email-link screen would pass a single visibility check.
- The email-link screen and the email code screen both have the title "Check your email". The code field, `clerk.auth.signIn.code`, tells them apart.
