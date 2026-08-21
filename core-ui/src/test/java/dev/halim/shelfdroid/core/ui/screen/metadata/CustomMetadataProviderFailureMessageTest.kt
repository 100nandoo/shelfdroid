package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomMetadataProviderFailureMessageTest {

  @Test
  fun createFailureIncludesSafeServerValidationDetailAndContext() {
    assertEquals(
      "Custom metadata provider creation failed: Invalid URL",
      customMetadataProviderFailureMessage(
        CustomMetadataProviderOperation.Create,
        "Invalid URL",
      ),
    )
  }

  @Test
  fun unsafeOrMissingDetailFallsBackToContextOnlyMessage() {
    assertEquals(
      "Custom metadata provider creation failed.",
      customMetadataProviderFailureMessage(
        CustomMetadataProviderOperation.Create,
        "authHeaderValue: Bearer super-secret",
      ),
    )
    assertEquals(
      "Custom metadata provider deletion failed.",
      customMetadataProviderFailureMessage(CustomMetadataProviderOperation.Delete, null),
    )
  }
}
