# Session tasks

A user signs in on an instance that requires more setup and is stopped on a session task before the session becomes active.

## Sub-features

- `setup-mfa` stops on the set-up-MFA task on an instance that requires MFA.
- `choose-organization` stops on the create-or-choose-organization task on an instance that forces an organization.

## How to get to it (user POV)

- Sign in on an MFA-required app: the "set up two-step verification" screen appears.
- Sign in on an app that requires an organization: the "Create organization" screen appears.

## Driving it with verify

Preconditions:

- `setup-mfa.e2e.ts` declares `config: { auth_multi_factor: { required_for_sign_up: true } }` and `environment: { 'user_settings.sign_up.mfa.required': true }`.
- `choose-organization.e2e.ts` declares `config: { organization_settings: { force_organization_selection: true } }` and `environment: { 'organization_settings.force_organization_selection': true }`.
- `run` puts the worktree's application on the declared settings before the test in each file starts. `SKILL.md` under Test instances describes the declaration.
- Each spec seeds a user and signs in with a ticket on `screen: 'auth'`.

- **Setup MFA.** Run `.claude/skills/verify-clerk-android/bin/control-clerk-android run session-tasks/setup-mfa`. It checks `ticket` `succeeded`, the seeded `userId`, `sessionStatus` `pending`, and `pendingTasks` containing `setup-mfa`, then expects `clerk.auth.sessionTask.setupMfa.authenticatorApp` and `clerk.auth.sessionTask.setupMfa.smsCode`. Screenshot `session-task`.
- **Choose organization.** Run `.claude/skills/verify-clerk-android/bin/control-clerk-android run session-tasks/choose-organization`. It checks `sessionStatus` `pending` and `pendingTasks` containing `choose-organization`, then expects `clerk.organization.profileForm.name`. Screenshot `choose-organization-task`.
- **Proof.** Both specs pass, and `states.jsonl` shows `sessionStatus` `pending` with the task in `pendingTasks`.

## Gotchas

- A ticket sign-in under either declaration reports `signedIn` true with `sessionStatus` `pending`. Read `sessionStatus`, not `signedIn`.
- No golden spec completes the setup-MFA task on Android. The authenticator screen has no test tags (its key and `Continue` are plain text), and the code step has not been driven.
