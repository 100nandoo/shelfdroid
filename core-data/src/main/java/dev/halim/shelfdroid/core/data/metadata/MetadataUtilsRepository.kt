package dev.halim.shelfdroid.core.data.metadata

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateCustomMetadataProviderRequest
import dev.halim.core.network.request.RenameGenreRequest
import dev.halim.core.network.request.RenameTagRequest
import dev.halim.core.network.response.CustomMetadataProvider as NetworkCustomMetadataProvider
import dev.halim.core.network.response.libraryitem.MEDIA_TYPE_BOOK
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationError
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationException
import dev.halim.shelfdroid.core.data.metadata.genre.GenreMutation
import dev.halim.shelfdroid.core.data.metadata.tag.TagMutation
import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

class MetadataUtilsRepository
@Inject
constructor(
  private val api: ApiService,
  private val tagRepository: TagRepository,
  private val helper: Helper,
) : MetadataUtilsContract {

  override suspend fun loadTags(): Result<List<String>> {
    val response =
      api.tags().getOrElse {
        return Result.failure(it)
      }
    tagRepository.save(response)
    return Result.success(response.tags.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameTag(tag: String, newTag: String): Result<TagMutation> {
    if (newTag.trim().isBlank()) {
      return Result.failure(
        MetadataValidationException(MetadataValidationError.TagNameRequired),
      )
    }
    val response =
      api.renameTag(RenameTagRequest(tag = tag, newTag = newTag)).getOrElse {
        return Result.failure(it)
      }
    try {
      refreshTagCacheOrFail()
    } catch (error: Throwable) {
      return Result.failure(error)
    }
    return Result.success(TagMutation(response.numItemsUpdated, response.tagMerged))
  }

  override suspend fun deleteTag(tag: String): Result<TagMutation> {
    val response =
      api.deleteTag(helper.encodeTagPath(tag)).getOrElse {
        return Result.failure(it)
      }
    try {
      refreshTagCacheOrFail()
    } catch (error: Throwable) {
      return Result.failure(error)
    }
    return Result.success(TagMutation(response.numItemsUpdated))
  }

  override suspend fun loadGenres(): Result<List<String>> {
    val response =
      api.genres().getOrElse {
        return Result.failure(it)
      }
    return Result.success(response.genres.sortedWith(String.CASE_INSENSITIVE_ORDER))
  }

  override suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> {
    if (newGenre.trim().isBlank()) {
      return Result.failure(
        MetadataValidationException(MetadataValidationError.GenreNameRequired),
      )
    }
    val response =
      api.renameGenre(RenameGenreRequest(genre = genre, newGenre = newGenre)).getOrElse {
        return Result.failure(it)
      }
    return Result.success(GenreMutation(response.numItemsUpdated, response.genreMerged))
  }

  override suspend fun deleteGenre(genre: String): Result<GenreMutation> {
    val response =
      api.deleteGenre(helper.encodeGenrePath(genre)).getOrElse {
        return Result.failure(it)
      }
    return Result.success(GenreMutation(response.numItemsUpdated))
  }

  override suspend fun loadCustomMetadataProviders(): Result<List<CustomMetadataProvider>> {
    val response =
      api.customMetadataProviders().getOrElse {
        return Result.failure(normalizeProviderFailure(it))
      }
    return Result.success(
      response.providers
        .asSequence()
        .filter { it.mediaType == MEDIA_TYPE_BOOK }
        .map(::mapProvider)
        .toList(),
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
      return Result.failure(
        MetadataValidationException(MetadataValidationError.CustomMetadataProviderNameRequired),
      )
    }
    if (normalizedUrl.isBlank()) {
      return Result.failure(
        MetadataValidationException(MetadataValidationError.CustomMetadataProviderUrlRequired),
      )
    }
    val response =
      api
        .createCustomMetadataProvider(
          CreateCustomMetadataProviderRequest(
            name = normalizedName,
            url = normalizedUrl,
            mediaType = MEDIA_TYPE_BOOK,
            authHeaderValue = authHeaderValue?.takeUnless { it.isBlank() },
          ),
        )
        .getOrElse { return Result.failure(normalizeProviderFailure(it)) }
    return Result.success(mapProvider(response.provider))
  }

  override suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> {
    if (providerId.isBlank()) {
      return Result.failure(
        MetadataValidationException(MetadataValidationError.CustomMetadataProviderIdRequired),
      )
    }
    return api
      .deleteCustomMetadataProvider(providerId)
      .fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(normalizeProviderFailure(it)) },
      )
  }

  private suspend fun refreshTagCacheOrFail() {
    tagRepository.refreshTags().getOrElse { throw it }
  }

  private fun mapProvider(provider: NetworkCustomMetadataProvider): CustomMetadataProvider =
    CustomMetadataProvider(
      id = provider.id,
      name = provider.name,
      url = provider.url,
      mediaType = provider.mediaType,
      slug = provider.slug,
      authHeaderValue = provider.authHeaderValue,
    )

  private fun normalizeProviderFailure(error: Throwable): Throwable {
    val httpException = error as? HttpException ?: return error
    val body = httpException.response()?.errorBody()?.string()?.trim()
    val detail = body?.let(::extractProviderFailureDetail)
    return if (detail.isNullOrBlank()) error else IllegalStateException(detail, error)
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
