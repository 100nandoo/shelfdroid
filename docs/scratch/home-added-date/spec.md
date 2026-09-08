# Home added-date secondary text

Status: implemented; verification and review complete.

## Behavior

- Apply to book and podcast libraries on Home, in both list and grid layouts.
- While sorting by `Added at`, in either direction, replace the author in `secondaryText` with the Library item added date.
- Format dates as `Added 7 Jan 2026` or `Added 27 Dec 2025`: unpadded day, abbreviated month, four-digit year.
- Use the device's local time zone and month abbreviations in the app's language, keeping day–month–year order.
- Put the `Added %1$s` label and `Unknown` fallback in Android string resources so they can be translated later.
- Display exactly `Unknown` when the added timestamp is missing or zero.
- Preserve the existing single-line secondary text and ellipsis when space is insufficient.
- Other sort modes retain their existing secondary text behavior. Sorting itself remains unchanged.

## Verification for implementation

Verify both library types, both layouts, and both sort directions; the two example date formats; local-date conversion near midnight; missing/zero timestamps; and restoration of existing secondary text after switching sort modes.

## Documentation decision

The domain meaning is recorded in the root glossary. No ADR is needed for this readily reversible presentation change.
