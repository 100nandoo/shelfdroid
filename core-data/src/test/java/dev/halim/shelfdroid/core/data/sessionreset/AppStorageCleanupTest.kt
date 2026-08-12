package dev.halim.shelfdroid.core.data.sessionreset

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStorageCleanupTest {

  @Test
  fun clear_deletesOnlyAppCacheDirectories() {
    val root = Files.createTempDirectory("app-storage-cleanup-test").toFile()
    try {
      val internalCache = root.resolve("cache").apply { mkdirs() }
      internalCache.resolve("cached-cover").writeText("cover")
      val externalCache = root.resolve("external-cache").apply { mkdirs() }
      externalCache.resolve("cached-audio").writeText("audio")
      val completedDownloads = root.resolve("Downloads").apply { mkdirs() }
      val completedDownload = completedDownloads.resolve("completed-book.m4b")
      completedDownload.writeText("download")
      val cleanup = AppStorageCleanup { listOf(internalCache, externalCache) }

      cleanup.clear()

      assertFalse(internalCache.exists())
      assertFalse(externalCache.exists())
      assertTrue(completedDownload.exists())
    } finally {
      root.deleteRecursively()
    }
  }
}
