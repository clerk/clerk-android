---
name: verify-clerk-android
description: Drive the clerk-android SDK UI (AuthView, UserButton, UserProfileView, OrganizationSwitcher, session tasks) in the :e2e host app on a lane Android emulator, on this machine or on a CI runner, against a real Clerk development instance that the session creates and deletes, and capture video, screenshots, and host state as evidence. Use it to prove any change to source/api, source/ui, or the :e2e host works before calling it done, to reproduce a UI bug, or to run the golden regression specs.
---

# verify-clerk-android

`.claude/skills/verify-clerk-android/bin/control-clerk-android` is a control CLI over [e2e](https://github.com/tester-army/e2e) 0.15.2 and `@e2e-dev/mobile` 0.9.0. It builds the `:e2e` debug APK, leases a lane emulator, creates one Clerk application for the worktree, puts its development instance on the settings each spec declares, seeds `+clerk_test` users, runs specs, and keeps the evidence. The application is this worktree's own and `down` deletes it. Read [Test instances](#test-instances) for where it comes from. The emulator runs on this machine when this machine can run it, and on a CI runner when it cannot. The CLI chooses, and every verb, spec, and evidence file is the same either way. Commands are shown from the repo root. Add `.claude/skills/verify-clerk-android/bin` to `PATH` and you can type `control-clerk-android` instead of the full path. Paths in this file that start with `specs/`, `features/`, `src/`, or `.verify/` are inside `.claude/skills/verify-clerk-android/`. The same directory is reachable as `.claude/skills/verify-clerk-android`, a committed symlink, so Cursor discovers this skill too. Every verb takes `--json` and then prints one `{ "ok": ... }` object. Exit codes are 0 for ok, 1 for spec failures, 2 for usage errors, and 3 for a failed precondition. Every error carries a `fix`.

The rule: no change to clerk-android UI or auth behavior is done until a `.claude/skills/verify-clerk-android/bin/control-clerk-android run` on the real host shows the changed behavior.

## Launch

```console
$ npm ci --prefix .claude/skills/verify-clerk-android   # once per worktree, before anything else
$ .claude/skills/verify-clerk-android/bin/control-clerk-android doctor            # exits 3 until the first up, because build is the one failing check
$ .claude/skills/verify-clerk-android/bin/control-clerk-android up                # build the :e2e APK for this tree, create this worktree's Clerk application meanwhile, then lease verify-android-<n> and install
wait    reading the team key from 1Password; approve the request in the 1Password app within 60s
backend local  this Mac runs the emulator itself, from the SDK at /Users/you/Library/Android/sdk
instances throwaway  1Password reaches the verification workspace org_3KHungJxbvIscuSvy8oos5MHAli
instance creating verify-throwaway-until-20261006t0550z-2573142c in org_3KHungJxbvIscuSvy8oos5MHAli
build   android-b17728aef656  local  building...
instance app_3KITo5GUI7R5ROWQGHvm0fqZtZK  up in 0.9s on standard, 212 settings match src/core/instances/base.json
clerk   Platform API: 5 requests by this command so far
clerk   Backend API on api.clerk.com
build   android-b17728aef656  local  built in 9s
device  verify-android-1  booting Clerk_Verify_Pixel -read-only on port 5560
install android-b17728aef656  on verify-android-1 (emulator-5560)
device  verify-android-1 (emulator-5560)  local  leased by this worktree  installed android-b17728aef656
```

The lane is ready when `up` prints its last line, `device <name> local leased by this worktree installed <build key>`. The `backend` line says where the emulator runs and why (see Where the emulator runs). The `build`, `device ... booting`, and `install` lines are progress. The `wait`, `instances`, `instance`, and `clerk` lines are about the Clerk application (see [Test instances](#test-instances)). `up` settles the credential before it builds or leases anything, so a `wait` line for 1Password comes first. The `instance ... up` line names the new application and says how many settings match the standard file. An `up` in a worktree that already holds its application prints `instance <application id>  held, on <settings>` instead. A reused build prints `build <key> local reused` instead of the two build lines, and a held lease skips the boot and install lines. When a lane is free, a fresh worktree takes about 40 seconds from no build and no emulator to a passing `run auth-start`, with the Gradle cache in `~/.gradle` warm. A held lease runs it in about 15. Waiting for a lane adds to both.

`up` is idempotent. It reuses a build whose key matches the current tree (a hash of `source/`, `e2e/`, `gradle/`, and the root Gradle files, minus docs and specs) and a lease this worktree already holds. It builds before it claims a lane, because a build needs no device. `run` calls `up` itself, so `up` exists to start the slow part early. `.claude/skills/verify-clerk-android/bin/control-clerk-android up &` followed by `.claude/skills/verify-clerk-android/bin/control-clerk-android run ...` is fine: `run` waits for the `up` to finish and uses its lease.

The build runs `./gradlew :e2e:assembleDebug` with Java 21, because the Gradle plugins refuse a Java 17 JVM. With `JAVA_HOME` unset, it uses Android Studio's bundled JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. With `JAVA_HOME` set to Java 21 or newer, it uses `JAVA_HOME`. With `JAVA_HOME` set to anything older, an `up` that has to build refuses with `NOT_READY` and does not fall back, so the build never uses a JDK you did not ask for. An `up` that reuses a build never runs Gradle, so it does not check the JDK. The fix is `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` or `unset JAVA_HOME`. It sets `ANDROID_HOME` to the SDK it found (see Where the emulator runs), so a fresh worktree with no `local.properties` builds. On Linux there is no bundled JBR to fall back on, so set `JAVA_HOME` to a Java 21 JDK.

A lane emulator boots the `Clerk_Verify_Pixel` AVD with `-read-only -no-window` on console port `5558 + 2 * slot`, so slot 1 is `emulator-5560` and slot 2 is `emulator-5562`. `-read-only` means nothing the run does reaches the AVD, and the next lease boots clean. `up` waits for `sys.boot_completed` and pins the `en-US` locale. The serial is `deviceId` in `.verify/leases/android.json`. A serial names a port, not a device: after `down`, another worktree can boot its own lane on the same serial at once. The lane's identity is `claimNonce` in the same lease file, which the emulator also holds as the property `debug.verify.lane`. Check `adb -s <deviceId> shell getprop debug.verify.lane` against it before you trust a serial you wrote down earlier.

Never drive `Pixel_9_Pro`, an emulator you did not lease, or a physical device. Two lane emulators can run on one machine at once, across all agents. Right after boot, `up` sets the property `debug.verify.lane` on each lane emulator to its claim nonce. Verify kills or drives an emulator only when it runs `Clerk_Verify_Pixel` with its own claim in that property. When a boot fails, verify stops the emulator process it started instead of killing whatever answers on the port. Any other emulator on a lane port, such as another AVD or a lane someone booted by hand, counts as taken, shows in the `POOL_FULL` list as `emulator-<port> (<AVD>, not a verify lane)`, and is never killed. When both lanes are taken, `up` and `run` fail with `POOL_FULL`. Pass `--wait <seconds>` to wait for a lane. While waiting, the CLI prints a `wait` line naming each lane and the worktree that holds it, prints it again when that changes, and prints `still waiting after <n>s` every minute otherwise. When a lane port holds an emulator that is not a verify lane, the `POOL_FULL` fix names `adb -s emulator-<port> emu kill`. Run it only if that emulator is yours; verify never kills it.

Each worktree runs its own agent-device daemon from its own `node_modules`, with state under `.verify/agent-device/`. The CLI passes `AGENT_DEVICE_STATE_DIR` to e2e and to every `agent-device` call, and `down` stops the daemon. If you call `agent-device` yourself, set `AGENT_DEVICE_STATE_DIR=.verify/agent-device` and use `node_modules/.bin/agent-device`. To find this worktree's daemon pid, read the `would stop agent-device <pid>` line from `.claude/skills/verify-clerk-android/bin/control-clerk-android down --dry-run`. Never print `.verify/agent-device/daemon.json`: it holds the daemon's auth token.

Interrupting `up` or `run` while a lane boots is safe. On Ctrl-C or SIGTERM, verify stops the emulator it started and prints `boot cancelled; stopped emulator <pid> on <serial>`. If the process dies harder than that (SIGKILL, a crash), the emulator, its claim, and `~/.verify/emulators/android-<slot>.pid` stay behind until the next verb reaps them. That pid file holds the emulator's pid, start time, and claim, and it is how verify recognizes its own orphaned emulator before the lane is marked: the next `up` or `run` in any worktree, or `.claude/skills/verify-clerk-android/bin/control-clerk-android down --stale` in this one, kills that emulator only when the process still matches the file. Plain `down` reports `released nothing` here, because the interrupted `up` never wrote a lease; use `down --stale`. An emulator verify did not start never matches.

Teardown is `.claude/skills/verify-clerk-android/bin/control-clerk-android down` (see Cleanup).

## Test instances

Each worktree has one Clerk application of its own. `up` creates it through Clerk's Platform API and puts its development instance on the standard settings, and `down` deletes it. A spec names no instance. A spec file that needs other settings declares them, and `run` changes the application to match before the tests in that file start. Nothing is shared with another worktree, another repo, or CI, so a spec never meets another session's users or a setting someone changed by hand.

- **The standard instance.** `src/core/instances/base.json` defines it, with everything that can coexist switched on. `config` is the whole body of the Platform API request that puts an instance on the standard settings. `environment` lists the leaves of the instance's public `/v1/environment` that a spec cannot run without. `defaults` lists every other leaf as a new application showed it when the file was written. After the CLI configures a new application, it reads that public environment. A difference in `environment` fails with `INSTANCE_MISCONFIGURED` and names the setting. Run `down`, then `up` once more. If it fails again, Clerk changed what a setting does. Say so in your report, and do not edit the file as part of unrelated work, because three repos share it (edit it, run `node src/core/manifest.ts --write`, and copy `src/core` to the others). A difference in `defaults` is drift in something no spec depends on, and only `doctor` reports it.
- **What a spec declares.** A spec file that runs on the standard instance declares nothing. A spec file that needs other settings exports them once, as a plain literal in the file itself:

  ```ts
  import { test, expect, type InstanceSettings } from '../../fixtures.ts';

  export const instanceSettings: InstanceSettings = {
    config: { auth_multi_factor: { required_for_sign_up: true } },
    environment: { 'user_settings.sign_up.mfa.required': true },
  };
  ```

  `config` is a fragment of Clerk's Platform API instance config. `environment` lists the leaves of the instance's public `/v1/environment` that the change moves, each by its full dotted path and with its new value. The declaration applies to every test in the file, so tests that need different settings go in different files.
- **The rules for a declaration.** The CLI reads the declaration from the text of the file and never runs the file. Each rule fails with a fix line that says what to write.
  - A file has one declaration, and the word `instanceSettings` appears nowhere else in it, comments included.
  - The declaration comes right after the imports. The CLI refuses one that follows code with a `/` in it (a division or a regular expression), because it cannot tell where such code ends without running it.
  - The declaration is a plain literal that holds `config` and `environment` and nothing else. A variable, a spread, a call, a computed key, or a `${}` is refused.
  - Every `config` leaf has a standard value under `config` in `base.json`, and at least one declared value differs from its standard value.
  - Every `environment` leaf is listed in `base.json`, under `environment` or `defaults`, and differs from its standard value.
  - Every leaf the change moves is listed. When one is missing, the failure lists the missing leaves with their values, ready to paste into `environment`.
  - Two files that declare the same `config` expect the same value of every leaf that both list.
- **A spec that names an instance.** `run` refuses a spec file that holds the name of a standing instance, an `instance` argument to a `host` call, or `LAUNCH_PRESETS`. It refuses before it builds or leases anything. `doctor` fails its `settings` check for such a file anywhere under `specs/`, and the fixture throws when a `host` call is given an instance. Each prints the same fix line.
- **How `run` applies declarations.** `run` reads the spec files it selected, groups them by declaration, and runs each group in its own e2e invocation. A run with more than one group prints the groups first. These are the `settings` lines of a `run --all --skip form-entry`, with the output between them left out:

  ```console
  settings 3 groups in this run: standard (8 spec files), organization_settings.force_organization_selection=true (1), auth_multi_factor.required_for_sign_up=true (1)
  settings standard  already on app_3KITo5GUI7R5ROWQGHvm0fqZtZK
  settings changing app_3KITo5GUI7R5ROWQGHvm0fqZtZK from standard to organization_settings.force_organization_selection=true, which specs/golden/session-tasks/choose-organization.e2e.ts declares
  settings organization_settings.force_organization_selection=true  on app_3KITo5GUI7R5ROWQGHvm0fqZtZK in 0.4s (Clerk answered in 0.30s, the instance showed it 0.09s later), 212 settings match
  settings changing app_3KITo5GUI7R5ROWQGHvm0fqZtZK from organization_settings.force_organization_selection=true to auth_multi_factor.required_for_sign_up=true, which specs/golden/session-tasks/setup-mfa.e2e.ts declares
  settings auth_multi_factor.required_for_sign_up=true  on app_3KITo5GUI7R5ROWQGHvm0fqZtZK in 0.3s (Clerk answered in 0.22s, the instance showed it 0.10s later), 212 settings match
  ```

  The group whose settings the application is already on runs first. The other groups follow in the order their first spec file comes in the run, and the standard group runs last unless it is first. One config request moves the application from one group to the next. The request is always the whole standard `config` with the declaration laid over it, so the result never depends on what was applied before. A group starts once the public environment shows its settings. After the run, the application stays on the settings of the last group. The CLI records them, so the next `run` starts with that group and changes nothing it does not need to.
- **Timing.** Clerk answers a settings change in about 0.25 s, and the instance's public environment shows it on the next read, about 0.1 s later. The first spec of the group starts within about a second. In 42 measured changes on iOS and Android, the spec that followed each change passed.
- **A declaration that fails.** A declaration that breaks a rule the CLI can check from the file stops `run` before it builds or leases anything. A declaration that Clerk refuses, or whose `environment` does not match what the instance shows after the change, fails its own group. Every spec file in that group gets a failed result in `run.json`, and the run goes on to the next group. The error names the spec file and quotes what Clerk said. Before the CLI blames the spec for a refusal, it sends the standard `config` alone as a dry run, so a problem in `base.json` is reported as one. Any other failure to apply settings stops the run, and the groups that did not run get failed results too. The Platform API cannot set reverification, the development-mode banner, test mode, or PII protection off.
- **The user limit.** A Clerk development instance holds 100 users. `up` and `run` read the user count first and replace an application that holds 60 or more. Users seeded in the replaced application are gone.
- **The credential.** One team key creates, changes, and deletes the application. The CLI looks in this order, and `doctor` names the one it found in its `instances` line.
  1. `CLERK_PLATFORM_API_KEY`, or `CLERK_PLATFORM_API_KEY_FILE` naming a file only you can read (mode 0600). A set variable that does not work is an error.
  2. A request with no key. In a cloud environment that holds the key as an API credential for `api.clerk.com`, the environment adds the key after the request leaves the machine, so no key is ever in the session.
  3. The 1Password CLI. The key's 1Password secret reference has the shape `op://<vault>/<item>/credential`, and this repository does not hold it. The team's private setup note has the real one. The CLI reads the reference from `VERIFY_PLATFORM_KEY_REFERENCE`, or from the one line of `~/.verify/clerk-platform-key-reference`. With neither set it skips 1Password, and the fix line says how to set one. With a reference set it prints `wait    reading the team key from 1Password; approve the request in the 1Password app within 60s`, and the 1Password app asks the person at the Mac to approve. An agent cannot approve it, so tell the person before the first command. A refused or unanswered request is an error. The output names the source as `1Password` and never prints the reference.
- **Which commands need it.** `doctor` checks it every time. `up` uses it when it has to create, repair, or replace the application. `run` uses it for the same reasons and when it has to change settings, so a `run` whose spec files all declare the settings the application is already on does not. `down` uses it when the worktree holds an application. `screen` and `attach` never do. Each command looks it up at most once, so with 1Password each of those commands asks once.
- **When it is missing.** `doctor` prints `FAIL  instances  no Clerk Platform API credential works here (...)` with what it tried and the fix. An `up` that has to create the application and a `run` that has to change settings exit 3 with `KEYS_MISSING` before they build or lease a device. `down` releases the device first and then fails the same way. The ledger keeps the application, and `down` again with the credential deletes it.
- **What the key can reach.** Only the verification workspace, `org_3KHungJxbvIscuSvy8oos5MHAli`, which holds nothing but these applications. The CLI refuses a key of any other workspace. It also refuses to create or delete anything while that workspace holds an application whose name does not start with `verify-throwaway-`. Never print the key or the 1Password reference, and never put either in a file inside a repository.
- **Lifetime.** An application's name carries a deadline, six hours after it was created (`verify-throwaway-until-<utc>-<hex>`; `VERIFY_THROWAWAY_HOURS` sets 2 to 72). `down` deletes this worktree's application at once and confirms it is gone from Clerk's list. If a session dies without `down`, a later command on any machine that creates, changes, or deletes an application also deletes applications whose deadline has passed by Clerk's clock, at most five per command, and prints a `reap` line for each. Nothing is deleted before its own deadline, so no session can remove another's live application. When this worktree's own application is within an hour of its deadline, the next `up` or `run` replaces it first, and users seeded in the old one are gone.
- **Recovery.** Every step can be repeated. The ledger gets the application's name before the create request is sent, and the keys go to a private file under `.verify/instances/` as soon as Clerk answers. The CLI records the settings an application is on only after the public environment shows them. A command that dies during a change leaves no record, and the next `up` or `run` then sends the standard settings again. A rerun also replaces an application whose keys were lost or that Clerk has stopped serving, and `down` deletes whatever the ledger names.
- **One application per worktree.** This repo has one platform, so a worktree holds one application. The shared core creates a second one only when two runs drive at the same time in one worktree on different declarations, which takes a host with two platforms. `down` deletes every application the worktree holds.
- **Which API host.** The Backend API calls that seed users use the instance's own secret key on `api.clerk.com`. A cloud API credential with path prefix `/v1/platform/` is attached to Platform API calls only, so those Backend API calls keep the instance's own key. A cloud API credential with no path prefix replaces that key on every request to the host, and the call gets 401. The CLI then sends the same request to `api.clerk.dev`, the older name of the same API, and stays there for the rest of the command if that succeeds. `up` prints `clerk   Backend API on <host>` and `doctor` prints the same in `clerk-api`. If neither name accepts the key, the command fails with `NOT_READY` and a fix that names the credential's path prefix and the allowed domains.
- **Requests.** `up` sends 4 Platform API requests: who the key belongs to, the list of applications, the create, and the configure. A `run` that changes settings sends 2 and one more for each change, and a `run` that changes nothing sends none. `down` sends 4 and `doctor` sends 2. A session of `up`, `run --all`, and `down` sends 12, and a session that stays on the standard instance sends 8. Each command that reads the key from 1Password sends one more. A command prints its count, as in `clerk   Platform API: 4 requests by this command so far`. Clerk's edge is configured for 100 a minute for the whole workspace, shared by every session. On a 429 the CLI prints a `wait` line, waits as long as Clerk asks, up to a minute, and retries up to six times. If the limit does not lift, the verb fails with `RATE_LIMITED`, which is retryable.
- **The standing instances.** Three long-lived instances from the JavaScript repo's integration set still work, for a machine with no credential: `with-email-codes`, `with-session-tasks`, and `with-session-tasks-setup-mfa`. `CLERK_TEST_KEYS_JSON`, or `VERIFY_INSTANCES=standing` with `.keys.json` in the main checkout, selects them. So does a machine that has `.keys.json`, no key variable, no key-attaching proxy, and no 1Password reference or no 1Password CLI. `VERIFY_INSTANCES=throwaway` refuses that fallback. A worktree that holds a throwaway application keeps using it until `down`. A standing instance cannot be changed, so the CLI checks a declaration there and does not apply it. A spec file with no declaration runs on `with-email-codes`. The forced organization selection declaration runs on `with-session-tasks`, and the required MFA at sign-up declaration runs on `with-session-tasks-setup-mfa`. Any other declaration fails with a fix line that says to run on a throwaway instance. With the standing instances `doctor` prints `keys` and one `instance:<name>` check per instance, with its enabled strategies, in place of `settings`. It reads the keys from `.keys.json` at the root of the main clerk-android checkout or from `CLERK_TEST_KEYS_JSON`. Other repos and CI share the standing instances, so their users and settings are not yours alone.

## Where the emulator runs

You do not choose. `up` and `run` print which backend they picked and why in a `backend` line, and `doctor` prints the same as its `backend` check:

```console
backend local  this Mac runs the emulator itself, from the SDK at /Users/you/Library/Android/sdk
backend local  this machine runs the emulator itself: /dev/kvm opens for reading and writing and the SDK at /usr/local/lib/android/sdk has the system image
backend remote  local is out: there is no /dev/kvm, so this machine has no hardware virtualization for the emulator; the device runs on a CI runner (blacksmith-4vcpu-ubuntu-2404 unless --runner names another), started through verify-remote.yml on clerk/clerk-android
backend remote  local is out: no Android SDK with an emulator and adb (looked in /root/Android/Sdk) (to run it here: install the Android SDK emulator and platform-tools and set ANDROID_HOME to the SDK); the device runs on a CI runner (...)
```

`local` is the Launch section above. This machine can run the emulator when all of these hold:

- It runs macOS or Linux.
- On Linux, `/dev/kvm` exists and this user can open it for reading and writing. A machine with no `/dev/kvm` is told so first and offered no fix, because nothing installed on it would help.
- It has an Android SDK with `emulator/emulator` and `platform-tools/adb`. The CLI looks in `ANDROID_HOME`, then `ANDROID_SDK_ROOT`, then `~/Library/Android/sdk` on macOS or `~/Android/Sdk` on Linux, then any SDK whose `platform-tools` or `emulator` directory is on `PATH`.
- That SDK has the system image `system-images;android-36;google_apis;arm64-v8a` on an ARM machine or `system-images;android-36;google_apis;x86_64` on an Intel or AMD one. When a `Clerk_Verify_Pixel` AVD already exists, the image that AVD names counts instead.

The check reads files and runs no tool, so a slow or broken `adb` cannot change the choice. When something is missing, the `backend` line names it and, where a command fixes it, prints the command after `to run it here:`. Two things do not send you to a runner, because each has a quick fix on this machine. A missing `Clerk_Verify_Pixel` AVD is written by the first `up`, with the panel the specs were written against (1280 by 2856 at 480 dpi), under `ANDROID_AVD_HOME`, or `avd` under `ANDROID_USER_HOME`, or `~/.android/avd`. An AVD you already have is never changed. A missing Java 21 fails `doctor`'s `jdk` check with its fix; on Linux that fix is to install a Java 21 JDK and export `JAVA_HOME`.

`--backend local` or `--backend remote` forces one, and fails with `UNSUPPORTED` and the same fix when this machine cannot use it. A worktree that holds a lease keeps that lease's backend until `down`.

A Linux machine boots its lanes with `-gpu swiftshader_indirect` as well, because a headless machine has no GPU the emulator can use. Everything else in Launch holds there: lane ports, the `debug.verify.lane` marker, the pool of two.

## Remote emulator

When this machine cannot run the emulator, the CLI leases one on a GitHub Actions runner and drives it through a tunnel. None of the local lane exists there: no local build, no `.verify/builds/`, no lane pool, and no JDK or SDK on this machine. `--wait` only bounds the wait for another `run` in this worktree. The remote loop is commit, push, run:

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android doctor  # the remote path: push access, GitHub REST, the tunnel host
$ git commit -am "..." && git push  # the session builds a pushed commit, never your working tree
$ .claude/skills/verify-clerk-android/bin/control-clerk-android up
backend remote  local is out: there is no /dev/kvm, so this machine has no hardware virtualization for the emulator; the device runs on a CI runner (...)
build   android-b17728aef656  github-actions  commit d3235a1449f8  the session builds it
device  remote android  starting session android1c7c4c on blacksmith-4vcpu-ubuntu-2404 (idle stop 15 min, cap 60 min)
device  remote android  run 37368813278 by dispatch  https://github.com/clerk/clerk-android/actions/runs/37368813278
wait    run 37368813278 has waited 1s, now for a blacksmith-2vcpu-ubuntu-2404 runner to read its request
wait    run 37368813278 has waited 14s, now for its request to be read on blacksmith-2vcpu-ubuntu-2404
wait    run 37368813278 has waited 24s, now for a blacksmith-4vcpu-ubuntu-2404 runner
wait    run 37368813278 has waited 33s, now for the tunnel on blacksmith-4vcpu-ubuntu-2404
device  remote android  tunnel up, Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404
install android-b17728aef656  on Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404
wait    build d3235a1449f8 building 115s; device booting; agent-device up
build   android-b17728aef656  github-actions  d3235a1449f8 built in 175s on blacksmith-4vcpu-ubuntu-2404
device  Clerk_Verify_Pixel on blacksmith-4vcpu-ubuntu-2404  remote  leased by this worktree  installed android-b17728aef656
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run auth-start  # reuses the session
$ .claude/skills/verify-clerk-android/bin/control-clerk-android down  # ends the runner job; do this as soon as you are done
```

- **Ready takes about four minutes.** The runner builds the `:e2e` APK while it downloads the system image and boots the emulator, and the cold Gradle build is the long part. Run `up` as soon as you have pushed, then write the spec while it builds. A runner can also take minutes to start when its label is busy. For that time `up` prints `wait run <id> has waited <n>s, now for <what>`, which names the label it is waiting on.
- **After an edit to the app, commit and push, then `run` again.** `run` sees that the app sources changed, asks the same session to build the new commit, and runs once it is installed. The build is incremental: 2 to 8 seconds measured for one-file edits to the host, longer for a change many modules depend on. Uncommitted changes to the app sources, or a HEAD that GitHub does not have, fail with `BUILD_FAILED` and the fix `git commit` or `git push`. A commit that only touches specs or docs needs no push: specs run from this machine.
- **A session keeps the verify code of the commit it started on.** The build steps in `src/platform/android/session-device.ts` are read again from each commit it builds, so a push that changes them takes effect at that build. The session agent in `src/core/` and the workflow steps are not: after a push that changes `src/core/`, `up` and `run` fail with `NOT_READY` and the fix `down`, then `up`.
- **A session costs money by the minute.** It stops itself after 15 minutes without a call from the CLI, and always after 60 minutes. `down` stops it at once. After an idle stop, the next `up` or `run` prints `lost ... renewing` and starts a new session. A session with under two minutes left before its cap is replaced the same way, with `ending ... renewing`. Set `VERIFY_REMOTE_IDLE_MINUTES` or `VERIFY_REMOTE_CAP_MINUTES` to change either before `up`.
- **A crashed run cannot strand a session for long.** Each checkout has a random id in `.verify/remote/owner`, and its sessions carry it. When the checkout holds no lease, `up` and `run` end any session with that id that is still running, and `down --stale` does so at any time. The idle stop ends whatever they miss, such as the session of a checkout that was deleted.
- **The runner label is one setting.** The default is `blacksmith-4vcpu-ubuntu-2404`, which is billed. `--runner ubuntu-latest` or `VERIFY_REMOTE_RUNNER=ubuntu-latest` uses a free GitHub-hosted label instead and needs no other change; there the cold build took about six minutes instead of two and a half to three. Any Linux x64 label with KVM, the Android SDK command-line tools, and `sudo` works. A held session keeps its label, and `--runner` with another label fails until `down`.
- **The job that reads the request has its own label.** Before the session's runner starts, a job of a few seconds checks the request. For a session on a Blacksmith label it runs on `blacksmith-2vcpu-ubuntu-2404`, so the session does not first wait in the queue for GitHub's own runners. That job is billed as one minute of a 2 vCPU Linux runner per session, about 0.004 USD at the public price. For a session on a GitHub-hosted label it runs on `ubuntu-latest`, which is free, and so do `doctor`'s probe and `doctor --live`. `VERIFY_REMOTE_PLAN_RUNNER=<label>` names another label. A session started by a pushed `verify-remote/*` branch has its request read on `ubuntu-latest`.
- **The runner's emulator is a lane.** The session boots `Clerk_Verify_Pixel` through the same code as a local lane, on the `x86_64` build of the same Android 36 image, so the panel, the locale, and the `debug.verify.lane` marker match. It is the only emulator on that machine.
- **`screen`, explored specs, video, screenshots, `app.log`, and `states.jsonl` work the same.** The video is recorded on the runner's emulator, stopped there, and then fetched, so it plays. `run.json` also has `remote`, with `provider`, `runner`, and `builtSha`.
- **`attach` needs `gh` with `gh pr comment --attach`.** A machine without it reports `gh-attach` as failing in `doctor`, and can still run and keep evidence. Name the run id in the PR and say the evidence was not attached.
- **What a session needs from the machine:** Node 24 (on an older Node the CLI reruns itself under `node@24` through `npx` and says so), a pushed branch that holds `.github/workflows/verify-remote.yml`, permission to dispatch that workflow or to push a branch named `verify-remote/*`, REST access to the repository, and network access to `*.trycloudflare.com`, `api.clerk.com`, `api.clerk.dev`, and `*.clerk.accounts.dev`. It needs no Android SDK, no JDK, and no `adb`. It also needs a credential for test instances, which in a cloud environment is an API credential for `api.clerk.com` with path prefix `/v1/platform/` (see [Test instances](#test-instances)). A machine that uses the standing instances instead passes their keys in `CLERK_TEST_KEYS_JSON`, the same JSON as `.keys.json`.
- **Secrets.** The Platform API key and the instances' secret keys never leave this machine, and in a cloud environment the Platform API key never enters it. The session's bearer token is made here, lives in `.verify/remote/<session>/token`, and is deleted by `down`; never print it. GitHub sees only the token's SHA-256. The runner sees sign-in tickets and publishable keys as launch arguments, uploads no artifact, and prints none of them in its job log. The tunnel ends TLS at Cloudflare, so Cloudflare can read what crosses it, and the tunnel's host name is public while the session lives. Every route on it needs the bearer.

A cloud sandbox works without proxy configuration. Node ignores `HTTPS_PROXY` unless told to honor it, so the CLI decides: it goes through the proxy when the direct path to GitHub does not work, meaning it cannot connect or GitHub rejects the machine's token on it, and it reruns itself with the proxy in effect for itself, e2e, and agent-device. `doctor`'s `remote-env` line says which path it took and why.

What a remote session needs from GitHub is split in two. REST starts, reads, and ends sessions. `git push` gets a commit you make to GitHub so the session can build it. In a Claude Code cloud session REST uses the session user's own access, and `git push` needs the Claude GitHub App installed on the repository; a 403 on push means it is not. Without push access you can still verify any commit GitHub already has.

## Doctor

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android doctor --json
```

Run it first, and again whenever anything looks off. It changes nothing on this machine. With the remote backend it starts one short probe run on GitHub, which needs no runner beyond a free job, and with `--live` one short session on a free runner with no emulator. `--live --runner <label>` boots the emulator on that label too, with no build, which is how to check a new label. If this machine may not dispatch the workflow, the probe pushes a `verify-remote/...` branch holding HEAD, which the run deletes; it does that only for a HEAD that is already on GitHub. It checks:

- Which backend it chose and why (`backend`).
- With the remote backend: git fetch and a dry-run push, that HEAD is not behind or off the branch on the remote (`git-head`, which is what a reused, stale clone looks like), GitHub REST, that GitHub has HEAD, that a session can be triggered and its answer read back, that `*.trycloudflare.com` and `api.clerk.com` are reachable, and whether a session of this checkout is still running (`remote-sessions`). A check that could not run because an earlier one failed still prints, as `not run: needs <check>`.
- Node 24 and the pinned e2e and agent-device versions. With the local backend, also the global `agent-device`.
- With the local backend, the JDK the build will use, by the same rule as `up`. With `JAVA_HOME` on Java 17 the check fails, because the next build would refuse. On a Mac the fix is `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, and on Linux it is to install a Java 21 JDK and export `JAVA_HOME` to it.
- With the local backend, that the Android SDK `emulator` and `adb` run, and whether the `Clerk_Verify_Pixel` AVD exists yet (`template`; the first `up` writes it when it does not).
- With the local backend, `lane-ports`: any emulator on 5560 or 5562 that is not a verify lane with a live claim and a matching `debug.verify.lane`. Such an emulator takes a lane from every worktree. The fix is `adb -s emulator-<port> emu kill`, but only if that emulator is yours. Verify never kills it.
- `instances`: where test instances come from, the credential it found and the workspace that reaches, and the application this worktree holds with the settings it is on. It fails with the fix when no credential works, also when the application is already up, because `down` will need it. `clerk-api`: the Backend API host in use, once an application exists to try it with. `settings`: for each application the worktree holds, the settings it is on, the spec file that asked for them, and how many settings match, then how many spec files declare settings. Before the first `up` it reads `none created yet; up creates one application from src/core/instances/base.json; 2 spec files declare settings`. It fails when a held application does not match its record, and when any spec file under `specs/` has a malformed declaration or names an instance. With the standing instances it prints other checks in place of `settings` (see [Test instances](#test-instances)). With `--live` it also creates one application, configures it, compares it with the standard file, finds the Backend API host, and deletes it, in about two seconds, unless this worktree already holds an application.
- Whether an `:e2e` APK build matches the current tree. With the remote backend, whether the held session has built the current tree.
- `gh pr comment --attach` support, stale device claims, and drift in `src/core/`.
- Whether an agent-device daemon, the machine-wide one in `~/.agent-device` or this worktree's own, runs from an install that no longer exists. The fix names the pid to kill.
- Whether every feature in the Feature Map has its feature file and at least one golden spec.

A failing check prints the command that fixes it, and `doctor` exits 3. With a working credential, before the first `up`, only `build` fails, with fix `.claude/skills/verify-clerk-android/bin/control-clerk-android up`, unless an emulator that is not a verify lane sits on 5560 or 5562; then `lane-ports` fails too, and `up` still works on the other lane. `build` turns `ok` as soon as `up` prints `build <key> local built in <n>s`, before `up` claims a lane. If all lanes are taken, `up` then waits for one. Stopping that waiting `up` with Ctrl-C is safe: the build is kept, no lane is claimed yet, and `doctor` stays green.

## Drive

Input only goes through specs. A spec is a TypeScript file that uses the `host` fixture from `specs/fixtures.ts` and e2e's `screen` locators.

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run auth-start                        # one feature (specs/golden/auth-start/)
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run sign-up/password-step             # one spec
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run specs/explored/resend.e2e.ts      # a spec you wrote
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run --all --skip form-entry           # every golden spec except form entry
$ .claude/skills/verify-clerk-android/bin/control-clerk-android screen                                # current UI tree with testIds and VerifyState
$ .claude/skills/verify-clerk-android/bin/control-clerk-android screen --png                          # plus a screenshot in scratch
```

`run` flags are `--skip form-entry`, `--include known-bug`, `--grep <regex>`, `--no-video`, and `--wait <seconds>`.

How `--wait` works:

- `--wait` bounds the wait for a free lane in the machine-wide pool, and separately the wait for another verb in this worktree that is driving the device. Each wait gets the full budget.
- It does not bound the wait for an `up` already running in this worktree. `run` waits for that `up` with no limit and prints that it is waiting.
- Waiters get no turn order. When a lane frees, any waiting worktree can take it.
- When the budget runs out, the verb exits 3 with `POOL_FULL` or `DEVICE_BUSY`.

A spec tagged `known-bug` reproduces a bug the SDK still has. `run` leaves it out by default and prints `skipped: known-bug` beside its title. `--include known-bug` runs it, and it fails while the bug reproduces. Report a run where it fails with the bug named, for the email-code specs "reproduces the known email-code bounce; not verified". When it passes, the bug is fixed: drop the tag in the same PR. The feature file's Gotchas describe each one. Today `sign-in-email-code/request-code` and `sign-in-email-code/complete` carry it.

The `host` fixture:

```ts
import { test, expect } from '../../fixtures.ts';

test('UserProfileView shows the seeded user', async ({ host, screen }) => {
  const user = await host.seedUser();                                          // BAPI user, ledgered for cleanup
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' }); // ticket sign-in, fresh storage
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userProfile.row.manageAccount'));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('profile');                                            // runs/<id>/screenshots/profile.png
});
```

- `host.launch({ signedInAs?, screen?, authMode?, debugLogs?, keepStorage? })` relaunches `com.clerk.e2e` with `am start` string extras and returns the first `VerifyState` for that launch that is ready. It takes no instance. The app runs against the worktree's application, on the settings the spec file declares. Screens are `home`, `auth`, `userProfile`, `orgSwitcher`, `orgList`, and `orgProfile`. Auth modes are `signIn`, `signUp`, and `signInOrUp`. `debugLogs: true` turns on `Clerk.debugMode`, which adds `ClerkLog` debug lines and `OkHttp` request lines to `app.log`.
- `host.seedUser({ phone? })` creates a `+clerk_test` user in that application. `host.newEmail()` reserves an email for a form sign-up.
- `host.state()` reads the footer. `host.waitForState(predicate, timeoutMs?)` polls it. The footer is not readable while a bottom sheet covers the host.
- `host.tap(locator)` taps the middle of the node's box, and `host.fill(locator, text)` taps it and types `text` into the focused field. Use them for every Compose button and field. A Compose Material `Button` with a test tag exposes the tag on a node whose `android.widget.Button` child is not clickable, so `locator.tap()` on the tag can fail; a tap at the middle of the box always lands on the button.
- The footer `verify.state` holds `verify ` plus one line of JSON: `screen`, `environmentLoaded`, `signedIn`, `userId`, `sessionId`, `sessionStatus`, `pendingTasks`, `orgId`, `signInStatus`, `signUpStatus`, `ticket`, `lastError`, `runId`, and `launchId`. `screen` is what is on screen, not what was asked for. `signedIn` is true for a pending session, so read `sessionStatus`.
- Locate with SDK test tags, `screen.getByTestId('clerk.auth.start.identifier')`. The full list is in `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. The `:e2e` host adds `verify.signOut` and `verify.state`. Its home screen is `HomeScreen` in `e2e/src/main/java/com/clerk/e2e/MainActivity.kt`. Its home buttons (`Prebuilt UI Sign In`, `Custom OTP Sign In`) have no tags, so specs find them with `screen.getByText(...)`.

There are two ways to check work.

1. **Golden specs** under `specs/golden/<feature>/` are committed, cover the Feature Map in `features/`, and run unchanged as regression. Run the features your change touches.
2. **New work.** Write a spec under `specs/explored/` (gitignored), run it, and read the end state with `.claude/skills/verify-clerk-android/bin/control-clerk-android screen`. Fix locators from the `screen` output until it passes. To explore a setting the standard instance does not have, export `instanceSettings` from the explored spec, as [Test instances](#test-instances) describes. Take the leaf names from `environment` and `defaults` in `src/core/instances/base.json`, and when the change moves a leaf you did not list, paste the leaves the failure prints. The declaration is part of the spec file, so it goes with the spec when the PR commits it into `specs/golden/<feature>/`. The PR commits that spec into `specs/golden/<feature>/` and updates the feature file when the change adds or changes a user-facing behavior. Otherwise the spec stays with the run evidence (`runs/<id>/specs/` keeps a copy of every spec a run used).

An explored spec sits one level below `specs/`, so it imports the fixture as `../fixtures.ts`, where a golden spec uses `../../fixtures.ts`:

```ts
import { test, expect } from '../fixtures.ts';
```

A failing spec prints `FAIL`, the first assertion message, and the path of its failure page, and `run` exits 1. The failure page (`runs/<id>/e2e/failures/*.md`) lists every step, the screen tree at the failure, and a screenshot. In a run with several groups it is under the e2e directory of the group the spec ran in, such as `runs/<id>/e2e-2/failures/`, and `run` prints the path. `next` points at it.

The first run below fails on purpose: the spec guessed a locator, and the failure plus `screen` show the right one.

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run specs/explored/probe.e2e.ts          # expected to fail: the locator is still a guess
  FAIL  explored/probe.e2e.ts  probe  9.1s
        expect.toBeVisible failed; locator: getByTestId("verify.probe"); expected: visible; observed: no node (match count 0)
        failure page  .verify/runs/r20261003-040814-846e/e2e/failures/specs_explored_probe.e2e.ts-....md
$ .claude/skills/verify-clerk-android/bin/control-clerk-android screen
text       "verify probe"  id=verify.probe  screen.getByTestId('verify.probe')
$ .claude/skills/verify-clerk-android/bin/control-clerk-android run specs/explored/probe.e2e.ts          # after fixing the locator
  pass  explored/probe.e2e.ts  probe  4.7s
$ cd .claude/skills/verify-clerk-android
$ mkdir -p specs/golden/<feature> && mv specs/explored/probe.e2e.ts specs/golden/<feature>/
$ sed -i '' "s|'../fixtures.ts'|'../../fixtures.ts'|" specs/golden/<feature>/probe.e2e.ts
$ bin/control-clerk-android run <feature>
```

`specs/explored/` is gitignored and does not exist in a fresh worktree, so create it with `mkdir -p .claude/skills/verify-clerk-android/specs/explored` first. The `sed` step is required: a golden spec sits one folder deeper and imports `../../fixtures.ts`, so the moved spec does not load until its import changes. Plain `mv` is right here, because git never tracked the explored file.

`<feature>` is the Feature Map entry whose user-facing behavior the change affects, one of the folders under `specs/golden/`. A change to the `:e2e` host alone (`e2e/**`), such as a probe or a new route, is not an SDK feature: keep its spec under `specs/explored/` and cite the run in the PR instead of committing it.

Every spec keeps at least one exact assertion on `verify.state` or an SDK test tag.

### AI judge (off by default)

e2e's `agent.assert` can judge visual claims that selectors cannot check. It is off. Golden specs never use it. To trial it in an explored spec, install `ai` (`npm i -D ai`), log in with `npx e2e login openai` (a ChatGPT Plus or Pro plan), and set `VERIFY_JUDGE_MODEL=chatgpt:<model-id>` (ids from `npx e2e models`). Only then does the composed config set `agents.default.model`. Without the variable the config has no model and any agent step fails.

### Test users and sign-in

Clerk's test mode makes all of this safe to type into the real app.

- **Emails.** Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints `verify_<runId>_<n>+clerk_test@example.com`, new per run, so sign-up never collides.
- **Phones.** Any US number from 555-0100 to 555-0199 is a test number. Type it as ten digits, for example `5555550142`. On the standing instances the numbers are shared across repos, CI, and agents, so always get one from `host.seedUser({ phone: true })` instead of picking one by hand.
- **One-time code.** `424242` verifies every email code and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`. It is public, so it is not an e2e secret, and screenshots after the fill are kept.
- **Passwords.** The standard instance requires a password at sign-up. Use a throwaway per run, such as `Verify-<runId>-Pw1!`.
- **Authenticator (TOTP) codes.** The setup-MFA authenticator screen shows the key as plain text with no test tag. Compute the 6-digit code with RFC 6238 (SHA-1, 30 second step) from it. No golden spec completes this task on Android yet.

The standard instance covers most specs. A spec file that needs something else declares it, as [Test instances](#test-instances) describes.

| A spec needs | Its file declares |
| --- | --- |
| Any auth method or signed-in feature: email code, email link, phone code, password, username, TOTP, backup codes, organizations, social buttons | Nothing. The standard instance has all of them. |
| Sign-ins that stop on the "set up MFA" session task | `config: { auth_multi_factor: { required_for_sign_up: true } }` and `environment: { 'user_settings.sign_up.mfa.required': true }`, as in `specs/golden/session-tasks/setup-mfa.e2e.ts` |
| Sign-ins that stop on the "choose or create an organization" session task | `config: { organization_settings: { force_organization_selection: true } }` and `environment: { 'organization_settings.force_organization_selection': true }`, as in `specs/golden/session-tasks/choose-organization.e2e.ts` |

| Step | Test tag |
| --- | --- |
| Identifier field (email or username) | `clerk.auth.start.identifier` |
| Switch between email and phone | `clerk.auth.start.identifierSwitcher` |
| Phone field | `clerk.auth.start.phoneNumber` |
| Continue on the start screen | `clerk.auth.start.continue` |
| Sign-in code field | `clerk.auth.signIn.code` |
| Sign-in password field | `clerk.auth.signIn.password` |
| Try another method (code, password, and email link screens) | `clerk.auth.signIn.useAnotherMethod` |
| Pick a method from the list | `clerk.auth.signIn.alternativeMethod.<strategy>`, for example `clerk.auth.signIn.alternativeMethod.email_code` |
| Sign-up email, password, continue | `clerk.auth.signUp.emailAddress`, `clerk.auth.signUp.password`, `clerk.auth.signUp.continue` |
| Sign-up code field | `clerk.auth.signUp.code` |
| Legal consent, when shown | `clerk.auth.signUp.legalAccepted` |
| MFA setup choices | `clerk.auth.sessionTask.setupMfa.smsCode`, `clerk.auth.sessionTask.setupMfa.authenticatorApp` |

Prove the result from the host's state, not from the screen alone:

```ts
import { test, expect, CLERK_TEST_CODE } from '../../fixtures.ts';

test('signs in with the email code', { tags: ['form-entry'] }, async ({ host, screen }) => {
  const user = await host.seedUser();
  await host.launch({ screen: 'auth', authMode: 'signIn' });
  await host.fill(screen.getByTestId('clerk.auth.start.identifier'), user.email);
  await host.tap(screen.getByTestId('clerk.auth.start.continue'));
  await host.tap(screen.getByTestId('clerk.auth.signIn.useAnotherMethod'));
  await host.tap(screen.getByTestId('clerk.auth.signIn.alternativeMethod.email_code'));
  await host.fill(screen.getByTestId('clerk.auth.signIn.code'), CLERK_TEST_CODE);
  const state = await host.waitForState((s) => s.signedIn);
  expect(state.userId).toBe(user.id);
  await host.screenshot('signed-in');
});
```

On the standard instance an email sign-in starts on the email-link screen ("Check your email"), so the spec switches to the email code through `Use another method`. For a phone sign-in, the start screen shows `clerk.auth.start.phoneNumber` first; fill it with the seeded user's phone, continue, and type the same code. For sign-up, launch with `authMode: 'signUp'`, fill the email from `host.newEmail()` into `clerk.auth.start.identifier`, continue, fill the run password into `clerk.auth.signUp.password`, continue, then type the code into `clerk.auth.signUp.code`. Android asks for the password before the code; iOS asks after.

Rules:

- Type only `+clerk_test` emails, 555-0100 to 0199 phones, `424242`, and the run password. Never a real person's address, number, or password. The repo is public, and every video lands on a PR.
- Use ticket sign-in (`host.launch({ signedInAs })`) only to reach signed-in screens for features that are not about authentication. A change to an auth method gets a spec that drives the real form.
- Tag every spec that types a code or a password `form-entry`. Those specs run by default. An agent runtime that refuses to type codes or passwords into an app that talks to hosted Clerk runs `.claude/skills/verify-clerk-android/bin/control-clerk-android run --skip form-entry`, which reports them as `skipped by --skip form-entry`, and says so in the PR. CI runs the skipped specs.
- The form-entry specs, one command each: `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-in-email-code/complete`, `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-up/request-code`, and `.claude/skills/verify-clerk-android/bin/control-clerk-android run sign-up/complete`.
- `.claude/skills/verify-clerk-android/bin/control-clerk-android down` deletes every user the run created, including users created through the sign-up form. With a throwaway application it deletes the whole application they live in. On the standing instances it deletes each user by test email.

## Evidence

Every `run` writes `.verify/runs/<run-id>/` and prints its path:

| File | What it is |
| --- | --- |
| `run.json` | The sealed record: `results` per spec, `gitHead`, `dirty`, `build` (the build key), `backend`, `device`, `identities`, `instances` (each instance the run used, as `application` with the application id or `standing` with the name of a standing instance), `settings`, and `tainted` files. `settings` has one entry per group, in the order the groups ran: `label`, `askedBy` (the spec file that declared the settings, or `null` for the standard ones), `specs`, `application`, `changed` (a config request was sent for the group), `held` (the instance still showed the settings when the group ended), and `e2eReport`. A remote run also has `remote`, with `provider`, `runner`, and `builtSha` |
| `video.mp4` | `adb shell screenrecord --size 720x1608` of the whole run, stopped on the device with SIGINT before it is pulled |
| `screenshots/<label>.png` | Every `host.screenshot(label)` |
| `states.jsonl` | Every `VerifyState` the fixture read, in order, across every test in the run |
| `state.json` | Only the last state of the whole run. With several tests, read per-test states from `states.jsonl` by `launchId` |
| `app.log` | `adb logcat` lines from the run for `ClerkVerify`, `ClerkLog`, `OkHttp` (network lines only with `debugLogs: true`), and `AndroidRuntime` errors |
| `e2e/` | e2e's `report.json`, failure pages, and `screen.txt` for failed steps. A run with several groups has one such directory per group, in the order they ran: `e2e/`, `e2e-2/`, `e2e-3/` |
| `e2e.log` | e2e's console output |
| `specs/` | A copy of every spec the run used |

Proof standards: drive the real user path, capture the action and the resulting state (the video plus `states.jsonl`), and check side effects in `states.jsonl` (`userId`, `orgId`, `pendingTasks`), not only the final screen.

After a run, sealing searches the run directory for every secret the run used (the Platform API key, secret keys, tickets). A hit marks the file tainted in `run.json`, and a tainted run cannot be attached.

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android attach <run-id> --pr <n>                       # video and every screenshot
$ .claude/skills/verify-clerk-android/bin/control-clerk-android attach <run-id> --pr <n> --screenshot profile  # video and one screenshot
```

`attach` posts once per run with `gh pr comment --attach`. It refuses a run that is tainted, failed, or shows a user id the run did not create.

Attach the focused run, not the regression run. Run your new or changed spec on its own (`.claude/skills/verify-clerk-android/bin/control-clerk-android run specs/explored/<name>.e2e.ts`, or `.claude/skills/verify-clerk-android/bin/control-clerk-android run <feature>/<spec>` once it is golden) and attach that run, so the PR video shows only the behavior the change is about. Run the golden specs for every feature you touched in a separate `run`, cite its run id in the PR as regression evidence, and leave its video in `.verify/runs/`.

## Cleanup

```console
$ .claude/skills/verify-clerk-android/bin/control-clerk-android down --dry-run   # what it would release, delete, and stop: this worktree's application, and on the standing instances each user and organization
$ .claude/skills/verify-clerk-android/bin/control-clerk-android down             # kill the lane emulator or end the remote session, delete this worktree's application with every user in it, stop recorders and this worktree's agent-device daemon
$ .claude/skills/verify-clerk-android/bin/control-clerk-android down --stale     # also finish cleanup left by a crashed run in this worktree
```

`down` deletes only what this worktree created: its lane emulator (with `adb emu kill`), every application in its ledger, and on the standing instances the users in its ledger and the organizations those users own. It needs the same credential `up` used, releases the emulator first, and if Clerk refuses a delete it fails with the fix `down` again, which finishes what is left. On the standing instances it finds the organizations through each user at cleanup time, not from ledger entries, so an organization a spec created in the UI is deleted too. Ledgers live at `~/.verify/ledgers/<id>.jsonl`, where `<id>` is a hash of the worktree path, and `<id>.owner` beside it holds the path. Find yours with `grep -l "$(git rev-parse --show-toplevel)" ~/.verify/ledgers/*.owner`. It never deletes `.verify/runs/`. Evidence survives teardown at `.claude/skills/verify-clerk-android/.verify/runs/<run-id>/`, and `down` lists the kept runs. Run `down` after a failed iteration too, so no emulator is stranded. Emulator console output goes to `~/.verify/emulators/android-<slot>.log`.

Evidence lives inside the worktree, so `git worktree remove` deletes `.verify/runs/` with it. Copy the runs you need out first.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it, including a worktree that already holds its own lease: every `up` and `run` reaps first. It kills that worktree's lane emulator, deletes its applications and the users in its ledger, then closes the ledger. Lane slots are machine-wide claims under `~/.verify/claims/android-<slot>/`. A slot changes hands only by compare-and-swap, so two worktrees never hold the same lane.

## Helpers

- `.claude/skills/verify-clerk-android/bin/control-clerk-android` is the only helper. It is executable, and every invocation is shown above. It works from any directory, through the `.claude/skills/verify-clerk-android` symlink too.
- `e2e.config.ts` composes the e2e config from the CLI's run context. `npx e2e list` works from `.claude/skills/verify-clerk-android/` while a lease is held.
- `specs/fixtures.ts` is the `host` fixture. It takes its screen names from `src/host.ts`, so it is the same file in every repo.
- `src/core/` is byte-identical to clerk-ios `.claude/skills/verify-clerk-ios/src/core/`, and `MANIFEST` pins it. Change it there first, then copy it here. `src/platform/android/` and `src/host.ts` are this repo's own.
- `src/core/instances/` creates, configures, and deletes the worktree's application. `base.json` is the standard settings. `settings.ts` reads a spec file's declaration, checks it, and groups spec files. `platform.ts` is the Platform API client and the one place that names the workspace. `throwaway.ts` is the application's lifecycle, and `instances.ts` chooses between a throwaway application and the standing instances.
- `src/core/remote/` is the remote backend: the session agent that runs on the runner, the GitHub Actions provider, and the tunnel settings (`tunnel.ts` is the one place that names the tunnel host). `.github/workflows/verify-remote.yml` is the session's workflow. `src/platform/android/session-device.ts` is what the session builds, records, and logs with, `session-lane.ts` boots the runner's emulator through the local backend, and `emulator.ts` holds the commands both backends share.
- `npm test` runs the CLI's unit tests (`node --test test/*.test.ts`), with no network, keys, or emulator. `testing/` holds helper processes those tests spawn; they are not tests themselves. `npm run typecheck` runs `tsc`.
- `features/` is the Feature Map. Start with `features/README.md`.

Keep the map honest with pstack's `maintain-verification-skill`, which finds this skill through the `.claude/skills/verify-clerk-android` symlink.
