# Custom sign-in flow

An app that builds its own sign-in screen on the SDK's API sends an SMS code to an existing user and signs them in with it. No prebuilt view is involved, so the `:e2e` host has a small form of its own for this.

## Sub-features

- `request-code` shows Clerk's error for a number no user holds.
- `complete` sends the SMS code, shows the code field, verifies the code, activates the session, and returns to the home, which shows the user and the session.

## How to get to it (user POV)

- Launch the app signed out and tap `Custom sign-in`. The screen has a phone field and `Send code`, then a code field and `Verify code`.

## Driving it with verify

Preconditions:

- The standard settings turn the `phone_code` strategy on. `up` fails with `INSTANCE_MISCONFIGURED` when the application does not show it.
- The spec seeds its own `+clerk_test` user with `host.seedUser({ phone: true })`.

- **Request the code.** Run `e2e-tests/bin/control-clerk-android run custom-flow-sign-in/complete`. Each test launches and taps `e2e.home.customSignIn`, which `customSignInLink(screen)` in `specs/compose.ts` finds. The spec fills `verify.customSignIn.phoneNumber` with the user's phone, taps `verify.customSignIn.sendCode`, and expects `verify.customSignIn.code` and `verify.customSignIn.verifyCode` with no `verify.customSignIn.error`. The form shows the code field only when `Clerk.auth.signInWithOtp` returned success. Screenshot `custom-code`.
- **Unknown number.** Run `e2e-tests/bin/control-clerk-android run custom-flow-sign-in/request-code`. It fills a number from `host.newPhone()`, which no user holds, and expects `verify.customSignIn.error` to read `Couldn't find your account.`, with no code field. Screenshot `custom-error`.
- **Enter the code.** `complete` then types `CLERK_TEST_CODE` and taps `verify.customSignIn.verifyCode`, which calls `SignIn.verifyCode` and then `Clerk.auth.setActive`. The host leaves the form only when both returned success, so `host.expectSignedInAs(user)`, which waits for the home to show the user's email, the seeded user ID, and a session ID, proves both. Screenshot `custom-signed-in`.
- **Proof.** Both specs pass.

## Gotchas

- The form trims what is typed and changes nothing else, so fill the whole number, `user.phone`, with its `+1`.
- A sign-in error shows in `verify.customSignIn.error`, and the form stays on screen.
- The host shows the form until the form reports that its sign-in succeeded. A session alone does not close it, because the SDK has an active session as soon as the code is verified, before `setActive` is called.
- `Couldn't find your account.` is Clerk's message for an identifier no user holds. It comes from the API, so a change of that wording fails `request-code`.
- The screen keeps no state across launches. Every launch starts on the home, and the link opens the form on the phone field.
