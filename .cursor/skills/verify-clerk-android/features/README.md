# clerk-android verification map

This directory is the maintained source for verifying the user-facing behavior of the clerk-android SDK UI (`source/ui`) through the `:e2e` host app. Read this index before driving the app, then use the matching feature file as the recipe. Every recipe runs through the `verify` CLI and golden specs under `specs/golden/<feature>/`.

## Baseline preconditions

- Run every command from `.cursor/skills/verify-clerk-android/` in a worktree of clerk-android. Run `npm ci` there once, before `bin/verify doctor`.
- Run `bin/verify doctor` first. Every check is `ok` except `build` before the first `bin/verify up`, so `doctor` exits 3 until then. `lane-ports` also fails while an emulator that is not a verify lane sits on 5560 or 5562; its fix names the kill command for that emulator's owner.
- The build needs Java 21. With `JAVA_HOME` unset the CLI uses Android Studio's JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. With `JAVA_HOME` on Java 21 or newer it uses `JAVA_HOME`. With `JAVA_HOME` on anything older, a `bin/verify up` that has to build refuses, and `doctor`'s `jdk` check fails with the same fix.
- `.keys.json` at the root of the main clerk-android checkout (not a linked worktree) holds `pk` and `sk` for `with-email-codes`, `with-session-tasks`, and `with-session-tasks-setup-mfa`. Only the CLI reads it. Never print a key.
- The CLI drives only its own lane emulator, `verify-android-<n>` (`emulator-5560` or `emulator-5562`), booted `-read-only` from the `Clerk_Verify_Pixel` AVD. Never drive another emulator, the AVD in Android Studio, or a physical device.
- Every launch gets a new `verifyStorageScope`, and the host clears Clerk's stored client when the scope changes, so no spec inherits a session from another spec.

### Test users and sign-in

- **Emails.** Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints `verify_<runId>_<n>+clerk_test@example.com` per run with `host.newEmail(instance)` or `host.seedUser({ instance })`.
- **Phones.** US numbers 555-0100 to 555-0199 are test numbers, typed as ten digits such as `5555550142`. They are shared across repos, CI, and agents, so get one from `host.seedUser({ instance, phone: true })` instead of picking one by hand.
- **One-time code.** `424242` verifies every email and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`. It is public and not an e2e secret, so screenshots after the fill are kept.
- **Passwords.** `with-email-codes` requires a password at sign-up. Use a throwaway per run, such as `Verify-<runId>-Pw1!`.
- **Authenticator codes.** The authenticator setup screen shows its key as untagged text. Compute the 6-digit RFC 6238 code (SHA-1, 30 second step) from it.

| Instance key in `.keys.json` | Use it for |
| --- | --- |
| `with-email-codes` (the `all-enabled` instance) | Every auth method and every signed-in feature. |
| `with-session-tasks-setup-mfa` | Sign-ins that must stop on the setup-MFA session task. |
| `with-session-tasks` | Sign-ins that must stop on the choose-organization session task. |

| Step | SDK test tag |
| --- | --- |
| Identifier field (email or username) | `clerk.auth.start.identifier` |
| Switch between email and phone | `clerk.auth.start.identifierSwitcher` |
| Phone field | `clerk.auth.start.phoneNumber` |
| Continue on the start screen | `clerk.auth.start.continue` |
| Sign-in code field | `clerk.auth.signIn.code` |
| Sign-in password field | `clerk.auth.signIn.password` |
| Try another method | `clerk.auth.signIn.useAnotherMethod` |
| Pick a method from the list | `clerk.auth.signIn.alternativeMethod.<strategy>` |
| Sign-up email, password, continue | `clerk.auth.signUp.emailAddress`, `clerk.auth.signUp.password`, `clerk.auth.signUp.continue` |
| Sign-up code field | `clerk.auth.signUp.code` |
| Legal consent, when shown | `clerk.auth.signUp.legalAccepted` |
| MFA setup choices | `clerk.auth.sessionTask.setupMfa.smsCode`, `clerk.auth.sessionTask.setupMfa.authenticatorApp` |

The source of truth for these names is `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. Keep this table in sync with it.

Rules:

- Type only `+clerk_test` emails, 555-0100 to 0199 phones, `424242`, and the run password. The repo is public and every video can land on a PR.
- Use ticket sign-in (`host.launch({ signedInAs })`) only to reach signed-in screens for features that are not about authentication. A change to an auth method gets a spec that drives the real form.
- Tag every spec that types a code or a password `form-entry`. Those specs run by default. A runtime that refuses to type codes or passwords into an app that talks to hosted Clerk runs `bin/verify run --skip form-entry` and says so in the PR.
- `bin/verify down` deletes every user the run created, including users created through the sign-up form, by their test email.

## Driving conventions

- Input only goes through specs. To look at any state past launch, write a spec, `bin/verify run` it, then `bin/verify screen`.
- Prefer SDK test tags (`screen.getByTestId('clerk....')`) and the host tags `verify.signOut` and `verify.state`. Fall back to visible text only where nothing is tagged, and name each fallback in the feature file's Gotchas.
- Act with `host.tap(locator)` and `host.fill(locator, text)`. A tagged Compose Material `Button` has a non-clickable `android.widget.Button` child, so `locator.tap()` on the tag can refuse; a tap at the middle of the box lands on the button.
- Prove results from `verify.state` (`host.launch`, `host.state`, `host.waitForState`), not from the screen alone. Every spec keeps at least one exact assertion on `verify.state` or an SDK test tag.
- `verifyScreen` routes the host: `home`, `auth`, `userProfile`, `orgSwitcher`, `orgList`, `orgProfile`. `state.screen` reports what is on screen: `launching` during a ticket sign-in, `error` for a rejected key, and `home` once an `auth` launch completes.

## Proof and skip reporting

- A proof is a passing `bin/verify run` whose run directory holds `video.mp4`, `screenshots/`, `states.jsonl`, `state.json`, `app.log`, and `e2e/report.json`.
- Name the run id and the specs in the PR. Attach with `bin/verify attach <run-id> --pr <n>`.
- Report a skipped `form-entry` spec as skipped with the reason. The CLI prints `skipped by --skip form-entry`. Never report it as verified through a ticket launch.
- A runtime that skips form entry proves each auth flow as far as it goes without typing a code or password: `sign-up/password-step`. It reports the rest as skipped.
- A `known-bug` spec is never verified. Run it with `--include known-bug` to see the bug: `bin/verify run sign-in-email-code/request-code --include known-bug` reproduces the email-code bounce today. Report a run that fails as "reproduces the known email-code bounce; not verified", and one left out by default as "skipped: known-bug". When it passes, the bug is fixed: drop the `known-bug` tag in that PR and report the spec as verified.
- Report an unreachable path with the attempted command and the unmet precondition.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior, then exactly four H2 sections in this order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify`, and `Gotchas`. `Driving it with verify` starts with `Preconditions:` and names the golden specs that prove each sub-feature.

## Features

- [Auth start](./auth-start.md) covers opening AuthView from the home button and from a direct launch.
- [Sign in with an email code](./sign-in-email-code.md) covers requesting and entering the email code. Both specs are tagged `known-bug` and skipped by default until the SDK's email-code bounce is fixed; see its Gotchas.
- [Sign up](./sign-up.md) covers the password step, requesting the sign-up code, and completing sign-up.
- [User button and profile](./user-button-and-profile.md) covers UserButton, UserProfileView, and sign-out.
- [Session tasks](./session-tasks.md) covers the setup-MFA and choose-organization tasks after sign-in.
- [Organizations](./organizations.md) covers creating an organization from OrganizationSwitcher.

Not mapped yet: phone code sign-in, password sign-in, completing the setup-MFA task with an authenticator code (its screens have no test tags), social providers (no real OAuth on emulators), passkeys, and biometrics.
