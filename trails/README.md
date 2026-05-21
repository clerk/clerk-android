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

## Run

```bash
scripts/run-trailblaze-tests.sh
```

By default, the script installs `:e2e:installDebug`, then runs checked-in
`*.trail.yaml` files under `trails/e2e` against `--device android`.

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

## Manual OAuth Trail

The OAuth trail lives under `trails-manual/` so the default local and CI runs stay deterministic.
The checked-in Android replay opens the E2E OAuth screen and triggers Google OAuth. The full
provider completion and profile interaction flow remains in `blaze.yaml`; it requires a Google
account session on the emulator and an LLM-backed Trailblaze run, or manual provider completion.

```bash
TRAILBLAZE_TRAILS_DIR=trails-manual/e2e/oauth-profile \
  TRAILBLAZE_DEVICE=android/emulator-5554 \
  scripts/run-trailblaze-tests.sh
```

## Layout

- `config/trailblaze.yaml` anchors this workspace for Trailblaze discovery.
- `config/packs/clerk-e2e/pack.yaml` defines the e2e Android target.
- `e2e/sign-in-profile-sign-out/blaze.yaml` is the human-readable source trail.
- `e2e/sign-in-profile-sign-out/android-phone.trail.yaml` is the deterministic Android phone replay.
- `../trails-manual/e2e/oauth-profile/` contains the manual Google OAuth profile trail.
- `config/packs/clerk-quickstart/pack.yaml` defines the quickstart Android target.
- `quickstart/auth-start/blaze.yaml` is the human-readable quickstart smoke trail.
- `quickstart/auth-start/android-phone.trail.yaml` is the deterministic quickstart replay.
