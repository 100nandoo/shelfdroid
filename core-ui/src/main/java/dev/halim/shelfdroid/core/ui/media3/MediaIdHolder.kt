package dev.halim.shelfdroid.core.ui.media3

import dev.halim.shelfdroid.core.ui.navigation.MediaIdWrapper

object MediaIdHolder {
  var mediaIdWrapper: MediaIdWrapper? = null
    private set

  fun setMediaId(mediaId: String) {
    mediaIdWrapper = MediaIdWrapper.fromMediaId(mediaId)
  }

  fun reset() {
    mediaIdWrapper = null
  }
}
