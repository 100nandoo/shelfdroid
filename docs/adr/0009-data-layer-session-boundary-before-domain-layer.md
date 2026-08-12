# Data-layer session boundary before a domain layer

ShelfDroid will move full logout and account-switch orchestration out of `SettingsRepository` into a data-layer session boundary, without introducing a domain layer yet. `SettingsRepository` should own app settings only; session reset code may coordinate authentication, preferences, cached catalog and playback data, transient download state, current playback, database cleanup, and app storage cleanup through data-layer repositories until a broader domain layer is justified.
