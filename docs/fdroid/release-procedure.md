# F-Droid Release Procedure

This is the release path for a ShelfDroid version that is intended to be submittable to the F-Droid main repository while preserving update continuity for existing GitHub APK installs.

## Preconditions

- Release work is merged onto `dev`.
- `app/version.properties` contains the exact release version and version code.
- The matching F-Droid changelog file exists at `fastlane/metadata/android/en-US/changelogs/<VERSION_CODE>.txt`.
- Signing is available in one of these ways:
  - local `../signing/keystore.properties`
  - local `../../signing/keystore.properties`
  - explicit override via `-Pshelfdroid.signingPropertiesFile=/abs/path/to/keystore.properties`
  - explicit override via `SHELFDROID_SIGNING_PROPERTIES_FILE=/abs/path/to/keystore.properties`
- GitHub `Production` environment secrets are configured for the same developer-controlled signing key used for the GitHub APK release line.

## Release steps

1. Merge the approved release work onto `dev` so the release tag will point at the intended source.

2. Bump the version in `app/version.properties`.

```bash
./update_version.sh 0.4.3
```

3. Update the matching release notes inputs:
   - `CHANGELOG.md`
   - `fastlane/metadata/android/en-US/changelogs/<new VERSION_CODE>.txt`

4. Verify the working tree and build from the intended source on `dev`.

```bash
./gradlew :app:assembleRelease
```

5. If local signing is configured, confirm the local release APK is developer-signed.

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

If `apksigner` is not on `PATH`, run the copy from your Android SDK `build-tools/` directory.

6. Commit the release prep, then create an exact tag that matches `VERSION_NAME`.

```bash
git tag 0.4.3
git push origin dev
git push origin 0.4.3
```

7. Let GitHub Actions publish the continuity artifact.
   - `.github/workflows/android.yaml` builds from the exact tag ref.
   - The workflow fails if the tag name and `VERSION_NAME` differ.
   - The workflow signs the release APK and publishes `shelfdroid-<tag>.apk` to the GitHub Release for that tag.

8. Verify the published GitHub Release asset before submitting to F-Droid.

```bash
gh release view 0.4.3
```

If you want to re-check signer continuity against the published asset, download the APK and inspect it with `apksigner verify --print-certs`.

The GitHub Release APK is the developer-signed upstream artifact line that existing GitHub APK users should continue to receive updates from. Do not create a separate primary artifact line for F-Droid with a different signing identity.

9. Prepare the F-Droid submission from the same source tag.
   - submit the upstream tag
   - point review notes at the GitHub Release asset for that same tag
   - start from [`dev.halim.shelfdroid.yml.example`](./dev.halim.shelfdroid.yml.example) when opening the `fdroiddata` merge request
   - keep `fastlane/metadata/android/` and [`asset-provenance.md`](./asset-provenance.md) aligned with the shipped release

## Verified path

The current release path has been verified against the GitHub APK continuity target:

- the GitHub workflow builds from the exact release tag and publishes a signed GitHub Release APK
- the local Gradle release signing lookup now resolves the developer keystore from the current workspace layout or an explicit override
- `.github/workflows/android.yaml` now pins `ilharp/sign-android-release@v2` to Android build-tools `34.0.0`, because F-Droid's reproducible-build docs warn that `apksigner` from build-tools `35+` can make upstream-signed APK verification fail
- on 2026-06-22, a clean rebuild of tag `0.4.2` from a detached worktree succeeded with `ANDROID_HOME` and `ANDROID_SDK_ROOT` exported, so the ignored local `local.properties` file is not a hard requirement for clean CI-style builds
- on 2026-06-22, that clean `0.4.2` rebuild produced `app-release-unsigned.apk` whose extracted non-`META-INF` file set, per-file SHA-256 hashes, and ZIP entry order matched the published `shelfdroid-0.4.2.apk`; the only extracted-content differences were the expected `META-INF` signature files in the published APK

## Crash export fallback

ShelfDroid currently keeps ACRA's local crash-file export enabled because it writes crash reports into the user's Downloads area and does not send them to a third-party service.

Fallback policy if F-Droid review rejects it:

- do not disable local crash-file export preemptively
- if review explicitly rejects it, ship a focused follow-up change that disables only the `CrashFileSender` integration wired through `app/src/main/java/dev/halim/shelfdroid/ShelfDroid.kt` and `app/src/main/java/dev/halim/shelfdroid/crash/`
- keep the release tag, GitHub APK continuity line, metadata, and signing flow unchanged unless review requires a narrower adjustment
