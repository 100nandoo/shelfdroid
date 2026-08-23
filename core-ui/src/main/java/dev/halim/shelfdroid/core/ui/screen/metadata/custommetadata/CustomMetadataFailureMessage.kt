package dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata

import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataOperation
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationError

data class CustomMetadataFailureMessages(
  val createFailed: String,
  val deleteFailed: String,
  val providerNameRequired: String,
  val providerUrlRequired: String,
)

fun customMetadataFailureMessage(
  operation: CustomMetadataOperation,
  validationError: MetadataValidationError?,
  detail: String?,
  messages: CustomMetadataFailureMessages,
): String {
  val prefix =
    when (validationError) {
      MetadataValidationError.CustomMetadataProviderNameRequired -> messages.providerNameRequired
      MetadataValidationError.CustomMetadataProviderUrlRequired -> messages.providerUrlRequired
      else ->
        when (operation) {
          CustomMetadataOperation.Create -> messages.createFailed
          CustomMetadataOperation.Delete -> messages.deleteFailed
        }
    }
  val safeDetail = detail?.trim()?.takeIf(::isSafeProviderFailureDetail)
  return if (validationError != null || safeDetail == null) {
    prefix
  } else {
    "${prefix.removeSuffix(".")}: $safeDetail"
  }
}

private fun isSafeProviderFailureDetail(detail: String): Boolean {
  if (detail.isEmpty() || detail.length > 160 || detail.any(Char::isISOControl)) return false
  val normalized = detail.lowercase()
  return "authorization" !in normalized &&
    "authheadervalue" !in normalized &&
    "bearer " !in normalized
}
