package dev.halim.core.network.response

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = MatchItemResultSerializer::class)
sealed interface MatchItemResult {
  @Serializable
  data class Success(
    @SerialName("libraryItem") val libraryItem: LibraryItem,
    @SerialName("updated") val updated: Boolean = false,
  ) : MatchItemResult

  @Serializable data class Warning(@SerialName("warning") val message: String) : MatchItemResult
}

object MatchItemResultSerializer :
  JsonContentPolymorphicSerializer<MatchItemResult>(MatchItemResult::class) {
  override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MatchItemResult> =
    when {
      "warning" in element.jsonObject -> MatchItemResult.Warning.serializer()
      else -> MatchItemResult.Success.serializer()
    }
}
