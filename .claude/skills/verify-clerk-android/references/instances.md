# The test instance

`up` creates one Clerk application for the worktree through Clerk's Platform API, and `down` deletes it. Its development instance is the test instance every spec runs on. Nothing is shared with another worktree, another repo, or CI, so a spec never meets another session's users or a setting someone changed by hand.

## The standard settings

`src/core/instances/base.json` of `@clerk/internal-mobile-e2e` defines the standard instance, with everything that can coexist switched on except the email link: email code, phone code, password, username, TOTP, backup codes, organizations, and social buttons. The email link is off so that an email sign-in opens on the code screen. It has two parts.

- `config` is the whole body of the Platform API request that puts an instance on the standard settings.
- `environment` lists the 62 leaves of the instance's public `/v1/environment` that a spec cannot run without. One of them, `user_settings.actions.delete_self`, has no `config` key. A new application lets a user delete their own account, and the check fails if Clerk changes that.

After the CLI configures a new application it reads that public environment. A difference in one of those leaves fails with `INSTANCE_MISCONFIGURED` and names the setting. Run `down`, then `up` once more. If it fails again, Clerk changed what a setting does. Say so in your report, and do not edit the file as part of unrelated work, because the package is shared with other repos.

The CLI checks those 62 leaves, and no others. The public environment has about 150 more leaves. One of them can differ between two applications, and no check reports it. A test that changes a setting and depends on such a leaf names it in the `environment` of its `host.useSettings` call.

## Change settings in a test

Every test starts on the standard settings. A test that needs another setting calls `host.useSettings` with the Platform API config leaves to change, each by its full dotted name:

```ts
await host.useSettings({ config: { 'auth_multi_factor.required_for_sign_up': true } });
```

The CLI reads the value that each leaf has now, changes it, and waits until the instance's public `/v1/environment` shows the change. Turning a feature off is the same call with the other value:

```ts
await host.useSettings({ config: { 'auth_multi_factor.authenticator_app.enabled': false } });
```

`environment` is optional. Without it, the CLI waits until any leaf of the public environment changes, up to 15 seconds, and records the leaves that changed. A setting that moves no public leaf goes on after the 15 seconds, and the CLI prints a `settings` line that says so. When you know the leaf that shows the change, name it. The wait then checks exactly that leaf, and a wrong expectation fails the call:

```ts
await host.useSettings({
  config: { 'organization_settings.force_organization_selection': true },
  environment: { 'organization_settings.force_organization_selection': true },
});
```

An `environment` leaf is the dotted path of its value in the JSON that the public `/v1/environment` returns. `environment` in `base.json` lists 62 of them.

Call `host.useSettings` anywhere in a test, as many times as it needs. A call changes the settings from that point on. Clerk's servers apply the change at once, so a session task that the change requires appears without a relaunch. The SDK keeps the environment it read at launch, so its screens can show the old settings: after a test turned the authenticator app off, the setup-MFA task kept offering it until the app launched again. Call `host.launch()` after `host.useSettings` unless the test is about an app that is already running. `session-tasks/setup-mfa-sms-only` does this.

Any leaf of the instance config works, not only the leaves in `base.json`. Clerk checks the request with a dry run before anything changes. A leaf that does not exist fails the call and lists the leaves beside it. A value Clerk refuses, or would store differently, also fails the call, and nothing changes. The Platform API cannot set reverification, the development-mode banner, test mode, or PII protection off.

## Restore

When a test ends, pass or fail, the CLI puts back every leaf that the test changed, to the value it read before the first change. A leaf changed twice goes back to its value before the first change. The next test starts on the standard settings again.

The restore runs in three places, so a crash does not leave the application changed:

- The test fixture restores when the test ends.
- `run` restores after e2e exits, in case e2e was stopped before the fixture could.
- The next `run` or `up` restores what a crashed CLI left. The CLI writes the old values to the worktree's state file before it changes anything, so it can always put them back.

`run.json` has one `settings` entry per call: `at` is the spec line that called `host.useSettings`, `changed` lists each leaf with `from` and `to`, `shown` lists the environment leaves that showed the change, and `restored` says the leaves went back. `attach` refuses a run with an entry that was not restored.

## See the settings

`settings` prints the live config of this worktree's application, one leaf per line, with its exact dotted name and value. Each line pastes into `config`. Name a prefix to see part of it:

```console
$ e2e-tests/bin/control-clerk-android settings auth_multi_factor
settings  app_… (verify-throwaway-…)  no change a test made is pending
"auth_multi_factor.authenticator_app.enabled": true,
"auth_multi_factor.backup_code.enabled": true,
"auth_multi_factor.required_for_sign_in": false,
"auth_multi_factor.required_for_sign_up": false,
```

`settings` reads Clerk's Platform API and never the device, so it works the same with a local and a remote device. It needs the application that `up` creates. When a crash left changes in place, its first line says how many leaves the next `run` or `up` restores.

## Settings tests often change

| Setting | Config leaf | Example |
| --- | --- | --- |
| MFA required at sign-up | `auth_multi_factor.required_for_sign_up` | `{ 'auth_multi_factor.required_for_sign_up': true }` |
| MFA required at sign-in | `auth_multi_factor.required_for_sign_in` | `{ 'auth_multi_factor.required_for_sign_in': true }` |
| Authenticator app | `auth_multi_factor.authenticator_app.enabled` | `{ 'auth_multi_factor.authenticator_app.enabled': false }` |
| Backup codes | `auth_multi_factor.backup_code.enabled` | `{ 'auth_multi_factor.backup_code.enabled': false }` |
| Email link | `auth_email.sign_in_strategies`, `auth_email.verification_strategies` | `{ 'auth_email.sign_in_strategies': ['email_code', 'email_link'], 'auth_email.verification_strategies': ['email_code', 'email_link'] }` |
| Password | `auth_password.enabled`, `auth_password.required` | `{ 'auth_password.required': false }` |
| Username | `auth_username.used_for_sign_in`, `auth_username.required_for_sign_up` | `{ 'auth_username.required_for_sign_up': true }` |
| Organizations | `organization_settings.enabled` | `{ 'organization_settings.enabled': false }` |
| Force organization selection | `organization_settings.force_organization_selection` | `{ 'organization_settings.force_organization_selection': true }` |

An array leaf is set whole: name every value it keeps. The standard values are in `config` of `base.json`, and `settings` prints the live ones.

## The credential

One team key creates, changes, and deletes the application, and reads its secret key. The key needs four scopes: `applications:read`, `applications:manage`, `applications:delete`, and `application_secret_keys:read`. The CLI looks in this order, and `doctor` names the one it found in its `instances` line.

1. `MOBILE_VERIFICATION_PLATFORM_API_KEY` holds the key, or `MOBILE_VERIFICATION_PLATFORM_API_KEY_FILE` names a file that holds it and that only you can read (mode 0600). A set variable that does not work is an error.
2. A cloud environment attaches the key to requests for `api.clerk.com` after they leave the machine. The CLI finds this with one request that carries no key, and the output names the source as `a key attached outside this machine (no key is in this process)`.
3. `MOBILE_VERIFICATION_PLATFORM_API_KEY_REFERENCE` holds a 1Password secret reference of the shape `op://<vault>/<item>/credential`, and the CLI reads the key through the 1Password CLI. The command then waits up to 60 seconds for the person at the machine to approve the request in the 1Password app.

`doctor` checks the credential every time. `up` and `run` use it every time, because both read the application's secret key with it. `up` also creates, repairs, or replaces the application with it, and `run` does the same and changes and restores settings with it. `settings` reads the application's config with it. `down` uses it when the worktree holds an application. `screen` and `attach` never do. Each command looks it up at most once.

With no working credential, `doctor` fails `instances` with what it tried and the fix. `up` and `run` exit 3 before they build or lease a device. `down` releases the device first and then fails the same way. `down` again with the credential deletes the application.

The key reaches only the verification workspace, `org_3KHungJxbvIscuSvy8oos5MHAli`, which holds nothing but these applications. The CLI refuses a key of any other workspace. It also refuses to create or delete anything while that workspace holds an application whose name does not start with `verify-throwaway-`. Never print the key, and never put it in a file inside a repository.

## Lifetime and recovery

An application's name carries a deadline, six hours after it was created: `verify-throwaway-until-<utc>-<hex>`. `VERIFY_THROWAWAY_HOURS` sets 2 to 72. `down` deletes this worktree's application at once and confirms it is gone from Clerk's list. If a session dies without `down`, a later command on any machine that creates, changes, or deletes an application also deletes applications whose deadline has passed by Clerk's clock, at most five per command, and prints a `reap` line for each. Nothing is deleted before its own deadline, so no session can remove another's live application. When this worktree's application is within an hour of its deadline, the next `up` or `run` replaces it first, and users seeded in the old one are gone.

A Clerk development instance holds 100 users. `up` and `run` read the user count first and replace an application that holds 60 or more.

Every step can be repeated.

No file holds the application's secret key. Clerk returns the key when it creates the application, and the command that created the application uses that copy. Every later command that needs the key reads it once from the Platform API and keeps it in memory until the command ends. `up`, `run`, and the `clerk-api` check of `doctor` need it. `down`, `screen`, `attach`, and `settings` do not. The read needs the `application_secret_keys:read` scope. A key that Clerk refuses the read fails with `KEYS_MISSING`, and the message names the scopes Clerk reports as missing. `doctor` finds a missing scope before any read: Clerk lists the key's scopes in the answer that names its workspace, and `doctor` fails `instances` when one of the four is not in that list.

A worktree holds one application, and `down` deletes it. The application serves one run at a time, because a test can change its settings. While one `run` drives, a second `run` in the same worktree fails with `DEVICE_BUSY`, or with `--wait <seconds>` waits that long for the device first.

On a 429 from Clerk the CLI prints a `wait` line and retries. If the limit does not lift, the verb fails with `RATE_LIMITED`, which is retryable.
