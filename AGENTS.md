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

## Pull requests

Follow `.github/pull_request_template.md` in every pull request description.

## Verifying changes

Prove every change to `source/api`, `source/ui`, or the `:e2e` host on an emulator before calling it done, and put the screenshots or video in the pull request's description, never in a comment. How you prove it is up to you. The verification skill in this repository is one way, and it also works on a machine with no device. An app of your own that exercises the change is just as good. If you could not run it on a device at all, say so in the pull request.

Show a before and an after of every screen the change touches, and of every state whose look the change alters, plus one state that should look the same after the change, when there is one. One such pair is enough, however many states should look the same. A state comes from a branch in the code that decides whether the changed lines run, what they show, or what the screen shows because of them, such as signed in or out, loading, empty, an error, or which layout a component picks. The `Device evidence` check in `.coderabbit.yaml` lists the kinds of branch that count. Parts of the screen that no such branch reaches, such as which other form fields show, a toggle's position, or text the user typed, are not states. A branch inside content that the changed code wraps or modifies, such as a closure passed to it or the view a changed modifier is applied to, isn't a state just because the changed code wraps or calls that content. It counts only when one of the other kinds of branch applies to it, such as when it encloses changed lines. Each required state needs its own pair, not each combination of states, unless one of the branches that count tests that combination. A language is not a separate state, so show the screens in English. When the change adds or changes translations, also show them on one screen, ideally the one where the text has the least room, or say in the pull request why you didn't. A screen that doesn't exist on the base branch has no before, so it needs only an after of each of its states. Lay out each pair in a table, with the before beside its after, as `attach` does. When a test user cannot reach a state with the standard instance settings and default roles, and no settings or test data can, force it with a small local patch on both builds, or on the after build for a screen with no before. Never commit the patch, and say in the pull request what it changed and why. A state that no real user can reach in the shipped app, such as a branch that only an internal task cancellation reaches or a value Clerk's backend never sends, needs no pair, even when a patch could force it. Name the branch's file and line in the pull request and say why no user can reach it. The same goes for a state that can't be produced on a simulator or on a physical device, even with a patch, such as one that needs hardware no test device has: name the branch's file and line, and a reason that rules out all three. A simulator lacking something a device has, or a sign-in a test can't complete when a patch could stand in for it, isn't such a reason. When you review a pull request that changes SDK or UI code, check that its evidence covers every screen the diff touches and every state this section requires.

The skill is `.claude/skills/verify-clerk-android/`. The tests and the CLI that runs them are in `e2e-tests/`. Read the skill's `SKILL.md`, then the page for the feature you touched, which `.claude/skills/verify-clerk-android/features/README.md` lists. This works on any machine: where the machine can run the Android emulator it runs there, and anywhere else the same commands borrow one on a CI runner. The skill needs a Clerk Platform API key, which Clerk team members have.

The loop is `doctor`, `up`, `run`, `screen`, `attach`, and `down`, each a verb of `e2e-tests/bin/control-clerk-android`.

Use only test users: `+clerk_test` emails, phones 555-0100 to 555-0199, and the code `424242`. Never type real credentials.

## Reviewing

Before you review a pull request, read `REVIEW.md`. It says what counts as a failure here, what not to flag, and how to report findings.
