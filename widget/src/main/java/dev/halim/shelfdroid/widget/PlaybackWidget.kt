package dev.halim.shelfdroid.widget

import android.content.Context
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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

    provideContent {
      GlanceTheme(colors = colors) {
        EmptyPlaybackWidget(
          title = context.getString(R.string.playback_widget_empty_title),
          compactMessage = context.getString(R.string.playback_widget_empty_compact_message),
          expandedMessage = context.getString(R.string.playback_widget_empty_expanded_message),
          brandDescription = context.getString(R.string.playback_widget_brand_description),
          openDescription = context.getString(R.string.playback_widget_open_description),
        )
      }
    }
  }
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
    val settingsRepository =
      EntryPointAccessors.fromApplication(
          context.applicationContext,
          PlaybackWidgetEntryPoint::class.java,
        )
        .settingsRepository()
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

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PlaybackWidgetEntryPoint {
  fun settingsRepository(): SettingsRepository
}

internal const val EMPTY_WIDGET_TEST_TAG = "empty_playback_widget"
