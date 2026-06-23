# New app: ShelfDroid

## Required

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy)
* [x] The original app author has been notified (and does not oppose the inclusion)
* [x] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request
* [ ] Builds with `fdroid build` and all pipelines pass
* [x] There is an issue tracker and contact info of the author so that we can report bugs and contact the author.

## Strongly Recommended

* [x] The upstream app source code repo contains the app metadata _(summary/description/images/changelog/etc)_ in a [Fastlane](https://gitlab.com/snippets/1895688) or [Triple-T](https://gitlab.com/snippets/1901490) folder structure
* [x] Releases are tagged and auto update is enabled

## Suggested

* [ ] External repos are added as git submodules instead of srclibs
* [x] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds)
* [ ] Multiple apks for native code

ShelfDroid is a third-party Android client for self-hosted Audiobookshelf servers.

The upstream repository is:
https://github.com/100nandoo/shelfdroid

Issue tracker / contact:
- https://github.com/100nandoo/shelfdroid/issues
- https://github.com/100nandoo

Upstream metadata is already in the source repo under:
`fastlane/metadata/android/en-US/`

Releases are tagged with exact version names such as:
`0.4.3`

The metadata for this merge request uses the upstream GitHub release APK as the
reference binary with `Binaries` and `AllowedAPKSigningKeys` for reproducible
upstream-signed releases.

There are currently no related `rfp` or `fdroiddata` issues to close.

/label ~"New App"
