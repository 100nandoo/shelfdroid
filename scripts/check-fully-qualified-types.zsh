#!/usr/bin/env zsh
set -euo pipefail

repo_root="${0:A:h:h}"
cd "$repo_root"

rg_args=(
  --line-number
  --color=never
  --pcre2
  --glob '*.kt'
  --glob '!**/build/**'
  --glob '!**/.gradle/**'
  --glob '!**/generated/**'
)

patterns=(
  '(?<!\?)(:|->)[[:space:]]*[a-z][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+\.[A-Z][A-Za-z0-9_]*'
  'typealias[[:space:]]+[A-Z][A-Za-z0-9_]*[[:space:]]*=[[:space:]]*[a-z][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+\.[A-Z][A-Za-z0-9_]*'
)

found=0

for pattern in "${patterns[@]}"; do
  if matches=$(rg "${rg_args[@]}" "$pattern" .); then
    print -r -- "$matches"
    found=1
  fi
done

if (( found )); then
  print -u2
  print -u2 "Fully qualified type declarations found. Prefer importing the type and using its short name."
  exit 1
fi
