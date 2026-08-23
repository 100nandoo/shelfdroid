package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.metadata.CustomMetadataOperation
import dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata.customMetadataFailureMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomMetadataFailureMessageTest {

  @Test
  fun createFailureIncludesSafeServerValidationDetailAndContext() {
    assertEquals(
      "Custom metadata provider creation failed: Invalid URL",
      customMetadataFailureMessage(
        CustomMetadataOperation.Create,
        "Invalid URL",
      ),
    )
  }

  @Test
  fun unsafeOrMissingDetailFallsBackToContextOnlyMessage() {
    assertEquals(
      "Custom metadata provider creation failed.",
      customMetadataFailureMessage(
        CustomMetadataOperation.Create,
        "authHeaderValue: Bearer super-secret",
      ),
    )
    assertEquals(
      "Custom metadata provider deletion failed.",
      customMetadataFailureMessage(CustomMetadataOperation.Delete, null),
    )
  }
}
