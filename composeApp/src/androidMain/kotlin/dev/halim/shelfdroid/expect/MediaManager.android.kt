package dev.halim.shelfdroid.expect


import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import dev.halim.shelfdroid.ContextUtils

actual class MediaManager {
    private var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession

    init {
        val context = ContextUtils.context
        exoPlayer = ExoPlayer.Builder(context).build()
        mediaSession = MediaSession.Builder(context, exoPlayer).build()
    }

    actual fun addItem(url: String, target: Long) {
        val mediaItem = ExoMediaItem.fromUri(url)
        exoPlayer.addMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(target)
    }

    actual fun currentItem(): MediaItem? {
        return exoPlayer.currentMediaItem?.let { MediaItem(it) }
    }

    actual fun play() {
        exoPlayer.play()
    }

    actual fun pause() {
        exoPlayer.pause()
    }

    actual fun release() {
        exoPlayer.release()
    }

    actual fun isPlaying():Boolean = exoPlayer.isPlaying
}

actual class MediaItem(val mediaItem: ExoMediaItem)