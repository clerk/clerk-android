## Strings

User-facing text is in `source/ui/src/main/res/values/strings.xml`. Each language has its translations in a `values-<qualifier>/strings.xml` next to it (such as `values-de` or `values-zh-rCN`), under the same names.

When you add a string, add it to every language. A string that a language lacks shows in English.

A string marked `translatable="false"` stays in `values/strings.xml` alone, whether you add it or change it.

## Comments

Don't write comments unless they explain a constraint from a platform, library,
or spec that we can't change. If our own code needs explaining, change the code:
rename it, give it a type, extract a helper, or pin it with a test.

Keep license headers, doc comments on public API, and `// region` markers.
Don't leave commented-out code.

## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on an emulator before calling it done, and put the screenshots or video in the pull request's description, never in a comment. How you prove it is up to you. The verification skill in this repository is one way, and it also works on a machine with no device. An app of your own that exercises the change is just as good. If you could not run it on a device at all, say so in the pull request.

Show a before and an after of every screen the change touches, and of every state of those screens, including a state that should look the same after the change. Lay them out in a table, with each before beside its after, as `attach` does. When a test user cannot reach a state with the standard instance settings and default roles, and no settings or test data can, force it with a small local patch on both builds. Never commit the patch, and say in the pull request what it changed and why. When you review a pull request that changes SDK or UI code, check that its evidence covers every screen and state the diff touches.

The skill is `.claude/skills/verify-clerk-android/`. The tests and the CLI that runs them are in `e2e-tests/`. Read the skill's `SKILL.md`, then the page for the feature you touched, which `.claude/skills/verify-clerk-android/features/README.md` lists. This works on any machine: where the machine can run the Android emulator it runs there, and anywhere else the same commands borrow one on a CI runner. The skill needs a Clerk Platform API key, which Clerk team members have.

The loop is `doctor`, `up`, `run`, `screen`, `attach`, and `down`, each a verb of `e2e-tests/bin/control-clerk-android`.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.

## Reviewing

Before you review a pull request, read `REVIEW.md`. It says what counts as a failure here, what not to flag, and how to report findings.
