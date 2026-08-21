package dev.halim.shelfdroid.core.data.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreManagementUiStateTest {
  @Test
  fun sortedGenres_isCaseInsensitiveAndDeterministic() {
    assertEquals(listOf("alpha", "Beta", "zeta"), sortedGenres(listOf("zeta", "Beta", "alpha")))
  }

  @Test
  fun renameCollision_detectsExactTarget() {
    val collision = genreRenameCollision("old", "new", listOf("old", "new"))

    assertTrue(collision.exact)
    assertFalse(collision.caseOnly)
  }

  @Test
  fun renameCollision_detectsCaseOnlyTarget() {
    val collision = genreRenameCollision("old", "NEW", listOf("old", "new"))

    assertFalse(collision.exact)
    assertTrue(collision.caseOnly)
  }
}
