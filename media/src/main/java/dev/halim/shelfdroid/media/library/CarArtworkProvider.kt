package dev.halim.shelfdroid.media.library

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.os.Process
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.auth.AuthStateRepository
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** The browsing callback can grant the requesting controller read access to returned cover URIs. */
class CarArtworkProvider : ContentProvider() {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface Dependencies {
    fun libraryItems(): LibraryItemRepository

    fun imageLoader(): ImageLoader

    fun authState(): AuthStateRepository
  }

  override fun onCreate(): Boolean = true

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    val context = requireNotNull(context)
    val itemId = uri.pathSegments.singleOrNull()
    if (mode != "r" || itemId == null) {
      throw FileNotFoundException("Invalid cover request")
    }

    val dependencies = EntryPointAccessors.fromApplication<Dependencies>(context)
    if (
      Binder.getCallingUid() != Process.myUid() &&
        runBlocking(Dispatchers.IO) { dependencies.authState().token.first().isBlank() }
    ) {
      throw FileNotFoundException("Cover is unavailable")
    }
    val cover =
      dependencies.libraryItems().byId(itemId)?.cover?.takeUnless(String::isBlank)
        ?: throw FileNotFoundException("Cover is unavailable")
    if (uri != uriFor(context, itemId) && uri != uriFor(context, itemId, cover)) {
      throw FileNotFoundException("Invalid cover request")
    }
    val imageLoader = dependencies.imageLoader()
    openCachedCover(imageLoader, cover)?.let {
      return it
    }

    // Only an artwork request can fetch the cover. Building browse items is synchronous and local.
    runBlocking(Dispatchers.IO) {
      withTimeoutOrNull(ARTWORK_TIMEOUT_MS) {
        val request =
          ImageRequest.Builder(context)
            .data(cover)
            .size(512)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        imageLoader.execute(request)
      }
    }
    return openCachedCover(imageLoader, cover)
      ?: throw FileNotFoundException("Cover is unavailable")
  }

  private fun openCachedCover(imageLoader: ImageLoader, cover: String): ParcelFileDescriptor? =
    imageLoader.diskCache?.openSnapshot(cover)?.use { snapshot ->
      ParcelFileDescriptor.open(snapshot.data.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

  override fun getType(uri: Uri): String = "image/*"

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? =
    throw UnsupportedOperationException()

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = throw UnsupportedOperationException()

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
    throw UnsupportedOperationException()

  companion object {
    private const val ARTWORK_TIMEOUT_MS = 3_000L

    internal fun uriFor(context: Context, itemId: String, cover: String? = null): Uri =
      Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority("${context.packageName}.car-artwork")
        .appendPath(itemId)
        .apply {
          if (cover != null) appendQueryParameter("v", cover.hashCode().toUInt().toString(16))
        }
        .build()
  }
}
