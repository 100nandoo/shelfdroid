package dev.halim.shelfdroid.widget.player.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasNoClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
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
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.seek_back))).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play))).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.seek_forward)))
        .assertExists()
      onNode(hasText("An Unexpected Party")).assertDoesNotExist()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
        .assertDoesNotExist()
    }

  @Test
  fun `compact layout is used just below the medium minimum height`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 104.dp))
    provideWidget()

    onNode(hasText("The Hobbit")).assertExists()
    onNode(hasText("An Unexpected Party")).assertDoesNotExist()
    onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.seek_back))).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertDoesNotExist()
  }

  @Test
  fun `medium layout shows chapter navigation in the playback control row`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 128.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertExists()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasText("An Unexpected Party")).assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play))).assertExists()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
        .assertExists()
    }

  @Test
  fun `medium layout is selected at its minimum supported height`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 116.dp))
    provideWidget()

    onNode(hasText("An Unexpected Party")).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play))).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
      .assertExists()
  }

  @Test
  fun `medium falls back to compact when larger text needs more height`() =
    runGlanceAppWidgetUnitTest {
      val scaledConfiguration =
        Configuration(context.resources.configuration).apply { fontScale = 2f }
      setContext(context.createConfigurationContext(scaledConfiguration))
      setAppWidgetSize(DpSize(width = 320.dp, height = 163.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertExists()
      onNode(hasText("An Unexpected Party")).assertDoesNotExist()
      onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play))).assertExists()
    }

  @Test
  fun `medium is selected at its larger font minimum height`() = runGlanceAppWidgetUnitTest {
    val scaledConfiguration =
      Configuration(context.resources.configuration).apply { fontScale = 2f }
    setContext(context.createConfigurationContext(scaledConfiguration))
    setAppWidgetSize(DpSize(width = 320.dp, height = 164.dp))
    provideWidget()

    onNode(hasText("An Unexpected Party")).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(CoreR.string.play))).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
      .assertExists()
  }

  @Test
  fun `large layout shows chapter and author without the book title`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertDoesNotExist()
      onNode(hasText("An Unexpected Party")).assertHasClickAction()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
        .assertExists()
    }

  @Test
  fun `large layout uses book title as headline when chapter is unavailable`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
      provideWidget(widgetData.copy(chapterTitle = null))

      onNode(hasText("The Hobbit")).assertHasClickAction()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasText("An Unexpected Party")).assertDoesNotExist()
    }

  @Test
  fun `large podcast layout shows chapter and podcast name without episode title`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
      provideWidget(
        widgetData.copy(
          id = "podcast",
          episodeId = "episode-one",
          title = "The Lighthouse",
          author = "Signal Lost",
          chapterTitle = "A Strange Arrival",
        )
      )

      onNode(hasText("The Lighthouse")).assertDoesNotExist()
      onNode(hasText("A Strange Arrival")).assertHasClickAction()
      onNode(hasText("Signal Lost")).assertExists()
    }

  @Test
  fun `large layout is selected at its minimum supported height`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 176.dp))
    provideWidget()

    onNode(hasText("The Hobbit")).assertDoesNotExist()
    onNode(hasText("An Unexpected Party")).assertHasClickAction()
    onNode(hasText("J. R. R. Tolkien")).assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertExists()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
      .assertExists()
  }

  @Test
  fun `large layout keeps chapter controls at the above-cover transition`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 302.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertDoesNotExist()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasText("An Unexpected Party")).assertHasClickAction()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
        .assertExists()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
        .assertExists()
    }

  @Test
  fun `large layout keeps chapter navigation with larger system text`() =
    runGlanceAppWidgetUnitTest {
      val scaledConfiguration =
        Configuration(context.resources.configuration).apply {
          fontScale = 2f
        }
      setContext(context.createConfigurationContext(scaledConfiguration))
      setAppWidgetSize(DpSize(width = 320.dp, height = 176.dp))
      provideWidget()

      onNode(hasText("The Hobbit")).assertDoesNotExist()
      onNode(hasText("J. R. R. Tolkien")).assertExists()
      onNode(hasText("An Unexpected Party")).assertHasClickAction()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
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
  fun `last chapter leaves previous enabled and disables next`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
    provideWidget(widgetData.copy(previousChapterEnabled = true, nextChapterEnabled = false))

    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter)))
      .assertHasClickAction()
    onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
      .assertHasNoClickAction()
  }

  @Test
  fun `chapter controls are hidden when fewer than two chapters are available`() =
    runGlanceAppWidgetUnitTest {
      setContext(context)
      setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
      provideWidget(
        widgetData.copy(
          chapterTitle = null,
          showChapterNavigation = false,
          previousChapterEnabled = false,
          nextChapterEnabled = false,
        )
      )

      onNode(hasText("An Unexpected Party")).assertDoesNotExist()
      onNode(
          hasContentDescriptionEqualTo(context.getString(R.string.player_widget_previous_chapter))
        )
        .assertDoesNotExist()
      onNode(hasContentDescriptionEqualTo(context.getString(R.string.player_widget_next_chapter)))
        .assertDoesNotExist()
    }

  @Test
  fun `empty widget offers an open ShelfDroid action`() = runGlanceAppWidgetUnitTest {
    setContext(context)
    setAppWidgetSize(DpSize(width = 320.dp, height = 180.dp))
    provideComposable { GlanceTheme { PlayerWidgetContent(content = null) } }

    onNode(hasTestTag("player_widget_empty_state")).assertHasClickAction()
    onNode(hasText(context.getString(R.string.player_widget_choose_something_to_play)))
      .assertExists()
    onNode(hasText(context.getString(R.string.player_widget_open_shelfdroid))).assertExists()
  }

  private val widgetData
    get() =
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

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.provideWidget(
    content: PlayerWidgetData = widgetData
  ) {
    provideComposable {
      GlanceTheme { PlayerWidgetContent(content = content) }
    }
  }

  private fun openPlayerIntent(mediaId: String) =
    Intent(Helper.ACTION_OPEN_PLAYER).apply {
      component =
        ComponentName(context.packageName, "dev.halim.shelfdroid.core.ui.screen.MainActivity")
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
