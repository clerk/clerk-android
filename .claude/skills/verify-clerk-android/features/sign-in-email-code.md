# Sign in with an email code

An existing user enters their email, asks for a one-time code instead of the email link, and signs in with the code.

## Sub-features

- `request-code` reaches the email code screen for an existing test user through `Use another method`.
- `complete` types `424242` and lands signed in.

## How to get to it (user POV)

- Open sign-in, type the email, tap Continue. On the standard instance the email-link screen ("Check your email") opens first. Tap `Use another method`, then `Email code to <email>`.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user on the standard instance and launches `screen: 'auth'`, `authMode: 'signIn'`.

- **Request code.** Run `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-in-email-code/request-code --include known-bug`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, waits for `signInStatus` `needs_first_factor`, taps `clerk.auth.signIn.useAnotherMethod` and `clerk.auth.signIn.alternativeMethod.email_code`, expects `clerk.auth.signIn.code`, and expects it still on screen 3 seconds later. Screenshot `code-screen`.
- **Complete.** Run `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-in-email-code/complete --include known-bug`. It is tagged `form-entry` and `known-bug`. After the code screen it fills `CLERK_TEST_CODE` and waits for `signedIn` with `sessionStatus` `active` and the seeded `userId`. Screenshots `code-screen` and `signed-in`.
- **Proof.** Both specs pass, and `states.jsonl` ends with the seeded user's id and an active session.

## Gotchas

- Both specs are tagged `known-bug`. They reproduce the known clerk-android email-code bounce: choosing `Email code` under `Use another method` returns to "Check your email" instead of staying on the code screen. `.claude/skills/verify-clerk-android/bin/control-clerk-android run` skips them by default and prints `skipped: known-bug`. `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-in-email-code --include known-bug` runs them, and `request-code` fails with `observed: no node (match count 0)` for `clerk.auth.signIn.code`. Reproduced on 2026-10-03 at clerk-android `275f08a8`: screenshots taken right after the tap and 1.5 and 4.5 seconds later all show "Check your email", and a second attempt bounces the same way. When `request-code` passes with `--include known-bug`, the SDK is fixed: drop the tag from both specs.
- The hold check (code field still visible 3 seconds later) exists because the code screen can render for a moment before the bounce, which would let a single visibility check pass.
