## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. This works on any machine: where the machine can run the Android emulator the lane is local, and anywhere else the same commands lease one on a CI runner. The CLI picks and says which. The skill is `.claude/skills/verify-clerk-android/`. Read its `SKILL.md`, then `features/README.md` for the feature you touched.

From the repo root, with Node 24, after `npm ci --prefix .claude/skills/verify-clerk-android`. Add `.claude/skills/verify-clerk-android/bin` to `PATH` to type `control-clerk-android` instead:

1. `.claude/skills/verify-clerk-android/bin/control-clerk-android doctor` names the backend, `local` or `remote`, and checks what that backend needs. Fix what it reports.
2. `.claude/skills/verify-clerk-android/bin/control-clerk-android up` builds the `:e2e` APK and leases a lane emulator. With the remote backend, commit and push first, because the session builds the pushed commit. `up` also creates one Clerk application for this worktree, whose development instance is the test instance every spec runs on. Creating the application needs the team's Platform API key. A cloud environment attaches it as an API credential. On a Mac the CLI reads it from 1Password and asks you to approve, once you have set the key's 1Password reference from the team's private setup note. `doctor` says which it found, and the skill's `SKILL.md` under Test instances has the rest.
3. `.claude/skills/verify-clerk-android/bin/control-clerk-android run <feature>` puts the instance on the settings each spec file declares, runs the golden specs, and writes evidence to `.claude/skills/verify-clerk-android/.verify/runs/<run-id>/`.
4. `.claude/skills/verify-clerk-android/bin/control-clerk-android down` releases the emulator and deletes the application with every test user in it, and keeps the evidence. With the remote backend run it as soon as you are done, because a remote session is billed by the minute.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
