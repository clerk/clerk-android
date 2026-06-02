#!/usr/bin/env bash
set -euo pipefail

: "${CLERK_ANDROID_VERSION:?CLERK_ANDROID_VERSION is required}"
: "${CLERK_ANDROID_RELEASE_URL:?CLERK_ANDROID_RELEASE_URL is required}"
: "${JAVASCRIPT_DIR:?JAVASCRIPT_DIR is required}"
: "${GH_TOKEN:?GH_TOKEN is required}"

MAVEN_BASE_URL="https://repo.maven.apache.org/maven2/com/clerk"
TARGET_REPO="clerk/javascript"
BUILD_FILE="packages/expo/android/build.gradle"
BRANCH="sam/expo-android-${CLERK_ANDROID_VERSION}"
PR_TITLE="fix(expo): Bump clerk-android to ${CLERK_ANDROID_VERSION}"

version_slug="${CLERK_ANDROID_VERSION//./-}"
CHANGESET_FILE=".changeset/expo-bump-clerk-android-${version_slug}.md"

wait_for_maven_artifacts() {
  local version="$1"
  local urls=(
    "${MAVEN_BASE_URL}/clerk-android-api/${version}/clerk-android-api-${version}.pom"
    "${MAVEN_BASE_URL}/clerk-android-ui/${version}/clerk-android-ui-${version}.pom"
  )

  for attempt in $(seq 1 60); do
    local missing=0

    for url in "${urls[@]}"; do
      if ! curl --fail --silent --head --location "$url" >/dev/null; then
        missing=$((missing + 1))
      fi
    done

    if [ "$missing" -eq 0 ]; then
      echo "Maven Central has clerk-android-api and clerk-android-ui ${version}."
      return 0
    fi

    if [ "$attempt" -eq 60 ]; then
      echo "::error::Timed out waiting for clerk-android ${version} on Maven Central."
      return 1
    fi

    echo "::notice::Waiting for ${missing} Maven Central artifact(s), attempt ${attempt}/60."
    sleep 30
  done
}

read_gradle_version() {
  local property="$1"
  local file="$2"

  sed -nE "s/^[[:space:]]*${property}[[:space:]]*=[[:space:]]*\"([^\"]+)\".*/\1/p" "$file" |
    head -n 1
}

write_changeset() {
  local current_api_version="$1"
  local current_ui_version="$2"
  local previous_version

  if [ "$current_api_version" = "$current_ui_version" ]; then
    previous_version="$current_api_version"
  else
    previous_version="${current_api_version} / ${current_ui_version}"
  fi

  {
    echo "---"
    echo "'@clerk/expo': patch"
    echo "---"
    echo ""
    echo "Bump the bundled \`clerk-android\` SDK (\`clerk-android-api\` and \`clerk-android-ui\`) from \`${previous_version}\` to \`${CLERK_ANDROID_VERSION}\`. See the Clerk Android release: ${CLERK_ANDROID_RELEASE_URL}."
  } >"$CHANGESET_FILE"
}

open_or_update_pr() {
  local body_file="$1"
  local existing_pr

  existing_pr=$(
    gh pr list \
      --repo "$TARGET_REPO" \
      --head "$BRANCH" \
      --base main \
      --state open \
      --json url \
      --jq '.[0].url // ""'
  )

  if [ -n "$existing_pr" ]; then
    gh pr edit "$existing_pr" \
      --repo "$TARGET_REPO" \
      --title "$PR_TITLE" \
      --body-file "$body_file"
    echo "Updated existing PR: ${existing_pr}"
    return 0
  fi

  gh pr create \
    --repo "$TARGET_REPO" \
    --base main \
    --head "$BRANCH" \
    --title "$PR_TITLE" \
    --body-file "$body_file"
}

main() {
  if ! command -v gh >/dev/null; then
    echo "::error::The GitHub CLI is required to open the Expo release PR."
    exit 1
  fi

  wait_for_maven_artifacts "$CLERK_ANDROID_VERSION"

  cd "$JAVASCRIPT_DIR"

  if [ ! -f "$BUILD_FILE" ]; then
    echo "::error::Cannot find ${BUILD_FILE} in ${TARGET_REPO}."
    exit 1
  fi

  git fetch origin main:refs/remotes/origin/main
  git fetch origin "$BRANCH:refs/remotes/origin/$BRANCH" || true
  git checkout -B "$BRANCH" refs/remotes/origin/main

  local current_api_version
  local current_ui_version
  local previous_version
  current_api_version=$(read_gradle_version "clerkAndroidApiVersion" "$BUILD_FILE")
  current_ui_version=$(read_gradle_version "clerkAndroidUiVersion" "$BUILD_FILE")

  if [ -z "$current_api_version" ] || [ -z "$current_ui_version" ]; then
    echo "::error::Failed to read clerk-android versions from ${BUILD_FILE}."
    exit 1
  fi

  if [ "$current_api_version" = "$current_ui_version" ]; then
    previous_version="$current_api_version"
  else
    previous_version="${current_api_version} / ${current_ui_version}"
  fi

  if [ "$current_api_version" = "$CLERK_ANDROID_VERSION" ] &&
    [ "$current_ui_version" = "$CLERK_ANDROID_VERSION" ]; then
    echo "::notice::${TARGET_REPO} already pins clerk-android ${CLERK_ANDROID_VERSION}; skipping PR."
    exit 0
  fi

  perl -0pi -e \
    's/clerkAndroidApiVersion = "[^"]+"/clerkAndroidApiVersion = "$ENV{CLERK_ANDROID_VERSION}"/g; s/clerkAndroidUiVersion = "[^"]+"/clerkAndroidUiVersion = "$ENV{CLERK_ANDROID_VERSION}"/g' \
    "$BUILD_FILE"

  if ! grep -q "clerkAndroidApiVersion = \"${CLERK_ANDROID_VERSION}\"" "$BUILD_FILE" ||
    ! grep -q "clerkAndroidUiVersion = \"${CLERK_ANDROID_VERSION}\"" "$BUILD_FILE"; then
    echo "::error::Failed to update clerk-android Gradle pins."
    exit 1
  fi

  write_changeset "$current_api_version" "$current_ui_version"
  git diff --check

  if git diff --quiet; then
    echo "::notice::No Expo Android release PR changes were needed."
    exit 0
  fi

  git config user.name "github-actions[bot]"
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  git add "$BUILD_FILE" "$CHANGESET_FILE"
  git commit -m "fix(expo): bump clerk-android to ${CLERK_ANDROID_VERSION}"
  git push --force-with-lease origin "HEAD:${BRANCH}"

  local body_file
  body_file=$(mktemp)
  cat >"$body_file" <<EOF
## Description

Bumps the bundled \`clerk-android\` SDK in \`@clerk/expo\` from \`${previous_version}\` to \`${CLERK_ANDROID_VERSION}\`.

Release: ${CLERK_ANDROID_RELEASE_URL}

## Checklist

- [ ] \`pnpm test\` runs as expected.
- [ ] \`pnpm build\` runs as expected.
- [ ] (If applicable) [JSDoc comments](https://jsdoc.app/about-getting-started.html) have been added or updated for any package exports
- [ ] (If applicable) [Documentation](https://github.com/clerk/clerk-docs) has been updated

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [x] Refactoring / dependency upgrade / documentation
- [ ] Other
EOF

  open_or_update_pr "$body_file"
}

main "$@"
