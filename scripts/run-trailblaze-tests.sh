#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi

if ! command -v trailblaze >/dev/null 2>&1; then
  echo "Trailblaze CLI was not found on PATH."
  echo "Install it with:"
  echo "  curl -fsSL https://raw.githubusercontent.com/block/trailblaze/main/install.sh | bash"
  exit 127
fi

install_task="${TRAILBLAZE_INSTALL_TASK:-:e2e:installDebug}"
trails_dir="${TRAILBLAZE_TRAILS_DIR:-trails/e2e}"
device="${TRAILBLAZE_DEVICE:-android}"

if [[ "${TRAILBLAZE_WARN_PLACEHOLDER_KEY:-1}" == "1" ]] &&
  [[ -z "${ORG_GRADLE_PROJECT_E2E_CLERK_PUBLISHABLE_KEY:-}" ]] &&
  grep -q "^E2E_CLERK_PUBLISHABLE_KEY=pk_test_placeholder" gradle.properties; then
  echo "Warning: E2E_CLERK_PUBLISHABLE_KEY is still the placeholder value."
  echo "The e2e app may remain on its loading state until a real Clerk test key is set."
fi

if [[ "${TRAILBLAZE_INSTALL_APP:-1}" == "1" ]]; then
  ./gradlew "$install_task"
fi

trail_files=()
if [[ "${TRAILBLAZE_INCLUDE_BLAZE:-0}" == "1" ]]; then
  while IFS= read -r file; do
    trail_files+=("$file")
  done < <(
    find "$trails_dir" \
      -type f \
      \( -name "*.trail.yaml" -o -name "blaze.yaml" \) \
      ! -path "*/config/*" \
      | sort
  )
else
  while IFS= read -r file; do
    trail_files+=("$file")
  done < <(find "$trails_dir" -type f -name "*.trail.yaml" ! -path "*/config/*" | sort)
fi

if [[ "${#trail_files[@]}" -eq 0 ]]; then
  echo "No Trailblaze trail files found under $trails_dir."
  exit 1
fi

cmd=(trailblaze trail "${trail_files[@]}")

if [[ -n "$device" ]]; then
  cmd+=(--device "$device")
fi

if [[ "${TRAILBLAZE_SAVE_RECORDING:-0}" == "1" ]]; then
  cmd+=(--save-recording)
else
  cmd+=(--no-save-recording)
fi

if [[ -n "${TRAILBLAZE_TAGS:-}" ]]; then
  cmd+=(--tags "$TRAILBLAZE_TAGS")
fi

if [[ -n "${TRAILBLAZE_EXTRA_ARGS:-}" ]]; then
  # Intentional shell-style splitting for optional CLI flags.
  extra_args=(${TRAILBLAZE_EXTRA_ARGS})
  cmd+=("${extra_args[@]}")
fi

"${cmd[@]}"
