# Sign up

A new user enters a test email, sets a password, enters the emailed code, and lands signed in.

## Sub-features

- `password-step` reaches the password step for a new test email.
- `request-code` sets the run password and reaches the sign-up code screen.
- `complete` types `424242` and lands signed in.

## How to get to it (user POV)

- Open AuthView in sign-up mode, type an email, tap Continue, type a password, tap Continue, then type the code.

## Driving it with verify

Preconditions:

- The spec reserves an email with `host.newEmail('with-email-codes')` and launches `screen: 'auth'`, `authMode: 'signUp'`. `.cursor/skills/verify-clerk-android/bin/control-clerk-android down` deletes the user the form creates.

- **Password step.** Run `.cursor/skills/verify-clerk-android/bin/control-clerk-android run sign-up/password-step`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, expects `clerk.auth.signUp.password` and `clerk.auth.signUp.continue`, and waits for `signUpStatus` `missing_requirements`. Screenshot `signup-password`.
- **Request code.** Run `.cursor/skills/verify-clerk-android/bin/control-clerk-android run sign-up/request-code`. It is tagged `form-entry` because it types the run password. It fills `clerk.auth.signUp.password` with `Verify-<runId>-Pw1!`, taps `clerk.auth.signUp.continue`, and expects `clerk.auth.signUp.code`. Screenshot `signup-code`.
- **Complete.** Run `.cursor/skills/verify-clerk-android/bin/control-clerk-android run sign-up/complete`. It is tagged `form-entry`. After the code screen it fills `CLERK_TEST_CODE` and waits for `signedIn` with `sessionStatus` `active`. Screenshots `signup-code` and `signed-up`.
- **Proof.** All three pass, and `run.json` lists the reserved email under `identities` with the user id the form created.

## Gotchas

- Android asks for the password before the code. iOS asks after.
- A runtime that runs `--skip form-entry` proves sign-up only up to the password step.
