package dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata

import dev.halim.shelfdroid.core.data.metadata.CustomMetadataOperation

/**
 * Maps an operation failure to a user-actionable message without displaying untrusted or secret
 * server details verbatim.
 */
fun customMetadataFailureMessage(
  operation: CustomMetadataOperation,
  detail: String?,
): String {
  val prefix =
    when (operation) {
      CustomMetadataOperation.Create -> "Custom metadata provider creation failed"
      CustomMetadataOperation.Delete -> "Custom metadata provider deletion failed"
    }
  val safeDetail = detail?.trim()?.takeIf(::isSafeProviderFailureDetail)
  return if (safeDetail == null) "$prefix." else "$prefix: $safeDetail"
}

private fun isSafeProviderFailureDetail(detail: String): Boolean {
  if (detail.isEmpty() || detail.length > 160 || detail.any(Char::isISOControl)) return false
  val normalized = detail.lowercase()
  return "authorization" !in normalized &&
    "authheadervalue" !in normalized &&
    "bearer " !in normalized
}
