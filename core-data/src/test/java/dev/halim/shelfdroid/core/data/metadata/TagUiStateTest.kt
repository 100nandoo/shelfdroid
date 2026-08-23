package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.metadata.tag.sortedTags
import dev.halim.shelfdroid.core.data.metadata.tag.tagRenameCollision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagUiStateTest {
  @Test
  fun sortedTags_isCaseInsensitiveAndDeterministic() {
    assertEquals(listOf("alpha", "Beta", "zeta"), sortedTags(listOf("zeta", "Beta", "alpha")))
  }

  @Test
  fun tagRenameCollision_detectsExactTarget() {
    val collision = tagRenameCollision("old", "Existing", listOf("old", "Existing", "other"))

    assertTrue(collision.exact)
    assertFalse(collision.caseOnly)
  }

  @Test
  fun tagRenameCollision_detectsCaseOnlyTarget() {
    val collision = tagRenameCollision("old", "EXISTING", listOf("old", "Existing"))

    assertFalse(collision.exact)
    assertTrue(collision.caseOnly)
  }
}
