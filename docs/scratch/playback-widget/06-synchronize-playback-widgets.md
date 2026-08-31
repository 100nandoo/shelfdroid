# 06: Keep every playback widget synchronized

**What to build:** Keep all placed playback widgets aligned with Current playback and ShelfDroid's theme when changes originate from the app, notification, headset, widget, player lifecycle, or preferences, without periodic position polling or a dependency cycle.

**Blocked by:** 03: Show Current playback in the widget.

**Status:** complete

- [x] The playback layer exposes an operation-agnostic presentation-observer port that remains valid with zero, one, or multiple consumers.
- [x] The widget module contributes an application-scoped observer implementation through dependency-injection multibinding without making the playback or UI modules depend on `:widget`.
- [x] A presentation notification requests an update of every placed playback-widget instance.
- [x] Current media, metadata, Chapter, playback intent/state, playback errors, command availability, and cover availability changes trigger presentation updates.
- [x] Player clear/stop and Full logout trigger an update that removes Current playback and stale artwork from every widget.
- [x] ShelfDroid light/dark and dynamic-color preference changes update every widget to the newly selected appearance.
- [x] Playback changes originating from the app, notification, headset, or another controller are reflected without requiring a widget action.
- [x] High-frequency position-only updates do not trigger widget recomposition or `updateAll()`.
- [x] No periodic worker or app-widget update interval is used to simulate live progress.
- [x] Multiple placed widget instances remain consistent with the same single Current playback and require no per-instance configuration.
- [x] Tests verify zero and multiple observers, presentation-visible notifications, suppression of position-only churn, clear/logout behavior, theme changes, and all-instance update requests at the public observer boundary.
