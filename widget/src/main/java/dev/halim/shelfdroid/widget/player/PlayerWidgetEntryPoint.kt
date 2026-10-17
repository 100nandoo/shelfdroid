package dev.halim.shelfdroid.widget.player

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.media.playback.PlayerStore

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PlayerWidgetEntryPoint {
  fun dataStoreManager(): DataStoreManager

  fun playerStore(): PlayerStore
}

internal fun playerWidgetStore(context: Context): PlayerStore =
  playerWidgetEntryPoint(context).playerStore()

internal fun playerWidgetDataStoreManager(context: Context): DataStoreManager =
  playerWidgetEntryPoint(context).dataStoreManager()

private fun playerWidgetEntryPoint(context: Context): PlayerWidgetEntryPoint =
  EntryPointAccessors.fromApplication(
    context.applicationContext,
    PlayerWidgetEntryPoint::class.java,
  )
