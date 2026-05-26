# Readable Shared Storage for Offline Downloads

ShelfDroid will store offline playback downloads for books and podcasts in shared storage outside
the app-owned sandbox so they can survive Android app-data clear. We intentionally prioritize simple
human-readable paths over stable identifier-based paths, using
`ShelfDroid/books/<title>_<author>/<server-filename>` for books and
`ShelfDroid/podcasts/<podcast-title>/<server-filename>` for podcasts, and we accept best-effort
rediscovery after app-data clear rather than deterministic identity matching. When a book folder
already exists, ShelfDroid will delete its existing contents before starting the new download into
that folder; when a podcast file already exists, ShelfDroid will delete the old file before starting
the new download; metadata changes will not trigger folder moves, and recovery will first try exact
expected paths before falling back to a best-effort scan of the `ShelfDroid` tree.
