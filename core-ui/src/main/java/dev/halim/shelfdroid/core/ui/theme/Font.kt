package dev.halim.shelfdroid.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.halim.shelfdroid.core.ui.R

const val inter = "Inter"
const val merriweather = "Merriweather"
const val jetbrainsMono = "JetBrains Mono"

val interGoogleFont = GoogleFont(inter)
val merriweatherGoogleFont = GoogleFont(merriweather)
val jetbrainsMonoGoogleFont = GoogleFont(jetbrainsMono)

val provider =
  GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
  )

val interFontFamily = FontFamily(Font(googleFont = interGoogleFont, fontProvider = provider))
val merriweatherFontFamily =
  FontFamily(Font(googleFont = merriweatherGoogleFont, fontProvider = provider))
val jetbrainsMonoFontFamily =
  FontFamily(Font(googleFont = jetbrainsMonoGoogleFont, fontProvider = provider))

fun shelfDroidTypography() =
  Typography().run {
    copy(
      displayLarge = displayLarge.copy(fontFamily = merriweatherFontFamily),
      displayMedium = displayMedium.copy(fontFamily = merriweatherFontFamily),
      displaySmall = displaySmall.copy(fontFamily = merriweatherFontFamily),
      headlineLarge = headlineLarge.copy(fontFamily = merriweatherFontFamily),
      headlineMedium = headlineMedium.copy(fontFamily = merriweatherFontFamily),
      headlineSmall = headlineSmall.copy(fontFamily = merriweatherFontFamily),
      titleLarge = titleLarge.copy(fontFamily = merriweatherFontFamily),
      titleMedium = titleMedium.copy(fontFamily = merriweatherFontFamily),
      titleSmall = titleSmall.copy(fontFamily = merriweatherFontFamily),
      bodyLarge = bodyLarge.copy(fontFamily = interFontFamily),
      bodyMedium = bodyMedium.copy(fontFamily = interFontFamily),
      bodySmall = bodySmall.copy(fontFamily = interFontFamily),
      labelLarge = labelLarge.copy(fontFamily = jetbrainsMonoFontFamily),
      labelMedium = labelMedium.copy(fontFamily = jetbrainsMonoFontFamily),
      labelSmall = labelSmall.copy(fontFamily = jetbrainsMonoFontFamily),
    )
  }
