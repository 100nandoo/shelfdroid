package dev.halim.socketio

sealed interface SocketEvent {
  val name: String

  data object Connect : SocketEvent { override val name = "connect" }
  data object Disconnect : SocketEvent { override val name = "disconnect" }
  data object ConnectError : SocketEvent { override val name = "connect_error" }

  sealed interface Task : SocketEvent {
    data object Started : Task { override val name = "task_started" }
    data object Finished : Task { override val name = "task_finished" }
  }

  sealed interface Episode : SocketEvent {
    data object DownloadQueued : Episode { override val name = "episode_download_queued" }
    data object DownloadStarted : Episode { override val name = "episode_download_started" }
    data object DownloadFinished : Episode { override val name = "episode_download_finished" }
  }

  sealed interface Library : SocketEvent {
    data object Added : Library { override val name = "library_added" }
    data object Updated : Library { override val name = "library_updated" }
    data object Removed : Library { override val name = "library_removed" }
  }
}
