## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. The skill is `.claude/skills/verify-clerk-android/`. Read its `SKILL.md`, then `features/README.md` for the feature you touched. It needs a machine that can run the Android emulator.

From the repo root, with Node 24, after `npm ci --prefix .claude/skills/verify-clerk-android`. Add `.claude/skills/verify-clerk-android/bin` to `PATH` to type `control-clerk-android` instead:

1. `.claude/skills/verify-clerk-android/bin/control-clerk-android doctor` checks what the machine needs and prints the fix for each thing that is missing.
2. `.claude/skills/verify-clerk-android/bin/control-clerk-android up` builds the `:e2e` APK, boots a lane emulator, and creates one Clerk application for this worktree, whose development instance is the test instance every spec runs on. Creating it needs the team's Platform API key. `doctor` says whether it found one and how to supply it.
3. `.claude/skills/verify-clerk-android/bin/control-clerk-android run <feature>` runs the golden specs and writes evidence to `.claude/skills/verify-clerk-android/.verify/runs/<run-id>/`.
4. `.claude/skills/verify-clerk-android/bin/control-clerk-android down` stops the emulator, deletes the application with every test user in it, and keeps the evidence.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
