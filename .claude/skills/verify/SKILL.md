---
name: verify
description: Drive the clerk-android SDK UI (AuthView, UserButton, UserProfileView, OrganizationSwitcher, session tasks) in the :e2e host app on a lane Android emulator against a real Clerk dev instance, and capture video, screenshots, and host state as evidence. Use it to prove any change to source/api, source/ui, or the :e2e host works before calling it done, to reproduce a UI bug, or to run the golden regression specs.
---

# verify

`bin/verify` is a control CLI over [e2e](https://github.com/tester-army/e2e) 0.15.2 and `@e2e-dev/mobile` 0.9.0. It builds the `:e2e` debug APK, leases a lane emulator, seeds `+clerk_test` users, runs specs, and keeps the evidence. Run every command from `.claude/skills/verify/`. Every verb takes `--json` and then prints one `{ "ok": ... }` object. Exit codes are 0 for ok, 1 for spec failures, 2 for usage errors, and 3 for a failed precondition. Every error carries a `fix`.

The rule: no change to clerk-android UI or auth behavior is done until a `bin/verify run` on the real host shows the changed behavior.

## Launch

```console
$ npm ci                       # once per worktree, before anything else
$ bin/verify doctor            # exits 3 until the first up, because build is the one failing check
$ bin/verify up                # build the :e2e APK for this tree, then lease verify-android-<n> and install
build   android-b17728aef656  local  building...
device  verify-android-1  booting Clerk_Verify_Pixel -read-only on port 5560
install android-b17728aef656  on verify-android-1
build   android-b17728aef656  local  41s
device  verify-android-1  local  leased by this worktree  installed android-b17728aef656
```

The lane is ready when `up` prints its last line, `device <name> local leased by this worktree installed <build key>`. The `device ... booting` and `install` lines are progress. A reused build prints `build <key> local reused` and no `building...` line.

`up` is idempotent. It reuses a build whose key matches the current tree (a hash of `source/`, `e2e/`, `gradle/`, and the root Gradle files, minus docs and specs) and a lease this worktree already holds. It builds before it claims a lane, because a build needs no device. `run` calls `up` itself, so `up` exists to start the slow part early. `bin/verify up &` followed by `bin/verify run ...` is fine: `run` waits for the `up` to finish and uses its lease.

The build runs `./gradlew :e2e:assembleDebug` with `JAVA_HOME` set to a Java 21 JDK, because `main` rejects Java 17. It uses `JAVA_HOME` when that is Java 21 or newer, and otherwise Android Studio's bundled JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. It sets `ANDROID_HOME` to `~/Library/Android/sdk` when the environment has none, so a fresh worktree with no `local.properties` builds.

A lane emulator boots the `Clerk_Verify_Pixel` AVD with `-read-only -no-window` on console port `5558 + 2 * slot`, so slot 1 is `emulator-5560` and slot 2 is `emulator-5562`. `-read-only` means nothing the run does reaches the AVD, and the next lease boots clean. `up` waits for `sys.boot_completed` and pins the `en-US` locale. Read the serial from `.verify/leases/android.json`.

Never drive `Pixel_9_Pro`, an emulator you did not lease, or a physical device. Two lane emulators can run on the Mac at once, across all agents. An emulator already listening on a lane port without a verify claim counts as taken and is never killed. When both lanes are taken, `up` and `run` fail with `POOL_FULL`. Pass `--wait <seconds>` to wait for a lane. While waiting, the CLI prints one `wait` line naming the lanes in use, and prints it again only when that set changes.

Each worktree runs its own agent-device daemon from its own `node_modules`, with state under `.verify/agent-device/`. The CLI passes `AGENT_DEVICE_STATE_DIR` to e2e and to every `agent-device` call, and `down` stops the daemon. If you call `agent-device` yourself, set `AGENT_DEVICE_STATE_DIR=.verify/agent-device` and use `node_modules/.bin/agent-device`.

Teardown is `bin/verify down` (see Cleanup).

## Doctor

```console
$ bin/verify doctor --json
```

Run it first, and again whenever anything looks off. It is read-only. It checks:

- Node 24 and the pinned e2e and agent-device versions against the global `agent-device`.
- The JDK the build will use. With `JAVA_HOME` on Java 17 it fails, and the fix is `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- The Android SDK `emulator` and `adb`, and the `Clerk_Verify_Pixel` AVD.
- The three instances' keys by name, and each instance's enabled strategies from `/v1/environment`. Keys come from `.keys.json` at the root of the main clerk-android checkout, not the linked worktree. CI can pass `CLERK_TEST_KEYS_JSON` instead.
- Whether an `:e2e` APK build matches the current tree.
- `gh pr comment --attach` support, stale device claims, and drift in `src/core/`.
- Whether an agent-device daemon, the Mac-wide one in `~/.agent-device` or this worktree's own, runs from an install that no longer exists. The fix names the pid to kill.
- Whether every feature in the Feature Map has its feature file and at least one golden spec.

A failing check prints the command that fixes it, and `doctor` exits 3. Before the first `up`, only `build` fails, with fix `bin/verify up`.

## Drive

Input only goes through specs. A spec is a TypeScript file that uses the `host` fixture from `specs/fixtures.ts` and e2e's `screen` locators.

```console
$ bin/verify run auth-start                        # one feature (specs/golden/auth-start/)
$ bin/verify run sign-up/password-step             # one spec
$ bin/verify run specs/explored/resend.e2e.ts      # a spec you wrote
$ bin/verify run --all --skip form-entry           # every golden spec except form entry
$ bin/verify screen                                # current UI tree with testIds and VerifyState
$ bin/verify screen --png                          # plus a screenshot in scratch
```

`run` flags are `--skip form-entry`, `--include known-bug`, `--grep <regex>`, `--no-video`, and `--wait <seconds>` (how long to wait for a free lane or for another verb in this worktree that holds the device).

A spec tagged `known-bug` reproduces a bug the SDK still has. `run` leaves it out by default and prints `skipped: known-bug` beside its title. `--include known-bug` runs it, and it fails while the bug reproduces. When it passes, the bug is fixed: drop the tag in the same PR. The feature file's Gotchas describe each one. Today `sign-in-email-code/request-code` and `sign-in-email-code/complete` carry it.

The `host` fixture:

```ts
import { test, expect } from '../../fixtures.ts';

test('UserProfileView shows the seeded user', async ({ host, screen }) => {
  const user = await host.seedUser({ instance: 'with-email-codes' });          // BAPI user, ledgered for cleanup
  const state = await host.launch({ signedInAs: user, screen: 'userProfile' }); // ticket sign-in, fresh storage
  expect(state.userId).toBe(user.id);
  await host.tap(screen.getByTestId('clerk.userProfile.row.manageAccount'));
  await expect(screen.getByText(user.email)).toBeVisible({ timeout: 20_000 });
  await host.screenshot('profile');                                            // runs/<id>/screenshots/profile.png
});
```

- `host.launch({ instance | signedInAs, screen?, authMode?, debugLogs?, keepStorage? })` relaunches `com.clerk.e2e` with `am start` string extras and returns the first `VerifyState` for that launch that is ready. Screens are `home`, `auth`, `userProfile`, `orgSwitcher`, `orgList`, and `orgProfile`. Auth modes are `signIn`, `signUp`, and `signInOrUp`. `debugLogs: true` turns on `Clerk.debugMode`, which adds `ClerkLog` debug lines and `OkHttp` request lines to `app.log`.
- `host.seedUser({ instance, phone? })` creates a `+clerk_test` user. `host.newEmail(instance)` reserves an email for a form sign-up.
- `host.state()` reads the footer. `host.waitForState(predicate, timeoutMs?)` polls it. The footer is not readable while a bottom sheet covers the host.
- `host.tap(locator)` taps the middle of the node's box, and `host.fill(locator, text)` taps it and types `text` into the focused field. Use them for every Compose button and field. A Compose Material `Button` with a test tag exposes the tag on a node whose `android.widget.Button` child is not clickable, so `locator.tap()` on the tag can fail; a tap at the middle of the box always lands on the button.
- The footer `verify.state` holds `verify ` plus one line of JSON: `screen`, `environmentLoaded`, `signedIn`, `userId`, `sessionId`, `sessionStatus`, `pendingTasks`, `orgId`, `signInStatus`, `signUpStatus`, `ticket`, `lastError`, `runId`, and `launchId`. `screen` is what is on screen, not what was asked for. `signedIn` is true for a pending session, so read `sessionStatus`.
- Locate with SDK test tags, `screen.getByTestId('clerk.auth.start.identifier')`. The full list is in `source/ui/src/main/java/com/clerk/ui/ClerkTestTags.kt`. The `:e2e` host adds `verify.signOut` and `verify.state`. Its home buttons (`Prebuilt UI Sign In`, `Custom OTP Sign In`) have no tags, so specs find them with `screen.getByText(...)`.

There are two ways to check work.

1. **Golden specs** under `specs/golden/<feature>/` are committed, cover the Feature Map in `features/`, and run unchanged as regression. Run the features your change touches.
2. **New work.** Write a spec under `specs/explored/` (gitignored), run it, and read the end state with `bin/verify screen`. Fix locators from the `screen` output until it passes. The PR commits that spec into `specs/golden/<feature>/` and updates the feature file when the change adds or changes a user-facing behavior. Otherwise the spec stays with the run evidence (`runs/<id>/specs/` keeps a copy of every spec a run used).

An explored spec sits one level below `specs/`, so it imports the fixture as `../fixtures.ts`, where a golden spec uses `../../fixtures.ts`:

```ts
import { test, expect } from '../fixtures.ts';
```

A failing spec prints `FAIL`, the first assertion message, and the path of its failure page, and `run` exits 1. The failure page (`runs/<id>/e2e/failures/*.md`) lists every step, the screen tree at the failure, and a screenshot. `next` points at it.

```console
$ bin/verify run specs/explored/probe.e2e.ts
  FAIL  explored/probe.e2e.ts  probe  9.1s
        expect.toBeVisible failed; locator: getByTestId("verify.probe"); expected: visible; observed: no node (match count 0)
        failure page  .verify/runs/r20261003-040814-846e/e2e/failures/specs_explored_probe.e2e.ts-....md
$ bin/verify screen
text       "verify probe"  id=verify.probe  screen.getByTestId('verify.probe')
$ git mv specs/explored/probe.e2e.ts specs/golden/<feature>/
```

Every spec keeps at least one exact assertion on `verify.state` or an SDK test tag.

### AI judge (off by default)

e2e's `agent.assert` can judge visual claims that selectors cannot check. It is off. Golden specs never use it. To trial it in an explored spec, install `ai` (`npm i -D ai`), log in with `npx e2e login openai` (a ChatGPT Plus or Pro plan), and set `VERIFY_JUDGE_MODEL=chatgpt:<model-id>` (ids from `npx e2e models`). Only then does the composed config set `agents.default.model`. Without the variable the config has no model and any agent step fails.

## Test users and sign-in

Clerk's test mode makes all of this safe to type into the real app.

- **Emails.** Any address that contains `+clerk_test@` is a test address. Clerk sends no mail and accepts the code below. The fixture mints `verify_<runId>_<n>+clerk_test@example.com`, new per run, so sign-up never collides.
- **Phones.** Any US number from 555-0100 to 555-0199 is a test number. Type it as ten digits, for example `5555550142`. The numbers are shared across repos, CI, and agents, so get one from `host.seedUser({ instance, phone: true })` instead of picking one by hand.
- **One-time code.** `424242` verifies every email code and SMS code for test addresses and phones. Specs use the constant `CLERK_TEST_CODE`. It is public, so it is not an e2e secret, and screenshots after the fill are kept.
- **Passwords.** `with-email-codes` (the `all-enabled` instance) requires a password at sign-up. Use a throwaway per run, such as `Verify-<runId>-Pw1!`.
- **Authenticator (TOTP) codes.** The setup-MFA authenticator screen shows the key as plain text with no test tag. Compute the 6-digit code with RFC 6238 (SHA-1, 30 second step) from it. No golden spec completes this task on Android yet.

| Instance key in `.keys.json` | Use it for |
| --- | --- |
| `with-email-codes` (the `all-enabled` instance) | Every auth method and every signed-in feature: email code, email link, phone code, password, username, TOTP, backup codes, organizations, social buttons. |
| `with-session-tasks-setup-mfa` | Sign-ins that must stop on the "set up MFA" session task. MFA is required for every user there. |
| `with-session-tasks` | Sign-ins that must stop on the "choose or create an organization" session task. |

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
  const user = await host.seedUser({ instance: 'with-email-codes' });
  await host.launch({ instance: 'with-email-codes', screen: 'auth', authMode: 'signIn' });
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

On `with-email-codes` an email sign-in starts on the email-link screen ("Check your email"), so the spec switches to the email code through `Use another method`. For a phone sign-in, the start screen shows `clerk.auth.start.phoneNumber` first; fill it with the seeded user's phone, continue, and type the same code. For sign-up, launch with `authMode: 'signUp'`, fill the email from `host.newEmail('with-email-codes')` into `clerk.auth.start.identifier`, continue, fill the run password into `clerk.auth.signUp.password`, continue, then type the code into `clerk.auth.signUp.code`. Android asks for the password before the code; iOS asks after.

Rules:

- Type only `+clerk_test` emails, 555-0100 to 0199 phones, `424242`, and the run password. Never a real person's address, number, or password. The repo is public, and every video lands on a PR.
- Use ticket sign-in (`host.launch({ signedInAs })`) only to reach signed-in screens for features that are not about authentication. A change to an auth method gets a spec that drives the real form.
- Tag every spec that types a code or a password `form-entry`. Those specs run by default. An agent runtime that refuses to type codes or passwords into an app that talks to hosted Clerk runs `bin/verify run --skip form-entry`, which reports them as `skipped by --skip form-entry`, and says so in the PR. CI runs the skipped specs.
- The form-entry specs, one command each: `bin/verify run sign-in-email-code/complete`, `bin/verify run sign-up/request-code`, and `bin/verify run sign-up/complete`.
- `bin/verify down` deletes every user the run created, including users created through the sign-up form, by their test email.

## Evidence

Every `run` writes `.verify/runs/<run-id>/` and prints its path:

| File | What it is |
| --- | --- |
| `run.json` | The sealed record: `results` per spec, `gitHead`, `dirty`, `build` (the build key), `device`, `identities`, and `tainted` files |
| `video.mp4` | `adb shell screenrecord --size 720x1608` of the whole run, stopped on the device with SIGINT before it is pulled |
| `screenshots/<label>.png` | Every `host.screenshot(label)` |
| `states.jsonl` | Every `VerifyState` the fixture read, in order, across every test in the run |
| `state.json` | Only the last state of the whole run. With several tests, read per-test states from `states.jsonl` by `launchId` |
| `app.log` | `adb logcat` lines from the run for `ClerkVerify`, `ClerkLog`, `OkHttp` (network lines only with `debugLogs: true`), and `AndroidRuntime` errors |
| `e2e/` | e2e's `report.json`, failure pages, and `screen.txt` for failed steps |
| `e2e.log` | e2e's console output |
| `specs/` | A copy of every spec the run used |

Proof standards: drive the real user path, capture the action and the resulting state (the video plus `states.jsonl`), and check side effects in `states.jsonl` (`userId`, `orgId`, `pendingTasks`), not only the final screen.

After a run, sealing searches the run directory for every secret the run used (secret keys, tickets). A hit marks the file tainted in `run.json`, and a tainted run cannot be attached.

```console
$ bin/verify attach <run-id> --pr <n>                       # video and every screenshot
$ bin/verify attach <run-id> --pr <n> --screenshot profile  # video and one screenshot
```

`attach` posts once per run with `gh pr comment --attach`. It refuses a run that is tainted, failed, or shows a user id the run did not create.

## Cleanup

```console
$ bin/verify down --dry-run   # what it would release, delete (users and organizations), and stop
$ bin/verify down             # kill the lane emulator, delete run users and their organizations, stop recorders and this worktree's agent-device daemon
$ bin/verify down --stale     # also finish cleanup left by a crashed run in this worktree
```

`down` deletes only what this worktree created: its lane emulator (with `adb emu kill`) and the users in its ledger. Ledgers live at `~/.verify/ledgers/<id>.jsonl`, where `<id>` is a hash of the worktree path, and `<id>.owner` beside it holds the path. Find yours with `grep -l "$(git rev-parse --show-toplevel)" ~/.verify/ledgers/*.owner`. It never deletes `.verify/runs/`. Evidence survives teardown at `.claude/skills/verify/.verify/runs/<run-id>/`, and `down` lists the kept runs. Run `down` after a failed iteration too, so no emulator is stranded. Emulator console output goes to `~/.verify/emulators/android-<slot>.log`.

Evidence lives inside the worktree, so `git worktree remove` deletes `.verify/runs/` with it. Copy the runs you need out first.

If a worktree is removed without `down`, the next `up` in any worktree finishes for it. It kills that worktree's lane emulator and deletes the users in its ledger, then closes the ledger. Lane slots are machine-wide claims under `~/.verify/claims/android-<slot>/`. A slot changes hands only by compare-and-swap, so two worktrees never hold the same lane.

## Helpers

- `bin/verify` is the only helper. It is executable. Every invocation is shown above.
- `e2e.config.ts` composes the e2e config from the CLI's run context. `npx e2e list` works from this directory while a lease is held.
- `specs/fixtures.ts` is the `host` fixture. It takes its screen names from `src/host.ts`, so it is the same file in every repo.
- `src/core/` is byte-identical to clerk-ios `.claude/skills/verify/src/core/`, and `MANIFEST` pins it. Change it there first, then copy it here. `src/platform/android/` and `src/host.ts` are this repo's own.
- `npm test` runs the CLI's unit tests (`node --test test/*.test.ts`), with no network, keys, or emulator. `testing/` holds helper processes those tests spawn; they are not tests themselves. `npm run typecheck` runs `tsc`.
- `features/` is the Feature Map. Start with `features/README.md`.

Keep the map honest with `/maintain-verification-skill`.
