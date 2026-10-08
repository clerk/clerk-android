# Sign up

A new user enters a test email, sets a password, enters the emailed code, and lands signed in.

## Sub-features

- `complete` reaches the password step for a new test email, sets the run password, types `424242` on the code screen, and lands signed in.

## How to get to it (user POV)

- Open AuthView in sign-up mode, type an email, tap Continue, type a password, tap Continue, then type the code.

## Driving it with verify

Preconditions:

- The spec reserves an email with `host.newEmail()` and launches with `authMode: 'signUp'`, then taps `e2e.auth.signInFullScreen`. `control-clerk-android down` deletes the user the form creates.

- **Password step.** Run `control-clerk-android run sign-up/complete`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, and expects `clerk.auth.signUp.password` and `clerk.auth.signUp.continue`. Screenshot `signup-password`.
- **Request code.** It then fills `clerk.auth.signUp.password` with `Verify-<runId>-Pw1!`, taps `clerk.auth.signUp.continue`, and expects `clerk.auth.signUp.code`. Screenshot `signup-code`.
- **Complete.** After the code screen it fills `CLERK_TEST_CODE` and waits for the home to show `Signed in as` the new email with a user ID. Screenshot `signed-up`.
- **Proof.** The spec passes. `run.json` lists the reserved email under `identities` with the id of the user the form created.

## Gotchas

- Android asks for the password before the code.
- The password field withholds its value from the screen tree. `host.fill` types the password once and cannot confirm that it landed, so assert on what the form does next.
