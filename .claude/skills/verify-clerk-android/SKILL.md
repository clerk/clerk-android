---
name: verify-clerk-android
description: Drive the clerk-android SDK UI (AuthView, UserButton, UserProfileView, OrganizationSwitcher, session tasks) in the :e2e host app on an Android emulator against a real Clerk development instance that the CLI creates and deletes, and capture video, screenshots, and the app log as evidence. Use it to prove any change to source/api, source/ui, or the :e2e host works before calling it done, to reproduce a UI bug, or to run the golden regression specs.
---

# verify-clerk-android

`bin/control-clerk-android` is a control CLI over [e2e](https://github.com/tester-army/e2e). It builds the debug APK of `:e2e`, the test app that this file calls the host, boots an emulator, creates one Clerk application for the worktree, seeds `+clerk_test` users, runs specs, and keeps the evidence.

The rule: no change to clerk-android UI or auth behavior is done until a `run` on the real host shows the changed behavior.

Short paths in this file (`bin/`, `specs/`, `features/`, `references/`, `src/`, `.verify/`) are inside `.claude/skills/verify-clerk-android/`. Commands are written as `control-clerk-android`. From the repo root that is `.claude/skills/verify-clerk-android/bin/control-clerk-android`, or add that `bin` directory to `PATH`. It works from any directory. `features/` is the Feature Map. Start with `features/README.md`.

## Launch

A machine needs these once:

- Node 24, at 24.8.0 or newer.
- The Android SDK with `emulator`, `platform-tools`, and the system image `system-images;android-36;google_apis;arm64-v8a` (`x86_64` on an Intel machine). The CLI looks in `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk`.
- Java 21 for the Gradle build. Android Studio's bundled JBR counts and is found by itself.
- A Clerk Platform API credential for the team's verification workspace (see below).

The first `up` writes the `Clerk_Verify_Pixel` AVD when the machine has none. An AVD you already have is never changed.

```console
$ npm ci --prefix .claude/skills/verify-clerk-android   # once per worktree
$ control-clerk-android doctor
$ control-clerk-android up
backend local  this Mac runs the emulator itself, from the SDK at /Users/you/Library/Android/sdk
instances CLERK_PLATFORM_API_KEY_FILE reaches the verification workspace org_3KHungJxbvIscuSvy8oos5MHAli
instance creating verify-throwaway-until-20261006t1126z-8ef8fb56 in org_3KHungJxbvIscuSvy8oos5MHAli
build   android-cba5fdb5c161  local  building...
instance app_<id>  up in 0.8s on standard, 62 settings match src/core/instances/base.json
build   android-cba5fdb5c161  local  built in 4s
device  verify-android-1  booting Clerk_Verify_Pixel -read-only on port 5560
install android-cba5fdb5c161  on verify-android-1 (emulator-5560)
device  verify-android-1 (emulator-5560)  local  leased by this worktree  installed android-cba5fdb5c161
```

The emulator is ready when `up` prints its last line. The `backend` line says where the emulator runs and why. `up` is idempotent. It reuses a build whose key matches the current tree (a hash of `source/`, `e2e/`, `gradle/`, and the root Gradle files, minus Markdown files and `docs/` directories), and a lease and an application this worktree already holds. `run` calls `up` itself, so `up` exists to start the slow part early.

A lane is the `Clerk_Verify_Pixel` AVD booted `-read-only -no-window` as `emulator-5560` or `emulator-5562`. A machine has those two lanes, shared by every worktree on it. `-read-only` means nothing a run does reaches the AVD, so the next lease boots clean. When both lanes are taken, `up` and `run` exit 3 with `POOL_FULL`, or wait with `--wait <seconds>`.

The CLI drives only the lane emulators it boots. Any other emulator on a lane port counts as taken and is never killed. [references/cli.md](references/cli.md) has the lane ports and what an interrupted boot leaves behind.

`up` also creates one Clerk application for this worktree, in the team's verification workspace. Its development instance is the test instance every spec runs on, and it starts on the standard settings in `src/core/instances/base.json`. `down` deletes it with every user in it. Creating it needs the team's Platform API key. `doctor` says whether it found a key and how to supply one. [references/instances.md](references/instances.md) has the ways to supply the key, what the key can reach, the application's deadline, and how a crashed session is cleaned up.

Each worktree runs its own agent-device daemon from its own `node_modules`, with state under `.verify/agent-device/`, and `down` stops it.

## Doctor

```console
$ control-clerk-android doctor
```

Run it first, and again whenever anything looks off. Without `--live` it only reads: it creates no file, starts nothing, and changes nothing at Clerk. Each line starts with `ok`, `warn`, `skip`, or `FAIL`. A failing check prints a `fix:` line with the command that fixes it, and `doctor` exits 3.

Before the first `up`, `build` is the one failing check, and its fix is `control-clerk-android up`.

`doctor --live` also proves that the credential can create, configure, and delete an application. It creates one application, configures it, compares it with the standard file, and deletes it, unless this worktree already holds one.

## Drive

Input only goes through specs. A spec is a TypeScript file that uses the `host` fixture from `specs/fixtures.ts` and e2e's `screen` locators.

```console
$ control-clerk-android run auth-start                     # one feature (specs/golden/auth-start/)
$ control-clerk-android run sign-up/complete               # one spec
$ control-clerk-android run specs/explored/resend.e2e.ts   # a spec you wrote
$ control-clerk-android screen                             # the current UI tree with test tags
$ control-clerk-android screen --png                       # plus a screenshot in .verify/scratch/
```

`run` also takes `--grep <regex>`, `--retries <n>`, `--no-video`, and `--wait <seconds>`. `--retries <n>` runs a failed test again, up to `n` more times. The default is 0, so a run of your own change shows exactly what happened. [references/cli.md](references/cli.md) has every flag, the `--json` output, and the exit codes.

```ts
import { tapMiddle } from '../../compose.ts';
import { test, expect } from '../../fixtures.ts';

test('the home UserButton opens the profile of the signed-in user', async ({ host, screen }) => {
  const user = await host.seedUser();
  await host.launch({ signedInAs: user });
  await tapMiddle(screen.getByTestId('clerk.userButton.profile'));
  await host.tap(screen.getByTestId('clerk.userProfile.row.manageAccount'));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('profile');
});
```

- `host.launch({ signedInAs?, authMode?, initialIdentifier?, debugLogs?, keepStorage?, landsOn? })` relaunches `com.clerk.e2e` and waits up to 60 seconds for its home. Every launch opens on the home, and a spec taps from there to the view it needs, as a user does. `authMode` is the mode of both AuthViews the home opens: `signIn`, `signUp`, or `signInOrUp`. `initialIdentifier` is the email, username, or phone number that the AuthView behind `Sign in full screen` gets through `AuthView(initialIdentifier)`. A launch starts with fresh storage unless `keepStorage` is true, so a launch with no `signedInAs` is signed out and lands on the home's `Sign in` button. A launch with `signedInAs` lands on the home, which must show that user's email and user ID. When the home should show anything else, pass its locator as `landsOn`. Two launches need it: one that signs in a user whose session stays pending on a task, where the home shows `Signed out`, and one that keeps storage and has no `signedInAs`. `host.launch` fails at once when the app shows its error screen, and the error has the message on that screen. `debugLogs: true` adds `ClerkLog` debug lines and `OkHttp` request lines to `app.log`.
- `host.seedUser({ phone?, password? })` creates a `+clerk_test` user in the worktree's application. With `password: true` the user also has a password that the run generated for that user, and `user.password` holds it. Pass it to `host.fill`, as `host.fill(field, user.password!)`, and to nothing else. It prints as `<secret:password>`, `run` redacts its value from the logs it writes, and `attach` refuses a run whose files hold it. `host.newEmail()` reserves an email, and `host.newPhone()` a 555-01xx number, for a form sign-up.
- `host.expectSignedInAs(user, timeoutMs?)` waits for the home to show `Signed in as <email>`, the user's ID, and a session ID. Pass the email alone for a user that the sign-up form created. `host.expectSignedOut(timeoutMs?)` waits for the home to show `Signed out` with no user ID and no session ID. Use them for the outcome of a flow that returns to the home.
- `host.app` has the locators of the home: `signIn`, `signInFullScreen`, `signedOut`, `signedIn`, `userId`, `sessionId`, `signOut`, and `error` for the error screen. `customSignInLink(screen)` from `specs/compose.ts` is the home's `Custom sign-in` link, which only this host has. `host.runId` is the id of the run, for a name or a password that must be new in each run.
- `host.tap(locator)` is `locator.tap()` with the assertion timeout, and `host.fill(locator, text)` taps a field and types into it. When the first tap left no field focused, it taps the field once more before it types. agent-device refuses a plain tap on a node whose whole box belongs to its children, with "no parent-owned touch point outside its interactive descendants". The UserButton, `clerk.userButton.profile`, is such a node, and so is a button found by its label, such as `Close`. Tap those with `tapMiddle(locator)` from `specs/compose.ts`, which taps the middle of the node's box. The SDK's tagged buttons with a text label, such as `clerk.auth.start.continue`, and the home's own buttons take a plain `host.tap`. After it types, `host.fill` reads the focused text input back. It types once more if the input is still empty, and replaces the input's contents once if the input holds something other than the text. It fails with "the text never reached the field" or "the field does not hold the typed text" if the input is still wrong. It compares letters and digits only, so the formatting a field adds does not count. A password field withholds its value, so a password is typed once and is not confirmed.
- `host.screenshot(label)` writes `screenshots/<label>.png` into the run.

Assert on what a user sees, and look in the SDK's own views first. The code screen names the address the code went to. A session task is on screen while the session is pending. The profile and the account sheet of a task view show the email of the signed-in user. The OrganizationSwitcher shows the active organization. Use the host's home for a fact that no SDK view shows, and for the outcome of a flow that returns to the home.

The home is the host's own screen. With no active session it shows `Signed out` (`e2e.auth.signedOut`) and three buttons. `Sign in` (`e2e.auth.signIn`) opens AuthView with a close button, which returns to the home. `Sign in full screen` (`e2e.auth.signInFullScreen`) shows `AuthView(isDismissible = false)` as the whole content of the activity, which is how an app shows AuthView until the auth flow completes. `Custom sign-in` (`e2e.home.customSignIn`) opens the host's own sign-in form. With an active session it shows the UserButton, `Signed in as <email>` (`e2e.auth.signedIn`), the user ID (`e2e.auth.userId`), the session ID (`e2e.auth.sessionId`), the OrganizationSwitcher, and a `Sign out` button (`e2e.auth.signOut`). A pending session counts as signed out there, and `Sign in full screen` opens AuthView on the task. The host shows the home again when that AuthView completes its flow or the custom form has signed in. It draws nothing on or around an SDK view. While Clerk loads or a ticket signs in, the host shows a spinner. When a launch cannot start, it shows `Something went wrong` and the reason (`e2e.launch.error`).

The host has a small flow of its own only where the SDK has no view for one. `Sign in` and `Sign in full screen` show `AuthView`, `Custom sign-in` opens a form built on the SDK's API, and `Sign out` calls `Clerk.auth.signOut()`. When a change needs a fact or a flow that no SDK view covers, add it to the home as a real app would show it, such as a button and its visible result, and assert on that.

Locate with test tags, `screen.getByTestId('clerk.auth.start.identifier')`. The SDK's tags start with `clerk.`, and `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt` lists them. The host's own tags start with `e2e.`, except those of the custom sign-in form, which start with `verify.customSignIn.`, and `e2e/src/main/java/com/clerk/e2e/E2ETags.kt` lists them. All of these tags are internal test hooks and may change. Fall back to visible text only where nothing is tagged. Every spec keeps at least one exact assertion on a test tag or on visible text.

### Signing in

A change that touches sign-in or sign-up gets a spec that drives the real form, with a `+clerk_test` identity and the test code `424242`. Anything else reaches a signed-in state with a sign-in ticket, `host.launch({ signedInAs: user })`.

Type only test identities: `+clerk_test` emails, phones 555-0100 to 555-0199, the code `424242`, and a throwaway password per run. [features/README.md](features/README.md) has the identities, the test tags of each sign-in step, and the order of the screens.

### Settings a spec needs

A spec names no instance. Most specs run on the standard settings and declare nothing. A spec file that needs something else declares it in a JSON file beside it. The file has the name of the spec, with `.settings.json` in place of `.e2e.ts`, so the settings of `specs/explored/mfa.e2e.ts` are in `specs/explored/mfa.settings.json`. `run` changes the application to match before the tests in that spec file start:

```json
{
  "config": { "auth_multi_factor": { "required_for_sign_up": true } },
  "environment": { "user_settings.sign_up.mfa.required": true }
}
```

[references/instances.md](references/instances.md) has the rules for a declaration and what `run` prints when it applies one.

### Golden and explored specs

Golden specs under `specs/golden/<feature>/` are committed, cover the Feature Map in `features/`, and run unchanged as regression. Run the features your change touches.

For new work, write a spec under `specs/explored/`, which is gitignored and does not exist in a fresh worktree. Run it, read the screen it ended on with `screen`, and fix locators from that output until it passes. A failing spec prints `FAIL`, the first assertion message, and the path of its failure page, which lists every step, the screen tree at the failure, and a screenshot. `run` exits 1.

With `--retries`, a test that fails and then passes prints `flaky` and the error of its failed attempt, and a `flaky` line under the results counts such tests. `run.json` records the test as `flaky` with its `attempts`, never as passed, and `run` exits 0. The failure page of the failed attempt and the files of every attempt stay in the run directory. A test that fails on every attempt prints `FAIL`, and `run` exits 1.

When the change adds or changes a user-facing behavior, the PR commits the spec into `specs/golden/<feature>/`, with its settings file if it has one, and updates the feature file. A golden spec sits one folder deeper, so its import of the fixture changes with the move:

```console
$ cd .claude/skills/verify-clerk-android
$ mkdir -p specs/golden/<feature> && mv specs/explored/probe.e2e.ts specs/golden/<feature>/
$ mv specs/explored/probe.settings.json specs/golden/<feature>/   # only when the spec has a settings file
$ sed -i.bak "s|'../fixtures.ts'|'../../fixtures.ts'|" specs/golden/<feature>/probe.e2e.ts && rm specs/golden/<feature>/probe.e2e.ts.bak
$ bin/control-clerk-android run <feature>
```

`<feature>` is one of the folders under `specs/golden/`. A change to the `:e2e` host alone is not an SDK feature: keep its spec under `specs/explored/` and cite the run in the PR.

### Agent steps

e2e has a built-in agent, which a test drives with `agent.act(...)` and `agent.assert(...)`. The CLI configures it only when the machine has a Vercel AI Gateway key. The model is `anthropic/claude-haiku-5.5`, and `openai/gpt-6-luna-fast` is the gateway's backup. Supply the key in `AI_GATEWAY_API_KEY_FILE`, a file that only you can read, or in `AI_GATEWAY_API_KEY`. The `agent` line of `doctor` shows the model and where the key came from. Committed golden specs do not use agent steps.

## Evidence

Every `run` writes `.verify/runs/<run-id>/` and prints its path:

| File | What it is |
| --- | --- |
| `run.json` | The record of the run: `results` per spec, `gitHead`, `dirty`, `build`, `device`, `identities`, `settings` (one entry per group of spec files that share a declaration), and `tainted` files |
| `video.mp4` | A screen recording of the whole run |
| `screenshots/<label>.png` | Every `host.screenshot(label)` |
| `app.log` | `adb logcat` lines from the run for `ClerkVerify`, `ClerkLog`, `OkHttp`, and `AndroidRuntime` errors. The host writes one `ClerkVerify` line each time its screen, user, session, or launch error changes, with the `launchId` that a failed `host.launch` names. No spec reads these lines. They are for diagnosing a failure that the screen does not explain |
| `e2e/`, `e2e.log` | e2e's `report.json` and `junit.xml`, failure pages, and console output. A run with several groups has `e2e/`, `e2e-2/`, `e2e-3/` in the order they ran |
| `specs/` | A copy of every spec the run used, and of its settings file |

Proof standards: drive the real user path. The video and the screenshots show the action and its result as a user sees them. Check side effects where the app shows them, such as the user ID on the home or the organization name on the switcher, not only that the last screen appeared.

After a run, the CLI searches the run directory for every secret the run used (the Platform API key, secret keys, tickets, and a GitHub token in `GITHUB_TOKEN` or `GH_TOKEN`). A hit marks the file tainted in `run.json`, and a tainted run cannot be attached.

```console
$ control-clerk-android attach <run-id> --pr <n>                       # video and every screenshot
$ control-clerk-android attach <run-id> --pr <n> --screenshot profile  # video and one screenshot
```

`attach` posts once per run with `gh pr comment --attach`, and says so when the `gh` on this machine has no such flag. It refuses a run that is tainted, failed, or whose `app.log` names a user that the run did not create. Without it, name the run id in the PR and say the evidence was not attached.

Attach the focused run, not the regression run. Run your new or changed spec on its own and attach that run, so the PR video shows only the behavior the change is about. Run the golden specs for every feature you touched in a separate `run` and cite its run id in the PR.

## Cleanup

```console
$ control-clerk-android down --dry-run   # what it would release, delete, and stop
$ control-clerk-android down             # kill the lane emulator, delete this worktree's application with every user in it, stop this worktree's agent-device daemon
$ control-clerk-android down --stale     # also finish cleanup left by a crashed run in this worktree
```

`down` deletes only what this worktree created. It needs the same credential `up` used, releases the emulator first, and if Clerk refuses a delete it fails with the fix `down` again, which finishes what is left. Run it after a failed iteration too, so no emulator is stranded.

It never deletes `.verify/runs/`, and it prints how many runs it kept (`--json` lists their ids). Evidence lives inside the worktree, so `git worktree remove` deletes it. Copy the runs you need out first.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it.

## For maintainers of the skill

- `src/core/` is shared with the clerk-ios skill and the Expo skill in clerk/javascript. It is a byte copy of the same directory in the clerk-ios skill, and `src/core/MANIFEST` pins it. Change it there first, run `node src/core/manifest.ts --write`, then copy it here. `specs/fixtures.ts` is shared with the clerk-ios skill too.
- `src/platform/android/` boots and owns lanes. The Expo skill holds copies of its files, so a change here goes there too. `src/host.ts`, `src/host-app.ts`, and `specs/compose.ts` are this repo's own.
- `npm test` runs the CLI's unit tests with no network, keys, or emulator, and `npm run typecheck` runs `tsc`. The `verify-skill` job of `.github/workflows/android-test.yml` runs both on a pull request that changes the skill. It runs no test on an emulator.
