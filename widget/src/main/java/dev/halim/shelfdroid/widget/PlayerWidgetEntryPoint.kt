package dev.halim.shelfdroid.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.media.playback.PlayerStore

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PlayerWidgetEntryPoint {
  fun playerStore(): PlayerStore
}

internal fun playerWidgetStore(context: Context): PlayerStore =
  EntryPointAccessors.fromApplication(
      context.applicationContext,
      PlayerWidgetEntryPoint::class.java,
    )
    .playerStore()
