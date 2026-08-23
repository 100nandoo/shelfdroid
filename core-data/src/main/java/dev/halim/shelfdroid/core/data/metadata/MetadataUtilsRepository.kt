package dev.halim.shelfdroid.core.data.metadata

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateCustomMetadataProviderRequest
import dev.halim.core.network.request.RenameGenreRequest
import dev.halim.core.network.request.RenameTagRequest
import dev.halim.core.network.response.CustomMetadataProvider as NetworkCustomMetadataProvider
import dev.halim.core.network.response.libraryitem.MEDIA_TYPE_BOOK
import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

class MetadataUtilsRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  private val tagRepository: TagRepository,
  private val helper: Helper,
) : MetadataUtilsContract {

  override suspend fun loadTags(): Result<List<String>> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.tags().getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    // The existing TagRepository remains the owner of the administrative cache.
    tagRepository.save(response)
    return Result.success(response.tags.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameTag(tag: String, newTag: String): Result<TagMutation> {
    if (newTag.trim().isBlank()) return Result.failure(TagNameRequiredException())
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.renameTag(RenameTagRequest(tag = tag, newTag = newTag)).getOrElse {
        return Result.failure(normalizeFailure(it))
      }
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
      api.deleteTag(helper.encodeTagPath(tag)).getOrElse {
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
    val response =
      api.genres().getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    return Result.success(response.genres.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> {
    if (newGenre.trim().isBlank()) return Result.failure(GenreNameRequiredException())
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.renameGenre(RenameGenreRequest(genre = genre, newGenre = newGenre)).getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    return Result.success(GenreMutation(response.numItemsUpdated, response.genreMerged))
  }

  override suspend fun deleteGenre(genre: String): Result<GenreMutation> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.deleteGenre(helper.encodeGenrePath(genre)).getOrElse {
        return Result.failure(normalizeFailure(it))
      }
    return Result.success(GenreMutation(response.numItemsUpdated))
  }

  override suspend fun loadCustomMetadataProviders(): Result<List<CustomMetadataProvider>> {
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api.customMetadataProviders().getOrElse {
        return Result.failure(normalizeProviderFailure(it))
      }
    return Result.success(
      response.providers
        .asSequence()
        .filter { it.mediaType == MEDIA_TYPE_BOOK }
        .map(::mapProvider)
        .toList()
    )
  }

  override suspend fun createCustomMetadataProvider(
    name: String,
    url: String,
    authHeaderValue: String?,
  ): Result<CustomMetadataProvider> {
    val normalizedName = name.trim()
    val normalizedUrl = url.trim()
    if (normalizedName.isBlank()) {
      return Result.failure(CustomMetadataProviderNameRequiredException())
    }
    if (normalizedUrl.isBlank()) {
      return Result.failure(CustomMetadataProviderUrlRequiredException())
    }
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    val response =
      api
        .createCustomMetadataProvider(
          CreateCustomMetadataProviderRequest(
            name = normalizedName,
            url = normalizedUrl,
            mediaType = MEDIA_TYPE_BOOK,
            authHeaderValue = authHeaderValue?.takeUnless { it.isBlank() },
          )
        )
        .getOrElse {
          return Result.failure(normalizeProviderFailure(it))
        }
    return Result.success(mapProvider(response.provider))
  }

  override suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> {
    if (providerId.isBlank()) return Result.failure(CustomMetadataProviderIdRequiredException())
    if (!isAdmin()) return Result.failure(MetadataAccessDeniedException())
    return api
      .deleteCustomMetadataProvider(providerId)
      .fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(normalizeProviderFailure(it)) },
      )
  }

  private suspend fun refreshTagCacheOrFail() {
    // A successful mutation must not expose stale values to later admin workflows. If refresh
    // fails, surface that failure to the caller rather than claiming the cache is current.
    tagRepository.refreshTags().getOrElse { throw normalizeFailure(it) }
  }

  private suspend fun isAdmin(): Boolean = dataStoreManager.userPrefs.first().isAdmin

  private fun mapProvider(provider: NetworkCustomMetadataProvider): CustomMetadataProvider =
    CustomMetadataProvider(
      id = provider.id,
      name = provider.name,
      url = provider.url,
      mediaType = provider.mediaType,
      slug = provider.slug,
      authHeaderValue = provider.authHeaderValue,
    )

  private fun normalizeFailure(error: Throwable): Throwable =
    if (error.isAccessDenied()) MetadataAccessDeniedException(error) else error

  private fun normalizeProviderFailure(error: Throwable): Throwable {
    val normalized = normalizeFailure(error)
    if (normalized is MetadataAccessDeniedException) return normalized
    val httpException = normalized as? HttpException ?: return normalized
    val body = httpException.response()?.errorBody()?.string()?.trim()
    val detail = body?.let(::extractProviderFailureDetail)
    return if (detail.isNullOrBlank()) normalized else IllegalStateException(detail, normalized)
  }

  private fun extractProviderFailureDetail(body: String): String {
    val payload = runCatching { Json.parseToJsonElement(body) }.getOrNull()
    val objectPayload = payload as? JsonObject ?: return body
    return listOf("error", "message")
      .asSequence()
      .mapNotNull { key -> (objectPayload[key] as? JsonPrimitive)?.contentOrNull }
      .firstOrNull() ?: body
  }
}

data class CustomMetadataProvider(
  val id: String,
  val name: String,
  val url: String,
  val mediaType: String = MEDIA_TYPE_BOOK,
  val slug: String = "",
  val authHeaderValue: String? = null,
)

class MetadataAccessDeniedException(cause: Throwable? = null) :
  IllegalStateException(
    "The Audiobookshelf server denied access to this administrative operation.",
    cause,
  )

class TagNameRequiredException : IllegalArgumentException("A Tag name cannot be blank.")

class GenreNameRequiredException : IllegalArgumentException("A Genre name cannot be blank.")

class CustomMetadataProviderNameRequiredException :
  IllegalArgumentException("Custom metadata provider name cannot be blank.")

class CustomMetadataProviderUrlRequiredException :
  IllegalArgumentException("Custom metadata provider URL cannot be blank.")

class CustomMetadataProviderIdRequiredException :
  IllegalArgumentException("Custom metadata provider ID cannot be blank.")

private fun Throwable.isAccessDenied(): Boolean =
  (this as? HttpException)?.code() == 403 ||
    message?.contains("403") == true ||
    cause?.isAccessDenied() == true
