# 02: Add the responsive empty playback widget

**What to build:** Let a listener add a ShelfDroid playback widget from the Android widget picker and use its responsive, branded empty state to open ShelfDroid when there is no Current playback.

**Blocked by:** None (can start immediately).

**Status:** complete

- [x] A dedicated Android library module named `:widget` with namespace `dev.halim.shelfdroid.widget` is packaged by the application.
- [x] The widget module owns its Glance widget, receiver, actions, provider metadata, preview assets, widget resources, and structural tests.
- [x] The module graph remains acyclic: the widget may consume existing UI, playback, and preference modules, while those modules do not depend on the widget.
- [x] The playback widget appears in the home-screen widget picker with a localized name, description, and representative preview.
- [x] Placing the widget requires no configuration.
- [x] The minimum supported layout is 3×2, with a responsive large breakpoint at 4×2 or wider; a 2×2 layout is not offered.
- [x] With no Current playback, every supported size shows a ShelfDroid-branded empty state without stale artwork or transport controls.
- [x] Tapping anywhere on the empty widget opens ShelfDroid at its normal app destination.
- [x] The empty state adapts ShelfDroid's current light/dark and dynamic-color preferences to Glance and uses system widget corner treatment.
- [x] Text uses supported system font families while preserving ShelfDroid's intended hierarchy; unsupported bundled fonts are not assumed to work in Glance.
- [x] Images and actions have meaningful accessibility descriptions.
- [x] Glance JVM structural tests verify the empty content and launch action at both responsive size classes.
- [x] The application and widget module build successfully together.
