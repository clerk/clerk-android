## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. This works on any machine: where the machine can run the Android emulator the lane is local, and anywhere else the same commands lease one on a CI runner. The CLI picks and says which. The skill is `.claude/skills/verify-clerk-android/`. Read its `SKILL.md`, then `features/README.md` for the feature you touched.

From the repo root, with Node 24, after `npm ci --prefix .claude/skills/verify-clerk-android`. Add `.claude/skills/verify-clerk-android/bin` to `PATH` to type `control-clerk-android` instead:

1. `.claude/skills/verify-clerk-android/bin/control-clerk-android doctor` names the backend, `local` or `remote`, and checks what that backend needs. Fix what it reports.
2. `.claude/skills/verify-clerk-android/bin/control-clerk-android up` builds the `:e2e` APK and leases a lane emulator. With the remote backend, commit and push first: the session builds the pushed commit. A machine with no `.keys.json` passes the keys in `CLERK_TEST_KEYS_JSON`.
3. `.claude/skills/verify-clerk-android/bin/control-clerk-android run <feature>` runs the golden specs and writes evidence to `.claude/skills/verify-clerk-android/.verify/runs/<run-id>/`.
4. `.claude/skills/verify-clerk-android/bin/control-clerk-android down` releases the emulator and deletes the run's users, and keeps the evidence. With the remote backend run it as soon as you are done, because a remote session is billed by the minute.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
