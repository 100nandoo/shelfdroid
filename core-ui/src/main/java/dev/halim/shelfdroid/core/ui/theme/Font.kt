package dev.halim.shelfdroid.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.halim.shelfdroid.core.ui.R


const val inter = "Inter"
const val dmSerifText = "DM Serif Text"
const val jetbrainsMono = "JetBrains Mono"

val interGoogleFont = GoogleFont(inter)
val dmSerifTextGoogleFont = GoogleFont(dmSerifText)
val jetbrainsMonoGoogleFont = GoogleFont(jetbrainsMono)


val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interFontFamily = FontFamily(Font(googleFont = interGoogleFont, fontProvider = provider),)
val dmSerifTextFontFamily = FontFamily(Font(googleFont = dmSerifTextGoogleFont, fontProvider = provider))
val jetbrainsMonoFontFamily = FontFamily(Font(googleFont = jetbrainsMonoGoogleFont, fontProvider = provider))

fun shelfDroidTypography() = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = dmSerifTextFontFamily),
        displayMedium = displayMedium.copy(fontFamily = dmSerifTextFontFamily),
        displaySmall = displaySmall.copy(fontFamily = dmSerifTextFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = dmSerifTextFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = dmSerifTextFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = dmSerifTextFontFamily),
        titleLarge = titleLarge.copy(fontFamily = dmSerifTextFontFamily),
        titleMedium = titleMedium.copy(fontFamily = dmSerifTextFontFamily),
        titleSmall = titleSmall.copy(fontFamily = dmSerifTextFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = interFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = interFontFamily),
        bodySmall = bodySmall.copy(fontFamily = interFontFamily),
        labelLarge = labelLarge.copy(fontFamily = jetbrainsMonoFontFamily),
        labelMedium = labelMedium.copy(fontFamily = jetbrainsMonoFontFamily),
        labelSmall = labelSmall.copy(fontFamily = jetbrainsMonoFontFamily)
    )
}
