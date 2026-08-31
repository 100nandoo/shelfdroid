package dev.halim.shelfdroid.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetThemeTest {
  @Test
  fun fixedLightPreferenceUsesFixedLightTheme() {
    assertTheme(
      isDark = false,
      useDynamicColor = false,
      supportsDynamicColor = true,
      expected = PlaybackWidgetThemeVariant.FixedLight,
    )
  }

  @Test
  fun fixedDarkPreferenceUsesFixedDarkTheme() {
    assertTheme(
      isDark = true,
      useDynamicColor = false,
      supportsDynamicColor = true,
      expected = PlaybackWidgetThemeVariant.FixedDark,
    )
  }

  @Test
  fun dynamicLightPreferenceUsesDynamicLightThemeWhenSupported() {
    assertTheme(
      isDark = false,
      useDynamicColor = true,
      supportsDynamicColor = true,
      expected = PlaybackWidgetThemeVariant.DynamicLight,
    )
  }

  @Test
  fun dynamicDarkPreferenceUsesDynamicDarkThemeWhenSupported() {
    assertTheme(
      isDark = true,
      useDynamicColor = true,
      supportsDynamicColor = true,
      expected = PlaybackWidgetThemeVariant.DynamicDark,
    )
  }

  @Test
  fun dynamicPreferenceFallsBackToFixedThemeWhenUnsupported() {
    assertTheme(
      isDark = true,
      useDynamicColor = true,
      supportsDynamicColor = false,
      expected = PlaybackWidgetThemeVariant.FixedDark,
    )
  }

  private fun assertTheme(
    isDark: Boolean,
    useDynamicColor: Boolean,
    supportsDynamicColor: Boolean,
    expected: PlaybackWidgetThemeVariant,
  ) {
    assertEquals(
      expected,
      resolvePlaybackWidgetThemeVariant(
        preferences =
          PlaybackWidgetThemePreferences(
            isDark = isDark,
            useDynamicColor = useDynamicColor,
          ),
        supportsDynamicColor = supportsDynamicColor,
      ),
    )
  }
}
