# Trailblaze Tests

This directory contains Trailblaze UI tests for runnable sample apps.

Trailblaze is currently wired through its CLI and checked-in trail files rather than a Gradle
instrumentation dependency. The Android test artifact is not published for normal consumer
use yet, while the CLI supports project-local trails and deterministic replay.

## Prerequisites

- Install the Trailblaze CLI:

  ```bash
  curl -fsSL https://raw.githubusercontent.com/block/trailblaze/main/install.sh | bash
  ```

- Start one Android emulator or connect one Android device.
- Configure `QUICKSTART_CLERK_PUBLISHABLE_KEY` in `gradle.properties`, or provide it as
  `ORG_GRADLE_PROJECT_QUICKSTART_CLERK_PUBLISHABLE_KEY`.

Recorded trails can run without a Trailblaze LLM provider. Natural-language authoring,
`verify` prompts without recordings, and self-heal require provider configuration; see
the Trailblaze docs for supported provider environment variables and `trailblaze config llm`.

## Run

```bash
scripts/run-trailblaze-tests.sh
```

The script installs `:samples:quickstart:installDebug`, then runs checked-in
`*.trail.yaml` files under `trails/` against `--device android`.

Useful overrides:

```bash
TRAILBLAZE_DEVICE=android/emulator-5554 scripts/run-trailblaze-tests.sh
TRAILBLAZE_TAGS=smoke scripts/run-trailblaze-tests.sh
TRAILBLAZE_INSTALL_APP=0 scripts/run-trailblaze-tests.sh
TRAILBLAZE_SAVE_RECORDING=1 scripts/run-trailblaze-tests.sh
TRAILBLAZE_INCLUDE_BLAZE=1 scripts/run-trailblaze-tests.sh
TRAILBLAZE_EXTRA_ARGS="--self-heal" scripts/run-trailblaze-tests.sh
```

## Layout

- `config/trailblaze.yaml` anchors this workspace for Trailblaze discovery.
- `config/packs/clerk-quickstart/pack.yaml` defines the quickstart Android target.
- `quickstart/auth-start/blaze.yaml` is the human-readable source trail.
- `quickstart/auth-start/android-phone.trail.yaml` is the deterministic Android phone replay.
