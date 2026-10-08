# Sign in with an SMS code

An existing user enters their phone number, asks for a one-time SMS code instead of their password, and signs in with the code.

## Sub-features

- `complete` reaches the SMS code screen for a test user who has a phone and a password, through the password screen and `Use another method`, stays on it, types `424242`, and lands signed in on the home.

## How to get to it (user POV)

- Tap `Sign in` on the home, tap `Use phone number`, type the number, tap Continue. A user with a password sees "Enter password" first. Tap `Use another method`, then `Send SMS code to <number>`.

## Driving it with verify

Preconditions:

- The spec seeds a `+clerk_test` user with `host.seedUser({ phone: true, password: true })` on the standard instance and launches on the home. It never types the password.

- **Request code.** Run `control-clerk-android run sign-in-phone-code/complete`. It expects `Signed out` on the home, taps `e2e.auth.signIn`, and expects `clerk.auth.start.identifier` and `clerk.auth.start.continue`. It taps `clerk.auth.start.identifierSwitcher`, fills `clerk.auth.start.phoneNumber` with the ten digits of the user's phone, taps `clerk.auth.start.continue`, and expects `clerk.auth.signIn.password`. It taps `clerk.auth.signIn.useAnotherMethod`, expects and taps `clerk.auth.signIn.alternativeMethod.phone_code`, and expects `clerk.auth.signIn.code`, still on screen 3 seconds later, with "Check your phone" and the user's phone number on the code screen. Screenshots `auth-start`, `password-screen`, `methods`, and `code-screen`.
- **Complete.** After the code screen it fills `CLERK_TEST_CODE` and waits for the home to show `Signed in as` the seeded user's email with that user's ID. The AuthView is the one the home's button opens, and the home closes it only in `onAuthComplete`, in `onDismiss`, or on back. The spec neither closes it nor presses back, so reaching the home proves that `onAuthComplete` ran. Screenshot `signed-in`.
- **Proof.** The spec passes, and screenshot `signed-in` shows the home with the seeded user's email and ID.

## Gotchas

- `clerk.auth.start.phoneNumber` formats as you type and shows `+1 201-555-01xx`. Fill it with ten digits, `nationalDigits(user.phone!)` from `specs/phone.ts`. `host.fill` compares only the digits with what the field shows. The code screen shows the number, and the spec checks it there.
- The code screen shows the number unformatted, as `+120155501xx`, so the spec matches `user.phone` as it is.
- "Check your phone" has no test tag, so that check uses text.
