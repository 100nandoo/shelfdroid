package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.download.ManagedDownload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditItemLibraryFileDownloadUseCaseTest {

  @Test
  fun prepare_resolvesSignedUrl_andBuildsManagedDownload_andClearsBusyState() = runTest {
    val resolver = FakeLibraryFileDownloadUrlResolver()
    val useCase = EditItemLibraryFileDownloadUseCase(resolver)
    val state =
      EditItemUiState(
        state = GenericState.Success,
        itemId = "item-1",
        activeFileActionIno = "ino-1",
        libraryFiles =
          listOf(
            LibraryFileRow(
              ino = "ino-1",
              path = "/books/example.m4b",
              filename = "example.m4b",
              sizeText = "12 MB",
              fileType = "audio",
            )
          ),
      )

    val result = useCase.prepare(state, "ino-1")

    assertTrue(result is EditItemLibraryFileDownloadResult.Success)
    val success = result as EditItemLibraryFileDownloadResult.Success
    assertEquals(null, success.state.activeFileActionIno)
    assertEquals("item-1" to "ino-1", resolver.lastRequest)
    assertEquals(
      ManagedDownload(
        url = "https://example.com/api/items/item-1/file/ino-1/download?token=test",
        title = "example.m4b",
        filename = "example.m4b",
      ),
      success.download,
    )
  }

  @Test
  fun prepare_whenResolutionFails_returnsError_andClearsBusyState() = runTest {
    val useCase =
      EditItemLibraryFileDownloadUseCase(
        object : LibraryFileDownloadUrlResolver {
          override suspend fun resolve(itemId: String, ino: String): String {
            error("Signed URL failed")
          }
        }
      )
    val state =
      EditItemUiState(
        state = GenericState.Success,
        itemId = "item-1",
        activeFileActionIno = "ino-1",
        libraryFiles =
          listOf(
            LibraryFileRow(
              ino = "ino-1",
              path = "/books/example.m4b",
              filename = "example.m4b",
              sizeText = "12 MB",
              fileType = "audio",
            )
          ),
      )

    val result = useCase.prepare(state, "ino-1")

    assertTrue(result is EditItemLibraryFileDownloadResult.Failure)
    val failure = result as EditItemLibraryFileDownloadResult.Failure
    assertEquals(null, failure.state.activeFileActionIno)
    assertEquals("Signed URL failed", failure.message)
  }
}

private class FakeLibraryFileDownloadUrlResolver : LibraryFileDownloadUrlResolver {
  var lastRequest: Pair<String, String>? = null

  override suspend fun resolve(itemId: String, ino: String): String {
    lastRequest = itemId to ino
    return "https://example.com/api/items/$itemId/file/$ino/download?token=test"
  }
}
