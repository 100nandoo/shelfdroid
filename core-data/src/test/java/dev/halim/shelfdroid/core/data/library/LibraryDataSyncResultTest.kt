package dev.halim.shelfdroid.core.data.library

import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDataSyncResultTest {

  @Test
  fun unknownHostFailure_isTransportFailure() {
    val result =
      LibraryDataSyncResult(
        libraries = Result.failure(UnknownHostException("example.com")),
        items = null,
      )

    assertTrue(result.isTransportFailure)
  }

  @Test
  fun wrappedIoFailure_isTransportFailure() {
    val result =
      LibraryDataSyncResult(
        libraries = Result.failure(IllegalStateException("request failed", IOException("timeout"))),
        items = null,
      )

    assertTrue(result.isTransportFailure)
  }

  @Test
  fun nonTransportFailure_isNotTransportFailure() {
    val result =
      LibraryDataSyncResult(
        libraries = Result.failure(IllegalStateException("invalid response")),
        items = null,
      )

    assertFalse(result.isTransportFailure)
  }

  @Test
  fun mixedItemFailures_preserveNonTransportFailure() {
    val networkFailure = UnknownHostException("example.com")
    val nonTransportFailure = IllegalStateException("invalid response")
    val result =
      LibraryDataSyncResult(
        libraries = Result.success(Unit),
        items =
          LibraryItemRefreshResult(
            refreshedLibraryIds = emptySet(),
            failures =
              listOf(
                LibraryItemRefreshFailure("books", networkFailure),
                LibraryItemRefreshFailure("podcasts", nonTransportFailure),
              ),
          ),
      )

    assertFalse(result.isTransportFailure)
    assertSame(nonTransportFailure, result.error)
  }
}
