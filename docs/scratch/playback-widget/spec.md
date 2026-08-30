# Playback widget

**Triage:** `ready-for-agent`

## Problem Statement

ShelfDroid listeners cannot see or control **Current playback** from their home screen. They must open ShelfDroid or use Android's system media controls even for common audiobook actions, and those generic controls do not fully represent ShelfDroid's domain-specific **Chapter** navigation. Listeners need a compact, recognizable ShelfDroid widget that exposes the right controls for its available size, reflects the live player accurately, and provides a useful way into the app when there is no Current playback.

## Solution

Add a responsive Jetpack Glance playback widget in a dedicated `:widget` Gradle module. A small 3×2 layout shows the current cover with seek backward, play/pause, and seek forward controls. A large 4×2-or-wider layout adds identifying metadata and previous/next Chapter controls. The widget mirrors the single live Current playback across all placed instances, uses ShelfDroid's existing theme preferences and visual identity within Glance's constraints, sends commands through the Media3 session, and opens ShelfDroid when playback is absent or cannot be controlled.

## User Stories

1. As a ShelfDroid listener, I want to add a playback widget to my home screen, so that I can reach playback without first opening the app.
2. As a ShelfDroid listener, I want every placed playback widget to mirror the same Current playback, so that its behavior is predictable across home-screen pages.
3. As a ShelfDroid listener, I want the widget to require no configuration, so that it works immediately after placement.
4. As a ShelfDroid listener, I want to see the cover for Current playback, so that I can recognize the Book or Podcast at a glance.
5. As a ShelfDroid listener using a 3×2 widget, I want to see only the cover and the three primary controls, so that each control remains legible and easy to tap.
6. As a ShelfDroid listener using a 4×2 or wider widget, I want all five playback controls, so that I can seek and navigate Chapters from the home screen.
7. As a ShelfDroid listener using a large widget, I want to see the audiobook or podcast title, so that similar covers are still identifiable.
8. As a ShelfDroid listener using a large widget, I want to see the current Chapter or Episode title, so that I know which playable segment is active.
9. As a ShelfDroid listener, I want long metadata to be truncated cleanly, so that it never pushes controls out of the widget.
10. As a ShelfDroid listener, I want to pause active playback from the widget, so that I can stop listening immediately.
11. As a ShelfDroid listener, I want to resume paused playback from the widget, so that I can continue listening immediately.
12. As a ShelfDroid listener, I want the widget to show Pause while playback is buffering but still intended to continue, so that the control does not misleadingly flip back to Play.
13. As a ShelfDroid listener, I want the widget to show Play when playback is paused or ended, so that the displayed action matches what tapping it will do.
14. As a ShelfDroid listener, I want to seek backward by ShelfDroid's existing 10-second interval, so that widget and in-app behavior remain consistent.
15. As a ShelfDroid listener, I want to seek forward by ShelfDroid's existing 10-second interval, so that widget and in-app behavior remain consistent.
16. As an audiobook listener more than three seconds into a Chapter, I want Previous Chapter to restart that Chapter, so that an accidental missed passage is easy to replay.
17. As an audiobook listener near the beginning of a Chapter, I want Previous Chapter to move to the preceding Chapter, so that navigation follows familiar media-player behavior.
18. As an audiobook listener on the first Chapter, I want Previous Chapter to restart it, so that the control remains useful at the beginning of the Book.
19. As an audiobook listener, I want Next Chapter to advance to the next valid Chapter, so that I can skip forward through the Book.
20. As an audiobook listener on the final Chapter, I want Next Chapter to remain visible but disabled, so that control positions stay stable without triggering an invalid transition.
21. As a listener to a Book with no Chapters, I want Previous to restart the playable item and Next to be disabled, so that the widget never attempts invalid Chapter navigation.
22. As a listener to a single-Chapter Book, I want Previous to restart the Chapter and Next to be disabled, so that the widget never attempts an out-of-range transition.
23. As a podcast listener, I want Previous to restart the Episode and Next Chapter to be disabled, so that audiobook Chapter semantics are not incorrectly mapped to podcast Episodes.
24. As a ShelfDroid listener, I want unavailable controls to remain in stable positions but be visibly disabled, so that the layout does not shift as playback changes.
25. As a ShelfDroid listener, I want tapping the active cover, metadata, or unused widget background to open Now Playing, so that I can inspect or manage Current playback in the app.
26. As a ShelfDroid listener with no Current playback, I want the widget to show a branded empty state, so that it does not display stale media or unusable controls.
27. As a ShelfDroid listener with no Current playback, I want tapping anywhere on the widget to open ShelfDroid normally, so that the widget remains useful.
28. As a ShelfDroid listener, I want a control tap to open ShelfDroid when playback disappeared after the widget rendered, so that stale launcher UI fails safely.
29. As a ShelfDroid listener encountering a playback error, I want the widget to retain identifying cover and metadata but offer to open ShelfDroid instead of showing broken controls, so that I can understand and recover from the error.
30. As an offline ShelfDroid listener, I want cached cover art to remain available when possible, so that the widget still identifies downloaded or buffered media.
31. As a ShelfDroid listener whose cover cannot be loaded, I want a ShelfDroid placeholder instead of a broken or stale image, so that the widget remains coherent.
32. As a ShelfDroid listener who clears playback or fully logs out, I want all widget instances to clear Current playback promptly, so that private or stale media is not left on the home screen.
33. As a ShelfDroid listener, I want headset, notification, in-app, and widget playback changes to update every widget, so that all playback surfaces agree.
34. As a battery-conscious listener, I want the widget to avoid continuous position updates, so that it does not spend power pretending to be a real-time progress display.
35. As a ShelfDroid listener, I want resizing between small and large layouts to add or remove secondary information and Chapter controls predictably, so that the widget remains usable at either supported size.
36. As a ShelfDroid listener, I want the widget to mirror ShelfDroid's light/dark preference, so that it visually belongs to the app.
37. As a ShelfDroid listener using dynamic color, I want the widget to honor ShelfDroid's dynamic-color toggle on supported Android versions, so that it matches the app and device palette.
38. As a ShelfDroid listener, I want the widget to use ShelfDroid's color hierarchy, weights, spacing, and Rounded icons, so that it is recognizable despite Glance's font limitations.
39. As a ShelfDroid listener using accessibility services, I want meaningful content descriptions and disabled-state semantics for every control and image, so that the widget is understandable without sight.
40. As a ShelfDroid listener browsing the widget picker, I want an accurate playback-widget name, description, and preview, so that I know what I am adding.
41. As a ShelfDroid listener, I want rapid or repeated taps to be delegated safely to the authoritative Media3 session, so that widget-local state cannot drift from playback.
42. As a ShelfDroid listener, I want invalid podcast and single-Chapter navigation to be corrected in the expanded in-app player as part of this work, so that the widget and player share safe behavior.

## Implementation Decisions

- Create a dedicated Android library module named `:widget` with namespace `dev.halim.shelfdroid.widget`. The application module packages it as a direct dependency.
- The widget module owns the `GlanceAppWidget`, receiver, action callbacks, responsive presentation, widget-provider metadata, preview assets, widget-specific resources, and structural tests.
- The widget module consumes `:core-ui` for the launcher activity, existing theme schemes, and drawable controls; `:media` for the playback service and command contracts; and `:core-data` for ShelfDroid theme preferences. Neither `:core-ui` nor `:media` depends on `:widget`.
- Keep playback and MediaSession behavior in `:media`. The widget is a playback client and must not call the player store or player repository directly.
- Use Jetpack Glance AppWidget and Glance Material 3. Add the Glance structural-testing libraries to the version catalog for the widget module.
- Declare the widget receiver and widget-provider metadata in the widget module so normal Android manifest and resource merging packages the complete widget through the application dependency.
- Support responsive layouts with 3×2 as the minimum small layout and 4×2 as the large-layout breakpoint. Do not support a 2×2 variant.
- The small layout contains the cover plus seek backward, play/pause, and seek forward. It omits metadata and Chapter controls to preserve useful touch targets.
- The large layout contains the cover, two compact metadata lines, seek backward, play/pause, seek forward, Previous Chapter, and Next Chapter.
- The first metadata line identifies the audiobook or podcast; the second identifies the current Chapter or Episode. Both lines ellipsize rather than displacing controls.
- Treat playing, paused, buffering, and ended media with a loaded item as Current playback. Treat a cleared player or stopped playback service as the empty state.
- Show Pause from playback intent, including while buffering, rather than deriving it only from `isPlaying`. Show Play when paused or ended.
- Use standard Media3 `Player` commands for play, pause, seek backward, and seek forward. Retain the configured 10-second seek increments rather than introducing widget-specific settings.
- Add Previous Chapter and Next Chapter as custom MediaSession commands because ShelfDroid Chapters do not map safely to native previous/next media-item commands. Advertise these commands only to trusted same-app controllers.
- Centralize Chapter command semantics and availability in the playback layer so the expanded player and widget share one behavior. This work corrects the existing invalid Next behavior for podcasts and single-Chapter Books.
- Previous Chapter restarts the current Chapter when playback is more than three seconds into it; near the beginning it selects the previous Chapter. On the first Chapter, a Book with no Chapters, a single-Chapter Book, or a Podcast Episode, Previous restarts the current playable unit.
- Next Chapter advances only when a valid next Chapter exists. It remains rendered but disabled on the final Chapter and for podcasts, Books without Chapters, and single-Chapter Books.
- Each widget action creates a short-lived asynchronous MediaController connected to ShelfDroid's playback service, sends one authoritative command, requests a widget refresh when appropriate, and releases the controller after pending commands have been dispatched.
- If an action finds no current media because playback disappeared after rendering, it opens ShelfDroid normally rather than silently failing or attempting playback resumption.
- Keep the Glance widget stateless and passive. During rendering, obtain a fresh presentation snapshot from application state, using the live Media3 session as the playback source of truth. Do not make in-memory Glance state or persisted “last played” widget data authoritative.
- Define a playback-layer presentation-observer port with zero-or-more consumers. The widget module contributes an application-scoped implementation through dependency-injection multibinding; its sole side effect is requesting an update of all playback-widget instances.
- Notify presentation observers only for widget-visible changes: current media or metadata, Chapter, playback intent/state, errors, command availability, cover availability, player clear/stop, logout, and relevant theme preferences. Do not notify from high-frequency playback-position updates.
- Widget actions also request an immediate refresh after command completion for responsiveness; playback-originated observer events remain authoritative and cover changes initiated by the app, notification, headset, or other controllers.
- Do not schedule periodic playback or progress polling. A stale rendered control remains safe because its action re-checks the live session and opens ShelfDroid if playback is gone.
- All widget instances mirror the same Current playback and require no per-instance configuration.
- In the empty state, remove media artwork and transport controls, show a ShelfDroid-branded invitation, and make the whole widget open the normal app destination.
- In the active state, make the cover, metadata, and otherwise unused surface open Now Playing for Current playback. Transport buttons perform only their assigned playback commands.
- In the playback-error state, retain identifying cover and metadata, replace transport controls with an “Open ShelfDroid” affordance, and navigate to Now Playing for recovery.
- Load cover art through ShelfDroid's authenticated/cached image facilities, downsample it to widget needs, and fall back to a ShelfDroid placeholder. Never retain the previous cover after playback clears.
- Adapt ShelfDroid's fixed light and dark Material 3 schemes into Glance color providers. Read and honor ShelfDroid's explicit dark-mode and dynamic-color preferences, including platform dynamic colors on supported Android versions.
- Glance cannot use ShelfDroid's bundled custom fonts. Approximate the established hierarchy using system Serif for titles and system Sans Serif for secondary metadata while preserving sizes, weights, colors, and truncation behavior.
- Reuse existing Rounded drawable controls when suitable and obtain any missing icons from Google Fonts Icons in the Rounded style. Use system widget corner radii and widget-specific surface resources where needed.
- Provide localized labels, content descriptions, state descriptions where supported, and stable disabled controls for accessibility.
- Supply widget-picker description and preview resources representing the default large layout.

## Testing Decisions

- Tests should assert externally observable behavior and contracts rather than internal class structure, dependency-injection mechanics, or exact implementation calls.
- Use two high-level seams. This is the minimum practical split because Glance structural output and playback command semantics are governed by different frameworks and cannot be verified meaningfully through one existing seam.
- In `:widget`, use Glance JVM structural tests to compose representative state at explicit sizes and assert visible content, omitted content, text, content descriptions, enabled/disabled semantics where exposed, and configured actions.
- Cover at least empty, playing, paused, buffering, ended, error, cover fallback, 3×2, and 4×2-or-wider presentations.
- Verify that the small layout exposes exactly the three primary controls and omits metadata and Chapter controls.
- Verify that the large layout exposes metadata and all five controls while keeping unavailable Chapter actions stable and disabled.
- Verify navigation contracts: the empty surface opens the normal app, active non-control surfaces open Now Playing, and the error affordance opens Now Playing.
- Test widget action orchestration against a fake controller/session gateway rather than starting a real MediaSession in JVM tests. Assert the externally requested command and fallback navigation outcome.
- In `:media`, use plain JVM tests around the shared playback-command/availability seam for 10-second seeking, playing/paused/buffering/ended mapping, previous restart threshold, valid previous/next transitions, first/final Chapter behavior, no-Chapter Books, single-Chapter Books, podcasts, and invalid-command rejection.
- Extend the behavioral style established by existing media tests such as `PlayPauseControlStateMapperTest`, `MediaControllerManagerTest`, and session lifecycle tests.
- Ensure the expanded in-app player and widget consume the same tested Chapter availability and semantics rather than maintaining duplicate rules.
- Verify observer notifications at the public playback-presentation boundary: presentation-visible events notify, high-frequency position-only updates do not, multiple observers coexist, and zero observers remains valid.
- Verify widget theme mapping for fixed light, fixed dark, supported dynamic light, and supported dynamic dark preferences without asserting private color-provider implementation.
- Do not reuse the existing Compose Preview Screenshot harness as a Glance renderer; it targets normal Compose UI and cannot validate RemoteViews-backed Glance composition.
- Do not add a launcher-host or device end-to-end widget harness in this phase. Add that infrastructure only if future regressions demonstrate that structural and command-boundary tests are insufficient.

## Out of Scope

- A 2×2 or smaller widget layout.
- Live elapsed time, remaining time, progress bars, or periodic position refreshes.
- Per-widget server, Library, Book, Podcast, Episode, or Chapter configuration.
- Persisting and showing the last played item after the playback service has stopped.
- Starting playback resumption from an empty widget state.
- Mapping Chapter controls to previous/next Podcast Episodes.
- Changing ShelfDroid's existing 10-second seek increments or adding widget-specific seek settings.
- Exact use of ShelfDroid's bundled Lora, Inter, or JetBrains Mono fonts, which Glance does not support.
- Lock-screen widgets or non-home-screen widget categories.
- A generalized widget framework or additional widget types beyond the playback widget.
- A launcher/device AppWidgetHost integration-test harness in the first version.
- Continuous polling, background progress workers, or per-minute widget updates.
- Redesigning the expanded player beyond sharing and correcting Chapter command availability and semantics.

## Further Notes

- The dedicated-module and dependency-inversion decision is recorded in [ADR 0015](../../adr/0015-dedicated-module-for-playback-widget.md).
- Repository vocabulary is deliberate: the widget represents **Current playback**, not an Audiobookshelf server **Open session**; a **Chapter** is distinct from a **Track** and a podcast **Episode**.
- Current repository inspection found no existing app-widget implementation. Glance Material 3 is present in the version catalog but is not consumed by a module yet.
- Current Android documentation was checked through Android CLI. It confirms that Glance action callbacks support suspend work, MediaController connection is asynchronous, pending commands are delivered before controller release completes, custom session commands must represent behavior not covered by standard Player commands, Glance widgets should be stateless and passive, and `updateAll()` is appropriate for event-driven refreshes.
- The existing app currently exposes an invalid Next action for podcast and single-Chapter playback in some states. This spec intentionally includes correcting that behavior at the shared command seam rather than reproducing it in the widget.
