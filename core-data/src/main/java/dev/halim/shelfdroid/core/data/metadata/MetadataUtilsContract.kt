package dev.halim.shelfdroid.core.data.metadata

interface MetadataUtilsContract {
  suspend fun loadTags(): Result<List<String>>

  suspend fun renameTag(tag: String, newTag: String): Result<TagMutation>

  suspend fun deleteTag(tag: String): Result<TagMutation>

  suspend fun loadGenres(): Result<List<String>> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

  suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

  suspend fun deleteGenre(genre: String): Result<GenreMutation> =
    Result.failure(UnsupportedOperationException("Genre management is not implemented."))

  suspend fun loadCustomMetadataProviders(): Result<List<CustomMetadataProvider>> =
    Result.failure(
      UnsupportedOperationException("Custom metadata provider management is not implemented.")
    )

  suspend fun createCustomMetadataProvider(
    name: String,
    url: String,
    authHeaderValue: String?,
  ): Result<CustomMetadataProvider> =
    Result.failure(
      UnsupportedOperationException("Custom metadata provider management is not implemented.")
    )

  suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> =
    Result.failure(
      UnsupportedOperationException("Custom metadata provider management is not implemented.")
    )
}
