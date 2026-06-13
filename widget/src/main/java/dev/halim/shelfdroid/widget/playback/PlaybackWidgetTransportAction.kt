package dev.halim.shelfdroid.widget.playback

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback

internal class PlaybackWidgetTransportAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val action =
      parameters.get(TransportActionKey)?.let(PlaybackTransportAction::fromParameterValue) ?: return

    when (
      PlaybackWidgetTransportDispatcher().dispatch(action) {
        Media3PlaybackWidgetControllerFactory.connect(context)
      }
    ) {
      PlaybackWidgetTransportResult.OpenAppFallback -> context.startActivity(openAppIntent(context))
      PlaybackWidgetTransportResult.Dispatched,
      PlaybackWidgetTransportResult.NotAvailable -> Unit
    }
  }

  companion object {
    private val TransportActionKey = ActionParameters.Key<String>("transport_action")

    fun actionFor(action: PlaybackTransportAction): Action =
      actionRunCallback<PlaybackWidgetTransportAction>(
        actionParametersOf(TransportActionKey.to(action.parameterValue))
      )
  }
}
