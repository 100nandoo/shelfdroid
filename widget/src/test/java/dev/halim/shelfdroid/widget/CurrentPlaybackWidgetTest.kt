package dev.halim.shelfdroid.widget

import android.graphics.Bitmap
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.material3.ColorProviders
import androidx.glance.testing.unit.assertHasContentDescriptionEqualTo
import androidx.glance.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasTextEqualTo
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class CurrentPlaybackWidgetTest {
  @Test
  fun smallActivePresentation_showsArtworkWithoutMetadataAndOpensNowPlaying() =
    runGlanceAppWidgetUnitTest {
      setAppWidgetSize(SmallPlaybackWidgetSize)
      provideCurrentPlaybackWidget(media = mediaWithArtwork(), isError = false)

      onNode(hasTestTag(ACTIVE_WIDGET_TEST_TAG))
        .assertHasContentDescriptionEqualTo(OPEN_DESCRIPTION)
        .assertHasStartActivityClickAction<MainActivity>()
      onNode(hasTestTag(ARTWORK_TEST_TAG))
        .assertHasContentDescriptionEqualTo(ARTWORK_DESCRIPTION)
        .assertHasStartActivityClickAction<MainActivity>()
      onNode(hasTextEqualTo(MEDIA_TITLE)).assertDoesNotExist()
      onNode(hasTextEqualTo(PLAYABLE_TITLE)).assertDoesNotExist()
      onNode(hasTestTag(METADATA_TEST_TAG)).assertDoesNotExist()
      onNode(hasTestTag(RECOVERY_TEST_TAG)).assertDoesNotExist()
    }

  @Test
  fun largeActivePresentation_showsTwoMetadataLinesAndNavigationActions() =
    runGlanceAppWidgetUnitTest {
      setAppWidgetSize(LargePlaybackWidgetSize)
      provideCurrentPlaybackWidget(media = mediaWithArtwork(), isError = false)

      onNode(hasTextEqualTo(MEDIA_TITLE)).assertHasTextEqualTo(MEDIA_TITLE)
      onNode(hasTextEqualTo(PLAYABLE_TITLE)).assertHasTextEqualTo(PLAYABLE_TITLE)
      onNode(hasTestTag(METADATA_TEST_TAG))
        .assertHasContentDescriptionEqualTo(METADATA_DESCRIPTION)
        .assertHasStartActivityClickAction<MainActivity>()
      onNode(hasTestTag(ACTIVE_WIDGET_TEST_TAG)).assertHasStartActivityClickAction<MainActivity>()
      onNode(hasTestTag(RECOVERY_TEST_TAG)).assertDoesNotExist()
    }

  @Test
  fun artworkFailure_showsShelfDroidPlaceholderWithoutStaleArtwork() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(LargePlaybackWidgetSize)
    provideCurrentPlaybackWidget(media = mediaWithArtwork().copy(artwork = null), isError = false)

    onNode(hasTestTag(ARTWORK_FALLBACK_TEST_TAG))
      .assertHasContentDescriptionEqualTo(FALLBACK_DESCRIPTION)
      .assertHasStartActivityClickAction<MainActivity>()
    onNode(hasTestTag(ARTWORK_TEST_TAG)).assertDoesNotExist()
  }

  @Test
  fun errorPresentation_retainsIdentityAndShowsRecoveryNavigation() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(LargePlaybackWidgetSize)
    provideCurrentPlaybackWidget(media = mediaWithArtwork(), isError = true)

    onNode(hasTestTag(ERROR_WIDGET_TEST_TAG))
      .assertHasContentDescriptionEqualTo(OPEN_DESCRIPTION)
      .assertHasStartActivityClickAction<MainActivity>()
    onNode(hasTextEqualTo(MEDIA_TITLE)).assertHasTextEqualTo(MEDIA_TITLE)
    onNode(hasTextEqualTo(PLAYABLE_TITLE)).assertHasTextEqualTo(PLAYABLE_TITLE)
    onNode(hasTestTag(RECOVERY_TEST_TAG))
      .assertHasContentDescriptionEqualTo(RECOVERY_DESCRIPTION)
      .assertHasStartActivityClickAction<MainActivity>()
    onNode(hasTextEqualTo(RECOVERY_LABEL)).assertHasTextEqualTo(RECOVERY_LABEL)
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .provideCurrentPlaybackWidget(media: CurrentPlaybackMedia, isError: Boolean) {
    provideComposable {
      GlanceTheme(colors = ColorProviders(light = lightScheme, dark = darkScheme)) {
        CurrentPlaybackWidget(
          media = media,
          isError = isError,
          artworkDescription = ARTWORK_DESCRIPTION,
          fallbackArtworkDescription = FALLBACK_DESCRIPTION,
          metadataDescription = METADATA_DESCRIPTION,
          openDescription = OPEN_DESCRIPTION,
          recoveryLabel = RECOVERY_LABEL,
          recoveryDescription = RECOVERY_DESCRIPTION,
          openAction = actionStartActivity<MainActivity>(),
        )
      }
    }
  }

  private fun mediaWithArtwork() =
    CurrentPlaybackMedia(
      mediaId = "book-id",
      mediaTitle = MEDIA_TITLE,
      playableTitle = PLAYABLE_TITLE,
      artwork = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888),
    )

  private companion object {
    const val MEDIA_TITLE = "The Left Hand of Darkness"
    const val PLAYABLE_TITLE = "Chapter 1"
    const val ARTWORK_DESCRIPTION = "Cover art for $MEDIA_TITLE"
    const val FALLBACK_DESCRIPTION = "ShelfDroid placeholder for $MEDIA_TITLE"
    const val METADATA_DESCRIPTION = "Current playback: $MEDIA_TITLE. $PLAYABLE_TITLE"
    const val OPEN_DESCRIPTION = "Open Now Playing for $MEDIA_TITLE"
    const val RECOVERY_LABEL = "Open ShelfDroid"
    const val RECOVERY_DESCRIPTION = "Open ShelfDroid for Current playback"
  }
}
