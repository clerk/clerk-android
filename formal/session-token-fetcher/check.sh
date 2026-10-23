#!/usr/bin/env bash
# Model-checks SessionTokenFetcher.tla with TLC.
#   check.sh                     routine check: NoReset.cfg then Reset.cfg (about 40s)
#   TLC_CONFIG=Full.cfg check.sh 3 callers with resets (about 25 minutes)
# Extra arguments go to TLC. Exits non-zero if any config reports a violation.
set -euo pipefail

TLA_VERSION=v1.7.4
TLA_SHA256=936a262061c914694dfd669a543be24573c45d5aa0ff20a8b96b23d01e050e88
JAR_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/clerk-tla/$TLA_VERSION"
JAR="$JAR_DIR/tla2tools.jar"
HERE="$(cd "$(dirname "$0")" && pwd)"

if [[ ! -f "$JAR" ]]; then
  mkdir -p "$JAR_DIR"
  curl -fsSL -o "$JAR.tmp" \
    "https://github.com/tlaplus/tlaplus/releases/download/$TLA_VERSION/tla2tools.jar"
  mv "$JAR.tmp" "$JAR"
fi
echo "$TLA_SHA256  $JAR" | shasum -a 256 -c --quiet

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
cd "$HERE"
status=0
for cfg in ${TLC_CONFIG:-NoReset.cfg Reset.cfg}; do
  echo "== $cfg"
  work="$(mktemp -d)"
  "$JAVA" -XX:+UseParallelGC -cp "$JAR" tlc2.TLC -workers "${TLC_WORKERS:-2}" \
    -metadir "$work" -config "$cfg" "$@" SessionTokenFetcher.tla || status=1
  rm -rf "$work"
done
exit $status
