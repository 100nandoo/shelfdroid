package dev.halim.shelfdroid.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
class PlayerWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      Box(
        modifier =
          GlanceModifier.fillMaxSize()
            .background(ColorProvider(Color(0xFFFFC981)))
            .cornerRadius(R.dimen.player_widget_background_radius)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "Hello Shelfdroid",
          style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp),
        )
      }
    }
  }
}
