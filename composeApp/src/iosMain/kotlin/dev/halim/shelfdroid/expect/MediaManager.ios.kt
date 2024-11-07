package dev.halim.shelfdroid.expect

actual class MediaManager {
    actual fun addItem(url: String, target: Long) {
    }

    actual fun currentItem(): MediaItem? {
        return null
    }


    actual fun play() {
    }

    actual fun pause() {
    }

    actual fun release() {
    }

    actual fun isPlaying():Boolean = false
}

actual class MediaItem