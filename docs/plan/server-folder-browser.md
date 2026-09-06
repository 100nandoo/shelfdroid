# Server folder browser

Status: Implemented for phones.

## Agreed design

- Browse directories on the Audiobookshelf server using the existing server filesystem requests.
- Use a dedicated navigation screen for folder selection on phones. Tablet-specific behavior is out of scope.
- Show server context, the current path, clickable breadcrumbs, and an Up action.
- Tapping a folder row opens it. Keep the folder list scrollable and the bottom Add this folder action fixed.
- Adding the current folder closes the browser and displays its path in the library form.
- Remember the last location within the current create/edit session, including rotation. Start new sessions at the server root.
- Make Browse server folders the primary action. Reveal manual path entry through Enter path manually.
- Do not include filtering in the first version.
- Back navigates up; the close action dismisses the browser.
- Keep loading and retry states inside the browser. Empty listings use No folders returned because the server can hide directory-read failures.

- Make the initial listing non-selectable on both POSIX and Windows servers. Users must open a folder or drive before adding it. Manual entry remains available for explicitly entering `/`.
- Disable Add this folder for duplicate or overlapping draft paths, using the existing validation rules. Explain the conflict with the relevant existing path; keep navigation available.

## Implementation considerations

- The server returns immediate child directories. Windows paths use forward slashes.
- Preserve server paths and navigation history without relying on Android device filesystem APIs.
- Cancel or invalidate pending requests on dismissal or subsequent navigation so late results cannot reopen the browser or replace newer results.
- Reuse existing draft path validation for duplicates and ancestor/descendant overlaps.
- Verify navigation, selection, rotation, request dismissal, loading/error states, and both server path styles.

## Validation

- Core UI unit tests passed, including folder navigation memory, Windows drive navigation, overlap rejection, and dismissal during a pending request.
- Library administration UI tests passed on the phone emulator, including the separate browser destination, scrolling, empty-folder selection, and disabled root/overlap selection.
- The browser shares the create/edit flow ViewModel to retain the session across rotation; physical rotation was not separately exercised.
