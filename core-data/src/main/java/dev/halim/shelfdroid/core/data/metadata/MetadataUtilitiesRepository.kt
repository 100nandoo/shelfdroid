package dev.halim.shelfdroid.core.data.metadata

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.RenameGenreRequest
import dev.halim.core.network.request.RenameTagRequest
import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

/**
 * Domain seam for the server-wide Library item metadata utilities.
 *
 * The server remains authoritative for authorization, merge outcomes, and mutation counts.
 */
interface MetadataUtilitiesRepositoryContract {
  suspend fun loadTags(): Result<List<String>>

  suspend fun renameTag(tag: String, newTag: String): Result<TagMutation>

  suspend fun deleteTag(tag: String): Result<TagMutation>

  suspend fun loadGenres(): Result<List<String>> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

  suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

  suspend fun deleteGenre(genre: String): Result<GenreMutation> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

}

class MetadataUtilitiesRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  private val tagRepository: TagRepository,
) : MetadataUtilitiesRepositoryContract {

  override suspend fun loadTags(): Result<List<String>> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response = api.tags().getOrElse { return Result.failure(normalizeFailure(it)) }
    // The existing TagRepository remains the owner of the administrative cache.
    tagRepository.save(response)
    return Result.success(response.tags.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameTag(tag: String, newTag: String): Result<TagMutation> {
    if (newTag.trim().isBlank()) return Result.failure(TagNameRequiredException())
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.renameTag(RenameTagRequest(tag = tag, newTag = newTag))
        .getOrElse { return Result.failure(normalizeFailure(it)) }
    try {
      refreshTagCacheOrFail()
    } catch (error: Throwable) {
      return Result.failure(normalizeFailure(error))
    }
    return Result.success(TagMutation(response.numItemsUpdated, response.tagMerged))
  }

  override suspend fun deleteTag(tag: String): Result<TagMutation> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.deleteTag(encodeTagPath(tag)).getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    try {
      refreshTagCacheOrFail()
    } catch (error: Throwable) {
      return Result.failure(normalizeFailure(error))
    }
    return Result.success(TagMutation(response.numItemsUpdated))
  }

  override suspend fun loadGenres(): Result<List<String>> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response = api.genres().getOrElse { return Result.failure(normalizeFailure(it)) }
    return Result.success(response.genres.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> {
    if (newGenre.trim().isBlank()) return Result.failure(GenreNameRequiredException())
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.renameGenre(RenameGenreRequest(genre = genre, newGenre = newGenre))
        .getOrElse { return Result.failure(normalizeFailure(it)) }
    return Result.success(GenreMutation(response.numItemsUpdated, response.genreMerged))
  }

  override suspend fun deleteGenre(genre: String): Result<GenreMutation> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.deleteGenre(encodeGenrePath(genre)).getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    return Result.success(GenreMutation(response.numItemsUpdated))
  }

  private suspend fun refreshTagCacheOrFail() {
    // A successful mutation must not expose stale values to later admin workflows. If refresh
    // fails, surface that failure to the caller rather than claiming the cache is current.
    tagRepository.refreshTags().getOrElse { throw normalizeFailure(it) }
  }

  private suspend fun isAdmin(): Boolean = dataStoreManager.userPrefs.first().isAdmin

  private fun normalizeFailure(error: Throwable): Throwable =
    if (error.isAccessDenied()) MetadataAccessDeniedException(error) else error
}

/** Standard UTF-8 Base64 followed by URI escaping, as required by the Audiobookshelf endpoint. */
fun encodeTagPath(tag: String): String {
  return encodeMetadataPath(tag)
}

/** Standard UTF-8 Base64 followed by URI escaping for Audiobookshelf's Genre endpoint. */
fun encodeGenrePath(genre: String): String = encodeMetadataPath(genre)

private fun encodeMetadataPath(value: String): String {
  val base64 = Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
  return URLEncoder.encode(base64, StandardCharsets.UTF_8)
}

class MetadataAccessDeniedException(cause: Throwable? = null) :
  IllegalStateException("The Audiobookshelf server denied access to this administrative operation.", cause)

class TagNameRequiredException : IllegalArgumentException("A Tag name cannot be blank.")

class GenreNameRequiredException : IllegalArgumentException("A Genre name cannot be blank.")

private fun Throwable.isAccessDenied(): Boolean =
  (this as? HttpException)?.code() == 403 ||
    message?.contains("403") == true ||
    cause?.isAccessDenied() == true
