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
trailblaze_attempts="${TRAILBLAZE_ATTEMPTS:-1}"
if ! [[ "$trailblaze_attempts" =~ ^[0-9]+$ ]] || [[ "$trailblaze_attempts" -lt 1 ]]; then
  trailblaze_attempts=1
fi

adb_serial_from_device() {
  if [[ "$device" == android/* ]]; then
    printf "%s" "${device#android/}"
  fi
}

wait_for_android_device() {
  local serial="$1"

  if [[ -z "$serial" ]] || ! command -v adb >/dev/null 2>&1; then
    return
  fi

  echo "Preparing Android device $serial for Trailblaze."
  adb start-server >/dev/null
  adb -s "$serial" wait-for-device

  local boot_completed=""
  for _ in {1..60}; do
    boot_completed="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$boot_completed" == "1" ]]; then
      break
    fi
    sleep 2
  done

  if [[ "$boot_completed" != "1" ]]; then
    echo "Android device $serial did not report boot completion before the preflight timeout."
    return 1
  fi

  adb -s "$serial" shell input keyevent 82 >/dev/null 2>&1 || true
  reset_adb_forward_state "$serial"
  verify_adb_forward "$serial"
}

reset_adb_forward_state() {
  local serial="$1"

  adb -s "$serial" forward --remove-all >/dev/null 2>&1 || true
  adb -s "$serial" reverse --remove-all >/dev/null 2>&1 || true
}

verify_adb_forward() {
  local serial="$1"
  local port="${TRAILBLAZE_ADB_PREFLIGHT_PORT:-56165}"

  if adb -s "$serial" forward "tcp:$port" "tcp:$port" >/dev/null 2>&1; then
    adb -s "$serial" forward --remove "tcp:$port" >/dev/null 2>&1 || true
    return
  fi

  echo "ADB port forward preflight failed on $serial; restarting adb server and retrying."
  adb kill-server >/dev/null 2>&1 || true
  adb start-server >/dev/null
  adb -s "$serial" wait-for-device
  adb -s "$serial" forward "tcp:$port" "tcp:$port"
  adb -s "$serial" forward --remove "tcp:$port" >/dev/null 2>&1 || true
}

if [[ "${TRAILBLAZE_WARN_PLACEHOLDER_KEY:-1}" == "1" ]] &&
  [[ -z "${ORG_GRADLE_PROJECT_E2E_CLERK_PUBLISHABLE_KEY:-}" ]] &&
  grep -q "^E2E_CLERK_PUBLISHABLE_KEY=pk_test_placeholder" gradle.properties; then
  echo "Warning: E2E_CLERK_PUBLISHABLE_KEY is still the placeholder value."
  echo "The e2e app may remain on its loading state until a real Clerk test key is set."
fi

if [[ "${TRAILBLAZE_INSTALL_APP:-1}" == "1" ]]; then
  ./gradlew "$install_task"
fi

adb_serial="$(adb_serial_from_device)"
wait_for_android_device "$adb_serial"

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

log_dir="${TRAILBLAZE_RUN_LOG_DIR:-build/trailblaze-artifacts}"
mkdir -p "$log_dir"

attempt=1
while true; do
  log_file="$log_dir/trailblaze-attempt-$attempt.log"
  set +e
  "${cmd[@]}" 2>&1 | tee "$log_file"
  status=${PIPESTATUS[0]}
  set -e

  if [[ "$status" -eq 0 ]]; then
    exit 0
  fi

  if [[ "$attempt" -lt "$trailblaze_attempts" ]] &&
    grep -q "Failed to start port forwarding" "$log_file"; then
    echo "Trailblaze failed while starting ADB port forwarding; resetting ADB and retrying."
    if [[ -n "$adb_serial" ]]; then
      adb kill-server >/dev/null 2>&1 || true
      wait_for_android_device "$adb_serial"
    fi
    attempt=$((attempt + 1))
    continue
  fi

  exit "$status"
done
