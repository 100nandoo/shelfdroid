package dev.halim.shelfdroid.core.ui.preview

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.text.CueGroup.EMPTY_TIME_ZERO
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi

@UnstableApi
class FakePlayer : Player {
  override fun getApplicationLooper(): Looper = Looper.getMainLooper()

  override fun addListener(listener: Player.Listener) {}

  override fun removeListener(listener: Player.Listener) {}

  override fun getPlaybackState(): Int = Player.STATE_READY

  override fun isPlaying(): Boolean = true

  override fun getDuration(): Long = 3 * 60 * 1000L // 3 minutes

  override fun getCurrentPosition(): Long = 60 * 1000L // 1 minute

  override fun getBufferedPosition(): Long = 90 * 1000L

  override fun getBufferedPercentage(): Int = 50

  override fun getTotalBufferedDuration(): Long = 30 * 1000L

  override fun play() {}

  override fun pause() {}

  override fun seekTo(positionMs: Long) {}

  override fun stop() {}

  override fun release() {}

  override fun prepare() {}

  override fun getPlaybackParameters(): PlaybackParameters = PlaybackParameters.DEFAULT

  override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {}

  override fun setPlaybackSpeed(speed: Float) {}

  override fun getPlayWhenReady(): Boolean = true

  override fun setPlayWhenReady(playWhenReady: Boolean) {}

  override fun getRepeatMode(): Int = Player.REPEAT_MODE_OFF

  override fun setRepeatMode(repeatMode: Int) {}

  override fun getShuffleModeEnabled(): Boolean = false

  override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {}

  override fun isLoading(): Boolean = false

  override fun getCurrentMediaItem(): MediaItem? = null

  override fun getCurrentMediaItemIndex(): Int = 0

  override fun getMediaItemCount(): Int = 0

  override fun getMediaItemAt(index: Int): MediaItem = MediaItem.Builder().build()

  override fun getCurrentTimeline(): Timeline = Timeline.EMPTY

  override fun getMediaMetadata(): MediaMetadata = MediaMetadata.Builder().build()

  override fun getPlaylistMetadata(): MediaMetadata = MediaMetadata.Builder().build()

  override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {}

  override fun getCurrentManifest(): Any? {
    return null
  }

  override fun getCurrentTracks(): Tracks = Tracks.EMPTY

  override fun getTrackSelectionParameters(): TrackSelectionParameters =
    TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT

  override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {}

  override fun getPlayerError(): PlaybackException? = null

  override fun isPlayingAd(): Boolean = false

  override fun getCurrentAdGroupIndex(): Int = 0

  override fun getCurrentAdIndexInAdGroup(): Int = 0

  override fun getCurrentCues(): CueGroup = EMPTY_TIME_ZERO

  override fun getDeviceInfo(): DeviceInfo =
    DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build()

  override fun getDeviceVolume(): Int = 0

  override fun isDeviceMuted(): Boolean = false

  override fun setDeviceVolume(volume: Int) {}

  override fun setDeviceVolume(volume: Int, flags: Int) {}

  override fun increaseDeviceVolume() {}

  override fun increaseDeviceVolume(flags: Int) {}

  override fun decreaseDeviceVolume() {}

  override fun decreaseDeviceVolume(flags: Int) {}

  override fun setDeviceMuted(muted: Boolean) {}

  override fun setDeviceMuted(muted: Boolean, flags: Int) {}

  override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {}

  override fun getAudioAttributes(): AudioAttributes = AudioAttributes.DEFAULT

  override fun setVolume(volume: Float) {}

  override fun getVolume(): Float {
    return 0.5f
  }

  override fun clearVideoSurface() {}

  override fun clearVideoSurface(surface: Surface?) {}

  override fun setVideoSurface(surface: Surface?) {}

  override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}

  override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {}

  override fun setVideoSurfaceView(surfaceView: SurfaceView?) {}

  override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {}

  override fun setVideoTextureView(textureView: TextureView?) {}

  override fun clearVideoTextureView(textureView: TextureView?) {}

  override fun getVideoSize(): VideoSize = VideoSize.UNKNOWN

  override fun getSurfaceSize(): Size = Size(0, 0)

  override fun setMediaItems(mediaItems: List<MediaItem>) {}

  override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {}

  override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {}

  override fun setMediaItem(mediaItem: MediaItem) {}

  override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {}

  override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {}

  override fun addMediaItem(mediaItem: MediaItem) {}

  override fun addMediaItem(index: Int, mediaItem: MediaItem) {}

  override fun addMediaItems(mediaItems: List<MediaItem>) {}

  override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {}

  override fun moveMediaItem(currentIndex: Int, newIndex: Int) {}

  override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {}

  override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {}

  override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {}

  override fun removeMediaItem(index: Int) {}

  override fun removeMediaItems(fromIndex: Int, toIndex: Int) {}

  override fun clearMediaItems() {}

  override fun isCommandAvailable(command: Int): Boolean = false

  override fun canAdvertiseSession(): Boolean = false

  override fun getAvailableCommands(): Player.Commands = Player.Commands.EMPTY

  override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE

  override fun seekToDefaultPosition() {}

  override fun seekToDefaultPosition(mediaItemIndex: Int) {}

  override fun seekTo(mediaItemIndex: Int, positionMs: Long) {}

  override fun getSeekBackIncrement(): Long = 5000L

  override fun seekBack() {}

  override fun getSeekForwardIncrement(): Long = 5000L

  override fun seekForward() {}

  override fun hasPreviousMediaItem(): Boolean = false

  override fun seekToPreviousWindow() {}

  override fun seekToPreviousMediaItem() {}

  override fun getMaxSeekToPreviousPosition(): Long = 0L

  override fun seekToPrevious() {}

  override fun hasNext(): Boolean = false

  override fun hasNextWindow(): Boolean = false

  override fun hasNextMediaItem(): Boolean = false

  override fun next() {}

  override fun seekToNextWindow() {}

  override fun seekToNextMediaItem() {}

  override fun seekToNext() {}

  override fun getCurrentPeriodIndex(): Int = 0

  override fun getCurrentWindowIndex(): Int = 0

  override fun getNextWindowIndex(): Int = 0

  override fun getNextMediaItemIndex(): Int = 0

  override fun getPreviousWindowIndex(): Int = 0

  override fun getPreviousMediaItemIndex(): Int = 0

  override fun getContentDuration(): Long = getDuration()

  override fun getContentPosition(): Long = getCurrentPosition()

  override fun getContentBufferedPosition(): Long = getBufferedPosition()

  override fun isCurrentWindowDynamic(): Boolean = false

  override fun isCurrentMediaItemDynamic(): Boolean = false

  override fun isCurrentWindowLive(): Boolean = false

  override fun isCurrentMediaItemLive(): Boolean = false

  override fun getCurrentLiveOffset(): Long = 0L

  override fun isCurrentWindowSeekable(): Boolean = true

  override fun isCurrentMediaItemSeekable(): Boolean = true
}
