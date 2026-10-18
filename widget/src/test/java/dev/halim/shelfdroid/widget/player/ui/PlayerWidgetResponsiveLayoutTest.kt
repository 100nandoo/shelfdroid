package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.assertHasNoClickAction
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerWidgetResponsiveLayoutTest {
  private val context: Context
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun `compact layout keeps existing playback controls and opens player from title`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 72.dp))
      provideWidget()

      onNode(hasText("The Hobbit"))
        .assertHasStartActivityClickAction(openPlayerIntent("the-hobbit"))
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.seek_back)))
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play)))
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.seek_forward)))
        .assertExists()
      onNode(hasText("An Unexpected Party")).assertDoesNotExist()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
        .assertDoesNotExist()
    }

  @Test
  fun `medium layout shows current chapter and a separate main control row`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 128.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertExists()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasText("An Unexpected Party")).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play)))
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
        .assertDoesNotExist()
    }

  @Test
  fun `large layout adds chapter navigation while keeping title clickable`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertHasClickAction()
      onNode(hasText("An Unexpected Party")).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
        .assertExists()
    }

  @Test
  fun `first chapter disables previous and leaves next enabled`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
    provideWidget()

    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertHasNoClickAction()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
      .assertHasClickAction()
  }

  @Test
  fun `empty widget offers an open ShelfDroid action`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
    provideComposable { GlanceTheme { PlayerWidgetContent(content = null) } }

    onNode(hasText(context.getString(R.string.player_widget_choose_something_to_play)))
      .assertExists()
    onNode(hasText(context.getString(R.string.player_widget_open_shelfdroid)))
      .assertHasClickAction()
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.provideWidget() {
    provideComposable {
      GlanceTheme {
        PlayerWidgetContent(
          content =
            PlayerWidgetData(
              id = "the-hobbit",
              episodeId = "",
              title = "The Hobbit",
              author = "J. R. R. Tolkien",
              cover = "",
              playPauseEnabled = true,
              showPlayIcon = true,
              seekBackEnabled = true,
              seekForwardEnabled = true,
              chapterTitle = "An Unexpected Party",
              showChapterNavigation = true,
              previousChapterEnabled = false,
              nextChapterEnabled = true,
            )
        )
      }
    }
  }

  private fun openPlayerIntent(mediaId: String) =
    Intent(Helper.ACTION_OPEN_PLAYER).apply {
      setPackage(context.packageName)
      putExtra(Helper.EXTRA_MEDIA_ID, mediaId)
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_OPENING_SCREEN,
        MediaNotificationOpeningScreen.Home.name,
      )
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_PLAYER_PRESENTATION,
        MediaNotificationPlayerPresentation.ExpandedPlayer.name,
      )
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
}
