package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.shelfdroid.core.data.download.ManagedDownload
import javax.inject.Inject

class EditItemLibraryFileDownloadUseCase
@Inject
constructor(private val urlResolver: LibraryFileDownloadUrlResolver) {

  suspend fun prepare(state: EditItemUiState, ino: String): EditItemLibraryFileDownloadResult {
    val file = state.libraryFiles.find { it.ino == ino }
    if (file == null) {
      return EditItemLibraryFileDownloadResult.Failure(
        state = state.copy(activeFileActionIno = null),
        message = "Failed to download file",
      )
    }

    val url =
      runCatching { urlResolver.resolve(state.itemId, ino) }
        .getOrElse {
          return EditItemLibraryFileDownloadResult.Failure(
            state = state.copy(activeFileActionIno = null),
            message = it.message ?: "Failed to download file",
          )
        }

    return EditItemLibraryFileDownloadResult.Success(
      state = state.copy(activeFileActionIno = null),
      download = ManagedDownload(url = url, title = file.filename, filename = file.filename),
    )
  }
}

sealed interface EditItemLibraryFileDownloadResult {
  val state: EditItemUiState

  data class Success(override val state: EditItemUiState, val download: ManagedDownload) :
    EditItemLibraryFileDownloadResult

  data class Failure(override val state: EditItemUiState, val message: String) :
    EditItemLibraryFileDownloadResult
}
