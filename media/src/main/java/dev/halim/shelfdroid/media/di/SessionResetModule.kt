package dev.halim.shelfdroid.media.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dev.halim.shelfdroid.core.data.sessionreset.CurrentPlaybackCleanup
import dev.halim.shelfdroid.media.sessionreset.MediaCurrentPlaybackCleanup

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class SessionResetModule {

  @Binds
  abstract fun bindCurrentPlaybackCleanup(
    cleanup: MediaCurrentPlaybackCleanup
  ): CurrentPlaybackCleanup
}
