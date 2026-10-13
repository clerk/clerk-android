## Strings

User-facing text is in `source/ui/src/main/res/values/strings.xml`. Each language has its translations in a `values-<qualifier>/strings.xml` next to it (such as `values-de` or `values-zh-rCN`), under the same names.

When you change English text, update the string of the same name in every language to say what the new text says. When you add a string, add it to every language. A string that a language lacks shows in English.

A string marked `translatable="false"` stays in `values/strings.xml` alone, whether you add it or change it.

## Comments

Don't write comments unless they explain a constraint from a platform, library,
or spec that we can't change. If our own code needs explaining, change the code:
rename it, give it a type, extract a helper, or pin it with a test.

Keep license headers, doc comments on public API, and `// region` markers.
Don't leave commented-out code.

## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on an emulator before calling it done, and put the screenshots or video on the pull request. How you prove it is up to you. The verification skill in this repository is one way. An app of your own that exercises the change is just as good. If you could not run it on a device at all, say so in the pull request.

The skill is `.claude/skills/verify-clerk-android/`. The tests and the CLI that runs them are in `e2e-tests/`. Read the skill's `SKILL.md`, then `.claude/skills/verify-clerk-android/features/README.md` for the feature you touched. The skill needs a Clerk Platform API key, which Clerk team members have.

The loop is `doctor`, `up`, `run`, `down`, and `attach`, each a verb of `e2e-tests/bin/control-clerk-android`.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.
