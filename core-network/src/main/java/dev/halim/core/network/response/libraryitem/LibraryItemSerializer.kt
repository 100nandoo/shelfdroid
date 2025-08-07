package dev.halim.core.network.response.libraryitem

import dev.halim.core.network.response.LibraryItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

const val MEDIA_TYPE_BOOK = "book"
const val MEDIA_TYPE_PODCAST = "podcast"

object LibraryItemSerializer : KSerializer<LibraryItem> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LibraryItem")

  override fun deserialize(decoder: Decoder): LibraryItem {
    require(decoder is JsonDecoder)
    val jsonObject = decoder.decodeJsonElement().jsonObject

    val mediaType =
      jsonObject["mediaType"]?.jsonPrimitive?.contentOrNull
        ?: throw SerializationException("mediaType field is missing")

    val media =
      jsonObject["media"]?.let {
        when (mediaType) {
          MEDIA_TYPE_BOOK -> decoder.json.decodeFromJsonElement<Book>(it)
          MEDIA_TYPE_PODCAST -> decoder.json.decodeFromJsonElement<Podcast>(it)
          else -> throw SerializationException("Unknown mediaType: $mediaType")
        }
      } ?: throw SerializationException("media field is missing")

    return LibraryItem(
      id = jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: "",
      ino = jsonObject["ino"]?.jsonPrimitive?.contentOrNull ?: "",
      libraryId = jsonObject["libraryId"]?.jsonPrimitive?.contentOrNull ?: "",
      folderId = jsonObject["folderId"]?.jsonPrimitive?.contentOrNull ?: "",
      path = jsonObject["path"]?.jsonPrimitive?.contentOrNull ?: "",
      relPath = jsonObject["relPath"]?.jsonPrimitive?.contentOrNull ?: "",
      isFile = jsonObject["isFile"]?.jsonPrimitive?.booleanOrNull ?: false,
      mtimeMs = jsonObject["mtimeMs"]?.jsonPrimitive?.longOrNull ?: 0L,
      ctimeMs = jsonObject["ctimeMs"]?.jsonPrimitive?.longOrNull ?: 0L,
      birthtimeMs = jsonObject["birthtimeMs"]?.jsonPrimitive?.longOrNull ?: 0L,
      addedAt = jsonObject["addedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
      updatedAt = jsonObject["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
      lastScan = jsonObject["lastScan"]?.jsonPrimitive?.longOrNull ?: 0L,
      scanVersion = jsonObject["scanVersion"]?.jsonPrimitive?.contentOrNull ?: "",
      isMissing = jsonObject["isMissing"]?.jsonPrimitive?.booleanOrNull ?: false,
      isInvalid = jsonObject["isInvalid"]?.jsonPrimitive?.booleanOrNull ?: false,
      mediaType = mediaType,
      media = media,
      libraryFiles =
        jsonObject["libraryFiles"]?.let { decoder.json.decodeFromJsonElement(it) } ?: emptyList(),
    )
  }

  override fun serialize(encoder: Encoder, value: LibraryItem) {
    require(encoder is JsonEncoder)

    val jsonObject = buildJsonObject {
      put("id", value.id)
      put("ino", value.ino)
      put("libraryId", value.libraryId)
      put("folderId", value.folderId)
      put("path", value.path)
      put("relPath", value.relPath)
      put("isFile", value.isFile)
      put("mtimeMs", value.mtimeMs)
      put("ctimeMs", value.ctimeMs)
      put("birthtimeMs", value.birthtimeMs)
      put("addedAt", value.addedAt)
      put("updatedAt", value.updatedAt)
      put("lastScan", value.lastScan)
      put("scanVersion", value.scanVersion)
      put("isMissing", value.isMissing)
      put("isInvalid", value.isInvalid)
      put("mediaType", value.mediaType)

      val mediaJson = encoder.json.encodeToJsonElement(value.media)
      put("media", mediaJson)

      val libraryFilesJson = encoder.json.encodeToJsonElement(value.libraryFiles)
      put("libraryFiles", libraryFilesJson)
    }

    encoder.encodeJsonElement(jsonObject)
  }
}
