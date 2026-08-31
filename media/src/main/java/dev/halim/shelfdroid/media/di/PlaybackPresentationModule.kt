package dev.halim.shelfdroid.media.di

import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.media.presentation.PlaybackPresentationObserver

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackPresentationModule {

  @Multibinds
  abstract fun playbackPresentationObservers(): Set<PlaybackPresentationObserver>
}
