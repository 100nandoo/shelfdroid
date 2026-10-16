package dev.halim.shelfdroid.widget.player

import dev.halim.shelfdroid.core.PlayerUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class PlayerWidgetContent(
  val id: String,
  val episodeId: String,
  val title: String,
  val author: String,
  val cover: String,
)

internal fun PlayerUiState.toWidgetContent(): PlayerWidgetContent? =
  if (id.isBlank()) null else PlayerWidgetContent(id, episodeId, title, author, cover)

internal fun Flow<PlayerUiState>.widgetContent(): Flow<PlayerWidgetContent?> = map {
  it.toWidgetContent()
}.distinctUntilChanged()
