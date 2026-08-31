package dev.halim.shelfdroid.widget

import android.graphics.Bitmap
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.assertHasRunCallbackClickAction
import androidx.glance.material3.ColorProviders
import androidx.glance.testing.unit.assertHasContentDescriptionEqualTo
import androidx.glance.testing.unit.assertHasNoClickAction
import androidx.glance.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.assertHasTextEqualTo
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
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
      assertPrimaryControls(PAUSE_DESCRIPTION)
      assertChapterControlsDoNotExist()
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
      assertPrimaryControls(PAUSE_DESCRIPTION)
      assertEnabledChapterControls()
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
    onNode(hasTestTag(SEEK_BACK_TEST_TAG)).assertDoesNotExist()
    onNode(hasTestTag(PLAY_PAUSE_TEST_TAG)).assertDoesNotExist()
    onNode(hasTestTag(SEEK_FORWARD_TEST_TAG)).assertDoesNotExist()
    assertChapterControlsDoNotExist()
  }

  @Test
  fun pausedAndEndedPlayback_showPlay() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(SmallPlaybackWidgetSize)
    provideCurrentPlaybackWidget(
      media = mediaWithArtwork(),
      isError = false,
      controls = primaryControls(showPause = false),
    )

    assertPrimaryControls(PLAY_DESCRIPTION)
    onNode(hasContentDescriptionEqualTo(PAUSE_DESCRIPTION)).assertDoesNotExist()
  }

  @Test
  fun unavailableControls_remainVisibleAndCommunicateDisabledState() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(SmallPlaybackWidgetSize)
    provideCurrentPlaybackWidget(
      media = mediaWithArtwork(),
      isError = false,
      controls =
        PrimaryPlaybackControls(
          showPause = true,
          playPauseEnabled = false,
          seekBackEnabled = false,
          seekForwardEnabled = false,
        ),
    )

    onNode(hasTestTag(SEEK_BACK_TEST_TAG))
      .assertHasContentDescriptionEqualTo("$SEEK_BACK_DESCRIPTION. $UNAVAILABLE_DESCRIPTION")
    onNode(hasTestTag(PLAY_PAUSE_TEST_TAG))
      .assertHasContentDescriptionEqualTo("$PAUSE_DESCRIPTION. $UNAVAILABLE_DESCRIPTION")
    onNode(hasTestTag(SEEK_FORWARD_TEST_TAG))
      .assertHasContentDescriptionEqualTo("$SEEK_FORWARD_DESCRIPTION. $UNAVAILABLE_DESCRIPTION")
  }

  @Test
  fun unavailableNextChapter_remainsVisibleWithoutAnAction() = runGlanceAppWidgetUnitTest {
    setAppWidgetSize(LargePlaybackWidgetSize)
    provideCurrentPlaybackWidget(
      media = mediaWithArtwork(),
      isError = false,
      chapterControls = ChapterPlaybackControls(previousEnabled = true, nextEnabled = false),
    )

    onNode(hasTestTag(PREVIOUS_CHAPTER_TEST_TAG))
      .assertHasContentDescriptionEqualTo(PREVIOUS_CHAPTER_DESCRIPTION)
      .assertHasRunCallbackClickAction<PreviousChapterPlaybackAction>()
    onNode(hasTestTag(NEXT_CHAPTER_TEST_TAG))
      .assertHasContentDescriptionEqualTo(
        "$NEXT_CHAPTER_DESCRIPTION. $UNAVAILABLE_DESCRIPTION"
      )
      .assertHasNoClickAction()
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .provideCurrentPlaybackWidget(
      media: CurrentPlaybackMedia,
      isError: Boolean,
      controls: PrimaryPlaybackControls? = primaryControls(showPause = true),
      chapterControls: ChapterPlaybackControls? = enabledChapterControls(),
    ) {
    provideComposable {
      GlanceTheme(colors = ColorProviders(light = lightScheme, dark = darkScheme)) {
        CurrentPlaybackWidget(
          media = media,
          isError = isError,
          controls = controls,
          chapterControls = chapterControls,
          artworkDescription = ARTWORK_DESCRIPTION,
          fallbackArtworkDescription = FALLBACK_DESCRIPTION,
          metadataDescription = METADATA_DESCRIPTION,
          openDescription = OPEN_DESCRIPTION,
          recoveryLabel = RECOVERY_LABEL,
          recoveryDescription = RECOVERY_DESCRIPTION,
          playDescription = PLAY_DESCRIPTION,
          pauseDescription = PAUSE_DESCRIPTION,
          seekBackDescription = SEEK_BACK_DESCRIPTION,
          seekForwardDescription = SEEK_FORWARD_DESCRIPTION,
          previousChapterDescription = PREVIOUS_CHAPTER_DESCRIPTION,
          nextChapterDescription = NEXT_CHAPTER_DESCRIPTION,
          unavailableDescription = UNAVAILABLE_DESCRIPTION,
          openAction = actionStartActivity<MainActivity>(),
        )
      }
    }
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .assertEnabledChapterControls() {
    onNode(hasTestTag(PREVIOUS_CHAPTER_TEST_TAG))
      .assertHasContentDescriptionEqualTo(PREVIOUS_CHAPTER_DESCRIPTION)
      .assertHasRunCallbackClickAction<PreviousChapterPlaybackAction>()
    onNode(hasTestTag(NEXT_CHAPTER_TEST_TAG))
      .assertHasContentDescriptionEqualTo(NEXT_CHAPTER_DESCRIPTION)
      .assertHasRunCallbackClickAction<NextChapterPlaybackAction>()
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .assertChapterControlsDoNotExist() {
    onNode(hasTestTag(PREVIOUS_CHAPTER_TEST_TAG)).assertDoesNotExist()
    onNode(hasTestTag(NEXT_CHAPTER_TEST_TAG)).assertDoesNotExist()
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.assertPrimaryControls(
    playPauseDescription: String,
  ) {
    onNode(hasTestTag(SEEK_BACK_TEST_TAG))
      .assertHasContentDescriptionEqualTo(SEEK_BACK_DESCRIPTION)
      .assertHasRunCallbackClickAction<SeekBackPlaybackAction>()
    onNode(hasTestTag(PLAY_PAUSE_TEST_TAG))
      .assertHasContentDescriptionEqualTo(playPauseDescription)
    onNode(hasTestTag(SEEK_FORWARD_TEST_TAG))
      .assertHasContentDescriptionEqualTo(SEEK_FORWARD_DESCRIPTION)
      .assertHasRunCallbackClickAction<SeekForwardPlaybackAction>()
    if (playPauseDescription == PAUSE_DESCRIPTION) {
      onNode(hasTestTag(PLAY_PAUSE_TEST_TAG))
        .assertHasRunCallbackClickAction<PausePlaybackAction>()
    } else {
      onNode(hasTestTag(PLAY_PAUSE_TEST_TAG))
        .assertHasRunCallbackClickAction<PlayPlaybackAction>()
    }
  }

  private fun primaryControls(showPause: Boolean) =
    PrimaryPlaybackControls(
      showPause = showPause,
      playPauseEnabled = true,
      seekBackEnabled = true,
      seekForwardEnabled = true,
    )

  private fun enabledChapterControls() =
    ChapterPlaybackControls(previousEnabled = true, nextEnabled = true)

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
    const val PLAY_DESCRIPTION = "Play"
    const val PAUSE_DESCRIPTION = "Pause"
    const val SEEK_BACK_DESCRIPTION = "Seek back 10 seconds"
    const val SEEK_FORWARD_DESCRIPTION = "Seek forward 10 seconds"
    const val PREVIOUS_CHAPTER_DESCRIPTION = "Previous Chapter"
    const val NEXT_CHAPTER_DESCRIPTION = "Next Chapter"
    const val UNAVAILABLE_DESCRIPTION = "Unavailable"
  }
}
