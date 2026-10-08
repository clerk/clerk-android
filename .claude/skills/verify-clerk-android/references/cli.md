# CLI reference

## Verbs and flags

```console
control-clerk-android doctor [--backend auto|local] [--live]
control-clerk-android up [--backend auto|local] [--wait <seconds>]
control-clerk-android run <feature | feature/spec | path.e2e.ts>... | --all [--backend auto|local] [--grep <regex>] [--retries <n>] [--github-report] [--no-video] [--wait <seconds>]
control-clerk-android screen [--png]
control-clerk-android attach <run-id> --pr <n> [--screenshot <label>]...
control-clerk-android down [--stale] [--dry-run]
```

Every verb takes `--json` and then prints one `{ "ok": ... }` object. Exit codes are 0 for ok, 1 for a failed or interrupted spec, 2 for a usage error, and 3 for a failed precondition. A spec that passed on a retry is `flaky` and does not fail the run. Every error carries a `fix`.

`--github-report` is for a CI job. After the run, `run` hands the results of every settings group to `@e2e-dev/github` as one report. The reporter writes that report to the job summary. When the event of the job names a pull request and the step has a `GITHUB_TOKEN` that may write pull request comments, it also posts one comment and updates that same comment on later runs. `run` prints one `github` line that says what the reporter did, the reporter never changes the exit code, and nothing is reported for a run whose files hold a secret. Without the flag, `run` reports nothing to GitHub.

`--backend auto` is the default and leaves the choice to the CLI.

## `--wait`

- `--wait` bounds the wait for a free lane in the machine-wide pool, and separately the wait for another verb in this worktree that is driving the device. Each wait gets the full budget. The default is 0, so a full pool fails at once.
- It does not bound the wait for an `up` already running in this worktree. `run` waits for that `up` with no limit and prints that it is waiting.
- Waiters get no turn order. When a lane frees, any waiting worktree can take it.
- When the budget runs out, the verb exits 3 with `POOL_FULL` or `DEVICE_BUSY`.

While it waits, the CLI prints a `wait` line naming each lane and the worktree that holds it, prints it again when that changes, and prints `still waiting after <n>s` every minute otherwise.

## Lanes and ownership

A lane emulator boots the `Clerk_Verify_Pixel` AVD with `-read-only -no-window` as `emulator-5560` or `emulator-5562`. Any other emulator on a lane port, such as another AVD or a lane someone booted by hand, counts as taken, shows in the `POOL_FULL` list as `emulator-<port> (<AVD>, not a verify lane)`, and is never killed. The `POOL_FULL` fix names `adb -s emulator-<port> emu kill`. Run it only if that emulator is yours.

Interrupting `up` or `run` while a lane boots is safe. On Ctrl-C or SIGTERM the CLI stops the emulator it started and prints `boot cancelled; stopped emulator <pid> on <serial>`. If the process dies harder than that, the next `up` or `run` in any worktree, or `down --stale` in this one, kills the emulator it left behind.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it: it kills that worktree's lane emulator and deletes its application.
