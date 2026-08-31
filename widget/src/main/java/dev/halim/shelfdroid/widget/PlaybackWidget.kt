package dev.halim.shelfdroid.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import dev.halim.shelfdroid.core.ui.R as CoreUiR
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_PLAYER
import dev.halim.shelfdroid.helper.Helper.Companion.EXTRA_MEDIA_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal val SmallPlaybackWidgetSize = DpSize(180.dp, 110.dp)
internal val LargePlaybackWidgetSize = DpSize(250.dp, 110.dp)

class PlaybackWidget : GlanceAppWidget() {
  override val sizeMode =
    SizeMode.Responsive(setOf(SmallPlaybackWidgetSize, LargePlaybackWidgetSize))

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val preferences = loadThemePreferences(context)
    val colors = playbackWidgetColorProviders(context, preferences)
    val presentation = playbackWidgetEntryPoint(context).presentationLoader().load()

    provideContent {
      GlanceTheme(colors = colors) {
        PlaybackWidgetContent(context, presentation)
      }
    }
  }
}

@Composable
private fun PlaybackWidgetContent(
  context: Context,
  presentation: PlaybackWidgetPresentation,
) {
  if (presentation == PlaybackWidgetPresentation.Empty) {
    EmptyPlaybackWidget(
      title = context.getString(R.string.playback_widget_empty_title),
      compactMessage = context.getString(R.string.playback_widget_empty_compact_message),
      expandedMessage = context.getString(R.string.playback_widget_empty_expanded_message),
      brandDescription = context.getString(R.string.playback_widget_brand_description),
      openDescription = context.getString(R.string.playback_widget_open_description),
    )
    return
  }

  val media =
    when (presentation) {
      is PlaybackWidgetPresentation.Active -> presentation.media
      is PlaybackWidgetPresentation.Error -> presentation.media
      PlaybackWidgetPresentation.Empty -> return
    }
  CurrentPlaybackWidget(
    media = media,
    isError = presentation is PlaybackWidgetPresentation.Error,
    controls = (presentation as? PlaybackWidgetPresentation.Active)?.controls,
    chapterControls =
      (presentation as? PlaybackWidgetPresentation.Active)?.chapterControls,
    artworkDescription =
      context.getString(R.string.playback_widget_artwork_description, media.mediaTitle),
    fallbackArtworkDescription =
      context.getString(
        R.string.playback_widget_artwork_fallback_description,
        media.mediaTitle,
      ),
    metadataDescription =
      context.getString(
        R.string.playback_widget_metadata_description,
        media.mediaTitle,
        media.playableTitle,
      ),
    openDescription =
      context.getString(R.string.playback_widget_now_playing_description, media.mediaTitle),
    recoveryLabel = context.getString(R.string.playback_widget_recovery_action),
    recoveryDescription = context.getString(R.string.playback_widget_recovery_description),
    playDescription = context.getString(R.string.playback_widget_play_description),
    pauseDescription = context.getString(R.string.playback_widget_pause_description),
    seekBackDescription = context.getString(R.string.playback_widget_seek_back_description),
    seekForwardDescription = context.getString(R.string.playback_widget_seek_forward_description),
    previousChapterDescription =
      context.getString(R.string.playback_widget_previous_chapter_description),
    nextChapterDescription =
      context.getString(R.string.playback_widget_next_chapter_description),
    unavailableDescription = context.getString(R.string.playback_widget_control_unavailable),
    openAction = openNowPlayingAction(context, media.mediaId),
  )
}

@Composable
internal fun CurrentPlaybackWidget(
  media: CurrentPlaybackMedia,
  isError: Boolean,
  controls: PrimaryPlaybackControls?,
  chapterControls: ChapterPlaybackControls?,
  artworkDescription: String,
  fallbackArtworkDescription: String,
  metadataDescription: String,
  openDescription: String,
  recoveryLabel: String,
  recoveryDescription: String,
  playDescription: String,
  pauseDescription: String,
  seekBackDescription: String,
  seekForwardDescription: String,
  previousChapterDescription: String,
  nextChapterDescription: String,
  unavailableDescription: String,
  openAction: Action,
) {
  val isLarge = LocalSize.current.width >= LargePlaybackWidgetSize.width
  val artworkSize = if (isLarge) 72.dp else 60.dp
  Row(
    modifier =
      GlanceModifier.fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(R.dimen.playback_widget_corner_radius)
        .clickable(openAction)
        .semantics {
          contentDescription = openDescription
          testTag = if (isError) ERROR_WIDGET_TEST_TAG else ACTIVE_WIDGET_TEST_TAG
        }
        .padding(horizontal = if (isLarge) 12.dp else 8.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.Start,
  ) {
    Image(
      provider =
        media.artwork?.let(::ImageProvider) ?: ImageProvider(R.drawable.widget_brand_headphones),
      contentDescription =
        if (media.artwork == null) fallbackArtworkDescription else artworkDescription,
      modifier =
        GlanceModifier.width(artworkSize)
          .height(artworkSize)
          .cornerRadius(8.dp)
          .clickable(openAction)
          .semantics {
            testTag = if (media.artwork == null) ARTWORK_FALLBACK_TEST_TAG else ARTWORK_TEST_TAG
          },
      contentScale = ContentScale.Crop,
    )
    if (isLarge) {
      Spacer(modifier = GlanceModifier.width(10.dp))
      Column(
        modifier =
          GlanceModifier.defaultWeight().clickable(openAction).semantics {
            contentDescription = metadataDescription
            testTag = METADATA_TEST_TAG
          },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = media.mediaTitle,
          style =
            TextStyle(
              color = GlanceTheme.colors.onSurface,
              fontFamily = FontFamily.Serif,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            ),
          maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
          text = media.playableTitle,
          style =
            TextStyle(
              color = GlanceTheme.colors.onSurfaceVariant,
              fontFamily = FontFamily.SansSerif,
              fontSize = 13.sp,
            ),
          maxLines = 1,
        )
        if (isError) {
          Spacer(modifier = GlanceModifier.height(6.dp))
          PlaybackRecoveryAffordance(
            label = recoveryLabel,
            description = recoveryDescription,
            openAction = openAction,
          )
        } else if (controls != null) {
          Spacer(modifier = GlanceModifier.height(4.dp))
          PlaybackControlRow(
            controls = controls,
            chapterControls = chapterControls,
            playDescription = playDescription,
            pauseDescription = pauseDescription,
            seekBackDescription = seekBackDescription,
            seekForwardDescription = seekForwardDescription,
            previousChapterDescription = previousChapterDescription,
            nextChapterDescription = nextChapterDescription,
            unavailableDescription = unavailableDescription,
          )
        }
      }
    } else if (isError) {
      Spacer(modifier = GlanceModifier.width(8.dp))
      PlaybackRecoveryAffordance(
        label = recoveryLabel,
        description = recoveryDescription,
        openAction = openAction,
      )
    } else if (controls != null) {
      Spacer(modifier = GlanceModifier.width(8.dp))
      PlaybackControlRow(
        controls = controls,
        chapterControls = null,
        playDescription = playDescription,
        pauseDescription = pauseDescription,
        seekBackDescription = seekBackDescription,
        seekForwardDescription = seekForwardDescription,
        previousChapterDescription = previousChapterDescription,
        nextChapterDescription = nextChapterDescription,
        unavailableDescription = unavailableDescription,
      )
    }
  }
}

@Composable
private fun PlaybackControlRow(
  controls: PrimaryPlaybackControls,
  chapterControls: ChapterPlaybackControls?,
  playDescription: String,
  pauseDescription: String,
  seekBackDescription: String,
  seekForwardDescription: String,
  previousChapterDescription: String,
  nextChapterDescription: String,
  unavailableDescription: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (chapterControls != null) {
      PlaybackControl(
        icon = CoreUiR.drawable.skip_previous,
        description = previousChapterDescription,
        unavailableDescription = unavailableDescription,
        enabled = chapterControls.previousEnabled,
        action = actionRunCallback<PreviousChapterPlaybackAction>(),
        testTag = PREVIOUS_CHAPTER_TEST_TAG,
      )
    }
    PlaybackControl(
      icon = CoreUiR.drawable.fast_rewind,
      description = seekBackDescription,
      unavailableDescription = unavailableDescription,
      enabled = controls.seekBackEnabled,
      action = actionRunCallback<SeekBackPlaybackAction>(),
      testTag = SEEK_BACK_TEST_TAG,
    )
    PlaybackControl(
      icon = if (controls.showPause) CoreUiR.drawable.pause else CoreUiR.drawable.play_arrow,
      description = if (controls.showPause) pauseDescription else playDescription,
      unavailableDescription = unavailableDescription,
      enabled = controls.playPauseEnabled,
      action =
        if (controls.showPause) {
          actionRunCallback<PausePlaybackAction>()
        } else {
          actionRunCallback<PlayPlaybackAction>()
        },
      testTag = PLAY_PAUSE_TEST_TAG,
    )
    PlaybackControl(
      icon = CoreUiR.drawable.fast_forward,
      description = seekForwardDescription,
      unavailableDescription = unavailableDescription,
      enabled = controls.seekForwardEnabled,
      action = actionRunCallback<SeekForwardPlaybackAction>(),
      testTag = SEEK_FORWARD_TEST_TAG,
    )
    if (chapterControls != null) {
      PlaybackControl(
        icon = CoreUiR.drawable.skip_next,
        description = nextChapterDescription,
        unavailableDescription = unavailableDescription,
        enabled = chapterControls.nextEnabled,
        action = actionRunCallback<NextChapterPlaybackAction>(),
        testTag = NEXT_CHAPTER_TEST_TAG,
      )
    }
  }
}

@Composable
private fun PlaybackControl(
  icon: Int,
  description: String,
  unavailableDescription: String,
  enabled: Boolean,
  action: Action,
  testTag: String,
) {
  val modifier =
    GlanceModifier.width(28.dp).height(28.dp).semantics {
      contentDescription = if (enabled) description else "$description. $unavailableDescription"
      this.testTag = testTag
    }
  Image(
    provider = ImageProvider(icon),
    contentDescription = null,
    modifier = if (enabled) modifier.clickable(action) else modifier,
    colorFilter =
      androidx.glance.ColorFilter.tint(
        if (enabled) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant
      ),
  )
}

@Composable
private fun PlaybackRecoveryAffordance(
  label: String,
  description: String,
  openAction: Action,
) {
  Text(
    text = label,
    modifier =
      GlanceModifier.background(GlanceTheme.colors.secondaryContainer)
        .cornerRadius(20.dp)
        .clickable(openAction)
        .semantics {
          contentDescription = description
          testTag = RECOVERY_TEST_TAG
        }
        .padding(horizontal = 10.dp, vertical = 6.dp),
    style =
      TextStyle(
        color = GlanceTheme.colors.onSecondaryContainer,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      ),
    maxLines = 2,
  )
}

@Composable
internal fun EmptyPlaybackWidget(
  title: String,
  compactMessage: String,
  expandedMessage: String,
  brandDescription: String,
  openDescription: String,
  openAction: Action = actionStartActivity<MainActivity>(),
) {
  val isLarge = LocalSize.current.width >= LargePlaybackWidgetSize.width
  val iconSize = if (isLarge) 56.dp else 48.dp
  val message = if (isLarge) expandedMessage else compactMessage

  Row(
    modifier =
      GlanceModifier.fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(R.dimen.playback_widget_corner_radius)
        .clickable(openAction)
        .semantics {
          contentDescription = openDescription
          testTag = EMPTY_WIDGET_TEST_TAG
        }
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.Start,
  ) {
    Image(
      provider = ImageProvider(R.drawable.widget_brand_headphones),
      contentDescription = brandDescription,
      modifier = GlanceModifier.width(iconSize).height(iconSize),
      colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
    )
    Spacer(modifier = GlanceModifier.width(if (isLarge) 16.dp else 12.dp))
    Column(
      modifier = GlanceModifier.defaultWeight(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style =
          TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontFamily = FontFamily.Serif,
            fontSize = if (isLarge) 20.sp else 18.sp,
            fontWeight = FontWeight.Bold,
          ),
        maxLines = 1,
      )
      Spacer(modifier = GlanceModifier.height(4.dp))
      Text(
        text = message,
        style =
          TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontFamily = FontFamily.SansSerif,
            fontSize = if (isLarge) 14.sp else 12.sp,
          ),
        maxLines = 2,
      )
    }
  }
}

internal data class PlaybackWidgetThemePreferences(
  val isDark: Boolean,
  val useDynamicColor: Boolean,
)

internal enum class PlaybackWidgetThemeVariant {
  FixedLight,
  FixedDark,
  DynamicLight,
  DynamicDark,
}

internal fun resolvePlaybackWidgetThemeVariant(
  preferences: PlaybackWidgetThemePreferences,
  supportsDynamicColor: Boolean,
): PlaybackWidgetThemeVariant =
  when {
    preferences.useDynamicColor && supportsDynamicColor && preferences.isDark ->
      PlaybackWidgetThemeVariant.DynamicDark
    preferences.useDynamicColor && supportsDynamicColor -> PlaybackWidgetThemeVariant.DynamicLight
    preferences.isDark -> PlaybackWidgetThemeVariant.FixedDark
    else -> PlaybackWidgetThemeVariant.FixedLight
  }

private suspend fun loadThemePreferences(context: Context): PlaybackWidgetThemePreferences =
  withContext(Dispatchers.IO) {
    val settingsRepository = playbackWidgetEntryPoint(context).settingsRepository()
    settingsRepository.darkMode
      .combine(settingsRepository.dynamicTheme, ::PlaybackWidgetThemePreferences)
      .first()
  }

private fun playbackWidgetColorProviders(
  context: Context,
  preferences: PlaybackWidgetThemePreferences,
): ColorProviders {
  val variant =
    resolvePlaybackWidgetThemeVariant(
      preferences = preferences,
      supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
  val scheme =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      when (variant) {
        PlaybackWidgetThemeVariant.FixedLight -> lightScheme
        PlaybackWidgetThemeVariant.FixedDark -> darkScheme
        PlaybackWidgetThemeVariant.DynamicLight -> dynamicLightColorScheme(context)
        PlaybackWidgetThemeVariant.DynamicDark -> dynamicDarkColorScheme(context)
      }
    } else {
      if (preferences.isDark) darkScheme else lightScheme
    }
  return scheme.asExplicitColorProviders()
}

private fun ColorScheme.asExplicitColorProviders(): ColorProviders =
  androidx.glance.material3.ColorProviders(light = this, dark = this)

internal fun playbackWidgetEntryPoint(context: Context): PlaybackWidgetEntryPoint =
  EntryPointAccessors.fromApplication(
    context.applicationContext,
    PlaybackWidgetEntryPoint::class.java,
  )

private fun openNowPlayingAction(context: Context, mediaId: String): Action =
  actionStartActivityIntent(createNowPlayingIntent(context, mediaId))

internal fun createNowPlayingIntent(context: Context, mediaId: String): Intent =
  Intent(context, MainActivity::class.java).apply {
    action = ACTION_OPEN_PLAYER
    putExtra(EXTRA_MEDIA_ID, mediaId)
    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
  }

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PlaybackWidgetEntryPoint {
  fun settingsRepository(): SettingsRepository

  fun presentationLoader(): PlaybackWidgetPresentationLoader

  fun commandHandler(): PlaybackWidgetCommandHandler
}

internal const val EMPTY_WIDGET_TEST_TAG = "empty_playback_widget"
internal const val ACTIVE_WIDGET_TEST_TAG = "active_playback_widget"
internal const val ERROR_WIDGET_TEST_TAG = "error_playback_widget"
internal const val ARTWORK_TEST_TAG = "current_playback_artwork"
internal const val ARTWORK_FALLBACK_TEST_TAG = "current_playback_artwork_fallback"
internal const val METADATA_TEST_TAG = "current_playback_metadata"
internal const val RECOVERY_TEST_TAG = "current_playback_recovery"
internal const val SEEK_BACK_TEST_TAG = "current_playback_seek_back"
internal const val PLAY_PAUSE_TEST_TAG = "current_playback_play_pause"
internal const val SEEK_FORWARD_TEST_TAG = "current_playback_seek_forward"
internal const val PREVIOUS_CHAPTER_TEST_TAG = "current_playback_previous_chapter"
internal const val NEXT_CHAPTER_TEST_TAG = "current_playback_next_chapter"
