## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. The skill is `.claude/skills/verify-clerk-android/`. Read its `SKILL.md`, then `features/README.md` for the feature you touched. This works on any machine: where the machine can run the Android emulator the lane is local, and anywhere else the same commands borrow one on a CI runner. The CLI picks and says which.

From the repo root, with Node 24, after `npm ci --prefix .claude/skills/verify-clerk-android`. Add `.claude/skills/verify-clerk-android/bin` to `PATH` to type `control-clerk-android` instead:

1. `.claude/skills/verify-clerk-android/bin/control-clerk-android doctor` checks what the machine needs and prints the fix for each thing that is missing.
2. `.claude/skills/verify-clerk-android/bin/control-clerk-android up` builds the `:e2e` APK, leases a lane emulator, and creates one Clerk application for this worktree, whose development instance is the test instance every spec runs on. Creating it needs the team's Platform API key. `doctor` says whether it found one and how to supply it. With a runner, commit and push first, because the session builds the pushed commit.
3. `.claude/skills/verify-clerk-android/bin/control-clerk-android run <feature>` runs the golden specs and writes evidence to `.claude/skills/verify-clerk-android/.verify/runs/<run-id>/`.
4. `.claude/skills/verify-clerk-android/bin/control-clerk-android down` releases the emulator, deletes the application with every test user in it, and keeps the evidence. With a runner, run it as soon as you are done, because the session is billed by the minute.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
