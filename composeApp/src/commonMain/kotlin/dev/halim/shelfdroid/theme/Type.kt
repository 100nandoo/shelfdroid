package dev.halim.shelfdroid.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import shelfdroid.composeapp.generated.resources.Res
import shelfdroid.composeapp.generated.resources.dm_serif_text_regular
import shelfdroid.composeapp.generated.resources.inter_bold
import shelfdroid.composeapp.generated.resources.inter_light
import shelfdroid.composeapp.generated.resources.inter_medium
import shelfdroid.composeapp.generated.resources.inter_regular
import shelfdroid.composeapp.generated.resources.inter_semibold
import shelfdroid.composeapp.generated.resources.jetbrains_mono


@Composable
fun InterFontFamily() = FontFamily(
    Font(Res.font.inter_light, FontWeight.Light),
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun DmSerifTextFontFamily() = FontFamily(Font(Res.font.dm_serif_text_regular))

@Composable
fun JetbrainsMonoFontFamily() = FontFamily(Font(Res.font.jetbrains_mono))

@Composable
fun InterTypography() = Typography().run {
    val dmSerifTextFontFamily = DmSerifTextFontFamily()
    val interFontFamily = InterFontFamily()
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
        bodyLarge = bodyLarge.copy(fontFamily =  interFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = interFontFamily),
        bodySmall = bodySmall.copy(fontFamily = interFontFamily),
        labelLarge = labelLarge.copy(fontFamily = interFontFamily),
        labelMedium = labelMedium.copy(fontFamily = interFontFamily),
        labelSmall = labelSmall.copy(fontFamily = interFontFamily)
    )
}