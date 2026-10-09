# clerk-android verification map

This directory is the maintained source for verifying the user-facing behavior of the clerk-android SDK UI (`source/ui`) through the `:e2e` host app. Read this index before driving the app, then use the matching feature file as the recipe. Every recipe runs through the `control-clerk-android` CLI and the golden specs under `specs/golden/<feature>/`. Run its commands from the repo root. Paths that begin `specs/` are inside `e2e-tests/`.

## Test users and sign-in

Clerk's test mode makes all of this safe to type into the real app.

- Emails. Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints `verify_<runId>_<n>+clerk_test@example.com` per run, through `host.newEmail()` or `host.seedUser()`.
- Phones. US numbers 555-0100 to 555-0199 are test numbers. Get one from `host.seedUser({ phone: true })`, or from `host.newPhone()` for a number no user holds. Both give `+1` and the ten digits, such as `+12015550142`. AuthView's phone field takes the ten digits, `nationalDigits(phone)` from `specs/phone.ts`. The host's own fields take the whole number.
- One-time code. `424242` verifies every email and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`.
- Authenticator code. Read the key from the `Add authenticator application` screen and pass it to `totp(key, Date.now() / 1000)` from `specs/totp.ts`, which gives the six digits an authenticator would show.
- Passwords. The standard instance requires a password at sign-up. Use a throwaway per run, such as `Verify-<runId>-Pw1!`. For a password sign-in, seed the user with `host.seedUser({ password: true })` and fill the field with `host.fill(field, user.password!)`.

| Step | SDK test tag |
| --- | --- |
| Identifier field (email or username) | `clerk.auth.start.identifier` |
| Switch between email and phone | `clerk.auth.start.identifierSwitcher` |
| Phone field | `clerk.auth.start.phoneNumber` |
| Continue on the start screen | `clerk.auth.start.continue` |
| Sign-in code field | `clerk.auth.signIn.code` |
| Sign-in password field | `clerk.auth.signIn.password` |
| `Use another method` (code, password, and email link screens) | `clerk.auth.signIn.useAnotherMethod` |
| Pick a method from the list | `clerk.auth.signIn.alternativeMethod.<strategy>`, for example `clerk.auth.signIn.alternativeMethod.email_code` |
| Sign-up email, password, continue | `clerk.auth.signUp.emailAddress`, `clerk.auth.signUp.password`, `clerk.auth.signUp.continue` |
| Sign-up code field | `clerk.auth.signUp.code` |
| Legal consent, when shown | `clerk.auth.signUp.legalAccepted` |
| MFA setup choices | `clerk.auth.sessionTask.setupMfa.smsCode`, `clerk.auth.sessionTask.setupMfa.authenticatorApp` |

The names come from `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. They are internal test hooks and may change.

On the standard instance an email sign-in starts on the email-link screen ("Check your email"), so a spec switches to the email code through `Use another method`. For a phone sign-in, tap `clerk.auth.start.identifierSwitcher`, fill `clerk.auth.start.phoneNumber` with the ten digits of the seeded user's phone, and continue. A user with a password sees the password screen first and reaches the SMS code through `Use another method`. For sign-up, launch with `authMode: 'signUp'`, fill the email from `host.newEmail()` into `clerk.auth.start.identifier`, continue, fill the run password into `clerk.auth.signUp.password`, continue, then type the code into `clerk.auth.signUp.code`.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior, then exactly four H2 sections in this order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify`, and `Gotchas`. `Driving it with verify` starts with `Preconditions:` and names the golden specs that prove each sub-feature. `Gotchas` names each locator that falls back to visible text.

## Features

- [Auth start](./auth-start.md) covers opening AuthView full screen from the home, and opening it with an initial identifier.
- [Sign in with an email code](./sign-in-email-code.md) covers requesting and entering the email code.
- [Sign in with an SMS code](./sign-in-phone-code.md) covers requesting and entering the SMS code from the AuthView the home opens.
- [Custom sign-in flow](./custom-flow-sign-in.md) covers the host's own form on `Clerk.auth.signInWithOtp`.
- [Sign up](./sign-up.md) covers the password step, requesting the sign-up code, completing sign-up, and signing up with a phone number.
- [User button and profile](./user-button-and-profile.md) covers UserButton, UserProfileView, its Manage account and Security screens, sign-out, and deleting the account.
- [Session tasks](./session-tasks.md) covers the setup-MFA and choose-organization tasks after sign-in, the setup-MFA task after sign-up, and finishing the setup-MFA task with an authenticator code.
- [Organizations](./organizations.md) covers creating an organization from OrganizationSwitcher.

Not mapped: a password sign-in past the password screen, completing the setup-MFA task with an SMS code, social providers (no real OAuth on emulators), passkeys, and biometrics.
