# The test instance

`up` creates one Clerk application for the worktree through Clerk's Platform API, and `down` deletes it. Its development instance is the test instance every spec runs on. Nothing is shared with another worktree, another repo, or CI, so a spec never meets another session's users or a setting someone changed by hand.

## The standard settings

`src/core/instances/base.json` defines the standard instance, with everything that can coexist switched on: email code, email link, phone code, password, username, TOTP, backup codes, organizations, and social buttons. It has two parts.

- `config` is the whole body of the Platform API request that puts an instance on the standard settings.
- `environment` lists the 62 leaves of the instance's public `/v1/environment` that a spec cannot run without. One of them, `user_settings.actions.delete_self`, has no `config` key. A new application lets a user delete their own account, and the check fails if Clerk changes that.

After the CLI configures a new application it reads that public environment. A difference in one of those leaves fails with `INSTANCE_MISCONFIGURED` and names the setting. Run `down`, then `up` once more. If it fails again, Clerk changed what a setting does. Say so in your report, and do not edit the file as part of unrelated work, because `src/core` is shared with other repos.

The CLI checks those 62 leaves and the leaves a settings file declares, and no others. The public environment has about 150 more leaves. One of them can differ between two applications, or move when a declaration is applied, and no check reports it. A spec that depends on such a leaf lists it in its settings file.

## What a spec file declares

A spec file that runs on the standard instance declares nothing. A spec file that needs other settings has a settings file beside it, with the same name and `.settings.json` in place of `.e2e.ts`. For `specs/golden/session-tasks/setup-mfa.e2e.ts`, that file is `specs/golden/session-tasks/setup-mfa.settings.json`:

```json
{
  "config": { "auth_multi_factor": { "required_for_sign_up": true } },
  "environment": { "user_settings.sign_up.mfa.required": true }
}
```

`config` is a fragment of Clerk's Platform API instance config. `environment` lists the leaves of the public environment that the change moves, each by its full dotted path and with its new value. The declaration applies to every test in the spec file, so tests that need different settings go in different spec files.

The CLI reads the settings file and never runs a spec to learn its settings. Each rule below fails with a fix line that says what to write.

- The settings file is one JSON object that holds `config` and `environment` and nothing else. JSON takes no comment and no trailing comma.
- Every JSON file under `specs/` is the settings file of a spec beside it. `run` and `doctor` refuse any other JSON file there, `specs/explored/` included, so a misnamed settings file is never left out in silence.
- Every `config` leaf has a standard value under `config` in `base.json`, and at least one declared value differs from it.
- An `environment` leaf that `base.json` lists differs from its standard value.
- Every leaf of `environment` in `base.json` that the change moves is listed. When one is missing, the failure prints the missing leaves with their values, ready to paste.
- Two settings files that declare the same `config` expect the same value of every leaf both list.

The CLI cannot check an `environment` leaf that `base.json` does not list until it applies the change. The group then fails if the instance does not show the declared value within 15 seconds. The error says what the instance shows for that leaf, or that its public environment has no such leaf, which is how a misspelt leaf fails.

To find the name of a leaf, read `environment` in `base.json`. A leaf that is not there has the dotted path of its value in the JSON that the instance's public `/v1/environment` returns. The Platform API cannot set reverification, the development-mode banner, test mode, or PII protection off.

## How `run` applies declarations

`run` reads the settings files of the spec files it selected, groups the spec files by declaration, and runs each group in its own e2e invocation. A run with more than one group prints the groups first. These are the `settings` lines of one `run --all`, with the output between them left out:

```console
settings 3 groups in this run: standard (8 spec files), organization_settings.force_organization_selection=true (1), auth_multi_factor.required_for_sign_up=true (1)
settings standard  already on app_<id>
settings changing app_<id> from standard to organization_settings.force_organization_selection=true, which specs/golden/session-tasks/choose-organization.e2e.ts declares
settings organization_settings.force_organization_selection=true  on app_<id> in 0.4s (Clerk answered in 0.28s, the instance showed it 0.09s later), 62 settings match
settings changing app_<id> from organization_settings.force_organization_selection=true to auth_multi_factor.required_for_sign_up=true, which specs/golden/session-tasks/setup-mfa.e2e.ts declares
settings auth_multi_factor.required_for_sign_up=true  on app_<id> in 0.3s (Clerk answered in 0.23s, the instance showed it 0.09s later), 62 settings match
```

The group whose settings the application is already on runs first.

A declaration that breaks a rule the CLI can check from the files stops `run` before it builds or leases anything. A declaration that Clerk refuses, or whose `environment` does not match what the instance shows after the change, fails its own group: every spec file in it gets a failed result in `run.json`, and the run goes on to the next group. The error names the spec file, quotes what Clerk said, and its fix line names the settings file to change. A spec file or a settings file that changes while the run is in progress fails its group the same way, because the run planned its groups from the files as they were when it started. Before the CLI blames the spec for a refusal, it sends the standard `config` alone as a dry run, so a problem in `base.json` is reported as one.

## The credential

One team key creates, changes, and deletes the application, and reads its secret key. The key needs four scopes: `applications:read`, `applications:manage`, `applications:delete`, and `application_secret_keys:read`. The CLI looks in this order, and `doctor` names the one it found in its `instances` line.

1. `CLERK_PLATFORM_API_KEY` holds the key, or `CLERK_PLATFORM_API_KEY_FILE` names a file that holds it and that only you can read (mode 0600). A set variable that does not work is an error.
2. A cloud environment attaches the key to requests for `api.clerk.com` after they leave the machine. The CLI finds this with one request that carries no key, and the output names the source as `a key attached outside this machine (no key is in this process)`.
3. `VERIFY_PLATFORM_KEY_REFERENCE` holds a 1Password secret reference of the shape `op://<vault>/<item>/credential`, and the CLI reads the key through the 1Password CLI. The command then waits up to 60 seconds for the person at the machine to approve the request in the 1Password app.

`doctor` checks the credential every time. `up` and `run` use it every time, because both read the application's secret key with it. `up` also creates, repairs, or replaces the application with it, and `run` does the same and changes settings with it. `down` uses it when the worktree holds an application. `screen` and `attach` never do. Each command looks it up at most once.

With no working credential, `doctor` fails `instances` with what it tried and the fix. `up` and `run` exit 3 before they build or lease a device. `down` releases the device first and then fails the same way. `down` again with the credential deletes the application.

The key reaches only the verification workspace, `org_3KHungJxbvIscuSvy8oos5MHAli`, which holds nothing but these applications. The CLI refuses a key of any other workspace. It also refuses to create or delete anything while that workspace holds an application whose name does not start with `verify-throwaway-`. Never print the key, and never put it in a file inside a repository.

## Lifetime and recovery

An application's name carries a deadline, six hours after it was created: `verify-throwaway-until-<utc>-<hex>`. `VERIFY_THROWAWAY_HOURS` sets 2 to 72. `down` deletes this worktree's application at once and confirms it is gone from Clerk's list. If a session dies without `down`, a later command on any machine that creates, changes, or deletes an application also deletes applications whose deadline has passed by Clerk's clock, at most five per command, and prints a `reap` line for each. Nothing is deleted before its own deadline, so no session can remove another's live application. When this worktree's application is within an hour of its deadline, the next `up` or `run` replaces it first, and users seeded in the old one are gone.

A Clerk development instance holds 100 users. `up` and `run` read the user count first and replace an application that holds 60 or more.

Every step can be repeated.

No file holds the application's secret key. Clerk returns the key when it creates the application, and the command that created the application uses that copy. Every later command that needs the key reads it once from the Platform API and keeps it in memory until the command ends. `up`, `run`, and the `clerk-api` check of `doctor` need it. `down`, `screen`, and `attach` do not. The read needs the `application_secret_keys:read` scope. A key that Clerk refuses the read fails with `KEYS_MISSING`, and the message names the scopes Clerk reports as missing. `doctor` finds a missing scope before any read: Clerk lists the key's scopes in the answer that names its workspace, and `doctor` fails `instances` when one of the four is not in that list.

A worktree holds one application, and `down` deletes it. The application serves one run at a time, because a run changes its settings. While one `run` drives, a second `run` in the same worktree fails with `DEVICE_BUSY`, or with `--wait <seconds>` waits that long for the device first.

On a 429 from Clerk the CLI prints a `wait` line and retries. If the limit does not lift, the verb fails with `RATE_LIMITED`, which is retryable.
