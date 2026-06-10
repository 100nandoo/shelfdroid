# Session Close API Findings

## Overview

This note documents how `/api/session/<ID>/close` is used in the Audiobookshelf mobile app so the same behavior can be implemented in another app without copying the wrong lifecycle.

## Endpoint Shape

- Android sends `POST /api/session/<ID>/close` with no request body.
- The request uses the shared bearer-token request helper.
- Source:
  - `android/app/src/main/java/com/audiobookshelf/app/server/ApiHandler.kt:73`
  - `android/app/src/main/java/com/audiobookshelf/app/server/ApiHandler.kt:718`

## Android Call Path

- The only direct caller of the close endpoint is `PlayerNotificationService.closePlayback()`.
- That method calls `apiHandler.closePlaybackSession(currentSessionId, config)`.
- The endpoint is only called for non-local sessions with a non-empty session id.
- Source:
  - `android/app/src/main/java/com/audiobookshelf/app/player/PlayerNotificationService.kt:1007`
  - `android/app/src/main/java/com/audiobookshelf/app/server/ApiHandler.kt:718`

## When It Fires

These paths all funnel into the same Android close flow:

- User taps the player close action in `components/app/AudioPlayer.vue:772`.
- The Vue player component is destroyed while a playback session is still active in `components/app/AudioPlayer.vue:985`.
- The Capacitor bridge forwards `AbsAudioPlayer.closePlayback()` to the native player service in `android/app/src/main/java/com/audiobookshelf/app/plugins/AbsAudioPlayer.kt:365`.
- Android media STOP / headset STOP calls `playerNotificationService.closePlayback()` in `android/app/src/main/java/com/audiobookshelf/app/player/MediaSessionCallback.kt:236`.
- Playback error shutdown calls `closePlayback(true)` in `android/app/src/main/java/com/audiobookshelf/app/player/PlayerNotificationService.kt:597`.

## Close Sequence

### Normal shutdown

For a normal remote-session shutdown, Android does this:

1. Stop the media progress syncer.
2. Do one last `/api/session/<ID>/sync` when current time is available.
3. Call `/api/session/<ID>/close`.
4. Stop and clear the player, reset local session state, and emit `onPlaybackClosed`.

Relevant sources:

- `android/app/src/main/java/com/audiobookshelf/app/player/PlayerNotificationService.kt:1013`
- `android/app/src/main/java/com/audiobookshelf/app/media/MediaProgressSyncer.kt:114`
- `android/app/src/main/java/com/audiobookshelf/app/media/MediaProgressSyncer.kt:299`

### Error shutdown

For `closePlayback(true)`, Android skips the final sync and still calls `/close`.

Relevant sources:

- `android/app/src/main/java/com/audiobookshelf/app/player/PlayerNotificationService.kt:1016`
- `android/app/src/main/java/com/audiobookshelf/app/player/PlayerNotificationService.kt:597`

## What Does Not Call It

- Starting a new server playback item does not call `/close`.
- That path stops the existing sync flow and starts a new `/play` session instead.
- Source:
  - `android/app/src/main/java/com/audiobookshelf/app/plugins/AbsAudioPlayer.kt:264`

## Platform Differences

### Android

- Calls `/api/session/<ID>/close` on final teardown of a remote playback session.

### iOS

- Does not call `/close` anywhere in the current codebase.
- `closePlayback()` stops playback through `PlayerHandler.stopPlayback()`.
- Progress is synced through `/api/session/<ID>/sync`, but there is no `/close` request.
- Source:
  - `ios/App/App/plugins/AbsAudioPlayer.swift:129`
  - `ios/App/Shared/player/PlayerHandler.swift:30`
  - `ios/App/Shared/player/PlayerProgress.swift:22`
  - `ios/App/Shared/util/ApiClient.swift:504`

### Web

- Does not call `/close`.
- The web plugin clears local player state and emits a close event only.
- Source:
  - `plugins/capacitor/AbsAudioPlayer.js:118`

## Implementation Guidance

If you want to mirror the Audiobookshelf Android behavior in another app:

- Call `/api/session/<ID>/close` only on final teardown of a remote playback session.
- Skip it for local-only sessions.
- Try to send a final `/sync` before `/close` during normal shutdown.
- If shutdown happens during an error path, it is acceptable to skip the final sync and still call `/close`.
- Do not treat “start playback for another item” as a close event unless your own session model requires that.
- Make the close endpoint idempotent or otherwise tolerant of repeated calls, because the client treats it as a fire-and-forget cleanup request.
