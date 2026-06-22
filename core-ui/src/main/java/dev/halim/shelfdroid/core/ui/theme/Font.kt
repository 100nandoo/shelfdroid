package dev.halim.shelfdroid.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.halim.shelfdroid.core.ui.R

private val interFontFamily = variableFontFamily(R.font.inter_variable)
private val loraFontFamily = variableFontFamily(R.font.lora_variable)
private val jetbrainsMonoFontFamily = variableFontFamily(R.font.jetbrains_mono_variable)

@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(fontResId: Int) =
  FontFamily(
    variableFont(fontResId, FontWeight.Normal),
    variableFont(fontResId, FontWeight.Medium),
    variableFont(fontResId, FontWeight.SemiBold),
    variableFont(fontResId, FontWeight.Bold),
  )

@OptIn(ExperimentalTextApi::class)
private fun variableFont(fontResId: Int, weight: FontWeight) =
  Font(
    resId = fontResId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
  )

fun shelfDroidTypography() =
  Typography().run {
    copy(
      displayLarge = displayLarge.copy(fontFamily = loraFontFamily),
      displayMedium = displayMedium.copy(fontFamily = loraFontFamily),
      displaySmall = displaySmall.copy(fontFamily = loraFontFamily),
      headlineLarge = headlineLarge.copy(fontFamily = loraFontFamily),
      headlineMedium = headlineMedium.copy(fontFamily = loraFontFamily),
      headlineSmall = headlineSmall.copy(fontFamily = loraFontFamily),
      titleLarge = titleLarge.copy(fontFamily = loraFontFamily),
      titleMedium = titleMedium.copy(fontFamily = loraFontFamily),
      titleSmall = titleSmall.copy(fontFamily = loraFontFamily),
      bodyLarge = bodyLarge.copy(fontFamily = interFontFamily),
      bodyMedium = bodyMedium.copy(fontFamily = interFontFamily),
      bodySmall = bodySmall.copy(fontFamily = interFontFamily),
      labelLarge = labelLarge.copy(fontFamily = jetbrainsMonoFontFamily),
      labelMedium = labelMedium.copy(fontFamily = jetbrainsMonoFontFamily),
      labelSmall = labelSmall.copy(fontFamily = jetbrainsMonoFontFamily),
    )
  }
