package dev.halim.shelfdroid.core.data.screen.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider.DATABASE_NAME as EXOPLAYER_DATABASE_NAME
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.database.di.DatabaseModule.DATABASE_NAME
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  private val fileDir: File,
  @ApplicationContext private val context: Context,
) {

  val darkMode = dataStoreManager.darkMode
  val dynamicTheme = dataStoreManager.dynamicTheme
  val token = dataStoreManager.userPrefs.map { it.accessToken }
  val userPrefs = dataStoreManager.userPrefs

  suspend fun logout(): Result<Unit> {
    val result = api.logout(userPrefs.first().refreshToken)
    result.onSuccess { _ ->
      clearDir()
      dataStoreManager.clear()
      return Result.success(Unit)
    }
    result.onFailure { error ->
      return Result.failure(error)
    }
    return Result.failure(Exception("Logout failed"))
  }

  suspend fun updateDarkMode(enabled: Boolean) {
    dataStoreManager.updateDarkMode(enabled)
  }

  suspend fun updateDynamicTheme(enabled: Boolean) {
    dataStoreManager.updateDynamicTheme(enabled)
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun clearDir() {
    val cacheDir = context.cacheDir
    cacheDir.deleteRecursively()

    context.deleteDatabase(EXOPLAYER_DATABASE_NAME)
    context.deleteDatabase(DATABASE_NAME)

    val externalCacheDir = context.externalCacheDir
    externalCacheDir?.deleteRecursively()

    fileDir.deleteRecursively()

    ProcessPhoenix.triggerRebirth(context)
  }
}
