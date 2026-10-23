# CLI reference

## Verbs and flags

```console
e2e-tests/bin/control-clerk-android doctor [--backend auto|local|remote] [--runner <label>] [--live]
e2e-tests/bin/control-clerk-android up [--backend auto|local|remote] [--runner <label>] [--wait <seconds>]
e2e-tests/bin/control-clerk-android run <feature | feature/spec | path.e2e.ts>... | --all [--backend auto|local|remote] [--runner <label>] [--grep <regex>] [--retries <n>] [--max-failures <n>] [--github-report] [--no-video] [--wait <seconds>]
e2e-tests/bin/control-clerk-android screen [--png]
e2e-tests/bin/control-clerk-android attach <run-id> --pr <n> --screenshot <label>... | --video [--before <run-id>]
e2e-tests/bin/control-clerk-android down [--stale] [--dry-run]
```

`attach` takes `--screenshot` or `--video`, and fails with a usage error with neither, or with both. `SKILL.md` has the choice under Evidence.

Every verb takes `--json`. [Output](#output) has what each verb prints, and the exit codes and error codes are below it.

`--github-report` is for a CI job. After the run, `run` hands the results of every settings group to `@e2e-dev/github` as one report. The reporter writes that report to the job summary. When the event of the job names a pull request and the step has a `GITHUB_TOKEN` that may write pull request comments, it also posts one comment and updates that same comment on later runs. `run` prints one `github` line that says what the reporter did, the reporter never changes the exit code, and nothing is reported for a run whose files hold a secret. Without the flag, `run` reports nothing to GitHub.

`--backend auto` is the default and leaves the choice to the CLI. `--runner` names the runner label of a remote session. With the local backend it is a usage error, and `doctor` takes it only with `--live`.

## `--wait`

- `--wait` bounds the wait for a free lane in the machine-wide pool, and separately the wait for another verb in this worktree that is driving the device. Each wait gets the full budget. The default is 0, so a full pool fails at once.
- It does not bound the wait for an `up` already running in this worktree. `run` waits for that `up` with no limit and prints that it is waiting.
- Waiters get no turn order. When a lane frees, any waiting worktree can take it.
- When the budget runs out, the verb exits 3 with `POOL_FULL` or `DEVICE_BUSY`.
- A remote session has no lane pool, so there `--wait` only bounds the wait for another verb in this worktree.

While it waits, the CLI prints a `wait` line naming each lane and the worktree that holds it, prints it again when that changes, and prints `still waiting after <n>s` every minute otherwise.

## Lanes and ownership

A lane emulator boots the `Clerk_Verify_Pixel` AVD with `-read-only -no-window` as `emulator-5560` or `emulator-5562`. Any other emulator on a lane port, such as another AVD or a lane someone booted by hand, counts as taken, shows in the `POOL_FULL` list as `emulator-<port> (<AVD>, not a verify lane)`, and is never killed. The `POOL_FULL` fix names `adb -s emulator-<port> emu kill`. Run it only if that emulator is yours.

Interrupting `up` or `run` while a lane boots is safe. On Ctrl-C or SIGTERM the CLI stops the emulator it started and prints `boot cancelled; stopped emulator <pid> on <serial>`. If the process dies harder than that, the next `up` or `run` in any worktree, or `down --stale` in this one, kills the emulator it left behind.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it: it kills that worktree's lane emulator and deletes its application.

## Output

A verb prints progress lines to stderr while it works and its result to stdout when it ends. An error goes to stderr as two lines: `error`, the error code, and the message, then `fix:` and what to do.

With `--json` a verb prints no progress lines, and prints one JSON object to stdout. On success the object is `{ "ok": true, "verb": "<verb>", ... }`. The remaining keys depend on the verb. `src/core/types.ts` of `@clerk/internal-mobile-e2e` defines them as `DoctorReport`, `UpResult`, `RunResult`, `ScreenResult`, `AttachResult`, and `DownResult`. On an error the object is `{ "ok": false, "error": { "code", "message", "fix", "retryable" } }`.

`doctor --json` prints its report even when a check fails, with `ok: false` and no `error`. Each check has `id`, `ok`, `detail`, and a `fix` when there is one. A warning has `ok: true` and `state: "warning"`. A check that did not run has `ok: true` and `state: "not-run"`.

`attach --json` prints one of two shapes. With `via: "gh"`, this machine edited the description, and the keys are `prUrl`, `posted`, and `alreadyPosted`. With `via: "runner"`, the evidence was handed to the session's runner, and the keys are `pr`, `handedOff`, `because`, `sessionRun`, and `sessionRunUrl`.

A dry run of `down` prints `dryRun: true`, `wouldRelease`, `wouldDelete`, `wouldStop`, and `keptRuns`. A real `down` prints `dryRun: false`, `released`, `deletedApplications`, `stoppedProcesses`, and `keptRuns`. Each entry of `wouldDelete` is `{ "kind": "application", "name" }`, and each entry of `deletedApplications` is `{ "name" }`. Neither lists users or organizations.

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | The verb succeeded. For `run`, no spec failed. A spec that passed on a retry is `flaky` and does not fail the run |
| 1 | `run` finished and at least one spec failed or was interrupted |
| 2 | The error code is `USAGE` |
| 3 | Any other error, or `doctor` has a failing check |

## Error codes

`retryable` is true for `POOL_FULL`, `DEVICE_BUSY`, `LEASE_LOST`, and `RATE_LIMITED`. For those, the same command can succeed later with no other change.

| Code | Meaning |
| --- | --- |
| `USAGE` | The command line, a spec's declaration, or an environment variable is malformed |
| `NOT_READY` | A precondition is missing. The fix names it |
| `POOL_FULL` | Both lanes on this machine are in use, by other worktrees or by emulators that are not lanes |
| `DEVICE_BUSY` | Another command in this worktree is driving the device |
| `LEASE_LOST` | The leased emulator or the remote session is gone |
| `BUILD_FAILED` | `./gradlew :e2e:assembleDebug` failed. The message has the last 15 lines of its output. With the remote backend, also that GitHub does not have HEAD, that the session could not build the commit, or that the uncommitted changes to the app sources do not apply to it, change a file outside the app sources, or are larger than 16 MiB. A session on `main` refuses uncommitted changes |
| `KEYS_MISSING` | No Platform API credential works on this machine, or `AI_GATEWAY_API_KEY_FILE` names a file that is missing, empty, or readable by other users |
| `INSTANCE_MISCONFIGURED` | The Clerk instance does not show the settings a spec needs |
| `NOT_TEST_IDENTITY` | Reserved. No command raises it. A run whose `app.log` names a user that the run did not create fails `attach` with `EVIDENCE_UNSAFE` |
| `NO_SPECS` | The selection matched no spec, or no selected spec ran |
| `E2E_CRASHED` | e2e exited without a readable report. Read `e2e.log` in the run directory |
| `EVIDENCE_UNSAFE` | The run cannot be attached, or a path is not safe to delete |
| `UNSUPPORTED` | This machine cannot do what the command asked, such as `--backend local` with no emulator to run |
| `RATE_LIMITED` | Clerk's Platform API kept answering 429 |
