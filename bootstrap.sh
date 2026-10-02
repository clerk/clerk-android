#!/bin/sh
# Installs the repository's pre-commit hook. Re-run after config/git/hooks/pre-commit changes.
set -eu

repo_root="$(git rev-parse --show-toplevel)"
# --git-path resolves the hooks directory in regular clones, worktrees (where .git is a file) and
# when core.hooksPath is set.
hooks_dir="$(git -C "$repo_root" rev-parse --path-format=absolute --git-path hooks)"

echo "Installing pre-commit hook into $hooks_dir"
mkdir -p "$hooks_dir"
cp "$repo_root/config/git/hooks/pre-commit" "$hooks_dir/pre-commit"
chmod +x "$hooks_dir/pre-commit"
