package dev.halim.shelfdroid.core.data.screen.backups

import dev.halim.shelfdroid.core.data.download.ManagedDownload
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupsUiStateTest {

  @Test
  fun backupItem_toManagedDownload_preservesBackupFilenameAndUrl() {
    val backup =
      BackupsUiState.BackupItem(
        id = "backup-1",
        filename = "backup-2026-05-18.audiobookshelf",
        fileSize = "4.20 MB",
        createdAt = "18 May 2026 2:00PM",
        serverVersion = "2.0.0",
        downloadUrl = "https://example.com/api/backups/backup-1/download?token=test",
      )

    assertEquals(
      ManagedDownload(
        url = "https://example.com/api/backups/backup-1/download?token=test",
        title = "backup-2026-05-18.audiobookshelf",
        filename = "backup-2026-05-18.audiobookshelf",
      ),
      backup.toManagedDownload(),
    )
  }
}
