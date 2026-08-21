# Library Item Metadata Utilities

**Triage:** ready-for-agent

## Problem Statement

ShelfDroid administrators must use the Audiobookshelf web client to perform server-wide metadata maintenance. They cannot manage the **Tags** and **Genres** shared by many **Books** and **Podcasts**, or manage **Custom metadata providers**, from the Android app.

This leaves a gap in ShelfDroid's server-administration experience and risks confusing global maintenance with the existing editor for one **Library item**. Renaming or deleting a Tag or Genre changes every matching Library item on the Audiobookshelf server. Deleting a Custom metadata provider returns affected Libraries to the server's default metadata source.

## Solution

Add an administrator-only **Library item metadata utilities** hub in ShelfDroid's server-administration area. The hub leads to separate Tags, Genres, and Custom metadata providers screens that mirror the Audiobookshelf server contract.

Tag and Genre screens load, rename, and delete values with clear warnings and confirmation for every server-wide mutation. The provider screen lists, adds, reveals or hides authorization headers through a password field, and deletes Book metadata providers. ShelfDroid keeps the server authoritative for authorization, merge outcomes, and final data.

## User Stories

1. As an Audiobookshelf admin, I want to find **Library item metadata utilities** in ShelfDroid's server-administration area, so that I can manage global metadata without using the web client.
2. As an Audiobookshelf root User, I want the same utility access as an admin, so that I can administer the server I own.
3. As a non-admin User, I want the utilities hidden from navigation, so that I am not offered actions the server will reject.
4. As an administrator whose role was revoked, I want an access-denied result handled clearly, so that stale local role state does not imply authorization.
5. As an administrator, I want a hub that separates Tags, Genres, and Custom metadata providers, so that I understand which server-wide concept I am changing.
6. As an administrator, I want the feature to remain separate from editing one **Library item**, so that I do not mistake a bulk mutation for a single Book or Podcast edit.
7. As an administrator, I want to load all server Tags, so that I can review the values used across the Catalog.
8. As an administrator, I want Tags shown in case-insensitive alphabetical order, so that I can find a value quickly.
9. As an administrator, I want an empty Tag state, so that I can distinguish a successful empty response from loading or failure.
10. As an administrator, I want to rename a Tag, so that I can correct or consolidate its wording across matching Books and Podcasts.
11. As an administrator, I want blank Tag names rejected before a request is made, so that I do not send an invalid server mutation.
12. As an administrator, I want an exact existing Tag target identified as a merge, so that I understand the result before confirming it.
13. As an administrator, I want a case-only Tag collision called out, so that I can decide deliberately whether to proceed.
14. As an administrator, I want every Tag rename confirmed as a change to all matching Library items, so that I do not make a broad change by accident.
15. As an administrator, I want to delete a Tag only after confirmation that it will be removed from all matching Library items, so that the destructive effect is explicit.
16. As an administrator, I want the completed Tag action to report the server's updated-item count, so that I know the scope of the completed mutation.
17. As an administrator, I want the Tag list and ShelfDroid's administrative Tag cache refreshed after a Tag mutation, so that later access-management work does not use stale values.
18. As an administrator, I want a failed Tag operation to preserve the current list and explain that it failed, so that I can retry without losing context.
19. As an administrator, I want to load all server Genres, so that I can review the classifications used across the Catalog.
20. As an administrator, I want to rename or delete a Genre using the same deliberate merge warnings, server-wide confirmations, and updated-item result as for Tags, so that bulk classification changes are predictable.
21. As an administrator, I want Genre names containing spaces, punctuation, or non-ASCII text handled correctly during deletion, so that a valid value is never targeted incorrectly.
22. As an administrator, I want to view the server's Custom metadata providers, so that I know which external Book metadata sources are available.
23. As an administrator, I want a clear empty-provider state, so that I know when no Custom metadata providers are configured.
24. As an administrator, I want to add a Custom metadata provider with a name, URL, optional authorization header, and the supported Book media type, so that the Audiobookshelf server can use a new metadata source.
25. As an administrator, I want required provider fields validated before submission and server validation errors presented clearly, so that I can correct a provider definition.
26. As an administrator, I want authorization-header entry to use a password field with an eye toggle, so that I can conceal it by default and verify it deliberately when needed.
27. As an administrator, I want stored provider authorization headers concealed by default with an eye toggle, so that shoulder-surfing is reduced while I can still inspect the value when authorized.
28. As an administrator, I want the app to avoid persisting or logging provider authorization headers, so that secrets do not outlive the active management screen.
29. As an administrator, I want to delete a Custom metadata provider only after confirmation that Libraries using it will fall back to the server's Google metadata source, so that I understand the downstream effect.
30. As an administrator, I want provider deletion to report success or failure without claiming an affected-Library count the server does not provide, so that the result is accurate.
31. As an administrator, I want only the provider operations supported by Audiobookshelf—list, add, and delete—so that ShelfDroid does not present an unsafe fake edit flow.
32. As an administrator, I want loading and mutation progress to prevent overlapping actions on the same screen, so that conflicting server-wide requests do not race.
33. As an administrator, I want server and network failures presented with operation-specific recovery context, so that I know whether to retry loading, renaming, deleting, or creating.
34. As a TalkBack user, I want every input, destructive action, confirmation, and reveal or hide control to have an accessible label and state, so that I can administer metadata independently.
35. As an administrator, I want leaving a management screen to discard transient authorization-header values and reset reveal state, so that sensitive data is not retained longer than necessary.

## Implementation Decisions

- Add a dedicated admin/root navigation destination under the existing server-administration grouping. It opens a **Library item metadata utilities** hub with three child destinations: Tags, Genres, and Custom metadata providers.
- Use the local administrative-role check only to control navigation and avoid unnecessary loads. The Audiobookshelf server remains authoritative; every request handles 403 as access denial.
- Keep this feature distinct from the existing single-**Library item** editor. It will not reuse that editor's save request or presentation.
- Introduce one feature-oriented, domain-level repository as the primary seam for loading and mutating the three metadata-utility domains. It owns remote coordination and exposes operation outcomes needed by the screens.
- Retain the existing **Tag** repository as the owner of ShelfDroid's administrative Tag cache. A successful Tag rename or deletion refreshes that cache from the server before the feature reports completion.
- Add dedicated API methods and wire models for Tag and Genre lists, rename responses, mutation counts, Custom metadata provider lists, provider creation, and provider deletion. Reuse an existing model only where its complete contract already matches.
- Call Audiobookshelf's Tag list, rename, and delete endpoints and the corresponding Genre endpoints. Encode Tag and Genre delete values as UTF-8 standard Base64 followed by URI escaping; never insert raw values into a path segment.
- Trim rename input and reject blank values locally. Detect exact and case-only collisions from the loaded list, display the appropriate warning, and send the requested name unchanged when the administrator confirms. Audiobookshelf determines whether the final operation merges values.
- Require confirmation before every Tag or Genre rename or delete. Confirmation identifies the current and target values when applicable and states that all matching Books and Podcasts are affected. The app will not invent a pre-action item count because the server exposes only the completed count.
- Treat the returned updated-item count as the authoritative success result. After successful mutations, refresh the corresponding list so the screen reflects server-canonical values.
- Call Audiobookshelf's Custom metadata provider list, creation, and deletion endpoints. Provider creation always submits the server-supported Book media type.
- The provider screen supports list, add, and delete only. It does not offer edit or simulate edit through delete-and-recreate, because deletion changes dependent Library configuration.
- Provider creation uses password-style authorization-header input with an explicit, accessible eye toggle. Provider list rows render an authorization header in a read-only password field with a per-row eye toggle, concealed by default.
- Authorization-header values remain in active screen memory only. They are never written to a database, preferences, navigation payload, analytics, or logs; they are cleared and re-concealed when the screen leaves composition.
- Provider creation validates locally required name and URL values, submits the server contract, and refreshes or reconciles the visible list only after server success.
- Provider deletion requires an explicit destructive confirmation naming the provider and explaining that Libraries using it fall back to Audiobookshelf's Google metadata source. The UI does not claim an affected-Library count because the server does not return one.
- Each child screen models loading, ready, empty, mutating, access-denied, and recoverable failure behavior. A mutation blocks conflicting actions on that child screen while it is running.
- Reuse established Compose confirmation, progress, error, navigation, and accessible password-field patterns. No new UI framework or cryptographic storage is introduced.
- Update the fake authenticated API implementation alongside the production API contract, so app-level tests can exercise all supported utility operations.

## Testing Decisions

- Tests assert externally visible behavior and HTTP contracts, not private Compose structure, ViewModel implementation details, or pixel positions.
- The primary test seam is the new metadata-utilities repository backed by a controlled HTTP server and current-User state. This single highest-level data seam verifies authorization, URL resolution, request serialization, responses, mutation outcomes, and failure behavior together.
- Primary-seam tests cover admin and root access, locally known non-admin no-request behavior, and server 403 access denial for every utility family.
- Primary-seam tests cover Tag and Genre list loading, standard-Base64-plus-URI path encoding, rename request bodies, merge results, updated-item counts, list reloads, and request failures.
- Primary-seam tests cover provider list, creation, deletion, supported Book media type, validation failures returned by the server, and successful and failed outcomes.
- A focused integration test verifies that successful Tag rename and deletion refresh the existing administrative Tag cache, while failed operations leave its canonical contents unchanged.
- Focused screen or ViewModel tests cover empty, loading, mutating, failure, and access-denied states; admin-only entry visibility; confirmation text; collision warnings; and disabling conflicting actions during mutations.
- Focused UI tests cover password masking by default, accessible reveal or hide behavior, automatic re-concealment, and clearing sensitive provider-header state when leaving the screen.
- UI tests assert labels, actions, messages, and accessibility semantics rather than exact layout hierarchy. Existing Compose preview screenshot infrastructure may cover stable loading, empty, ready, error, and confirmation states where that adds coverage beyond interaction tests.
- App-level tests use the extended fake API to verify the full navigation and screen behavior without a live Audiobookshelf server.
- Manual verification covers admin, root, regular User, guest, stale-role 403, a server hosted under a URL subpath, exact and case-only merge warnings, Unicode Tag or Genre deletion, provider authorization-header reveal or hide, and provider-delete fallback disclosure.

## Out of Scope

- Editing metadata for a single **Library item**; the existing Book and Podcast editor remains unchanged.
- Creating standalone Tags or Genres; Audiobookshelf derives them from Library-item metadata and exposes only list, rename, and delete management operations.
- Editing an existing Custom metadata provider or adding a delete-and-recreate substitute.
- Managing metadata providers for media types other than Books.
- Changing Audiobookshelf server endpoints, adding server-side previews or affected-Library counts, or changing the provider fallback behavior.
- Persisting, copying, sharing, exporting, or logging Custom metadata provider authorization headers.
- Offline mutation queues or local ownership of server metadata utilities.
- A full migration of other admin destinations to any new access guard introduced for this feature.
- Screen-capture or recent-app-preview protection beyond the concealment and in-memory lifetime of provider headers.

## Further Notes

The design follows ShelfDroid's existing dedicated admin-screen pattern and its domain-oriented data-layer direction. It does not require a new ADR because it extends established server-administration navigation and API-boundary practice.

The active Audiobookshelf server contract is the authority for access control, merge behavior, metadata-file updates, and the provider-deletion fallback. ShelfDroid's role is to present those effects accurately, make destructive actions deliberate, and keep provider credentials confined to the active UI.

