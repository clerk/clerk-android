#!/usr/bin/env bash
# Proves each property can fail: swaps one action for the broken version in
# mutants/<name>.tla, runs TLC on NoReset.cfg, and expects the property named
# on that file's "Expect:" line to be violated.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
status=0
for m in "$HERE"/mutants/*.tla; do
  name="$(basename "$m" .tla)"
  expect="$(sed -n 's/^\\\* Expect: //p' "$m")"
  action="$(grep -m1 -oE '^[A-Za-z]+(\(c\))? ==' "$m" | sed 's/(c)//; s/ ==//')"
  work="$(mktemp -d)"
  cp "$HERE"/SessionTokenFetcher.tla "$HERE"/NoReset.cfg "$HERE"/check.sh "$work"/
  M="$m" A="$action" perl -0pi -e '
    BEGIN { local $/; open my $f, "<", $ENV{M} or die; $r = <$f>;
            $r =~ s/^\\\*.*\n//mg; $r =~ s/\n\z//; }
    s/^\Q$ENV{A}\E(\(c\))? ==.*?(?=\n\n)/$r/ms or die "no action $ENV{A}\n";
  ' "$work/SessionTokenFetcher.tla"
  out="$(TLC_CONFIG=NoReset.cfg "$work"/check.sh 2>&1 || true)"
  rm -rf "$work"
  if [[ "$expect" == "EveryCallReturns" ]]; then
    pattern="Temporal properties were violated"
  else
    pattern="Invariant $expect is violated"
  fi
  if grep -q "$pattern" <<<"$out"; then
    echo "caught   $name ($expect)"
  else
    echo "MISSED   $name (expected $expect)"
    grep -E "^Error|violated|No error" <<<"$out" | head -3 || true
    status=1
  fi
done
exit $status
