package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserUiState
import dev.halim.shelfdroid.core.navigation.NavEditUser
import org.junit.Assert.assertEquals
import org.junit.Test

class EditUserViewModelTest {

  @Test
  fun `returns username error when create mode username is missing but password exists`() {
    val uiState = EditUserUiState(editUser = NavEditUser(username = "", password = "secret"))

    assertEquals(EditUserState.UsernameFieldError, createUserInfoValidation(uiState))
  }

  @Test
  fun `returns password error when create mode password is missing but username exists`() {
    val uiState = EditUserUiState(editUser = NavEditUser(username = "fernando", password = ""))

    assertEquals(EditUserState.PasswordFieldError, createUserInfoValidation(uiState))
  }

  @Test
  fun `returns combined error when create mode username and password are missing`() {
    val uiState = EditUserUiState(editUser = NavEditUser(username = "", password = ""))

    assertEquals(EditUserState.UsernameAndPasswordFieldError, createUserInfoValidation(uiState))
  }
}
