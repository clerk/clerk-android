# Sign in with an email code

An existing user enters their email, gets a one-time code, and signs in with the code.

## Sub-features

- `complete` reaches the email code screen for an existing test user straight from Continue, stays on it, types `424242`, and lands signed in.

## How to get to it (user POV)

- Open sign-in, type the email, tap Continue. On the standard instance a user with no password lands on the email code screen ("Check your email"). Type the code.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and launches with `authMode: 'signIn'`, then taps `e2e.auth.signInFullScreen`.

- **Request code.** Run `e2e-tests/bin/control-clerk-android run sign-in-email-code/complete`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, expects `clerk.auth.signIn.code`, and expects it still on screen 3 seconds later, with the email on the code screen. Screenshot `code-screen`.
- **Complete.** After the code screen it fills `CLERK_TEST_CODE` and waits for the home to show `Signed in as` the seeded user's email with that user's ID. Screenshot `signed-in`.
- **Proof.** The spec passes, and screenshot `signed-in` shows the home with the seeded user's email and ID.

## Gotchas

- The spec checks the code field a second time after 3 seconds. A sign-in that shows the code screen for a moment and then leaves it would pass a single visibility check.
- The standard instance has the email link off, because a test address gets no mail, so a run has no link to follow. With the link on, AuthView opens the email-link screen first. It has the same title as the code screen, "Check your email", and no code field, so `clerk.auth.signIn.code` tells them apart. A spec that needs the link declares `auth_email.sign_in_strategies` in a settings file.
