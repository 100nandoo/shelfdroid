#!/bin/bash

# Path to the file
FILE="./gradle/libs.versions.toml"

# Ensure the script accepts a version name as an argument
if [ $# -ne 1 ]; then
  echo "Usage: $0 <new_version_name>"
  exit 1
fi

# Get the new version name from the command line argument
new_version_name="$1"

# Extract current version code from the file
current_version_code=$(grep 'code = "' "$FILE" | sed 's/code = "\([0-9]*\)"/\1/')

# Check if the version code is found
if [ -z "$current_version_code" ]; then
  echo "Version code not found in $FILE"
  exit 1
fi

# Increment the version code by 1
new_version_code=$((current_version_code + 1))

# Check if the file exists
if [ ! -f "$FILE" ]; then
  echo "File $FILE not found!"
  exit 1
fi

# Replace the old version code with the new one
sed -i "s/code = \"$current_version_code\"/code = \"$new_version_code\"/" "$FILE"

# Replace the version name with the new version name (only in the versions section)
sed -i "/\[versions\]/,/^$/s/name = \".*\"/name = \"$new_version_name\"/" "$FILE"

echo "Version code has been updated to $new_version_code"
echo "Version name has been updated to \"$new_version_name\""