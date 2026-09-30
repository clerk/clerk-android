#!/bin/sh
# Shared helpers for the config/bin tool wrappers.
#
# The pre-commit hook must run exactly the same tool versions as Gradle
# (Spotless ktfmt, detekt). Versions are read from gradle/libs.versions.toml,
# which build.gradle.kts also uses, so there is a single source of truth.
# Jars are downloaded once from Maven Central, verified against the SHA-256
# pinned in config/bin/tool-checksums.sha256, and cached per version.

die() {
  printf '%s\n' "$*" >&2
  exit 1
}

# catalog_version <key> -> prints the [versions] entry for <key>.
catalog_version() {
  catalog="$REPO_ROOT_DIR/gradle/libs.versions.toml"
  version="$(sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$catalog" | head -n 1)"
  [ -n "$version" ] || die "ERROR: no '$1' version found in $catalog"
  printf '%s\n' "$version"
}

sha256_of() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | cut -d ' ' -f 1
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  else
    die "ERROR: neither 'shasum' nor 'sha256sum' is available to verify downloads"
  fi
}

# pinned_sha256 <file name> -> prints the checksum pinned for <file name>.
pinned_sha256() {
  checksums="$REPO_ROOT_DIR/config/bin/tool-checksums.sha256"
  sum="$(awk -v name="$1" '$1 !~ /^#/ && $2 == name { print $1; exit }' "$checksums")"
  [ -n "$sum" ] || die "ERROR: no SHA-256 pinned for $1 in $checksums"
  printf '%s\n' "$sum"
}

# tool_jar <maven directory> <file name> -> prints the local path of the cached
# jar, downloading and checksum-verifying it on first use.
tool_jar() {
  cache_root="${CLERK_TOOLS_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/clerk-android/tools}"
  jar="$cache_root/$2"
  if [ ! -f "$jar" ]; then
    expected="$(pinned_sha256 "$2")" || exit 1
    command -v curl >/dev/null 2>&1 || die "ERROR: 'curl' is required to download $2"
    mkdir -p "$cache_root" || die "ERROR: cannot create $cache_root"
    url="https://repo1.maven.org/maven2/$1/$2"
    tmp="$jar.tmp.$$"
    printf 'Downloading %s\n' "$url" >&2
    if ! curl -fsSL -o "$tmp" "$url"; then
      rm -f "$tmp"
      die "ERROR: failed to download $url"
    fi
    actual="$(sha256_of "$tmp")"
    if [ "$expected" != "$actual" ]; then
      rm -f "$tmp"
      die "ERROR: checksum mismatch for $url (expected $expected, got $actual)"
    fi
    mv "$tmp" "$jar"
  fi
  printf '%s\n' "$jar"
}

java_cmd() {
  if [ -n "$JAVA_HOME" ]; then
    [ -x "$JAVA_HOME/bin/java" ] || die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    printf '%s\n' "$JAVA_HOME/bin/java"
  else
    command -v java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    printf 'java\n'
  fi
}
