---
name: verify-clerk-android
description: Drive the clerk-android SDK UI (AuthView, UserButton, UserProfileView, OrganizationSwitcher, session tasks) in the :e2e host app on an Android emulator, on this machine or on a CI runner, against a real Clerk development instance that the CLI creates and deletes, and capture video, screenshots, and the app log as evidence. Use it to prove any change to source/api, source/ui, or the :e2e host works before calling it done, to reproduce a UI bug, or to run the golden regression specs.
---

# verify-clerk-android

`e2e-tests/bin/control-clerk-android` is a control CLI over [e2e](https://github.com/tester-army/e2e). It builds the debug APK of `:e2e`, the test app that this file calls the host, boots an emulator, creates one Clerk application for the worktree, seeds `+clerk_test` users, runs specs, and keeps the evidence. The emulator runs on this machine when this machine can run it, and on a CI runner when it cannot. The CLI chooses and says which, and every verb, spec, and evidence file is the same either way.

The tests are an ordinary e2e project. `e2e.config.ts` and `specs/` run under `npx e2e run` when the environment names a device, a build of the test app, and a development instance's keys. The CLI sits on top: it makes those three for a run and keeps the evidence. [The package README](../../../e2e-tests/README.md) has the commands for a run by hand and what such a run leaves out.

The rule: no change to clerk-android UI or auth behavior is done until a `run` on the real host shows the changed behavior.

Run every command from the repo root. The tests and the CLI are the Node package `e2e-tests/`, and paths that begin `specs/`, `src/`, or `.verify/` are inside it. `features/` and `references/` are beside this file. `features/` is the Feature Map. Start with `features/README.md`.

## Launch

A machine needs these once:

- Node 24, at 24.8.0 or newer.
- The Android SDK with `emulator`, `platform-tools`, and the system image `system-images;android-36;google_apis;arm64-v8a` (`x86_64` on an Intel machine). The CLI looks in `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk`.
- Java 21 for the Gradle build. Android Studio's bundled JBR counts and is found by itself.
- A Clerk Platform API credential for the team's verification workspace (see below).

The first `up` writes the `Clerk_Verify_Pixel` AVD when the machine has none. An AVD you already have is never changed.

A machine that cannot run the emulator, such as one with no SDK or a Linux machine with no KVM, needs only Node 24 (24.8 or newer), the credential, and access to GitHub. The CLI then borrows an emulator on a CI runner, as the end of this section describes.

```console
$ npm ci --prefix e2e-tests   # once per worktree
$ e2e-tests/bin/control-clerk-android doctor
$ e2e-tests/bin/control-clerk-android up
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

When this machine cannot run the emulator, `up` borrows one on a GitHub Actions runner. The runner session builds a pushed commit, never your working tree, so the loop is commit, push, `up`. This `up` forces the runner with `--backend remote`. Its first line, one of its `wait` lines, and its last line:

```console
$ git commit -am "..." && git push
$ e2e-tests/bin/control-clerk-android up --backend remote
backend remote  forced by --backend remote
wait    run <run> has waited 14s, now for the tunnel on ubuntu-24.04
device  Clerk_Verify_Pixel on ubuntu-24.04  remote  leased by this worktree  installed android-cba5fdb5c161
```

With no flag, the first line gives the reason instead, such as `backend remote  local is out: there is no /dev/kvm, so this machine has no hardware virtualization for the emulator; the device runs on a CI runner (ubuntu-24.04 unless --runner names another), started through verify-remote.yml on clerk/clerk-android`.

A session runs on a free GitHub-hosted runner. Allow about ten minutes for the first `up` on a runner, most of that the first Gradle build. `--runner blacksmith-4vcpu-ubuntu-2404` on `up` leases a Blacksmith runner instead, which is billed by the minute. A session holds its runner until it stops, so run `down` as soon as you are done. `--backend local` or `--backend remote` forces the choice, and `--runner <label>` names another runner label. [references/remote.md](references/remote.md) has how the CLI chooses, what a session needs from this machine, the labels, the cost, and what crosses the tunnel.

## Doctor

```console
$ e2e-tests/bin/control-clerk-android doctor
```

Run it first, and again whenever anything looks off. Without `--live` it only reads: it creates no file, starts nothing, and changes nothing at Clerk. Each line starts with `ok`, `warn`, `skip`, or `FAIL`. A failing check prints a `fix:` line with the command that fixes it, and `doctor` exits 3.

Before the first `up`, `build` is the one failing check, and its fix is `e2e-tests/bin/control-clerk-android up`.

`doctor --live` also proves that the credential can create, configure, and delete an application. It creates one application, configures it, compares it with the standard file, and deletes it, unless this worktree already holds one.

With the remote backend, plain `doctor` checks that GitHub has your commit. It starts no workflow run and pushes nothing. `doctor --live` starts one short session on a free runner, with no emulator, to prove that a session can start.

## Drive

Input only goes through specs. A spec is a TypeScript file that uses the `host` fixture from `specs/fixtures.ts` and e2e's `screen` locators.

```console
$ e2e-tests/bin/control-clerk-android run auth-start                     # one feature (specs/golden/auth-start/)
$ e2e-tests/bin/control-clerk-android run sign-up/complete               # one spec
$ e2e-tests/bin/control-clerk-android run specs/explored/resend.e2e.ts   # a spec you wrote
$ e2e-tests/bin/control-clerk-android screen                             # the current UI tree with test tags
$ e2e-tests/bin/control-clerk-android screen --png                       # plus a screenshot in .verify/scratch/
```

`run` also takes `--grep <regex>`, `--retries <n>`, `--no-video`, and `--wait <seconds>`. `--retries <n>` runs a failed test again, up to `n` more times. The default is 0, so a run of your own change shows exactly what happened. [references/cli.md](references/cli.md) has every flag, the `--json` output, and the exit codes.

With a remote session, an edit to the app reaches the emulator only through a pushed commit: commit and push, then `run` again, and the same session builds it. A commit that only touches specs needs no push, because specs run from this machine.

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
- `host.seedUser({ phone?, password? })` creates a `+clerk_test` user in the worktree's application. With `password: true` the user also has a password that the run generated for that user, and `user.password` holds it. Pass it to `host.fill`, as `host.fill(field, user.password!)`, and to nothing else. It prints as `<secret:password>`, `run` redacts its value from the logs it writes, and `attach` refuses a run whose files hold it. For a form sign-up, `host.newEmail()` returns a new address of this run, and `host.newPhone()` returns a 555-01xx number that no user holds. `host.newEmail()` calls nothing and records nothing. After the tests, `run` finds the user by the run's prefix in the address.
- `host.expectSignedInAs(user, timeoutMs?)` waits for the home to show `Signed in as <email>`, the user's ID, and a session ID. Pass the email alone for a user that the sign-up form created. `host.expectSignedOut(timeoutMs?)` waits for the home to show `Signed out` with no user ID and no session ID. Use them for the outcome of a flow that returns to the home. When one of these waits or the wait of `host.launch` runs out, its message says what the home showed at the last reading, how many passes and screen reads the wait made, how long the slowest read took, and the error of a read that failed.
- `host.app` has the locators of the home: `signIn`, `signInFullScreen`, `signedOut`, `signedIn`, `userId`, `sessionId`, `signOut`, and `error` for the error screen. `customSignInLink(screen)` from `specs/compose.ts` is the home's `Custom sign-in` link, which only this host has. `host.runId` is the id of the run, for a name or a password that must be new in each run.
- `host.tap(locator)` is `locator.tap()` with the assertion timeout, and `host.fill(locator, text)` taps a field and types into it. When the first tap left no field focused, it taps the field once more before it types. agent-device refuses a plain tap on a node whose whole box belongs to its children, with "no parent-owned touch point outside its interactive descendants". The UserButton, `clerk.userButton.profile`, is such a node, and so is a button found by its label, such as `Close`. Tap those with `tapMiddle(locator)` from `specs/compose.ts`, which taps the middle of the node's box. The SDK's tagged buttons with a text label, such as `clerk.auth.start.continue`, and the home's own buttons take a plain `host.tap`. After it types, `host.fill` reads the focused text input back. When the screen tree marks no text input as focused, as it does on iOS for a while after a slow snapshot, it reads the value that the node the locator names shows. It sends a long text sixteen characters to a command, because the iOS runner abandons a command after 30 seconds. It types once more if the input is still empty and the screen shows no text that it did not show before the typing, and replaces the input's contents once if the input holds something other than the text. It fails with "the text never reached the field" or "the field does not hold the typed text" if the input is still wrong. A screen that shows new text has taken the typed text and moved on, as a code screen does when its code is complete, so `host.fill` types nothing more there. For this it compares the letters of the screen's text only, so the seconds that a code screen counts down beside `Resend` are not new text. It compares letters and digits only, so the formatting a field adds does not count. A password field withholds its value, so a password is typed once and is not confirmed. When the iOS runner refuses a tap, a typing command, or a screenshot because it is still finishing an earlier command, `host.tap`, `host.fill`, and `host.screenshot` wait five seconds and try the step again, up to six times, and then fail with the refusal. After a refused typing command, `host.fill` replaces the input's contents with the text and does not type that command again, so characters that did land are not typed twice.
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

Golden specs under `specs/golden/<feature>/` are committed and cover the Feature Map in `features/`. Prove your own change, and leave the rest of the golden specs to the `Verify end-to-end tests` workflow (`.github/workflows/verify-e2e.yml`). The release workflow runs every one of them unchanged before it creates a release, and publishes nothing when one fails. When your change is to behavior a golden spec already covers, that spec is your proof: run it. Run another feature's specs yourself when you changed code that feature shares, because nothing else runs them until a release, and a failure there stops the release.

For new work, write a spec under `specs/explored/`, which is gitignored and does not exist in a fresh worktree. Run it, read the screen it ended on with `screen`, and fix locators from that output until it passes. A failing spec prints `FAIL`, the first assertion message, and the path of its failure page, which lists every step, the screen tree at the failure, and a screenshot. `run` exits 1.

With `--retries`, a test that fails and then passes prints `flaky` and the error of its failed attempt, and a `flaky` line under the results counts such tests. `run.json` records the test as `flaky` with its `attempts`, never as passed, and `run` exits 0. The failure page of the failed attempt and the files of every attempt stay in the run directory. A test that fails on every attempt prints `FAIL`, and `run` exits 1.

When the change adds or changes a user-facing behavior, the PR commits the spec into `specs/golden/<feature>/`, with its settings file if it has one, and updates the feature file. A golden spec sits one folder deeper, so its import of the fixture changes with the move:

```console
$ mkdir -p e2e-tests/specs/golden/<feature> && mv e2e-tests/specs/explored/probe.e2e.ts e2e-tests/specs/golden/<feature>/
$ mv e2e-tests/specs/explored/probe.settings.json e2e-tests/specs/golden/<feature>/   # only when the spec has a settings file
$ sed -i.bak "s|'../fixtures.ts'|'../../fixtures.ts'|" e2e-tests/specs/golden/<feature>/probe.e2e.ts && rm e2e-tests/specs/golden/<feature>/probe.e2e.ts.bak
$ e2e-tests/bin/control-clerk-android run <feature>
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
| `driver/` | `summary.txt`, which says what agent-device state the run found and what it copied, and the part of agent-device's `daemon.log` that the run added. They are for a failure that the driver caused, such as a daemon that did not start |
| `specs/` | A copy of every spec the run used, and of its settings file |

Proof standards: drive the real user path. The video and the screenshots show the action and its result as a user sees them. Check side effects where the app shows them, such as the user ID on the home or the organization name on the switcher, not only that the last screen appeared.

After a run, the CLI searches the run directory for every secret the run used (the Platform API key, secret keys, tickets, the passwords the tests typed, and a GitHub token in `GITHUB_TOKEN` or `GH_TOKEN`), and for any token shaped like a JWT. A hit marks the file tainted in `run.json`, and a tainted run cannot be attached.

A remote run is laid out the same. Its `run.json` also has `remote`, with `provider`, `runner`, and `builtSha`, and its video is recorded on the runner's emulator and then fetched.

```console
$ e2e-tests/bin/control-clerk-android attach <run-id> --pr <n>                       # video and every screenshot
$ e2e-tests/bin/control-clerk-android attach <run-id> --pr <n> --screenshot profile  # video and one screenshot
```

`attach` puts the video and the screenshots in the description of the pull request with `gh pr edit --attach`. It writes one block: a line that names the run, the device, and the commit, then the files, between the comments `<!-- verify-evidence:android -->` and `<!-- /verify-evidence:android -->`. The first `attach` adds the block after the description. A later `attach` replaces the block, so the description holds the latest run and the media does not pile up. `attach` changes nothing outside the block. Keep both comments or remove both: `attach` refuses a description that has one without the other, or either one twice. A comment counts only when it is a whole line outside a code fence, so a description can quote one in a sentence or show a whole block as an example.

`attach` reads the description again just before it writes, and builds on the newer text once if it changed. It cannot see an edit that someone saves while the files upload, and that edit is lost, so do not edit the description while `attach` runs.

On a machine whose `gh` can attach, `attach` edits the description itself and prints `posted`. That needs gh 2.99.0 or newer, whose `gh pr edit` has `--attach`. It uploads a run to a PR once, and a second `attach` of the same run and PR prints `already posted`.

`attach` always tries `gh pr edit --attach` itself first, on every kind of machine, and hands off only when that cannot work. On a machine whose `gh` cannot attach, such as a cloud sandbox, it hands the files of a remote run to the session's runner and prints `handed off`. Run it before `down`. The evidence reaches the description after `down` ends the session, when the `verify-attach` workflow publishes it. Its line says that the session reported the result, links the session's run, and names the account that started the session and the commit the run was made at. That workflow publishes only to the open pull request of the session's branch, and only when the commit the session started on and the commit of the run are both commits of that pull request. So open the pull request before `up`. A later push does not lose the evidence: the line then says that the pull request has newer commits. To show the newer commit, `run` and `attach` again, which replaces the block. `attach` checks the pull request first, and fails with the reason when the workflow would refuse it.

When this machine cannot attach and there is no session to hand the files to, or the hand-off fails, `attach` fails and says which. Then name the run id in the PR and say that the evidence was not attached. A run on a local emulator has no session, so its fix is a newer `gh`. [The emulator on a CI runner](references/remote.md) has what the repository needs before a hand-off can be published, and its limits.

`attach` refuses a run that is tainted, failed, or whose `app.log` names a user that the run did not create.

Attach the run of your own change. Run your new or changed spec on its own and attach that run, so the PR video shows only the behavior the change is about. If you ran other golden specs too, cite that run's id in the PR.

## Cleanup

```console
$ e2e-tests/bin/control-clerk-android down --dry-run   # what it would release, delete, and stop
$ e2e-tests/bin/control-clerk-android down             # kill the lane emulator or end the remote session, delete this worktree's application with every user in it, stop this worktree's agent-device daemon
$ e2e-tests/bin/control-clerk-android down --stale     # also finish cleanup left by a crashed run in this worktree
```

`down` deletes only what this worktree created. It needs the same credential `up` used, releases the emulator first, and if Clerk refuses a delete it fails with the fix `down` again, which finishes what is left. Run it after a failed iteration too, so no emulator is stranded.

With a remote session `down` ends the runner job and deletes `.verify/remote/<session>/`. No other checkout ends it. A session that nobody ends stops itself after 15 idle minutes, and always after 60.

It never deletes `.verify/runs/`, and it prints how many runs it kept (`--json` lists their ids). Evidence lives inside the worktree, so `git worktree remove` deletes it. Copy the runs you need out first.

`up` builds the app with a Gradle daemon, and `down` leaves that daemon running. It exits by itself after three hours without a build. `./gradlew --stop` from the repo root stops it at once, with every other idle daemon of the same Gradle version.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it.

## For maintainers of the tests and the CLI

- `src/core/`, `specs/support/`, `specs/fixtures.ts`, and `e2e.config.ts` are shared with the same package in clerk-ios and in clerk/javascript. They are byte copies of the same files in clerk-ios, and `src/core/MANIFEST` pins them. Change them there first, run `node e2e-tests/src/core/manifest.ts --write`, then copy them here. Nothing under `specs/` or `e2e.config.ts` imports the CLI, and `test/seam.test.ts` fails when a file does.
- `src/platform/android/` boots and owns lanes. The package in clerk/javascript holds copies of its files, so a change here goes there too. `src/host.ts`, `src/host-app.ts`, `specs/app.ts`, `specs/compose.ts`, `specs/phone.ts`, and `specs/totp.ts` are this repo's own.
- A remote session runs in `.github/workflows/verify-remote.yml`. Its code is `src/core/remote/` (the session agent that runs on the runner, the GitHub Actions provider, and the tunnel), `src/platform/android/session-device.ts`, and `src/platform/android/session-lane.ts`. A running session keeps the `src/core/` of the commit it started on, so after a change to a file that `src/core/MANIFEST` lists, commit, push, `down`, then `up`.
- `.github/workflows/verify-attach.yml` publishes the evidence that `attach` hands to a session's runner, with `.github/scripts/verify-attach.mjs`. It reads the manifest that `src/core/remote/handoff.ts` writes. It and `src/core/publish.ts` each replace the block the other wrote, and each applies the same rules to the comments and to the start of the line. So a change to the manifest, the comments, or those rules is a change in both places. `node --test .github/scripts/verify-attach.test.mjs` runs its tests.
- `npm test --prefix e2e-tests` runs the CLI's unit tests with no network, keys, or emulator, and `npm run typecheck --prefix e2e-tests` runs `tsc`. The `e2e-runner-tests` job of `.github/workflows/android-test.yml` runs both on a pull request that changes the package.
- `.github/workflows/verify-e2e.yml` runs `up`, `run --all --retries 1 --github-report`, and `down` on an emulator on a CI runner. The release workflow (`.github/workflows/manual-release.yml`) calls it before it creates the release, and publishes to Maven Central only when it passed. Nothing runs it for a pull request. `gh workflow run verify-e2e.yml --ref <branch>` starts it by hand on any branch. A test that fails and then passes is reported as flaky and does not fail the job. It needs the repository secret `MOBILE_VERIFICATION_PLATFORM_API_KEY` and fails without it. The repository variable `VERIFY_CI_RUNNER` names a runner label other than `ubuntu-24.04`.
