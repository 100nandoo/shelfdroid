package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataOperation
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationError
import dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata.CustomMetadataFailureMessages
import dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata.customMetadataFailureMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomMetadataFailureMessageTest {

  private val messages =
    CustomMetadataFailureMessages(
      createFailed = "Create failed.",
      deleteFailed = "Delete failed.",
      providerNameRequired = "Name required.",
      providerUrlRequired = "URL required.",
    )

  @Test
  fun createFailureIncludesSafeServerValidationDetailAndContext() {
    assertEquals(
      "Custom metadata provider creation failed: Invalid URL",
      customMetadataFailureMessage(
        CustomMetadataOperation.Create,
        null,
        "Invalid URL",
        messages.copy(createFailed = "Custom metadata provider creation failed."),
      ),
    )
  }

  @Test
  fun unsafeOrMissingDetailFallsBackToContextOnlyMessage() {
    assertEquals(
      "Custom metadata provider creation failed.",
      customMetadataFailureMessage(
        CustomMetadataOperation.Create,
        null,
        "authHeaderValue: Bearer super-secret",
        messages.copy(createFailed = "Custom metadata provider creation failed."),
      ),
    )
    assertEquals(
      "Custom metadata provider deletion failed.",
      customMetadataFailureMessage(
        CustomMetadataOperation.Delete,
        null,
        null,
        messages.copy(deleteFailed = "Custom metadata provider deletion failed."),
      ),
    )
  }

  @Test
  fun validationFailureUsesLocalizedMessageProvidedByUi() {
    assertEquals(
      "Name required.",
      customMetadataFailureMessage(
        CustomMetadataOperation.Create,
        MetadataValidationError.CustomMetadataProviderNameRequired,
        null,
        messages,
      ),
    )
  }
}
