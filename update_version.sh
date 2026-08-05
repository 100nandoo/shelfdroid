#!/bin/bash

# Path to the file
FILE="./app/version.properties"
CHANGELOG_DIR="./fastlane/metadata/android/en-US/changelogs"
CHANGELOG_FILE="${CZ_PRE_CHANGELOG_FILE_NAME:-./CHANGELOG.md}"

# Ensure the script accepts a version name as an argument
if [ $# -ne 1 ]; then
  echo "Usage: $0 <new_version_name>"
  exit 1
fi

# Get the new version name from the command line argument
new_version_name="$1"

# Check if the file exists
if [ ! -f "$FILE" ]; then
  echo "File $FILE not found!"
  exit 1
fi

# Check if the changelog directory exists
if [ ! -d "$CHANGELOG_DIR" ]; then
  echo "Directory $CHANGELOG_DIR not found!"
  exit 1
fi

# Extract current version code from the file
current_version_code=$(grep '^VERSION_CODE=' "$FILE" | cut -d'=' -f2)

# Check if the version code is found
if [ -z "$current_version_code" ]; then
  echo "Version code not found in $FILE"
  exit 1
fi

# Increment the version code by 1
new_version_code=$((current_version_code + 1))
changelog_file="$CHANGELOG_DIR/$new_version_code.txt"

perl -0pi -e "s/^VERSION_CODE=.*/VERSION_CODE=$new_version_code/m; s/^VERSION_NAME=.*/VERSION_NAME=$new_version_name/m" "$FILE"

release_notes=""
if [ -f "$CHANGELOG_FILE" ]; then
  release_notes=$(awk -v version="$new_version_name" '
    $0 ~ "^## " version " \\(" { in_section=1; next }
    in_section && $0 ~ "^## " { exit }
    in_section { print }
  ' "$CHANGELOG_FILE")
fi

plain_release_notes=""
if [ -n "$release_notes" ]; then
  plain_release_notes=$(printf "%s\n" "$release_notes" | perl -0pe '
    s/\r\n/\n/g;
    s/^###\s+Feat$/Features:/gm;
    s/^###\s+Fix$/Fixes:/gm;
    s/^###\s+Refactor$/Improvements:/gm;
    s/^###\s+Test$/Tests:/gm;
    s/^###\s+(.+)$/$1:/gm;
    s/^\s*-\s+/• /gm;
    s/\*\*([^*]+)\*\*/$1/g;
    s/`([^`]+)`/$1/g;
    s/\[([^\]]+)\]\([^)]+\)/$1/g;
    s/^[ \t]+|[ \t]+$//gm;
    s/\n{3,}/\n\n/g;
    s/\A\s+|\s+\z//g;
  ')
fi

if [ -n "$plain_release_notes" ]; then
  printf "%s\n" "$plain_release_notes" > "$changelog_file"
else
  touch "$changelog_file"
fi

git add "$changelog_file"

echo "Version code has been updated to $new_version_code"
echo "Version name has been updated to \"$new_version_name\""
echo "Changelog file has been updated at $changelog_file"
