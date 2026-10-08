# Session tasks

A user signs in on an instance that requires more setup and is stopped on a session task before the session becomes active.

## Sub-features

- `setup-mfa` stops on the set-up-MFA task on an instance that requires MFA.
- `choose-organization` stops on the create-or-choose-organization task on an instance that forces an organization.

## How to get to it (user POV)

- Sign in on an MFA-required app: the "set up two-step verification" screen appears.
- Sign in on an app that requires an organization, as a user with no organization to choose: the "Create organization" screen appears.

## Driving it with verify

Preconditions:

- `setup-mfa.settings.json` declares `"config": { "auth_multi_factor": { "required_for_sign_up": true } }` and `"environment": { "user_settings.sign_up.mfa.required": true }`.
- `choose-organization.settings.json` declares `"config": { "organization_settings": { "force_organization_selection": true } }` and `"environment": { "organization_settings.force_organization_selection": true }`.
- `run` puts the worktree's application on the settings in a spec's settings file before the test in that spec file starts. `references/instances.md` describes the declaration.
- Each spec seeds a user and launches with a sign-in ticket for that user. The session stays pending, so the home shows `Signed out`, and the launch names that with `landsOn: host.app.signedOut`. The spec then taps `e2e.auth.signInFullScreen`, and AuthView adopts the pending session and shows the task.

- **Setup MFA.** Run `control-clerk-android run session-tasks/setup-mfa`. After the tap AuthView shows `clerk.auth.sessionTask.setupMfa.authenticatorApp`, and the spec expects `clerk.auth.sessionTask.setupMfa.smsCode` beside it. Screenshot `session-task`. It then taps the avatar at the top of the task view and expects the seeded user's email in the account sheet. Screenshot `session-task-account`.
- **Choose organization.** Run `control-clerk-android run session-tasks/choose-organization`. After the tap AuthView shows the organization form field `clerk.organization.profileForm.name`. Screenshot `choose-organization-task`. The spec then taps the task view's UserButton, `clerk.userButton.profile`, and expects the seeded user's email in the account sheet. Screenshot `choose-organization-task-account`.
- **Proof.** Both specs pass. The screenshots show each task screen, and the account sheet names the user whose session is pending.

## Gotchas

- A task view is the proof that the session is pending. AuthView shows one only for a pending session, and the home shows `Signed in as` only for an active one.
- The home treats a pending session as signed out and does not present the task by itself. Pass `landsOn: host.app.signedOut` to the launch, then open AuthView from the home.
- The avatar of the setup-MFA task view has no test tag. The spec finds it by its label, `User avatar`, and taps it with `tapMiddle`. The organization task view has a UserButton with the tag `clerk.userButton.profile`. The two account sheets differ: the first says `Log out` and the second `Sign out`.
- No golden spec completes the setup-MFA task on Android. The authenticator screen has no test tags (its key and `Continue` are plain text).
- When the real on-screen keyboard is up, it covers the `clerk.organization.profileForm.submit` button of the organization task form. agent-device's own input method draws no keyboard, so a normal run reaches the button.
