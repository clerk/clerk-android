## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. The skill is `.cursor/skills/verify-clerk-android/`. Read its `SKILL.md`, then `features/README.md` for the feature you touched.

From `.cursor/skills/verify-clerk-android/`, after `npm ci`:

1. `bin/verify doctor` checks the JDK, the emulator, the keys, and the build.
2. `bin/verify up` builds the `:e2e` APK and leases a lane emulator.
3. `bin/verify run <feature>` runs the golden specs and writes evidence to `.verify/runs/<run-id>/`.
4. `bin/verify down` releases the emulator and deletes the run's users, and keeps the evidence.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
