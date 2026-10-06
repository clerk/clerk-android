# CLI reference

## Verbs and flags

```console
control-clerk-android doctor [--live]
control-clerk-android up [--wait <seconds>]
control-clerk-android run <feature | feature/spec | path.e2e.ts>... | --all [--skip form-entry] [--include known-bug] [--grep <regex>] [--no-video] [--wait <seconds>]
control-clerk-android screen [--png]
control-clerk-android attach <run-id> --pr <n> [--screenshot <label>]...
control-clerk-android down [--stale] [--dry-run]
```

Every verb takes `--json` and then prints one `{ "ok": ... }` object. Exit codes are 0 for ok, 1 for a failed or interrupted spec, 2 for a usage error, and 3 for a failed precondition. Every error carries a `fix`.

## `--wait`

- `--wait` bounds the wait for a free lane in the machine-wide pool, and separately the wait for another verb in this worktree that is driving the device. Each wait gets the full budget. The default is 0, so a full pool fails at once.
- It does not bound the wait for an `up` already running in this worktree. `run` waits for that `up` with no limit and prints that it is waiting.
- Waiters get no turn order. When a lane frees, any waiting worktree can take it.
- When the budget runs out, the verb exits 3 with `POOL_FULL` or `DEVICE_BUSY`.

While it waits, the CLI prints a `wait` line naming each lane and the worktree that holds it, prints it again when that changes, and prints `still waiting after <n>s` every minute otherwise.

## `known-bug`

A spec tagged `known-bug` reproduces a bug the SDK still has. `run` leaves it out by default and prints `skipped: known-bug` beside its title. `--include known-bug` runs it, and it fails while the bug reproduces. Report such a run with the bug named and "not verified". When it passes, the bug is fixed: drop the tag in the same PR. No golden spec carries the tag today.

## The AI judge

e2e's `agent.assert` can judge visual claims that selectors cannot check. It is off, and golden specs never use it. To try it in an explored spec, install `ai` and `@ai-sdk/openai` (`npm i -D ai @ai-sdk/openai`, and do not commit the change to `package.json`), log in with `npx e2e login openai`, and set `VERIFY_JUDGE_MODEL=chatgpt:<model-id>` (ids from `npx e2e models`). Without the variable the composed config has no model and any agent step fails.

## Lanes and ownership

A lane emulator boots the `Clerk_Verify_Pixel` AVD with `-read-only -no-window` on console port `5558 + 2 * slot`, so slot 1 is `emulator-5560` and slot 2 is `emulator-5562`. `up` waits for `sys.boot_completed` and pins the `en-US` locale. Lane slots are machine-wide claims under `~/.verify/claims/android-<slot>/`. A slot changes hands only by compare-and-swap, so two worktrees never hold the same lane.

A serial names a port, not a device: after `down`, another worktree can boot its own lane on the same serial at once. The lane's identity is `claimNonce` in `.verify/leases/android.json`, which the emulator also holds as the property `debug.verify.lane`. Check `adb -s <deviceId> shell getprop debug.verify.lane` against it before you trust a serial you wrote down earlier.

The CLI kills or drives an emulator only when it runs `Clerk_Verify_Pixel` with the CLI's own claim in that property. Any other emulator on a lane port, such as another AVD or a lane someone booted by hand, counts as taken, shows in the `POOL_FULL` list as `emulator-<port> (<AVD>, not a verify lane)`, and is never killed. The `POOL_FULL` fix names `adb -s emulator-<port> emu kill`. Run it only if that emulator is yours.

Interrupting `up` or `run` while a lane boots is safe. On Ctrl-C or SIGTERM the CLI stops the emulator it started and prints `boot cancelled; stopped emulator <pid> on <serial>`. If the process dies harder than that, the emulator, its claim, and `~/.verify/emulators/android-<slot>.pid` stay behind. The next `up` or `run` in any worktree, or `down --stale` in this one, kills that emulator only when the process still matches the pid file. Plain `down` reports `released nothing` there, because the interrupted `up` never wrote a lease. Emulator console output goes to `~/.verify/emulators/android-<slot>.log`.

## Ledgers

Everything a worktree creates at Clerk is written to a ledger first, so cleanup never depends on a process that survived. Ledgers live at `~/.verify/ledgers/<id>.jsonl`, where `<id>` is a hash of the worktree path, and `<id>.owner` beside it holds the path. Find yours with `grep -l "$(git rev-parse --show-toplevel)" ~/.verify/ledgers/*.owner`.

If a worktree is removed without `down`, the next `up` or `run` in any worktree finishes for it: it kills that worktree's lane emulator, deletes its applications, then closes the ledger.
