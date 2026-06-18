# ShelfDroid

ShelfDroid is an Android client for an Audiobookshelf server. Its context is browsing, playing, downloading, and administering audiobook and podcast media from that server.

## Language

### Media catalog

**Audiobookshelf server**:
The remote server that stores media, metadata, users, and administrative settings for ShelfDroid.
_Avoid_: Backend, API

**Library**:
A server-managed collection of media of a single primary kind, exposed in ShelfDroid as either a book library or a podcast library.
_Avoid_: Shelf, folder

**Library item**:
A single media entry inside a library that ShelfDroid can open, play, edit, or download files from.
_Avoid_: Item, media object

**Book**:
A library item representing an audiobook, potentially as a single track or multiple tracks.
_Avoid_: Book file, title

**Podcast**:
A library item representing a podcast feed that contains episodes.
_Avoid_: Show, channel

**Episode**:
A playable unit inside a podcast.
_Avoid_: Track, chapter

**Episode published date**:
The date on which a podcast **Episode** was published.
_Avoid_: pubDate, published timestamp

**Episode update cutoff**:
The date and time after which the Audiobookshelf server should look for new **Episodes** when running an **Episode update check** for a **Podcast**.
_Avoid_: lastEpisodeCheck, last checked

**Chapter**:
A named segment inside a book.
_Avoid_: Track, episode

**Library file**:
An individual file attached to a library item, such as an audio file, cover-related file, or other stored asset.
_Avoid_: Download, attachment

### Playback and access

**Track**:
An individual downloadable or playable audio file belonging to a book.
_Avoid_: Chapter, episode

**Download**:
A local copy of server media or a server-exported file that ShelfDroid stores on the Android device for offline access.
_Avoid_: Stream, library file

**Book download batch**:
One user-initiated download of a **Book**, which may enqueue multiple **Track** downloads but should be presented as a single book-scoped download operation in the UI and notifications, with progress represented at the batch level such as completed tracks over total tracks.
_Avoid_: Track download, file batch

**Progress**:
The listener's current completion state for a playable media unit.
_Avoid_: Position, status

**Current playback**:
The locally active playable media context inside ShelfDroid, derived from on-device player state and used for now-playing UI. It is not the same as an **Open session**, which is server-tracked by the Audiobookshelf server.
_Avoid_: Open session, now playing state

**Listening session**:
A recorded playback session reported by the server for a user, device, item, and time range.
_Avoid_: Session, player state

**Open session**:
An active server-tracked playback session for a user on a specific client, where one server may hold many open sessions and one user may own more than one of them concurrently.
_Avoid_: Listening session, current player

### Administration

**Backup**:
A server backup artifact that can be created, downloaded, uploaded, restored, or deleted from ShelfDroid.
_Avoid_: Export, snapshot

**User**:
An Audiobookshelf account that can sign in to the server and consume or administer media through ShelfDroid.
_Avoid_: Listener, account

**API key**:
A server credential owned by a user and managed separately from username-password login.
_Avoid_: Token, session

**Server settings**:
The editable Audiobookshelf server configuration exposed through ShelfDroid administrative screens.
_Avoid_: Preferences, app settings

**Episode update check**:
An administrative action that asks the Audiobookshelf server to look for new **Episodes** for a **Podcast**, using an **Episode update cutoff** and a requested maximum number of new episodes to download.
_Avoid_: Add episode, refresh feed, sync podcast

## Relationships

- An **Audiobookshelf server** contains one or more **Libraries**
- A **Library** contains many **Library items**
- A **Library item** is either a **Book** or a **Podcast**
- A **Podcast** contains many **Episodes**
- A **Book** may contain many **Chapters**
- A **Book** may be backed by one or more **Tracks**
- A **Library item** may have one or more **Library files**
- A **Download** is derived from server media or a server file and belongs to a device, not to the server catalog
- A **Book download batch** belongs to one **Book** and may enqueue one or more **Track** downloads
- A **Listening session** belongs to one **User** and one server-reported media context
- An **Open session** is the currently active subset of **Listening sessions**
- One **Audiobookshelf server** may have many **Open sessions** at the same time
- One **User** may have multiple **Open sessions** at the same time across clients
- A **Backup** belongs to the **Audiobookshelf server**, not to a specific **Library**
- An **API key** belongs to exactly one **User**
- **Server settings** belong to the **Audiobookshelf server**

## Example dialogue

> **Dev:** "When a user opens a podcast, are they looking at a library item or at episodes directly?"
> **Domain expert:** "They open a **Podcast**, which is a **Library item**; from there they browse its **Episodes**."
>
> **Dev:** "And for audiobooks, should I treat each chapter as a downloadable unit?"
> **Domain expert:** "No. A **Book** may contain **Chapters** for navigation, but downloads are tied to **Tracks** or other **Library files**."
>
> **Dev:** "So a currently playing device entry belongs under open sessions, not backup or progress history?"
> **Domain expert:** "Correct. An **Open session** is active now; the **Audiobookshelf server** may have many of them at once, and one **User** may have several across clients, while a **Listening session** is the recorded playback history."

## Flagged ambiguities

- "item" is too vague on its own in this repo; prefer **Library item** when referring to a server catalog entry.
- "session" is overloaded; use **Listening session** for historical playback records and **Open session** for active server-tracked playback, where the server may hold many concurrent sessions and a user may have more than one.
- "current playback" and **Open session** are distinct; **Current playback** is local player state inside ShelfDroid, while an **Open session** is server-tracked.
- "track", "chapter", and "episode" are distinct; a **Track** is a file unit for books, a **Chapter** is a navigation segment in a book, and an **Episode** belongs to a podcast.
- "download" and **Library file** are not the same thing; a **Library file** exists on the server, while a **Download** is a local device copy or downloaded server artifact.
