# clerk-android verification map

This directory is the maintained source for verifying the user-facing behavior of the clerk-android SDK UI (`source/ui`) through the `:e2e` host app. Read this index before driving the app, then use the matching feature file as the recipe. Every recipe runs through the `control-clerk-android` CLI and the specs under `specs/features/<feature>/`. Run its commands from the repo root. Paths that begin `specs/` are inside `e2e-tests/`.

## Baseline preconditions

`SKILL.md` under Launch has the setup and the commands. These facts hold for every recipe here.

- The standard settings have every auth method and signed-in feature that these recipes use, except the session tasks. A spec that needs a task declares it in its settings file, and `run` puts the application on those settings before that spec. The `settings` check of `doctor` compares the live instance with the settings that `up` or `run` last put it on, once the worktree holds an application.
- Every launch gets a new `verifyStorageScope`, so no spec inherits a session from another spec. A launch with `keepStorage: true` reuses the scope of the launch before it in the same test.
- `down` deletes this worktree's application, and with it every user and organization a run created, including users created through the sign-up form.
- The AuthView that `Sign in` opens takes the place of the home, so the home is readable only after AuthView has closed.

## Test users and sign-in

Clerk's test mode makes all of this safe to type into the real app.

- Emails. Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints an address such as `maya.7c1e+clerk_test@example.com` through `host.newEmail()` or `host.seedUser()`: a first name that no other test of the run gets, and the last four characters of the run id, here `r20261002-141210-7c1e`.
- Phones. US numbers 555-0100 to 555-0199 are test numbers. Get one from `host.seedUser({ phone: true })`, or from `host.newPhone()` for a number no user holds. Both give `+1` and the ten digits, such as `+12015550142`. AuthView's phone field takes the ten digits, `nationalDigits(phone)` from `specs/phone.ts`. The host's own fields take the whole number.
- One-time code. `424242` verifies every email and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`.
- Authenticator code. Read the key from the `Add authenticator application` screen and pass it to `totp(key, Date.now() / 1000)` from `specs/totp.ts`, which gives the six digits an authenticator would show.
- Passwords. The standard instance requires a password at sign-up. Fill it with `runPassword(host.runId)` from `@clerk/internal-mobile-e2e/specs/support/clerk`, such as `Otter-Lamp-3917`. For a password sign-in, seed the user with `host.seedUser({ password: true })` and fill the field with `host.fill(field, user.password!)`.

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

On the standard instance an email sign-in for a user with no password goes straight to the email code screen ("Check your email"). The email link is off there. For a phone sign-in, tap `clerk.auth.start.identifierSwitcher`, fill `clerk.auth.start.phoneNumber` with the ten digits of the seeded user's phone, and continue. A user with a password sees the password screen first and reaches the SMS code through `Use another method`. For sign-up, launch with `authMode: 'signUp'`, fill the email from `host.newEmail()` into `clerk.auth.start.identifier`, continue, fill the run password into `clerk.auth.signUp.password`, continue, then type the code into `clerk.auth.signUp.code`.

## Driving conventions

- Input reaches the app only through specs. To look at any screen past launch, write a spec, `run` it, then run `screen`.
- Prefer SDK test tags (`screen.getByTestId('clerk....')`) and the locators of the home in `host.app`. Fall back to visible text only where nothing is tagged.

## Proof

- A proof is a passing `run` whose run directory holds `video.mp4`, `screenshots/`, `app.log`, and `e2e/report.json`.
- Name the run id and the specs in the PR. Attach with `e2e-tests/bin/control-clerk-android attach <run-id> --pr <n>` and `--screenshot <label>` for a still change or `--video` for behavior or a flow, with the part that shows the change inside `host.evidence`. Add `--before <run-id>` when the change is to something that exists. `SKILL.md` has the choice under Evidence.
- Report an unreachable path with the attempted command and the unmet precondition.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior, then exactly four H2 sections in this order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify`, and `Gotchas`. `Driving it with verify` starts with `Preconditions:` and names the specs that prove each sub-feature. `Gotchas` names each locator that falls back to visible text.

## Features

- [Auth start](./auth-start.md) covers opening AuthView from the home with a close button and full screen, closing it, and opening it with an initial identifier.
- [Sign in with an email code](./sign-in-email-code.md) covers requesting and entering the email code.
- [Sign in with an SMS code](./sign-in-phone-code.md) covers requesting and entering the SMS code from the AuthView the home opens.
- [Custom sign-in flow](./custom-flow-sign-in.md) covers the host's own form on `Clerk.auth.signInWithOtp`.
- [Sign up](./sign-up.md) covers the password step, requesting the sign-up code, completing sign-up in each AuthView the home opens, and signing up with a phone number.
- [Session persistence](./session-persistence.md) covers a session that survives a restart of the app.
- [User button and profile](./user-button-and-profile.md) covers UserButton, UserProfileView, its Manage account and Security screens, sign-out, and deleting the account.
- [Session tasks](./session-tasks.md) covers the setup-MFA and choose-organization tasks after sign-in, the setup-MFA task after sign-up, and finishing the setup-MFA task with an authenticator code.
- [Organizations](./organizations.md) covers creating an organization from OrganizationSwitcher and switching between it and the personal account.
- [Organization profile](./organization-profile.md) covers the profile that `Manage organization` opens: its rows, the Members tabs, sending an invitation, verified domains, and renaming. It covers a member who can read domains but not manage them, and a request to join on the Requests tab, with organizations that specs seed, and it lists the states no spec reaches.

Not mapped: a password sign-in past the password screen, the email link (off on the standard instance), completing the setup-MFA task with an SMS code, the reset-password session task, the choose-organization task's list of memberships, invitations, and suggestions, organization invitations in the switcher (the harness cannot seed them; the Requests test in [Organization profile](./organization-profile.md) covers a suggestion), social providers (no real OAuth on emulators), passkeys, biometrics, and Restore Credentials (the host never turns it on).
