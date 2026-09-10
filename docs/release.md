# Release Guide

This is the maintainer release path for ShelfDroid.

## Preconditions

- Release work is merged onto `dev`.
- The working tree is clean before starting `cz bump`.
- Commits intended for the release follow the Commitizen convention so changelog generation works as expected.
- GitHub `Production` environment secrets are configured for the Android signing key.
- GitHub `Production` environment variables `GCP_WORKLOAD_IDENTITY_PROVIDER` and `GCP_SERVICE_ACCOUNT` are configured for Google Cloud Workload Identity Federation.
- The Google Play Android Developer API is enabled, and the service account is granted release access to the ShelfDroid app in Play Console.
- Local signing is available if you want to verify the release build before pushing.

## Google Play Actions setup

The workflow uses Workload Identity Federation, so no long-lived Google service-account JSON key is stored in GitHub. The provider and service-account values are non-sensitive configuration; the Android signing material remains secret.

### GitHub Production environment

Under **Settings → Environments → Production**, configure these existing signing secrets:

- `ANDROID_SIGNING_KEY`: base64-encoded Android `.jks` file
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

Add these environment variables:

- `GCP_WORKLOAD_IDENTITY_PROVIDER`: the complete provider resource name, using the Google Cloud project number:

  ```text
  projects/<PROJECT_NUMBER>/locations/global/workloadIdentityPools/<POOL>/providers/<PROVIDER>
  ```

- `GCP_SERVICE_ACCOUNT`: the service-account email, for example:

  ```text
  shelfdroid-release@<PROJECT_ID>.iam.gserviceaccount.com
  ```

### Google Cloud and Play Console

1. Enable the **Google Play Developer API** in the Google Cloud project.
2. Create a service account for release uploads.
3. Create a Workload Identity Federation OIDC provider with issuer `https://token.actions.githubusercontent.com`.
4. Map the `repository`, `ref`, `ref_type`, and `event_name` claims, and restrict the provider to this repository and tag pushes. For example:

   ```text
   assertion.repository == '<OWNER>/<REPO>' &&
   assertion.ref_type == 'tag' &&
   assertion.event_name == 'push'
   ```

5. Grant the workload identity pool permission to impersonate the service account (`roles/iam.workloadIdentityUser`).
6. In Play Console → **Users and permissions**, invite the service-account email and grant the ShelfDroid app permission to release to production.

Allow several minutes for new identity-provider and IAM permissions to propagate.

## Testing the workflow

Push the feature branch without creating a release tag:

```bash
git push -u origin feat/upload-google-play
```

Use **Actions → Android CI → Run workflow** and select that branch. A manual dispatch builds and signs only the APK; the GitHub Release and Play upload jobs remain skipped.

For an end-to-end Play test, create the next release with a new `VERSION_CODE` and matching tag. Do not reuse an existing version code or tag. Push the tag and verify that:

- `build` produces and signs both APK and AAB artifacts
- `github-release` publishes the APK
- `play-upload` authenticates and creates a draft on the `production` track
- the draft contains the changelog and R8 mapping file

If authentication fails, inspect the WIF provider condition, full provider resource name, and service-account impersonation grant. If authentication succeeds but the upload returns a Play API permission error, review the service account’s Play Console app permissions.

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
- The workflow signs the APK and AAB, then starts the GitHub Release and Google Play upload in parallel.
- The GitHub Release publishes `shelfdroid-<tag>.apk` with the generated release notes.
- The Google Play upload creates a draft on the `production` track, including the localized release notes and R8 mapping file.
- A failed Google Play upload leaves the GitHub Release intact but marks the workflow failed for follow-up.
- The workflow uses `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt` as the GitHub Release notes body.

9. Verify the published GitHub Release.

```bash
gh release view <tag>
```

Check:

- the APK asset name matches the tag
- the release notes match the generated Fastlane changelog file
- the tag points at the intended release commit

10. Verify the Google Play draft.

- the draft is on the `production` track
- the bundle version matches the tag
- the “What’s new” text matches the generated Fastlane changelog
- the release has its mapping file attached

## Release checklist

- `dev` contains the intended release code
- `cz bump` completed successfully
- `app/version.properties` contains the intended `VERSION_NAME` and `VERSION_CODE`
- `CHANGELOG.md` contains the new release section
- `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt` contains the new release notes
- local release build succeeded
- bump commit and tag were pushed
- GitHub Release contains the signed APK and the expected release notes
- Google Play contains the signed AAB as a production-track draft
