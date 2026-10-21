# Session tasks

A user signs in or signs up on an instance that requires more setup and is stopped on a session task before the session becomes active. Finishing the task activates the session.

## Sub-features

- `setup-mfa` stops on the set-up-MFA task on an instance that requires MFA.
- `complete-setup-mfa` enrolls an authenticator app with a code and activates the session.
- `sign-up-setup-mfa` ends a sign-up on the set-up-MFA task, with no second sign-in, and finishes it with an authenticator code.
- `choose-organization` stops on the create-or-choose-organization task on an instance that forces an organization.

## How to get to it (user POV)

- Sign in on an MFA-required app: the "set up two-step verification" screen appears.
- Sign up on an MFA-required app: the same screen appears after the email code.
- On that screen tap `Authenticator application`, add the key to an authenticator, tap `Continue`, and type the code the authenticator shows.
- Sign in on an app that requires an organization, as a user with no organization to choose: the "Create organization" screen appears.

## Driving it with verify

Preconditions:

- `setup-mfa.settings.json`, `complete-setup-mfa.settings.json`, and `sign-up-setup-mfa.settings.json` each declare `"config": { "auth_multi_factor": { "required_for_sign_up": true } }` and `"environment": { "user_settings.sign_up.mfa.required": true }`.
- `choose-organization.settings.json` declares `"config": { "organization_settings": { "force_organization_selection": true } }` and `"environment": { "organization_settings.force_organization_selection": true }`.
- `run` puts the worktree's application on the settings in a spec's settings file before the test in that spec file starts. `.claude/skills/verify-clerk-android/references/instances.md` describes the declaration.
- Each spec but one seeds a user and launches with a sign-in ticket for that user. The session stays pending, so the home shows `Signed out`, and the launch names that with `landsOn: host.app.signedOut`. The spec then taps `e2e.auth.signInFullScreen`, and AuthView adopts the pending session and shows the task. `sign-up-setup-mfa` seeds nobody. It gets an email from `host.newEmail()` and signs up through the form.

- **Setup MFA.** Run `e2e-tests/bin/control-clerk-android run session-tasks/setup-mfa`. After the tap AuthView shows `clerk.auth.sessionTask.setupMfa.authenticatorApp`, and the spec expects `clerk.auth.sessionTask.setupMfa.smsCode` beside it. Screenshot `session-task`. It then taps the avatar at the top of the task view and expects the seeded user's email in the account sheet. Screenshot `session-task-account`.
- **Complete MFA setup.** Run `e2e-tests/bin/control-clerk-android run session-tasks/complete-setup-mfa`. It reaches the task the same way and takes screenshot `session-task-before-totp`. It taps `clerk.auth.sessionTask.setupMfa.authenticatorApp`, reads the key from the `Add authenticator application` screen, takes screenshot `totp-secret`, and taps `Continue`. It expects `Verify authenticator app` and fills the code field with the code that `totp` in `specs/totp.ts` computes from the key. It expects `Backup codes` and ten codes, takes screenshot `backup-codes`, taps `Close`, and waits for the home to show `Signed in as` the seeded user's email with that user's ID. Screenshot `task-complete`.
- **Sign up into the task.** Run `e2e-tests/bin/control-clerk-android run session-tasks/sign-up-setup-mfa`. It launches with `authMode: 'signUp'`, taps `e2e.auth.signInFullScreen`, signs up with the email, a run password, and `CLERK_TEST_CODE`, and expects `clerk.auth.sessionTask.setupMfa.authenticatorApp` and `clerk.auth.sessionTask.setupMfa.smsCode` with no further step. Screenshot `sign-up-session-task`. It then enrolls an authenticator app the same way, without the `totp-secret` and `backup-codes` screenshots or the count of codes, closes the backup codes, and waits for the home to show `Signed in as` the new email with a user ID. Screenshot `sign-up-task-complete`.
- **Choose organization.** Run `e2e-tests/bin/control-clerk-android run session-tasks/choose-organization`. After the tap AuthView shows the organization form field `clerk.organization.profileForm.name`. Screenshot `choose-organization-task`. The spec then taps the task view's UserButton, `clerk.userButton.profile`, and expects the seeded user's email in the account sheet. Screenshot `choose-organization-task-account`.
- **Proof.** All four specs pass. The screenshots show each task screen, the account sheet names the user whose session is pending, and the home names the user whose task is finished.

## Gotchas

- A task view is the proof that the session is pending. AuthView shows one only for a pending session, and the home shows `Signed in as` only for an active one.
- The home treats a pending session as signed out and does not present the task by itself. Pass `landsOn: host.app.signedOut` to the launch, then open AuthView from the home.
- The avatar of the setup-MFA task view has no test tag. The spec finds it by its label, `User avatar`, and taps it with `host.tap(middleOf(...))`. The organization task view has a UserButton with the tag `clerk.userButton.profile`. The two account sheets differ: the first says `Log out` and the second `Sign out`.
- The authenticator screens have no test tags. The spec finds the key with `screen.getByText(/^[A-Z2-7]{16,}$/)`, which matches the one text on the screen that is a base32 key and nothing else. The line under it is the same key inside an `otpauth://` address, and the pattern does not match that. `Continue` and the title `Verify authenticator app` are found by text, and the code field is the only `textbox` on its screen.
- The spec computes the code as an authenticator does, from the key and the clock of the machine that runs it (RFC 6238, SHA-1, six digits, a 30 second step). A failure right at a 30 second boundary can be a clock edge, so rerun once before you debug.
- The backup codes show once Clerk accepts the code, and the task stays up until they are closed. The session is already active at Clerk by then, so the home is the proof that the task view has finished, not that the session changed. The backup codes screen has no tag and no `Continue` button. Its title is still `Add authenticator application`, and `Backup codes` is the label over the codes. Find it by the text `Backup codes`, and close it with `host.tap(middleOf(screen.getByLabel('Close')))`.
- When the real on-screen keyboard is up, it covers the `clerk.organization.profileForm.submit` button of the organization task form. agent-device's own input method draws no keyboard, so a normal run reaches the button.
