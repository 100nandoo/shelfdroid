package dev.halim.shelfdroid.widget.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.media.service.PlayerStore
import dev.halim.shelfdroid.widget.R
import kotlinx.coroutines.flow.first
import okio.FileSystem

class PlaybackWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Single

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val dependencies =
      EntryPoints.get(context.applicationContext, PlaybackWidgetDependencies::class.java)
    val settingsRepository = dependencies.settingsRepository()
    val colorScheme =
      resolveColorScheme(
        context = context,
        darkMode = settingsRepository.darkMode.first(),
        dynamicTheme = settingsRepository.dynamicTheme.first(),
      )
    val model =
      createModel(
        context = context,
        playerStore = dependencies.playerStore(),
        imageLoader = dependencies.imageLoader(),
      )
    val openAppAction = actionStartActivity(openAppIntent(context))

    provideContent {
      Box(
        modifier =
          GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(colorScheme.surfaceContainerHigh))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (model == null) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = context.getString(R.string.playback_widget_empty_title),
              style = TextStyle(color = ColorProvider(colorScheme.onSurface)),
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            FilledButton(
              text = context.getString(R.string.playback_widget_open_app),
              onClick = openAppAction,
            )
          }
        } else {
          Column {
            Row(
              modifier = GlanceModifier.fillMaxWidth().clickable(openAppAction)
            ) {
              model.cover?.let { cover ->
                Image(
                  provider = ImageProvider(cover),
                  contentDescription = context.getString(R.string.playback_widget_cover_art),
                  modifier = GlanceModifier.size(64.dp),
                  contentScale = ContentScale.Crop,
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
              }
              Column {
                Text(
                  text = model.title,
                  maxLines = 2,
                  style =
                    TextStyle(
                      color = ColorProvider(colorScheme.onSurface),
                      fontSize = 16.sp,
                    ),
                )
                if (model.author.isNotBlank()) {
                  Spacer(modifier = GlanceModifier.height(4.dp))
                  Text(
                    text = model.author,
                    maxLines = 1,
                    style =
                      TextStyle(
                        color = ColorProvider(colorScheme.onSurfaceVariant),
                        fontSize = 13.sp,
                      ),
                  )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
                if (model.isLoading) {
                  Row {
                    CircularProgressIndicator(
                      modifier = GlanceModifier.size(18.dp),
                      color = ColorProvider(colorScheme.primary),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                      text = model.stateLabel,
                      style =
                        TextStyle(
                          color = ColorProvider(colorScheme.primary),
                          fontSize = 12.sp,
                        ),
                    )
                  }
                } else {
                  Text(
                    text = model.stateLabel,
                    style =
                      TextStyle(
                        color = ColorProvider(colorScheme.primary),
                        fontSize = 12.sp,
                      ),
                  )
                }
              }
            }
            Spacer(modifier = GlanceModifier.height(12.dp))
            Row {
              FilledButton(
                text = context.getString(R.string.playback_widget_seek_back),
                onClick = PlaybackWidgetTransportAction.actionFor(PlaybackTransportAction.SeekBack),
                enabled = model.seekBackEnabled,
              )
              Spacer(modifier = GlanceModifier.width(8.dp))
              FilledButton(
                text = model.playPauseLabel,
                onClick =
                  PlaybackWidgetTransportAction.actionFor(PlaybackTransportAction.PlayPause),
                enabled = model.playPauseEnabled,
              )
              Spacer(modifier = GlanceModifier.width(8.dp))
              FilledButton(
                text = context.getString(R.string.playback_widget_seek_forward),
                onClick =
                  PlaybackWidgetTransportAction.actionFor(PlaybackTransportAction.SeekForward),
                enabled = model.seekForwardEnabled,
              )
            }
          }
        }
      }
    }
  }

  private suspend fun createModel(
    context: Context,
    playerStore: PlayerStore,
    imageLoader: ImageLoader,
  ): PlaybackWidgetModel? {
    val uiState = playerStore.uiState.value
    if (uiState.id.isBlank() || uiState.state is PlayerState.Hidden) return null

    val stateLabel =
      when {
        uiState.playPause.showLoadingIndicator -> context.getString(R.string.playback_widget_loading)
        uiState.playPause.showPlayIcon -> context.getString(R.string.playback_widget_paused)
        else -> context.getString(R.string.playback_widget_playing)
      }

    return PlaybackWidgetModel(
      title = uiState.title,
      author = uiState.author,
      stateLabel = stateLabel,
      isLoading = uiState.playPause.showLoadingIndicator,
      playPauseLabel =
        if (uiState.playPause.showPlayIcon) context.getString(R.string.playback_widget_play)
        else context.getString(R.string.playback_widget_pause),
      playPauseEnabled = uiState.playPause.enabled,
      seekBackEnabled = uiState.seekControls.seekBackEnabled,
      seekForwardEnabled = uiState.seekControls.seekForwardEnabled,
      cover = loadCoverBitmap(context, imageLoader, uiState.cover),
    )
  }

  private suspend fun loadCoverBitmap(
    context: Context,
    imageLoader: ImageLoader,
    coverUrl: String,
  ): Bitmap? {
    if (coverUrl.isBlank()) return null
    readCachedBitmap(imageLoader, coverUrl)?.let { return it }

    imageLoader.execute(ImageRequest.Builder(context).data(coverUrl).build())
    return readCachedBitmap(imageLoader, coverUrl)
  }

  private fun readCachedBitmap(imageLoader: ImageLoader, coverUrl: String): Bitmap? {
    val artworkPath = imageLoader.diskCache?.openSnapshot(coverUrl)?.data ?: return null
    val bytes = FileSystem.SYSTEM.read(artworkPath) { readByteArray() }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return Bitmap.createScaledBitmap(bitmap, COVER_SIZE_PX, COVER_SIZE_PX, true)
  }

  private fun resolveColorScheme(
    context: Context,
    darkMode: Boolean,
    dynamicTheme: Boolean,
  ): ColorScheme {
    if (dynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (darkMode) darkColorScheme() else lightColorScheme()
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface PlaybackWidgetDependencies {
    fun imageLoader(): ImageLoader

    fun playerStore(): PlayerStore

    fun settingsRepository(): SettingsRepository
  }

  private data class PlaybackWidgetModel(
    val title: String,
    val author: String,
    val stateLabel: String,
    val isLoading: Boolean,
    val playPauseLabel: String,
    val playPauseEnabled: Boolean,
    val seekBackEnabled: Boolean,
    val seekForwardEnabled: Boolean,
    val cover: Bitmap?,
  )

  private companion object {
    const val COVER_SIZE_PX = 160
  }
}
