package dev.halim.shelfdroid.expect

expect class MediaItem
expect class MediaManager(){
    fun addItem(url: String, target: Long)
    fun currentItem(): MediaItem?
    fun play()
    fun pause()
    fun release()
    fun isPlaying(): Boolean
}

