package dev.halim.shelfdroid.core.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Devices.PIXEL_7_PRO
import androidx.compose.ui.tooling.preview.Preview

@Preview(
  device = PIXEL_7_PRO,
  name = "Dark",
  group = "Dark",
  showBackground = true,
  uiMode = UI_MODE_NIGHT_YES,
  fontScale = 1f,
)
@Preview(
  device = PIXEL_7_PRO,
  name = "Light",
  group = "Light",
  showBackground = true,
  uiMode = UI_MODE_NIGHT_NO,
  fontScale = 1f,
)
annotation class ShelfDroidPreview
