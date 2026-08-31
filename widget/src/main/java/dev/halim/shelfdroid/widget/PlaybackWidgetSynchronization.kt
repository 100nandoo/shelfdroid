package dev.halim.shelfdroid.widget

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.media.presentation.PlaybackPresentationObserver
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Singleton
internal class PlaybackWidgetPresentationObserver
@Inject
constructor(private val refreshRequester: PlaybackWidgetRefreshRequester) :
  PlaybackPresentationObserver {
  override suspend fun onPlaybackPresentationChanged() {
    refreshRequester.requestAllInstancesRefresh()
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackWidgetSynchronizationModule {

  @Binds
  abstract fun bindPlaybackWidgetRefreshRequester(
    requester: GlancePlaybackWidgetRefreshRequester
  ): PlaybackWidgetRefreshRequester

  @Binds
  @IntoSet
  abstract fun bindPlaybackWidgetPresentationObserver(
    observer: PlaybackWidgetPresentationObserver
  ): PlaybackPresentationObserver
}

@Singleton
class PlaybackWidgetSynchronization
@Inject
internal constructor(
  settingsRepository: SettingsRepository,
  private val observer: PlaybackWidgetPresentationObserver,
  @param:Named("io") private val applicationScope: CoroutineScope,
) {
  private val themePreferences =
    settingsRepository.darkMode.combine(
      settingsRepository.dynamicTheme,
      ::PlaybackWidgetThemePreferences,
    )
  private var themeObserverJob: Job? = null

  fun start() {
    if (themeObserverJob?.isActive == true) return
    themeObserverJob =
      applicationScope.launch {
        observePlaybackWidgetThemeChanges(themePreferences, observer)
      }
  }
}

internal suspend fun observePlaybackWidgetThemeChanges(
  preferences: Flow<PlaybackWidgetThemePreferences>,
  observer: PlaybackPresentationObserver,
) {
  preferences.distinctUntilChanged().collect {
    observer.onPlaybackPresentationChanged()
  }
}
