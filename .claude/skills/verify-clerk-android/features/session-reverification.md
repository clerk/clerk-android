# Session reverification

A signed-in user who can't reverify with their password, such as one whose password showed up in a breach, reverifies the session with a reset password code sent to their verified email address instead. The same step sets a new password. The SDK has no prebuilt view for reverification, so the `:e2e` host has a small form of its own on the SDK's API.

## Sub-features

- `reset-password-code` starts a first-factor reverification, sends a reset password code to the user's email with `Session.sendResetPasswordEmailCode`, verifies it with `Session.verifyWithResetPasswordEmailCode` and a new password, and then signs in again with the new password.

## How to get to it (user POV)

- Sign in and tap `Reverify with a reset code` on the home. The screen has `Send reset code`, then the address the code went to, a code field, a new password field, and `Verify and set password`. A completed reverification returns to the home.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user with `host.seedUser({ password: true })` and signs in with a ticket. It declares no settings. The server offers `reset_password_email_code` in a reverification's `supported_first_factors` for every user with a password, unless the instance turns off password reverification, which the standard settings do not.

- **Send the code.** Run `e2e-tests/bin/control-clerk-android run session-reverification`. The spec taps `e2e.home.resetPasswordReverification`, then `verify.resetPasswordReverification.sendCode`. The form calls `Session.startVerification` at the first-factor level, picks the `reset_password_email_code` factor, and calls `Session.sendResetPasswordEmailCode` with its email address ID. It shows `verify.resetPasswordReverification.sentTo` only when both returned success. Screenshot `reset-code-sent`.
- **Verify and set the password.** The spec fills `verify.resetPasswordReverification.code` with `CLERK_TEST_CODE` and `verify.resetPasswordReverification.newPassword` with `runPassword(host.runId)`, then taps `verify.resetPasswordReverification.verify`. The form returns to the home only when the verification's status is `complete`, so `host.expectSignedInAs(user)` proves it. Screenshot `reverified`.
- **Sign in with the new password.** The spec signs out, opens `Sign in full screen`, enters the user's email, fills `clerk.auth.signIn.password` with the new password, and taps `clerk.auth.signIn.continue`. The home shows the same user. The run never set that password any other way, so this proves the reverification set it. Screenshot `signed-in-with-new-password`.
- **Proof.** `specs/features/session-reverification/reset-password-code.e2e.ts` passes.

## Gotchas

- A reverification error shows in `verify.resetPasswordReverification.error`, and the form stays on screen. When the server does not offer the strategy, the error reads `Reverification does not offer reset_password_email_code.`
- A status of `needs_second_factor` also shows as an error, because the form has no second-factor step. The standard instance does not require MFA, so the spec does not reach it.
- Screenshots blank the password field.
