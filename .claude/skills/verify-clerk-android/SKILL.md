---
name: verify-clerk-android
description: Drive the clerk-android SDK UI (AuthView, UserButton, UserProfileView, OrganizationSwitcher, session tasks) in the :e2e host app on a lane Android emulator, on this machine or on a CI runner, against a real Clerk development instance that the session creates and deletes, and capture video, screenshots, and host state as evidence. Use it to prove any change to source/api, source/ui, or the :e2e host works before calling it done, to reproduce a UI bug, or to run the golden regression specs.
---

# verify-clerk-android

`bin/control-clerk-android` is a control CLI over [e2e](https://github.com/tester-army/e2e) and `@e2e-dev/mobile`. It builds the `:e2e` debug APK, boots a lane emulator, creates one Clerk application for the worktree, seeds `+clerk_test` users, runs specs, and keeps the evidence. The emulator runs on this machine when this machine can run it, and on a CI runner when it cannot. The CLI chooses and says which, and every verb, spec, and evidence file is the same either way.

The rule: no change to clerk-android UI or auth behavior is done until a `run` on the real host shows the changed behavior.

Short paths in this file (`bin/`, `specs/`, `features/`, `references/`, `src/`, `.verify/`) are inside `.claude/skills/verify-clerk-android/`. Commands are written as `control-clerk-android`. From the repo root that is `.claude/skills/verify-clerk-android/bin/control-clerk-android`, or add that `bin` directory to `PATH`. It works from any directory. `.cursor/skills/verify-clerk-android` is a committed symlink to this directory, so Cursor finds the skill too.

## Launch

A machine needs these once:

- Node 24.
- The Android SDK with `emulator`, `platform-tools`, and the system image `system-images;android-36;google_apis;arm64-v8a` (`x86_64` on an Intel machine). The CLI looks in `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk`.
- Java 21 for the Gradle build. Android Studio's bundled JBR counts and is found by itself.
- A Clerk Platform API credential for the team's verification workspace (see below).

The first `up` writes the `Clerk_Verify_Pixel` AVD when the machine has none. An AVD you already have is never changed.

A machine that cannot run the emulator, such as one with no SDK or a Linux machine with no KVM, needs only Node 24, the credential, and access to GitHub. The CLI then borrows an emulator on a CI runner, as the end of this section describes.

```console
$ npm ci --prefix .claude/skills/verify-clerk-android   # once per worktree
$ control-clerk-android doctor
$ control-clerk-android up
backend local  this Mac runs the emulator itself, from the SDK at /Users/you/Library/Android/sdk
instances throwaway  CLERK_PLATFORM_API_KEY_FILE reaches the verification workspace org_3KHungJxbvIscuSvy8oos5MHAli
instance creating verify-throwaway-until-20261006t0848z-3aa89b5c in org_3KHungJxbvIscuSvy8oos5MHAli
build   android-cba5fdb5c161  local  building...
instance app_<id>  up in 1.1s on standard, 212 settings match src/core/instances/base.json
clerk   Platform API: 4 requests by this command so far
clerk   Backend API on api.clerk.com
build   android-cba5fdb5c161  local  built in 1s
device  verify-android-1  booting Clerk_Verify_Pixel -read-only on port 5560
install android-cba5fdb5c161  on verify-android-1 (emulator-5560)
device  verify-android-1 (emulator-5560)  local  leased by this worktree  installed android-cba5fdb5c161
```

The lane is ready when `up` prints its last line. The `backend` line says where the emulator runs and why. `up` is idempotent. It reuses a build whose key matches the current tree (a hash of `source/`, `e2e/`, `gradle/`, and the root Gradle files, minus Markdown files and `docs/` directories), and a lease and an application this worktree already holds. `run` calls `up` itself, so `up` exists to start the slow part early.

The build runs `./gradlew :e2e:assembleDebug` with Java 21, because the Gradle plugins refuse a Java 17 JVM. With `JAVA_HOME` unset it uses Android Studio's JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. With `JAVA_HOME` on Java 21 or newer it uses that. With `JAVA_HOME` on anything older, an `up` that has to build fails with `NOT_READY` and the fix, and does not fall back to a JDK you did not ask for.

A lane is the `Clerk_Verify_Pixel` AVD booted `-read-only -no-window` as `emulator-5560` or `emulator-5562`. A machine has those two lanes, shared by every worktree on it. `-read-only` means nothing a run does reaches the AVD, so the next lease boots clean. When both lanes are taken, `up` and `run` exit 3 with `POOL_FULL`, or wait with `--wait <seconds>`.

Never drive an emulator you did not lease, such as the AVD you use in Android Studio, or a physical device. The CLI marks each lane it boots and kills or drives only an emulator that carries its own mark. Any other emulator on a lane port counts as taken and is never killed. [references/cli.md](references/cli.md) has the lane ports, the mark, and what an interrupted boot leaves behind.

`up` also creates one Clerk application for this worktree, in the team's verification workspace. Its development instance is the test instance every spec runs on, and it starts on the standard settings in `src/core/instances/base.json`. `down` deletes it with every user in it. Creating it needs the team's Platform API key, which the CLI looks for in this order:

1. `CLERK_PLATFORM_API_KEY`, or `CLERK_PLATFORM_API_KEY_FILE` naming a file only you can read.
2. A cloud environment that attaches the key to requests for `api.clerk.com` outside the session.
3. 1Password, once `VERIFY_PLATFORM_KEY_REFERENCE` or the one line of `~/.verify/clerk-platform-key-reference` holds the key's secret reference. The reference is in the team's private setup note, not in this repository. The 1Password app asks the person at the machine to approve, so tell them before the first command.

Never print the key or the reference. [references/instances.md](references/instances.md) has what the key can reach, the application's deadline, and how a crashed session is cleaned up.

Each worktree runs its own agent-device daemon from its own `node_modules`, with state under `.verify/agent-device/`, and `down` stops it. If you call `agent-device` yourself, use `node_modules/.bin/agent-device` with `AGENT_DEVICE_STATE_DIR=.verify/agent-device`. Never print `.verify/agent-device/daemon.json`, which holds the daemon's auth token.

When this machine cannot run the emulator, `up` borrows one on a GitHub Actions runner. The session builds a pushed commit, never your working tree, so the loop is commit, push, `up`. This `up` forced the runner with `--backend remote` on a machine that could have run the emulator:

```console
$ git commit -am "..." && git push
$ control-clerk-android up --backend remote
backend remote  forced by --backend remote
instances throwaway  CLERK_PLATFORM_API_KEY_FILE reaches the verification workspace org_3KHungJxbvIscuSvy8oos5MHAli
instance creating verify-throwaway-until-20261006t0859z-53b6fa32 in org_3KHungJxbvIscuSvy8oos5MHAli
build   android-b17728aef656  github-actions  commit <sha>  the session builds it
device  remote android  starting session <session> on blacksmith-4vcpu-ubuntu-2404 (idle stop 15 min, cap 60 min)
instance app_<id>  up in 1.1s on standard, 212 settings match src/core/instances/base.json
clerk   Platform API: 4 requests by this command so far
clerk   Backend API on api.clerk.com
device  remote android  run <run> by dispatch  https://github.com/clerk/clerk-android/actions/runs/<run>
wait    run <run> has waited 0s, now for a blacksmith-2vcpu-ubuntu-2404 runner to read its request
wait    run <run> has waited 9s, now for its request to be read on blacksmith-2vcpu-ubuntu-2404
wait    run <run> has waited 19s, now for a blacksmith-4vcpu-ubuntu-2404 runner
wait    run <run> has waited 28s, now for the tunnel on blacksmith-4vcpu-ubuntu-2404
device  remote android  tunnel up, Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404
install android-b17728aef656  on Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404
wait    build <sha> building 23s; device booting; agent-device starting
wait    build <sha> building 72s; device booting; agent-device up
build   android-b17728aef656  github-actions  <sha> built in 150s on blacksmith-4vcpu-ubuntu-2404
device  Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404  remote  leased by this worktree  installed android-b17728aef656
```

With no flag, the first line gives the reason instead, such as `backend remote  local is out: there is no /dev/kvm, so this machine has no hardware virtualization for the emulator; the device runs on a CI runner (blacksmith-4vcpu-ubuntu-2404 unless --runner names another), started through verify-remote.yml on clerk/clerk-android`.

A session is billed by the minute, so run `down` as soon as you are done. `--backend local` or `--backend remote` forces the choice, and `--runner <label>` names another runner label. [references/remote.md](references/remote.md) has how the CLI chooses, what a session needs from this machine, the labels, the cost, and what crosses the tunnel.

## Doctor

```console
$ control-clerk-android doctor
```

Run it first, and again whenever anything looks off. It only reads: it creates no file, starts nothing, and changes nothing at Clerk. A failing check prints the command that fixes it, and `doctor` exits 3. It checks:

| Check | What it tells you |
| --- | --- |
| `backend` | Where the emulator will run, and why |
| `node`, `e2e-pins` | Node 24, and the pinned `e2e` and `@e2e-dev/mobile` versions |
| `jdk` | The JDK the build will use, by the same rule as `up` |
| `template` | Whether the `Clerk_Verify_Pixel` AVD exists yet. The first `up` writes it |
| `lane-ports` | Any emulator on 5560 or 5562 that is not a lane of this CLI. Such an emulator takes a lane from every worktree. The fix names `adb -s emulator-<port> emu kill`, for its owner to run |
| `instances` | The credential it found and the workspace that reaches, and the application this worktree holds. With no working credential it fails with one fix line that says how to supply a key |
| `clerk-api`, `settings` | The Backend API host in use, the settings the application is on, and whether any spec file under `specs/` has a malformed declaration |
| `build` | Whether an `:e2e` APK matches the current tree |
| `gh-attach` | Whether `gh pr comment` has `--attach`. A `gh` without it is a warning, not a failure: everything works except `attach` |
| `stale-claims`, `agent-device-daemon`, `core-drift`, `feature-map` | Leftovers of a crashed run, a daemon from an install that no longer exists, `src/core` against its `MANIFEST`, and that every feature has its file and a golden spec |

Before the first `up`, `build` is the one failing check, and its fix is `control-clerk-android up`.

`doctor --live` also proves the instance path end to end. It creates one application, configures it, compares it with the standard file, and deletes it, unless this worktree already holds one.

With the remote backend, `jdk`, `template`, and `lane-ports` do not apply, and `build` says whether the held session has built the current tree. `doctor` then checks GitHub with git and REST reads only: a fetch and a dry-run push, that HEAD is not behind or off its branch on the remote (`git-head`), that GitHub has HEAD (`remote-commit`), that `*.trycloudflare.com` and `api.clerk.com` are reachable, and whether a session of this checkout is still running (`remote-sessions`). It starts no workflow run and pushes nothing, and says that `--live` proves the rest.

`doctor --live` with the remote backend also starts one short probe run and one short session on a free runner with no emulator. `--live --runner <label>` boots the emulator on that label too, with no build, which is how to check a new label. If this machine may not dispatch the workflow, the probe pushes a `verify-remote/...` branch holding HEAD, which the run deletes.

## Drive

Input only goes through specs. A spec is a TypeScript file that uses the `host` fixture from `specs/fixtures.ts` and e2e's `screen` locators.

```console
$ control-clerk-android run auth-start                     # one feature (specs/golden/auth-start/)
$ control-clerk-android run sign-up/password-step          # one spec
$ control-clerk-android run specs/explored/resend.e2e.ts   # a spec you wrote
$ control-clerk-android run --all --skip form-entry        # every golden spec except the ones that type a code or a password
$ control-clerk-android screen                             # the current UI tree with test tags and the host state
$ control-clerk-android screen --png                       # plus a screenshot in .verify/scratch/
```

`run` also takes `--grep <regex>`, `--no-video`, `--include known-bug`, and `--wait <seconds>`. [references/cli.md](references/cli.md) has every flag, the `--json` output, and the exit codes.

With a remote session, an edit to the app reaches the emulator only through a pushed commit: commit and push, then `run` again, and the same session builds it. A commit that only touches specs needs no push, because specs run from this machine.

```ts
import { test, expect } from '../../fixtures.ts';

test('UserProfileView shows the seeded user', async ({ host, screen }) => {
  const user = await host.seedUser();
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' });
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userProfile.row.manageAccount'));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('profile');
});
```

- `host.launch({ signedInAs?, screen?, authMode?, debugLogs?, keepStorage? })` relaunches `com.clerk.e2e` with fresh storage and returns the first ready state of that launch. Screens are `home`, `auth`, `userProfile`, `orgSwitcher`, `orgList`, and `orgProfile`. Auth modes are `signIn`, `signUp`, and `signInOrUp`. `debugLogs: true` adds `ClerkLog` debug lines and `OkHttp` request lines to `app.log`.
- `host.seedUser({ phone? })` creates a `+clerk_test` user in the worktree's application. `host.newEmail()` reserves an email for a form sign-up.
- `host.state()` reads the host's footer, and `host.waitForState(predicate, timeoutMs?)` polls it. The footer `verify.state` holds one line of JSON: `screen`, `environmentLoaded`, `signedIn`, `userId`, `sessionId`, `sessionStatus`, `pendingTasks`, `orgId`, `signInStatus`, `signUpStatus`, `ticket`, `lastError`, `runId`, and `launchId`. `signedIn` is true for a pending session, so read `sessionStatus`. The footer is not readable while a bottom sheet covers the host.
- `host.tap(locator)` taps the middle of the node's box, and `host.fill(locator, text)` taps it and types. Use them for every Compose button and field. A tagged Compose Material `Button` has a child that is not clickable, so `locator.tap()` on the tag can fail.
- `host.screenshot(label)` writes `screenshots/<label>.png` into the run.

Locate with the SDK's test tags, `screen.getByTestId('clerk.auth.start.identifier')`. They are listed in `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. The `clerk.*` tags and the host's `verify.signOut` and `verify.state` are internal test hooks and may change. Fall back to visible text only where nothing is tagged. Every spec keeps at least one exact assertion on the host state or a test tag: prove the result from the state, not from the screen alone.

### Signing in

A change that touches sign-in or sign-up gets a spec that drives the real form, with a `+clerk_test` identity and the test code `424242`. Anything else reaches a signed-in state with a sign-in ticket, `host.launch({ signedInAs: user })`, which is the intended shortcut and not a fallback. A runtime that cannot type a code or a password into an app on hosted Clerk skips the typing specs with `--skip form-entry` and says so in the PR.

Tag every spec that types a code or a password `form-entry`. Those specs run by default, and `--skip form-entry` reports each as `skipped by --skip form-entry`. Never report a skipped spec as verified through a ticket launch. The three today, one command each:

```console
$ control-clerk-android run sign-in-email-code/complete
$ control-clerk-android run sign-up/request-code
$ control-clerk-android run sign-up/complete
```

Type only test identities: `+clerk_test` emails, phones 555-0100 to 555-0199, the code `424242`, and a throwaway password per run. Never a real person's address, number, or password. The repo is public, and every video can land on a PR. [features/README.md](features/README.md) has the identities, the test tags of each sign-in step, and the order of the screens.

### Settings a spec needs

A spec names no instance. Most specs run on the standard settings and declare nothing. A spec file that needs something else exports it once, right after its imports, and `run` changes the application to match before the tests in that file start:

```ts
export const instanceSettings: InstanceSettings = {
  config: { auth_multi_factor: { required_for_sign_up: true } },
  environment: { 'user_settings.sign_up.mfa.required': true },
};
```

[references/instances.md](references/instances.md) has the rules for a declaration and what `run` prints when it applies one.

### Golden and explored specs

Golden specs under `specs/golden/<feature>/` are committed, cover the Feature Map in `features/`, and run unchanged as regression. Run the features your change touches.

For new work, write a spec under `specs/explored/`, which is gitignored and does not exist in a fresh worktree. Run it, read the end state with `screen`, and fix locators from that output until it passes. A failing spec prints `FAIL`, the first assertion message, and the path of its failure page, which lists every step, the screen tree at the failure, and a screenshot. `run` exits 1.

When the change adds or changes a user-facing behavior, the PR commits the spec into `specs/golden/<feature>/` and updates the feature file. A golden spec sits one folder deeper, so its import of the fixture changes with the move:

```console
$ cd .claude/skills/verify-clerk-android
$ mkdir -p specs/golden/<feature> && mv specs/explored/probe.e2e.ts specs/golden/<feature>/
$ sed -i.bak "s|'../fixtures.ts'|'../../fixtures.ts'|" specs/golden/<feature>/probe.e2e.ts && rm specs/golden/<feature>/probe.e2e.ts.bak
$ bin/control-clerk-android run <feature>
```

`<feature>` is one of the folders under `specs/golden/`. A change to the `:e2e` host alone is not an SDK feature: keep its spec under `specs/explored/` and cite the run in the PR.

## Evidence

Every `run` writes `.verify/runs/<run-id>/` and prints its path:

| File | What it is |
| --- | --- |
| `run.json` | The sealed record: `results` per spec, `gitHead`, `dirty`, `build`, `device`, `identities`, `instances`, `settings` (one entry per group of spec files that share a declaration), and `tainted` files |
| `video.mp4` | A screen recording of the whole run |
| `screenshots/<label>.png` | Every `host.screenshot(label)` |
| `states.jsonl` | Every host state the fixture read, in order, across every test in the run |
| `state.json` | Only the last state of the whole run. With several tests, read per-test states from `states.jsonl` by `launchId` |
| `app.log` | `adb logcat` lines from the run for `ClerkVerify`, `ClerkLog`, `OkHttp`, and `AndroidRuntime` errors |
| `e2e/`, `e2e.log` | e2e's `report.json`, failure pages, and console output. A run with several groups has `e2e/`, `e2e-2/`, `e2e-3/` in the order they ran |
| `specs/` | A copy of every spec the run used |

Proof standards: drive the real user path, capture the action and the resulting state (the video plus `states.jsonl`), and check side effects in `states.jsonl` (`userId`, `orgId`, `pendingTasks`), not only the final screen.

After a run, sealing searches the run directory for every secret the run used (the Platform API key, secret keys, tickets). A hit marks the file tainted in `run.json`, and a tainted run cannot be attached.

A remote run is laid out the same. Its `run.json` also has `remote`, with `provider`, `runner`, and `builtSha`, and its video is recorded on the runner's emulator and then fetched.

```console
$ control-clerk-android attach <run-id> --pr <n>                       # video and every screenshot
$ control-clerk-android attach <run-id> --pr <n> --screenshot profile  # video and one screenshot
```

`attach` posts once per run with `gh pr comment --attach`, and says so when the `gh` on this machine has no such flag. It refuses a run that is tainted, failed, or shows a user id the run did not create. Without it, name the run id in the PR and say the evidence was not attached.

Attach the focused run, not the regression run. Run your new or changed spec on its own and attach that run, so the PR video shows only the behavior the change is about. Run the golden specs for every feature you touched in a separate `run` and cite its run id in the PR.

## Cleanup

```console
$ control-clerk-android down --dry-run   # what it would release, delete, and stop
$ control-clerk-android down             # kill the lane emulator or end the remote session, delete this worktree's application with every user in it, stop this worktree's agent-device daemon
$ control-clerk-android down --stale     # also finish cleanup left by a crashed run in this worktree
```

`down` deletes only what this worktree created. It needs the same credential `up` used, releases the emulator first, and if Clerk refuses a delete it fails with the fix `down` again, which finishes what is left. Run it after a failed iteration too, so no emulator is stranded.

With a remote session `down` ends the runner job and deletes `.verify/remote/<session>/`. No other checkout ends it. A session that nobody ends stops itself after 15 idle minutes, and always after 60.

It never deletes `.verify/runs/`, and it prints how many runs it kept (`--json` lists their ids). Evidence lives inside the worktree, so `git worktree remove` deletes it. Copy the runs you need out first.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it.

## Helpers

- `bin/control-clerk-android` is the only helper. It is executable, and every verb is shown above.
- `specs/fixtures.ts` is the `host` fixture. `e2e.config.ts` composes the e2e config from the CLI's run context.
- `src/core/` is a byte copy of the same directory in the clerk-ios skill, and `src/core/MANIFEST` pins it. Change it there first, then copy it here.
- `src/platform/android/` boots and owns lanes. The Expo skill in clerk/javascript holds copies of its files, so a change here goes there too. `src/host.ts` is this repo's own: the Gradle build, the screens, and the feature list.
- `src/core/remote/` is the remote backend: the session agent that runs on the runner, the GitHub Actions provider, and the tunnel. `.github/workflows/verify-remote.yml` is the session's workflow. `src/platform/android/session-device.ts` is what a session builds, records, and logs with, and `session-lane.ts` boots the runner's emulator through the local backend.
- `npm test` runs the CLI's unit tests with no network, keys, or emulator, and `npm run typecheck` runs `tsc`. The repo's unit test workflow runs both on a pull request that changes the skill. It runs no device spec.
- `features/` is the Feature Map. Start with `features/README.md`.
- `references/cli.md`, `references/instances.md`, and `references/remote.md` hold what a reader rarely needs.
