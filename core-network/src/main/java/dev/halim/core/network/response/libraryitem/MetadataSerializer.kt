package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object MetadataSerializer : JsonContentPolymorphicSerializer<Metadata>(Metadata::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Metadata> {
    val jsonObject = element.jsonObject
    return when {
      "itunesPageUrl" in jsonObject -> PodcastMetadata.serializer()
      else -> BookMetadata.serializer()
    }
  }
}
