# Live integration test migration

The previous integration workflow invoked
`:source:api:testDebugUnitTest --tests "com.clerk.api.integration.*"` after the
API source sets had moved to `NativeCoreTests`. Reproducing that command failed
with **No tests found for given includes**. The old Robolectric files did not
exercise the packaged QuickJS SDK and still called deleted singleton/domain APIs.

All three old integration files were read in full and checked against their
hashed baseline inventory (`1ea9f97250e9e3b266b7fbcfe37d9373e4fc393f`). Their two
live scenarios and helper are now migrated to
`NativeCoreTests/Android/integration/`. The [test audit](test-audit.md) tracks
the separate assertion reviews and current disposition of the remaining files.

| Old assertion or responsibility | Migrated behavior |
| --- | --- |
| Initialize the singleton and await readiness | Await a new connected owner executing the packaged QuickJS core, with real HTTP and memory-only credentials. Each test closes its own owner. |
| Sign up with email, fictional test phone, password and names | Supply the same fields through generated `SignUpCreateParams`. |
| Send and verify email code | Await `signUp.verifications.sendEmailCode` and `verifyEmailCode`; read the same generated resource afterward. |
| Verify phone when it remains unverified | Preserve the conditional phone-code verification through `signUp.verifications`. |
| Old implicit session adoption | Require complete sign-up, verify that session/user are absent before `finalize`, then verify the created user/session IDs after it. Explicit low-level finalization is the new-major contract. |
| Sign out and perform email-code sign-in | Use generated `signOut`, `signIn.emailCode.sendCode` and `verifyCode`. Require complete state and explicit `finalize`, then match the newly adopted session and original user. |
| Best-effort account deletion | Require deletion on the passing path. On failure, attempt bounded cleanup with password/finalize when needed and verify the owned test email before deleting; preserve the original failure. |
| Initialized state and nonblank application name | Check loaded/uninvalidated state, reload the generated environment, and retain the nonblank `displayConfig.applicationName` assertion. Also require development/staging environment. |
| Host-side `.keys.json` lookup | Gradle supplies the development key as an instrumentation argument from `CLERK_INTEGRATION_TEST_PK` or `.keys.json` under `with-email-codes.pk`. No raw key file is copied into the test APK. |

## Configuration and execution

The instance must permit the existing synthetic email/phone test identifiers,
password sign-up, email codes and the optional required phone verification. These
are live mutation tests: a configured run creates and deletes its own synthetic
account. No live key was configured for the verification recorded below.

```sh
# Configure .keys.json or CLERK_INTEGRATION_TEST_PK without printing its contents.
bash scripts/run-integration-tests.sh
```

The workflow first compiles the packaged tests. When its development-key secret
is absent it explicitly reports **live integration not run**. With a key, it runs
the package on an Android emulator, enables the required-configuration guard and
checks the resulting XML. Both named scenarios must appear exactly once without
skip/failure/error outcomes. Merely receiving Gradle exit code zero is insufficient.
The existing GET-only `LiveStartupTest` remains a separate opt-in smoke test.

The local script and configured workflow now use the same entrypoint. It resolves
the repository from its own location, runs the packaged tests once in required
mode, and checks their XML. Automatic retries of live account mutations were
removed. A September 10 run from outside the repository, with no key configured,
discovered both scenarios and exited with failure; each XML case reported
`Required live integration publishable key is missing` before connection.

## Verified evidence

The [discovery and guard report](evidence/live-integration-discovery.json) records
local checks on the Android 16 arm64 emulator:

- The migrated instrumentation APK compiles against the generated public API.
- With the key omitted, both scenarios are discovered and return JUnit assumption
  outcomes before connection. This toolchain writes those assumptions as XML
  failure elements despite a successful Gradle exit; the strict result checker
  rejects them as a live pass.
- Required execution without a key fails both scenarios with the expected
  missing-configuration message and a failing Gradle exit.
- A deliberately fake `pk_live_` prefix fails both scenarios before connection,
  with the expected development-instance requirement and a failing Gradle exit.
- An initial probe passing an explicitly empty instrumentation argument produced
  `Invalid userId -2`, no test cases and a successful Gradle exit. Omitting the
  absent argument restored discovery. The strict XML check rejects the empty
  result. This is a retained runner diagnostic, not a claimed SDK auth defect.
- Workflow lint passes with the existing Blacksmith runner label configured.
  The XML validator accepts a synthetic complete/pass report and rejects empty,
incomplete, duplicate, skipped, failed and errored reports. Those parser checks
  are not live-service execution.

Raw Gradle logs and XML are retained in `work/android-live-integration-gates/`;
the old-command reproduction and compile logs are adjacent. No real account,
HTTP authentication, OS prompt or physical phone was used in these checks.
An actual configured run is still required to verify the live flow. These tests
also do not prove secure-storage continuity or an old-major app upgrade: they
intentionally use fresh memory-only credentials. Failed cleanup of a completed
account may still require instance-side cleanup; it is never converted into a
passing authentication result.
