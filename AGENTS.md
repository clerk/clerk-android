## Comments

Don't write comments unless they explain a constraint from a platform, library,
or spec that we can't change. If our own code needs explaining, change the code:
rename it, give it a type, extract a helper, or pin it with a test.

Keep license headers, doc comments on public API, and `// region` markers.
Don't leave commented-out code.

## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on a real emulator before calling it done. The skill is `.claude/skills/verify-clerk-android/`. The tests and the CLI that runs them are in `e2e-tests/`. Read the skill's `SKILL.md`, then the page for the feature you touched, which `.claude/skills/verify-clerk-android/features/README.md` lists. This works on any machine: where the machine can run the Android emulator it runs there, and anywhere else the same commands borrow one on a CI runner. The skill needs a Clerk Platform API key, which Clerk team members have. Without one, say in the pull request that the change was not run on a device.

The loop is `doctor`, `up`, `run`, `screen`, `down`, and `attach`, each a verb of `e2e-tests/bin/control-clerk-android`.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
