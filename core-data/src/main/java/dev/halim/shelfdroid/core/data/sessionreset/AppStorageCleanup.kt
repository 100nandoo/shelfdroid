package dev.halim.shelfdroid.core.data.sessionreset

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AppStorageCleanup internal constructor(private val cacheDirectories: () -> List<File>) {
  @Inject
  constructor(
    @ApplicationContext context: Context
  ) : this(cacheDirectories = { listOfNotNull(context.cacheDir, context.externalCacheDir) })

  fun clear() {
    cacheDirectories().forEach(File::deleteRecursively)
  }
}
