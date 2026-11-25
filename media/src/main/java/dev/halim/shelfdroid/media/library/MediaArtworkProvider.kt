package dev.halim.shelfdroid.media.library

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.AtomicFile
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.auth.AuthStateRepository
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.media.R
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Authenticator
import okhttp3.OkHttpClient

/** Serves cover artwork for the Catalog and Current playback. */
class MediaArtworkProvider : ContentProvider() {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface Dependencies {
    fun libraryItems(): LibraryItemRepository

    fun imageLoader(): ImageLoader

    fun authState(): AuthStateRepository

    fun httpClient(): OkHttpClient
  }

  private val dependencies by lazy {
    EntryPointAccessors.fromApplication<Dependencies>(requireNotNull(context))
  }
  private val coverLoader by lazy {
    // Artwork is opportunistic. A failed request must not start session recovery.
    val client = dependencies.httpClient().newBuilder().authenticator(Authenticator.NONE).build()
    ImageLoader.Builder(requireNotNull(context))
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
      .diskCache { dependencies.imageLoader().diskCache }
      .build()
  }

  override fun onCreate(): Boolean = true

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    val context = requireNotNull(context)
    val itemId = uri.pathSegments.singleOrNull()
    if (mode != "r" || itemId == null) {
      throw FileNotFoundException("Invalid cover request")
    }

    val isExternal = Binder.getCallingUid() != Process.myUid()
    if (isExternal && !hasAccessToken()) {
      throw FileNotFoundException("Cover is unavailable")
    }
    val item =
      dependencies.libraryItems().byId(itemId)
        ?: throw FileNotFoundException("Cover is unavailable")
    val cover = item.cover
    val unversionedUri = uriFor(context, itemId)
    if (uri != unversionedUri && uri != uriFor(context, itemId, cover)) {
      val query = uri.encodedQuery
      if (
        query == null ||
          !COVER_VERSION_QUERY.matches(query) ||
          uri != unversionedUri.buildUpon().encodedQuery(query).build()
      ) {
        throw FileNotFoundException("Invalid cover request")
      }
      return openFallback(context)
    }
    if (cover.isBlank()) return openFallback(context)

    val imageLoader = dependencies.imageLoader()
    openCachedCover(imageLoader, cover)?.let {
      return it
    }

    // Only an artwork request can fetch the cover. Building browse items is synchronous and local.
    runBlocking(Dispatchers.IO) {
      withTimeoutOrNull(ARTWORK_TIMEOUT_MS) {
        // Send the request through the normal server selection and Bearer header interceptor.
        val requestUrl =
          AudiobookshelfBaseUrl.DEFAULT.resolveEncoded(
            "/api/items/${Uri.encode(itemId)}/cover",
            encodedQuery = item.updatedAt.takeIf { it > 0 }?.let { "v=$it" },
          )
        val request =
          ImageRequest.Builder(context)
            .data(requestUrl)
            .diskCacheKey(cover)
            .size(512)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        coverLoader.execute(request)
      }
    }
    if (isExternal && !hasAccessToken()) throw FileNotFoundException("Cover is unavailable")
    return openCachedCover(imageLoader, cover) ?: openFallback(context)
  }

  private fun hasAccessToken(): Boolean =
    runBlocking(Dispatchers.IO) { dependencies.authState().token.first().isNotBlank() }

  @Synchronized
  private fun openFallback(context: Context): ParcelFileDescriptor {
    val file = context.cacheDir.resolve("car-artwork-fallback.png")
    if (!file.isFile) {
      val drawable = requireNotNull(context.getDrawable(R.drawable.ic_notification))
      val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
      try {
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
          if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            throw IOException("Unable to encode fallback artwork")
          }
          atomicFile.finishWrite(stream)
        } catch (error: Exception) {
          atomicFile.failWrite(stream)
          throw error
        }
      } finally {
        bitmap.recycle()
      }
    }
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
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
    private val COVER_VERSION_QUERY = Regex("v=[0-9a-f]{1,8}")

    internal fun authority(context: Context): String = "${context.packageName}.car-artwork"

    internal fun uriFor(context: Context, itemId: String, cover: String? = null): Uri =
      Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(authority(context))
        .appendPath(itemId)
        .apply {
          if (!cover.isNullOrBlank()) {
            appendQueryParameter("v", cover.hashCode().toUInt().toString(16))
          }
        }
        .build()
  }
}
