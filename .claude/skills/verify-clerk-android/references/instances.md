# The test instance

`up` creates one Clerk application for the worktree through Clerk's Platform API, and `down` deletes it. Its development instance is the test instance every spec runs on. Nothing is shared with another worktree, another repo, or CI, so a spec never meets another session's users or a setting someone changed by hand.

## The standard settings

`src/core/instances/base.json` defines the standard instance, with everything that can coexist switched on: email code, email link, phone code, password, username, TOTP, backup codes, organizations, and social buttons. It has three parts.

- `config` is the whole body of the Platform API request that puts an instance on the standard settings.
- `environment` lists the leaves of the instance's public `/v1/environment` that a spec cannot run without.
- `defaults` lists every other leaf as a new application showed it when the file was written.

After the CLI configures a new application it reads that public environment. A difference in `environment` fails with `INSTANCE_MISCONFIGURED` and names the setting. Run `down`, then `up` once more. If it fails again, Clerk changed what a setting does. Say so in your report, and do not edit the file as part of unrelated work, because `src/core` is shared with other repos. A difference in `defaults` is drift in something no spec depends on, and only `doctor` reports it.

## What a spec file declares

A spec file that runs on the standard instance declares nothing. A spec file that needs other settings exports them once, as a plain literal, right after its imports:

```ts
import { test, expect, type InstanceSettings } from '../../fixtures.ts';

export const instanceSettings: InstanceSettings = {
  config: { auth_multi_factor: { required_for_sign_up: true } },
  environment: { 'user_settings.sign_up.mfa.required': true },
};
```

`config` is a fragment of Clerk's Platform API instance config. `environment` lists the leaves of the public environment that the change moves, each by its full dotted path and with its new value. The declaration applies to every test in the file, so tests that need different settings go in different files.

The CLI reads the declaration from the text of the file and never runs the file. Each rule below fails with a fix line that says what to write.

- A file has one declaration, and the word `instanceSettings` appears nowhere else in it.
- The declaration comes right after the imports. One that follows code with a `/` in it is refused, because the CLI cannot tell a division from a regular expression without running the file.
- The declaration is a plain literal that holds `config` and `environment` and nothing else. A variable, a spread, a call, a computed key, or a `${}` is refused.
- Every `config` leaf has a standard value under `config` in `base.json`, and at least one declared value differs from it.
- Every `environment` leaf is listed in `base.json`, under `environment` or `defaults`, and differs from its standard value.
- Every leaf the change moves is listed. When one is missing, the failure prints the missing leaves with their values, ready to paste.
- Two files that declare the same `config` expect the same value of every leaf both list.

The two declarations the golden specs use:

| A spec needs | Its file declares |
| --- | --- |
| Sign-ins that stop on the "set up MFA" session task | `config: { auth_multi_factor: { required_for_sign_up: true } }` and `environment: { 'user_settings.sign_up.mfa.required': true }`, as in `specs/golden/session-tasks/setup-mfa.e2e.ts` |
| Sign-ins that stop on the "choose or create an organization" session task | `config: { organization_settings: { force_organization_selection: true } }` and `environment: { 'organization_settings.force_organization_selection': true }`, as in `specs/golden/session-tasks/choose-organization.e2e.ts` |

The Platform API cannot set reverification, the development-mode banner, test mode, or PII protection off.

## How `run` applies declarations

`run` groups the spec files it selected by declaration and runs each group in its own e2e invocation. A run with more than one group prints the groups first. These are the `settings` lines of one `run --all --skip form-entry`, with the output between them left out:

```console
settings 3 groups in this run: standard (8 spec files), organization_settings.force_organization_selection=true (1), auth_multi_factor.required_for_sign_up=true (1)
settings standard  already on app_<id>
settings changing app_<id> from standard to organization_settings.force_organization_selection=true, which specs/golden/session-tasks/choose-organization.e2e.ts declares
settings organization_settings.force_organization_selection=true  on app_<id> in 0.4s (Clerk answered in 0.28s, the instance showed it 0.09s later), 212 settings match
settings changing app_<id> from organization_settings.force_organization_selection=true to auth_multi_factor.required_for_sign_up=true, which specs/golden/session-tasks/setup-mfa.e2e.ts declares
settings auth_multi_factor.required_for_sign_up=true  on app_<id> in 0.3s (Clerk answered in 0.23s, the instance showed it 0.09s later), 212 settings match
```

The group whose settings the application is already on runs first. The others follow in the order their first spec file comes in the run, and the standard group runs last unless it is first. One config request moves the application from one group to the next. The request is always the whole standard `config` with the declaration laid over it, so the result never depends on what was applied before. A group starts once the public environment shows its settings. After the run the application stays on the settings of the last group, and the CLI records them, so the next `run` starts with that group.

A declaration that breaks a rule the CLI can check from the file stops `run` before it builds or leases anything. A declaration that Clerk refuses, or whose `environment` does not match what the instance shows after the change, fails its own group: every spec file in it gets a failed result in `run.json`, and the run goes on to the next group. The error names the spec file and quotes what Clerk said. Before the CLI blames the spec for a refusal, it sends the standard `config` alone as a dry run, so a problem in `base.json` is reported as one.

## The credential

One team key creates, changes, and deletes the application. The CLI looks in this order, and `doctor` names the one it found in its `instances` line.

1. `CLERK_PLATFORM_API_KEY`, or `CLERK_PLATFORM_API_KEY_FILE` naming a file only you can read (mode 0600). A set variable that does not work is an error.
2. The 1Password CLI. The key's secret reference has the shape `op://<vault>/<item>/credential`. This repository does not hold it, and the team's private setup note does. The CLI reads the reference from `VERIFY_PLATFORM_KEY_REFERENCE`, or from the one line of `~/.verify/clerk-platform-key-reference`. With neither set it does not try 1Password. With one set, the 1Password app asks the person at the machine to approve within 60 seconds. An agent cannot approve it, so tell the person before the first command. The output names the source as `1Password` and never prints the reference.

`doctor` checks the credential every time. `up` uses it when it has to create, repair, or replace the application. `run` uses it for the same reasons and when it has to change settings. `down` uses it when the worktree holds an application. `screen` and `attach` never do. Each command looks it up at most once.

With no working credential, `doctor` fails `instances` with what it tried and the fix. An `up` that has to create the application and a `run` that has to change settings exit 3 before they build or lease a device. `down` releases the device first and then fails the same way. The ledger keeps the application, and `down` again with the credential deletes it.

The key reaches only the verification workspace, `org_3KHungJxbvIscuSvy8oos5MHAli`, which holds nothing but these applications. The CLI refuses a key of any other workspace. It also refuses to create or delete anything while that workspace holds an application whose name does not start with `verify-throwaway-`. Never print the key or the 1Password reference, and never put either in a file inside a repository.

## Lifetime and recovery

An application's name carries a deadline, six hours after it was created: `verify-throwaway-until-<utc>-<hex>`. `VERIFY_THROWAWAY_HOURS` sets 2 to 72. `down` deletes this worktree's application at once and confirms it is gone from Clerk's list. If a session dies without `down`, a later command on any machine that creates, changes, or deletes an application also deletes applications whose deadline has passed by Clerk's clock, at most five per command, and prints a `reap` line for each. Nothing is deleted before its own deadline, so no session can remove another's live application. When this worktree's application is within an hour of its deadline, the next `up` or `run` replaces it first, and users seeded in the old one are gone.

A Clerk development instance holds 100 users. `up` and `run` read the user count first and replace an application that holds 60 or more.

Every step can be repeated. The ledger gets the application's name before the create request is sent, and the keys go to a private file under `.verify/instances/` as soon as Clerk answers. The CLI records the settings an application is on only after the public environment shows them. A command that dies during a change leaves no record, and the next `up` or `run` sends the standard settings again. A rerun also replaces an application whose keys were lost or that Clerk has stopped serving, and `down` deletes whatever the ledger names.

This repo has one platform, so a worktree holds one application. `down` deletes every application the worktree holds.

## Request counts

`up` sends 4 Platform API requests: who the key belongs to, the list of applications, the create, and the configure. A `run` that changes settings sends 2 and one more for each change, and a `run` that changes nothing sends none. `down` sends 4 and `doctor` sends 2. A command prints its count, as in `clerk   Platform API: 4 requests by this command so far`. Clerk's edge allows 100 a minute for the whole workspace, shared by every session. On a 429 the CLI prints a `wait` line, waits as long as Clerk asks, up to a minute, and retries up to six times. If the limit does not lift, the verb fails with `RATE_LIMITED`, which is retryable.
