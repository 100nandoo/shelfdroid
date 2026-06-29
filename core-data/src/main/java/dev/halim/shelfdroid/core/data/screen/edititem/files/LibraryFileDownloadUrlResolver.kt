package dev.halim.shelfdroid.core.data.screen.edititem.files

import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

interface LibraryFileDownloadUrlResolver {
  suspend fun resolve(itemId: String, ino: String): String
}

class HelperLibraryFileDownloadUrlResolver @Inject constructor(private val helper: Helper) :
  LibraryFileDownloadUrlResolver {

  override suspend fun resolve(itemId: String, ino: String): String {
    return helper.fileDownloadUrl(itemId, ino)
  }
}
