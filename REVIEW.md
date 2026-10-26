# Review guide

This file tells reviewers, human or AI, what matters in this repository. Repository rules for strings, comments, and device evidence are in `AGENTS.md`. Apply them, but don't restate them here.

## What counts as a failure

SDK paths below are under `source/api/src/main/kotlin/com/clerk/api/`.

- A user is signed out, sees another user's session, or keeps a token after sign-out. Look hardest at `storage/`, `session/`, `state/ClientStateStore.kt`, `configuration/CachedClerkState.kt`, and `sharedsession/`.
- An auth flow accepts a callback or result it should reject, in `hostedauth/`, `sso/`, or `passkeys/`.
- A token, session ID, nonce, or full redirect URI reaches a log. Check `network/middleware/outgoing/RequestLoggingMiddleware.kt` and every `ClerkLogger` call the diff adds.
- An older network response overwrites newer client state.
- A release breaks: `.github/workflows/manual-release.yml`, the `mavenPublishing` block in `source/*/build.gradle.kts`, `source/*/consumer-rules.pro`, or the versions in `gradle.properties`.
- A workflow gets broader permissions or exposes a secret to code from a fork.

## Always check

- Shared state that is read and then written without holding its lock, or a cache write that can land after sign-out or a reload.
- A paginated or retried request whose late response can replace a list that was already reloaded.
- A `ClerkResult` failure that keeps the error type but drops the original error or its details.
- Cleanup that must finish even if the caller is cancelled, such as local state after a delete, runs in a scope that survives the cancellation.
- `LaunchedEffect` and `remember` keys include every value the block reads. Saved state is keyed by the right identity.
- Agent docs and skill pages, such as `.claude/skills/verify-clerk-android/`, still match the code and commands they describe.
- In `e2e-tests/`, every wait has a timeout, and every child process and lock file is cleaned up on failure.
- The pull request has the device evidence that `AGENTS.md` asks for, or says why there is none.

## Don't flag

- Formatting, import order, or wildcard imports. Spotless and detekt run in CI.
- A `ClerkResult` function that throws, or a swallowed `CancellationException`. The detekt rules `ClerkResultThrows` and `SwallowedCancellation` catch those.
- Binary compatibility or a missing deprecation cycle. `apiCheck` makes every public API change show in the `.api` dump, and the team decides release policy.
- Cancellation handling in a scope that is cancelled only at teardown, such as `viewModelScope`.
- Behavior that matches clerk-js. Check `clerk/javascript` before you call a difference a bug.
- Naming, wording, and test layout, unless a name is wrong about what the code does.

## Skip these paths

These are generated or mechanical. `.coderabbit.yaml` excludes them too.

- `source/*/api/*.api`, written by `./gradlew apiDump`.
- `**/detekt-baseline.xml` and `source/ui/lint-baseline.xml`.
- Snapshot images under `source/ui/src/test/**/snapshots/images/`.
- `**/package-lock.json`.

## How to report

- Tie every finding to a file and line in the diff. If you can't point at a line, don't post the finding. Never guess a file name.
- Label each finding with one severity:
  - **Important.** It breaks behavior, security, user data, or a release. It blocks the merge.
  - **Nit.** It is worth fixing but doesn't block. Post at most three per review.
  - **Pre-existing.** It is a real bug in code this pull request didn't change. Say so, so the author isn't blamed for it.
- Start the top-level summary with the counts, for example "1 Important, 2 Nit, 0 Pre-existing."
- If you find nothing, write "No issues found." and nothing else.
