package dev.halim.shelfdroid.media.di

import com.google.common.util.concurrent.SettableFuture
import org.junit.Test

class MediaControllerManagerTest {
  @Test
  fun clear_and_stop_before_init_does_not_crash() {
    val manager = MediaControllerManager(SettableFuture.create())

    manager.clearAndStop()
  }
}
