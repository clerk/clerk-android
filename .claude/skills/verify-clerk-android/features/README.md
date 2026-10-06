# clerk-android verification map

This directory is the maintained source for verifying the user-facing behavior of the clerk-android SDK UI (`source/ui`) through the `:e2e` host app. Read this index before driving the app, then use the matching feature file as the recipe. Every recipe runs through `control-clerk-android` and the golden specs under `specs/golden/<feature>/`. Commands and short paths are written as in `SKILL.md`.

## Baseline preconditions

- Run `npm ci --prefix .claude/skills/verify-clerk-android` once per worktree, then `control-clerk-android doctor`. Before the first `control-clerk-android up`, `build` is its one failing check. `lane-ports` also fails while an emulator that is not a lane of this CLI sits on 5560 or 5562.
- The build needs Java 21. With `JAVA_HOME` unset the CLI uses Android Studio's JBR. With `JAVA_HOME` on anything older than 21, an `up` that has to build refuses, and `doctor`'s `jdk` check fails with the same fix.
- Specs run on the standard test instance, in one Clerk application that `up` creates for this worktree and `down` deletes. A spec names no instance, and a spec file that needs other settings declares them. `references/instances.md` has the declaration and the Platform API key the CLI needs. Never print a key.
- The CLI drives only its own lane emulator, `verify-android-<n>` (`emulator-5560` or `emulator-5562`), booted `-read-only` from the `Clerk_Verify_Pixel` AVD. Never drive another emulator, the AVD you use in Android Studio, or a physical device.
- Every launch gets a new storage scope, and the host clears Clerk's stored client when the scope changes, so no spec inherits a session from another spec.

## Test users and sign-in

Clerk's test mode makes all of this safe to type into the real app.

- Emails. Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints `verify_<runId>_<n>+clerk_test@example.com` per run, through `host.newEmail()` or `host.seedUser()`.
- Phones. US numbers 555-0100 to 555-0199 are test numbers, typed as ten digits such as `5555550142`. Get one from `host.seedUser({ phone: true })`.
- One-time code. `424242` verifies every email and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`. It is public, so screenshots after the fill are kept.
- Passwords. The standard instance requires a password at sign-up. Use a throwaway per run, such as `Verify-<runId>-Pw1!`.
- Authenticator codes. The authenticator setup screen shows its key as untagged text. Compute the 6-digit RFC 6238 code (SHA-1, 30 second step) from it.

| Step | SDK test tag |
| --- | --- |
| Identifier field (email or username) | `clerk.auth.start.identifier` |
| Switch between email and phone | `clerk.auth.start.identifierSwitcher` |
| Phone field | `clerk.auth.start.phoneNumber` |
| Continue on the start screen | `clerk.auth.start.continue` |
| Sign-in code field | `clerk.auth.signIn.code` |
| Sign-in password field | `clerk.auth.signIn.password` |
| Try another method (code, password, and email link screens) | `clerk.auth.signIn.useAnotherMethod` |
| Pick a method from the list | `clerk.auth.signIn.alternativeMethod.<strategy>`, for example `clerk.auth.signIn.alternativeMethod.email_code` |
| Sign-up email, password, continue | `clerk.auth.signUp.emailAddress`, `clerk.auth.signUp.password`, `clerk.auth.signUp.continue` |
| Sign-up code field | `clerk.auth.signUp.code` |
| Legal consent, when shown | `clerk.auth.signUp.legalAccepted` |
| MFA setup choices | `clerk.auth.sessionTask.setupMfa.smsCode`, `clerk.auth.sessionTask.setupMfa.authenticatorApp` |

The names come from `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. They are internal test hooks and may change, so keep this table in sync with that file.

On the standard instance an email sign-in starts on the email-link screen ("Check your email"), so a spec switches to the email code through `Use another method`. A phone sign-in shows `clerk.auth.start.phoneNumber` first: fill it with the seeded user's phone, continue, and type the same code. For sign-up, launch with `authMode: 'signUp'`, fill the email from `host.newEmail()` into `clerk.auth.start.identifier`, continue, fill the run password into `clerk.auth.signUp.password`, continue, then type the code into `clerk.auth.signUp.code`. Android asks for the password before the code.

Rules:

- Type only `+clerk_test` emails, 555-0100 to 0199 phones, `424242`, and the run password. The repo is public and every video can land on a PR.
- A change to sign-in or sign-up gets a spec that drives the real form. Anything else signs in with a ticket, `host.launch({ signedInAs })`, which is the intended shortcut for features that are not about authentication.
- Tag every spec that types a code or a password `form-entry`. Those specs run by default. A runtime that cannot type a code or a password into an app on hosted Clerk runs `control-clerk-android run --skip form-entry` and says so in the PR.
- `control-clerk-android down` deletes this worktree's application, and with it every user the run created, including users created through the sign-up form.

## Driving conventions

- Input only goes through specs. To look at any state past launch, write a spec, `run` it, then `control-clerk-android screen`.
- Prefer SDK test tags (`screen.getByTestId('clerk....')`) and the host tags `verify.signOut` and `verify.state`. Fall back to visible text only where nothing is tagged, and name each fallback in the feature file's Gotchas. The host's home buttons (`Prebuilt UI Sign In`, `Custom OTP Sign In`) have no tags.
- Act with `host.tap(locator)` and `host.fill(locator, text)`. A tagged Compose Material `Button` has a non-clickable `android.widget.Button` child, so `locator.tap()` on the tag can refuse. A tap at the middle of the box lands on the button.
- Prove results from `verify.state` (`host.launch`, `host.state`, `host.waitForState`), not from the screen alone. Every spec keeps at least one exact assertion on `verify.state` or an SDK test tag.
- The `screen` launch option routes the host: `home`, `auth`, `userProfile`, `orgSwitcher`, `orgList`, `orgProfile`. `state.screen` reports what is on screen: `launching` during a ticket sign-in, `error` for a rejected key, and `home` once an `auth` launch completes.

## Proof and skip reporting

- A proof is a passing `control-clerk-android run` whose run directory holds `video.mp4`, `screenshots/`, `states.jsonl`, `state.json`, `app.log`, and `e2e/report.json`.
- Name the run id and the specs in the PR. Attach with `control-clerk-android attach <run-id> --pr <n>`.
- Report a skipped `form-entry` spec as skipped with the reason. The CLI prints `skipped by --skip form-entry`. Never report it as verified through a ticket launch.
- A runtime that skips form entry proves each auth flow as far as it goes without typing a code or a password: `sign-in-email-code/request-code` and `sign-up/password-step`. It reports the rest as skipped.
- Report an unreachable path with the attempted command and the unmet precondition.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior, then exactly four H2 sections in this order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify`, and `Gotchas`. `Driving it with verify` starts with `Preconditions:` and names the golden specs that prove each sub-feature.

## Features

- [Auth start](./auth-start.md) covers opening AuthView from the home button and from a direct launch.
- [Sign in with an email code](./sign-in-email-code.md) covers requesting and entering the email code.
- [Sign up](./sign-up.md) covers the password step, requesting the sign-up code, and completing sign-up.
- [User button and profile](./user-button-and-profile.md) covers UserButton, UserProfileView, and sign-out.
- [Session tasks](./session-tasks.md) covers the setup-MFA and choose-organization tasks after sign-in.
- [Organizations](./organizations.md) covers creating an organization from OrganizationSwitcher.

Not mapped yet: phone code sign-in, password sign-in, completing the setup-MFA task with an authenticator code (its screens have no test tags), social providers (no real OAuth on emulators), passkeys, and biometrics.
