#!/usr/bin/env bash
set -euo pipefail

KEYS_FILE=".keys.json"
MAX_ATTEMPTS=3
BACKOFF_SECONDS=5

if [ ! -f "$KEYS_FILE" ]; then
  echo "Warning: $KEYS_FILE not found at project root."
  echo "Integration tests will be skipped (Assume.assumeNotNull)."
  echo "To run integration tests, create $KEYS_FILE with your Clerk test instance key."
fi

attempt=1
while [ $attempt -le $MAX_ATTEMPTS ]; do
  echo "=== Attempt $attempt of $MAX_ATTEMPTS ==="

  if ./gradlew :source:api:testDebugUnitTest \
    --tests "com.clerk.api.integration.*" \
    --no-daemon; then
    echo "Integration tests passed."
    exit 0
  fi

  if [ $attempt -lt $MAX_ATTEMPTS ]; then
    sleep_time=$((BACKOFF_SECONDS * attempt))
    echo "Tests failed. Retrying in ${sleep_time}s..."
    sleep $sleep_time
  fi

  attempt=$((attempt + 1))
done

echo "Integration tests failed after $MAX_ATTEMPTS attempts."
exit 1
