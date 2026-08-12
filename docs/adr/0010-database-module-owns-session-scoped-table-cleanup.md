# Database module owns session-scoped table cleanup

ShelfDroid keeps **Full logout** and **Account switch** orchestration in `core-data`, but the SQLDelight operation that clears session-scoped persisted rows belongs in `core-database`. This keeps reset policy with the data-layer session boundary while keeping table deletion order and transaction ownership close to the schema they must track.

**Consequences**

`core-database` exposes a plain `SessionDatabaseCleanup` operation that knows SQLDelight tables, not user flows such as **Full logout**. `core-data` wires that operation through dependency injection and decides when it runs alongside authentication, local app preferences, transient downloads, current playback, and app storage cleanup.
