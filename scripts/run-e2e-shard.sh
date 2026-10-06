#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/run-e2e-shard.sh <shard>

Shards mirror the clerk-ios E2EHost CI matrix:
  custom-flow              Custom OTP and prebuilt sign-in on the default instance
  auth-email               Email code sign-up                (auth-email-code-password)
  auth-phone               Phone code sign-up                (auth-phone-code)
  user-profile             User Profile Security deletion    (auth-email-code-password)
  session-task-setup-mfa   Setup MFA session task with TOTP  (session-task-setup-mfa)
  all                      Every shard in sequence
USAGE
}

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

shard="${1:-}"
if [[ -z "$shard" ]]; then
  usage
  exit 64
fi

SIGN_UP_PHONE_NUMBER="5555550160"
E2E_APP_ID="com.clerk.e2e"
CLEANUP_ACTION="com.clerk.e2e.action.CLEANUP_ACCOUNT"

shard_key_name() {
  case "$1" in
    custom-flow) echo "" ;;
    auth-email | user-profile) echo "auth-email-code-password" ;;
    auth-phone) echo "auth-phone-code" ;;
    session-task-setup-mfa) echo "session-task-setup-mfa" ;;
    *) return 1 ;;
  esac
}

shard_trail_dir() {
  case "$1" in
    custom-flow) echo "trails/e2e/sign-in-profile-sign-out" ;;
    auth-email) echo "trails/e2e/auth-email-code-sign-up" ;;
    auth-phone) echo "trails/e2e/auth-phone-code-sign-up" ;;
    user-profile) echo "trails/e2e/user-profile-security-delete-account" ;;
    session-task-setup-mfa) echo "trails/e2e/session-task-setup-mfa" ;;
    *) return 1 ;;
  esac
}

adb_serial() {
  local device="${TRAILBLAZE_DEVICE:-android}"
  if [[ "$device" == android/* ]]; then
    printf "%s" "${device#android/}"
  fi
}

adb_for_device() {
  local serial
  serial="$(adb_serial)"
  if [[ -n "$serial" ]]; then
    adb -s "$serial" "$@"
  else
    adb "$@"
  fi
}

cleanup_test_account() {
  if ! command -v adb >/dev/null 2>&1; then
    return
  fi

  echo "Deleting any test account left signed in on the e2e app."
  local output
  output="$(adb_for_device shell am broadcast -n "$E2E_APP_ID/.E2ECleanupReceiver" -a "$CLEANUP_ACTION" 2>&1 || true)"
  echo "$output"
  if [[ "$output" != *"result=-1"* ]]; then
    echo "::warning::E2E account cleanup did not report success. A test user may remain on the instance."
  fi
}

run_shard() {
  local shard="$1"
  local key_name trail_dir started_at status
  key_name="$(shard_key_name "$shard")" || {
    echo "Unknown shard: $shard" >&2
    usage >&2
    return 64
  }
  trail_dir="$(shard_trail_dir "$shard")"
  started_at="$(date +%s)"
  echo "E2E shard $shard: key=${key_name:-default} trails=$trail_dir"

  if [[ -n "$key_name" ]]; then
    if [[ "${E2E_SKIP_PREFLIGHT:-0}" != "1" ]]; then
      python3 scripts/e2e/e2e_instances.py validate "$key_name"
    fi
    if [[ "$shard" == "auth-phone" ]]; then
      python3 scripts/e2e/e2e_instances.py delete-phone-users "$key_name" "$SIGN_UP_PHONE_NUMBER"
    fi
  fi

  set +e
  ORG_GRADLE_PROJECT_E2E_CLERK_KEY_NAME="$key_name" \
    TRAILBLAZE_TRAILS_DIR="$trail_dir" \
    TRAILBLAZE_RUN_LOG_DIR="build/trailblaze-artifacts/$shard" \
    scripts/run-trailblaze-tests.sh
  status=$?
  set -e

  if [[ -n "$key_name" ]]; then
    cleanup_test_account
  fi

  local duration=$(($(date +%s) - started_at))
  echo "E2E timing: shard $shard finished with status $status in ${duration}s"
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    local result="passed"
    [[ "$status" -eq 0 ]] || result="failed"
    {
      echo "| Shard | Instance | Result | Duration |"
      echo "| --- | --- | --- | ---: |"
      echo "| \`$shard\` | \`${key_name:-default}\` | $result | ${duration}s |"
    } >> "$GITHUB_STEP_SUMMARY"
  fi
  return "$status"
}

if [[ "$shard" == "all" ]]; then
  failures=()
  for each in custom-flow auth-email auth-phone user-profile session-task-setup-mfa; do
    run_shard "$each" || failures+=("$each")
  done
  if [[ "${#failures[@]}" -gt 0 ]]; then
    echo "E2E shards failed: ${failures[*]}" >&2
    exit 1
  fi
  exit 0
fi

run_shard "$shard"
