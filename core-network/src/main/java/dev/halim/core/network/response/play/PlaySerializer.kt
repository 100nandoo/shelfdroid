package dev.halim.core.network.response.play

import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.PodcastMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object PlaySerializer : KSerializer<PlayResponse> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlayResponse")

  override fun deserialize(decoder: Decoder): PlayResponse {
    require(decoder is JsonDecoder)
    val jsonObject = decoder.decodeJsonElement().jsonObject

    val mediaType =
      jsonObject["mediaType"]?.jsonPrimitive?.contentOrNull
        ?: throw SerializationException("mediaType field is missing")

    val mediaMetadata =
      jsonObject["mediaMetadata"]?.let {
        when (mediaType) {
          "book" -> decoder.json.decodeFromJsonElement<BookMetadata>(it)
          "podcast" -> decoder.json.decodeFromJsonElement<PodcastMetadata>(it)
          else -> throw SerializationException("Unknown mediaType: $mediaType")
        }
      } ?: throw SerializationException("mediaMetadata field is missing")

    return PlayResponse(
      id = jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: "",
      userId = jsonObject["userId"]?.jsonPrimitive?.contentOrNull ?: "",
      libraryId = jsonObject["libraryId"]?.jsonPrimitive?.contentOrNull ?: "",
      libraryItemId = jsonObject["libraryItemId"]?.jsonPrimitive?.contentOrNull ?: "",
      bookId = jsonObject["bookId"]?.jsonPrimitive?.contentOrNull ?: "",
      episodeId = jsonObject["episodeId"]?.jsonPrimitive?.contentOrNull,
      mediaType = mediaType,
      mediaMetadata = mediaMetadata,
      chapters = jsonObject["chapters"]?.let { decoder.json.decodeFromJsonElement(it) } ?: listOf(),
      displayTitle = jsonObject["displayTitle"]?.jsonPrimitive?.contentOrNull ?: "",
      displayAuthor = jsonObject["displayAuthor"]?.jsonPrimitive?.contentOrNull ?: "",
      coverPath = jsonObject["coverPath"]?.jsonPrimitive?.contentOrNull ?: "",
      duration = jsonObject["duration"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
      playMethod = jsonObject["playMethod"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
      mediaPlayer = jsonObject["mediaPlayer"]?.jsonPrimitive?.contentOrNull ?: "",
      deviceInfo =
        jsonObject["deviceInfo"]?.let { decoder.json.decodeFromJsonElement(it) } ?: DeviceInfo(),
      serverVersion = jsonObject["serverVersion"]?.jsonPrimitive?.contentOrNull ?: "",
      date = jsonObject["date"]?.jsonPrimitive?.contentOrNull ?: "",
      dayOfWeek = jsonObject["dayOfWeek"]?.jsonPrimitive?.contentOrNull ?: "",
      timeListening = jsonObject["timeListening"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
      startTime = jsonObject["startTime"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
      currentTime =
        jsonObject["currentTime"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
      startedAt = jsonObject["startedAt"]?.jsonPrimitive?.longOrNull ?: 0,
      updatedAt = jsonObject["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0,
      audioTracks =
        jsonObject["audioTracks"]?.let { decoder.json.decodeFromJsonElement(it) } ?: listOf(),
      libraryItem =
        jsonObject["libraryItem"]?.let { decoder.json.decodeFromJsonElement(it) }
          ?: throw SerializationException("libraryItem field is missing"),
    )
  }

  override fun serialize(encoder: Encoder, value: PlayResponse) {
    require(encoder is JsonEncoder)

    val jsonObject = buildJsonObject {
      put("id", value.id)
      put("userId", value.userId)
      put("libraryId", value.libraryId)
      put("libraryItemId", value.libraryItemId)
      put("bookId", value.bookId)
      value.episodeId?.let { put("episodeId", it) }
      put("mediaType", value.mediaType)
      put("displayTitle", value.displayTitle)
      put("displayAuthor", value.displayAuthor)
      put("coverPath", value.coverPath)
      put("duration", value.duration)
      put("playMethod", value.playMethod)
      put("mediaPlayer", value.mediaPlayer)
      put("serverVersion", value.serverVersion)
      put("date", value.date)
      put("dayOfWeek", value.dayOfWeek)
      put("timeListening", value.timeListening)
      put("startTime", value.startTime)
      put("currentTime", value.currentTime)
      put("startedAt", value.startedAt)
      put("updatedAt", value.updatedAt)

      val mediaMetadataJson = encoder.json.encodeToJsonElement(value.mediaMetadata)
      put("media", mediaMetadataJson)

      val chaptersJson = encoder.json.encodeToJsonElement(value.chapters)
      put("chapters", chaptersJson)

      val deviceInfoJson = encoder.json.encodeToJsonElement(value.deviceInfo)
      put("deviceInfo", deviceInfoJson)

      val audioTracksJson = encoder.json.encodeToJsonElement(value.audioTracks)
      put("audioTracks", audioTracksJson)

      val libraryItemJson = encoder.json.encodeToJsonElement(value.libraryItem)
      put("libraryItem", libraryItemJson)
    }

    encoder.encodeJsonElement(jsonObject)
  }
}
