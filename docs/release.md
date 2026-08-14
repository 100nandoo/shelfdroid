# Release Guide

This is the maintainer release path for ShelfDroid.

## Preconditions

- Release work is merged onto `dev`.
- The working tree is clean before starting `cz bump`.
- Commits intended for the release follow the Commitizen convention so changelog generation works as expected.
- GitHub `Production` environment secrets are configured for the Android signing key.
- Local signing is available if you want to verify the release build before pushing.

## What `cz bump` does in this repo

A successful `cz bump` run:

- updates `VERSION_NAME` in `app/version.properties`
- increments `VERSION_CODE`
- regenerates `CHANGELOG.md`
- writes the latest release notes into `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt`
- creates the bump commit
- creates the matching git tag

The GitHub release workflow then uses that Fastlane changelog file as the GitHub Release body.

## How `update_version.sh` works

`update_version.sh` is configured as a Commitizen `pre_bump_hook` in `.cz.toml`:

```toml
pre_bump_hooks = [
    "./update_version.sh $CZ_PRE_NEW_VERSION",
]
```

With the current Commitizen behavior, the release flow is:

1. Commitizen generates the new release section in `CHANGELOG.md`.
2. Commitizen updates the configured version file.
3. Commitizen runs `update_version.sh`.
4. The script reads the newly generated release section and writes the Fastlane changelog.
5. Commitizen commits the changes and creates the tag.

The script performs these updates:

- increments `VERSION_CODE` in `app/version.properties`
- sets `VERSION_NAME` to the new version passed by Commitizen
- creates `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt`
- extracts the matching `## <VERSION_NAME> (<DATE>)` section from `CHANGELOG.md`
- converts the Markdown notes to plain text suitable for Fastlane
- stages the generated Fastlane changelog so it is included in the bump commit

The script uses `CZ_PRE_CHANGELOG_FILE_NAME` when Commitizen provides it, and otherwise falls back to `./CHANGELOG.md`. If no matching release section is found, it creates an empty Fastlane changelog file. Keep this script as a `pre_bump_hook`: a `post_bump_hook` runs after the commit and tag, which would leave the generated Fastlane changelog out of the bump commit.

## Release steps

1. Switch to `dev` and make sure it is current.

```bash
git switch dev
git pull
```

2. Preview the next version if needed.

```bash
cz bump --get-next
```

If the detected version is not what you want, use an explicit increment or exact version:

```bash
cz bump --increment PATCH --yes
cz bump 0.4.8 --yes
```

3. Run the release bump.

```bash
cz bump --yes
```

4. Inspect the generated files.

- `app/version.properties`
- `CHANGELOG.md`
- `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt`

5. Build the release APK locally.

```bash
./gradlew :app:assembleRelease
```

6. If local signing is configured, verify the APK signature.

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

7. Push the bump commit and tag.

```bash
git push origin dev
git push origin --tags
```

8. Let GitHub Actions publish the release.

- `.github/workflows/android.yaml` builds from the exact tag.
- The workflow fails if the tag and `VERSION_NAME` do not match.
- The workflow signs the APK and publishes `shelfdroid-<tag>.apk`.
- The workflow uses `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt` as the GitHub Release notes body.

9. Verify the published GitHub Release.

```bash
gh release view <tag>
```

Check:

- the APK asset name matches the tag
- the release notes match the generated Fastlane changelog file
- the tag points at the intended release commit

## Release checklist

- `dev` contains the intended release code
- `cz bump` completed successfully
- `app/version.properties` contains the intended `VERSION_NAME` and `VERSION_CODE`
- `CHANGELOG.md` contains the new release section
- `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt` contains the new release notes
- local release build succeeded
- bump commit and tag were pushed
- GitHub Release contains both the signed APK and the expected release notes
