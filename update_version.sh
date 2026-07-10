#!/bin/bash

# Path to the file
FILE="./app/version.properties"
CHANGELOG_DIR="./fastlane/metadata/android/en-US/changelogs"

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

touch "$changelog_file"
git add "$changelog_file"

echo "Version code has been updated to $new_version_code"
echo "Version name has been updated to \"$new_version_name\""
echo "Changelog file has been created at $changelog_file"
