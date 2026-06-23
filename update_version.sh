#!/bin/bash

# Path to the file
FILE="./app/version.properties"

# Ensure the script accepts a version name as an argument
if [ $# -ne 1 ]; then
  echo "Usage: $0 <new_version_name>"
  exit 1
fi

# Get the new version name from the command line argument
new_version_name="$1"

# Extract current version code from the file
current_version_code=$(grep '^VERSION_CODE=' "$FILE" | cut -d'=' -f2)

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

perl -0pi -e "s/^VERSION_CODE=.*/VERSION_CODE=$new_version_code/m; s/^VERSION_NAME=.*/VERSION_NAME=$new_version_name/m" "$FILE"

echo "Version code has been updated to $new_version_code"
echo "Version name has been updated to \"$new_version_name\""
