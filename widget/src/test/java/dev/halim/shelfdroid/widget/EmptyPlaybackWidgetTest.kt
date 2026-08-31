package dev.halim.shelfdroid.widget

import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.material3.ColorProviders
import androidx.glance.testing.unit.assertHasContentDescriptionEqualTo
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
class EmptyPlaybackWidgetTest {
  @Test
  fun smallSize_showsCompactEmptyStateAndOpensNormalAppDestination() =
    runGlanceAppWidgetUnitTest {
      setAppWidgetSize(SmallPlaybackWidgetSize)
      provideEmptyPlaybackWidget()

      onNode(hasTextEqualTo(TITLE)).assertHasTextEqualTo(TITLE)
      onNode(hasTextEqualTo(COMPACT_MESSAGE)).assertHasTextEqualTo(COMPACT_MESSAGE)
      onNode(hasTextEqualTo(EXPANDED_MESSAGE)).assertDoesNotExist()
      onNode(hasContentDescriptionEqualTo(BRAND_DESCRIPTION))
        .assertHasContentDescriptionEqualTo(BRAND_DESCRIPTION)
      onNode(hasTestTag(EMPTY_WIDGET_TEST_TAG))
        .assertHasContentDescriptionEqualTo(OPEN_DESCRIPTION)
        .assertHasStartActivityClickAction<MainActivity>()
      assertNoPlaybackControls()
    }

  @Test
  fun largeSize_showsExpandedEmptyStateAndOpensNormalAppDestination() =
    runGlanceAppWidgetUnitTest {
      setAppWidgetSize(LargePlaybackWidgetSize)
      provideEmptyPlaybackWidget()

      onNode(hasTextEqualTo(TITLE)).assertHasTextEqualTo(TITLE)
      onNode(hasTextEqualTo(EXPANDED_MESSAGE)).assertHasTextEqualTo(EXPANDED_MESSAGE)
      onNode(hasTextEqualTo(COMPACT_MESSAGE)).assertDoesNotExist()
      onNode(hasContentDescriptionEqualTo(BRAND_DESCRIPTION))
        .assertHasContentDescriptionEqualTo(BRAND_DESCRIPTION)
      onNode(hasTestTag(EMPTY_WIDGET_TEST_TAG))
        .assertHasContentDescriptionEqualTo(OPEN_DESCRIPTION)
        .assertHasStartActivityClickAction<MainActivity>()
      assertNoPlaybackControls()
    }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .provideEmptyPlaybackWidget() {
    provideComposable {
      GlanceTheme(colors = ColorProviders(light = lightScheme, dark = darkScheme)) {
        EmptyPlaybackWidget(
          title = TITLE,
          compactMessage = COMPACT_MESSAGE,
          expandedMessage = EXPANDED_MESSAGE,
          brandDescription = BRAND_DESCRIPTION,
          openDescription = OPEN_DESCRIPTION,
        )
      }
    }
  }

  private fun androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
    .assertNoPlaybackControls() {
    CONTROL_DESCRIPTIONS.forEach { description ->
      onNode(hasContentDescriptionEqualTo(description)).assertDoesNotExist()
    }
  }

  private companion object {
    const val TITLE = "ShelfDroid"
    const val COMPACT_MESSAGE = "Open ShelfDroid"
    const val EXPANDED_MESSAGE = "Nothing playing · Open ShelfDroid"
    const val BRAND_DESCRIPTION = "ShelfDroid headphones"
    const val OPEN_DESCRIPTION = "Open ShelfDroid"

    val CONTROL_DESCRIPTIONS =
      listOf("Play", "Pause", "Seek back", "Seek forward", "Previous chapter", "Next chapter")
  }
}
