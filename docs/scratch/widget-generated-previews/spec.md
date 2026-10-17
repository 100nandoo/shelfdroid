# Player widget generated previews

Status: Implemented; review complete.

## Purpose

Show a populated Player widget in the Android widget picker using curated sample content, independent of the listener's library, authentication, and current playback.

## Decisions

- Use *The Adventures of Sherlock Holmes* by Arthur Conan Doyle (1892) as the public-domain sample book.
- Bundle Sidney Paget's 1904 public-domain portrait of Sherlock Holmes as the cover art. Record its source and attribution in `widget/assets/README.md`; do not use a modern edition's copyrighted cover.
- Show paused playback with the play button visible.
- Provide generated previews on Android 15 and later.
- Bundle a static preview image for older supported devices and when generated previews are unavailable.
- Provide compact and wide preview sizes. Match the minimum widget size for compact; choose a wide size above the existing 280dp cover visibility threshold.
- Publish on app launch with current appearance settings. If publishing is rate limited, retry on a later launch. Appearance changes may wait until the next launch.

## Implementation constraints

- Reuse the Player widget layout so previews reflect the actual widget.
- Supply bundled artwork synchronously: generated preview composition does not run the existing cover-loading effect.
- Keep publishing independent of playback-driven widget updates.
- Existing Glance version is 1.2.0; compile/target SDK is 37 and minimum SDK is 29.
- No ADR is needed: these are reversible feature choices without a lasting architectural trade-off.

## Acceptance checks

- A signed-out or empty-library user sees the same populated sample preview.
- Compact and wide previews match their widget layouts; wide preview includes artwork.
- Android 15+ can display the generated preview after app launch.
- Older supported Android versions display the bundled fallback.
- A rejected preview publish does not interrupt launch; a later launch can retry.
- Sample preview content never replaces the real widget's current playback.
- Build the affected module and inspect previews in a launcher before declaring implementation complete.

## Implementation and verification

- Added Glance generated previews on app launch for Android 15+, retaining the widget's existing exact sizing and giving previews compact and wide responsive sizes.
- Added a bundled static preview image and documented the image source.
- `:widget:compileDebugKotlin`, `:app:compileDebugKotlin`, and `:widget:testDebugUnitTest` pass.
- `:app:assembleDebug` passes.
- Installed and launched the app on an API 37 emulator and confirmed the widget picker shows the sample book, portrait, and paused play control.

## Sources

- [Android generated preview documentation](https://developer.android.com/develop/ui/compose/glance/generated-previews)
- [Project Gutenberg: The Adventures of Sherlock Holmes](https://www.gutenberg.org/ebooks/1661) — identifies the 1892 collection and lists public-domain status in the USA.
- [Domain glossary](../../../CONTEXT.md)
