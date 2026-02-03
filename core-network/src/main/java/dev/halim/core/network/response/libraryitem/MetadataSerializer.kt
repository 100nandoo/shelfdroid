package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object MetadataSerializer : JsonContentPolymorphicSerializer<Metadata>(Metadata::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Metadata> {
    val jsonObject = element.jsonObject
    return when {
      "authors" in jsonObject -> BookMetadata.serializer()
      "feedUrl" in jsonObject -> PodcastMetadata.serializer()
      // Fallback or error handling
      else ->
        throw SerializationException(
          "Unknown Metadata type. Missing 'authors' or 'feedUrl' in: $jsonObject"
        )
    }
  }
}
