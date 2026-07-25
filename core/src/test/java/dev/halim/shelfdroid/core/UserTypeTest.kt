package dev.halim.shelfdroid.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTypeTest {

  @Test
  fun isAdminOrUp_includesAdminAndRootOnly() {
    assertTrue(UserType.Admin.isAdminOrUp())
    assertTrue(UserType.Root.isAdminOrUp())
    assertFalse(UserType.User.isAdminOrUp())
    assertFalse(UserType.Guest.isAdminOrUp())
    assertFalse(UserType.Unknown.isAdminOrUp())
  }
}
