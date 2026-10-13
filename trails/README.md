# Trailblaze Tests

This directory contains Trailblaze UI tests for Android sample apps and the dedicated `:e2e`
module.

Trailblaze is currently wired through its CLI and checked-in trail files rather than a Gradle
instrumentation dependency. The Android test artifact is not published for normal consumer
use yet, while the CLI supports project-local trails and deterministic replay.

## Prerequisites

- Install the Trailblaze CLI:

  ```bash
  curl -fsSL https://raw.githubusercontent.com/block/trailblaze/main/install.sh | bash
  ```

- Start one Android emulator or connect one Android device.
- Configure `E2E_CLERK_PUBLISHABLE_KEY` in `gradle.properties`, or provide it as
  `ORG_GRADLE_PROJECT_E2E_CLERK_PUBLISHABLE_KEY`.

Recorded trails can run without a Trailblaze LLM provider. Natural-language authoring,
`verify` prompts without recordings, and self-heal require provider configuration; see
the Trailblaze docs for supported provider environment variables and `trailblaze config llm`.

## E2E shards

The `:e2e` trails mirror the clerk-ios E2EHost suite. Each shard builds the e2e app against one
named Clerk test instance from `.keys.json` at the repository root. The file uses the same format
and instance names as clerk-ios, so CI reads the same `CLERK_TEST_KEYS_JSON` secret.

| Shard | Instance | Trail |
| --- | --- | --- |
| `custom-flow` | `E2E_CLERK_PUBLISHABLE_KEY` | `e2e/sign-in-profile-sign-out` |
| `auth-email` | `auth-email-code-password` | `e2e/auth-email-code-sign-up` |
| `auth-phone` | `auth-phone-code` | `e2e/auth-phone-code-sign-up` |
| `user-profile` | `auth-email-code-password` | `e2e/user-profile-security-delete-account` |
| `session-task-setup-mfa` | `session-task-setup-mfa` | `e2e/session-task-setup-mfa` |

```bash
TRAILBLAZE_DEVICE=android/emulator-5554 scripts/run-e2e-shard.sh auth-email
TRAILBLAZE_DEVICE=android/emulator-5554 scripts/run-e2e-shard.sh all
```

For each named shard the runner:

1. Runs `scripts/e2e/e2e_instances.py validate`, which fails fast when the instance does not
   support the trail. Recorded trails replay a fixed list of screens, so the preflight also
   pins the exact sign-up fields each trail expects. Set `E2E_SKIP_PREFLIGHT=1` to skip it.
2. For `auth-phone`, deletes existing users holding the fixture phone number through the
   Backend API. This needs the instance's `sk` in `.keys.json`.
3. Builds and installs the app with `-PE2E_CLERK_KEY_NAME=<instance>` and runs the trail.
4. Broadcasts `com.clerk.e2e.action.CLEANUP_ACCOUNT` so the app deletes any test user left
   signed in, even when the trail failed.

Pass `TRAILBLAZE_DEVICE=android/<serial>` whenever more than one device is connected. Gradle
otherwise installs the app on every device.

`scripts/e2e/check_e2e_contracts.py` runs on pull requests. It checks that trail selectors exist
in `E2ETags.kt`, phone numbers stay in the reserved 555-555-0100 to 0199 range, every trail
directory belongs to a shard, and each recorded trail keeps the prompts of its `blaze.yaml`.

### E2E host app

The `:e2e` app exposes stable `e2e.*` test tags as resource ids, matching the iOS E2EHost
identifiers. A status panel shows signed-in, session status, pending task, and cleanup markers
above the prebuilt `AuthView`. The sign-up routes prefill a generated `+clerk_test` email or the
fixture phone number, so recorded trails never type a value that must be unique per run. For
the setup MFA task, the trail copies the TOTP secret with the prebuilt copy button, and the
panel's "Type TOTP code" button types the current code into the focused field.

## Run

```bash
scripts/run-trailblaze-tests.sh
```

By default, the script installs `:e2e:installDebug`, then runs the checked-in
`*.trail.yaml` files under `trails/e2e/sign-in-profile-sign-out` against `--device android`.
Use `scripts/run-e2e-shard.sh` for the other e2e trails, since they need their own instances.

Useful overrides:

```bash
TRAILBLAZE_DEVICE=android/emulator-5554 scripts/run-trailblaze-tests.sh
TRAILBLAZE_TAGS=smoke scripts/run-trailblaze-tests.sh
TRAILBLAZE_TRAILS_DIR=trails/quickstart \
  TRAILBLAZE_INSTALL_TASK=:samples:quickstart:installDebug \
  scripts/run-trailblaze-tests.sh
TRAILBLAZE_INSTALL_APP=0 scripts/run-trailblaze-tests.sh
TRAILBLAZE_SAVE_RECORDING=1 scripts/run-trailblaze-tests.sh
TRAILBLAZE_INCLUDE_BLAZE=1 scripts/run-trailblaze-tests.sh
TRAILBLAZE_EXTRA_ARGS="--self-heal" scripts/run-trailblaze-tests.sh
```

## Layout

- `config/trailblaze.yaml` anchors this workspace for Trailblaze discovery.
- `config/packs/clerk-e2e/pack.yaml` defines the e2e Android target.
- Each `e2e/<scenario>/blaze.yaml` is the human-readable source trail.
- Each `e2e/<scenario>/android-phone.trail.yaml` is the deterministic Android phone replay.
- `config/packs/clerk-quickstart/pack.yaml` defines the quickstart Android target.
- `quickstart/auth-start/blaze.yaml` is the human-readable quickstart smoke trail.
- `quickstart/auth-start/android-phone.trail.yaml` is the deterministic quickstart replay.
